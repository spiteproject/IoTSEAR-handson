package be.kuleuven.spite.iotsearhandon.server;

import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextSource;
import be.distrinet.spite.iotsear.core.model.context.proof.AuthenticityProof;
import be.distrinet.spite.iotsear.core.model.context.proof.OwnershipProof;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.systemProviders.context.encoders.JsonStringEncoderDecoder;
import org.json.simple.JSONObject;
import spark.Request;
import spark.Response;


import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class ServerAPI {

    private IoTSEAR iotsear;
    private IoTSEARServer iotsearServer;
    private JsonStringEncoderDecoder encoderDecoder = new JsonStringEncoderDecoder();

    public ServerAPI(){
        this.configuerIoTSEAR();
        this.iotsearServer = new IoTSEARServer();

        //for each context source, print the context attribute after IoTSEAR has processed it.
        for(ContextSource source: iotsear.getContextSources()){
            source.addOnContextProcessedListener(this::printReceivedContext);
        }
    }



    public void configuerIoTSEAR(){
        iotsear = IoTSEAR.getInstance();
        iotsear.configure(new File("./IoTSEARserver.json"));
    }

    public static void main(String... args) {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for(Handler h: handlers){
            h.setLevel(Level.WARNING);
        }
        ServerAPI server = new ServerAPI();

        get("/login", server::login);
        get("/nfcChallenge", server::nfcChallenge);
        post("/door/:resource/:action", server::authorizeRequest);
        post("/fileshare/:resource/:action", server::authorizeRequest);
    }

    /**
     * Authenticate login credentials
     *
     * the login credentials are decoded from the request
     * after which they are authenticated using iotsearServer.authenticate
     * if the authentication succeeds, this results in the creation of a ContextAttribute
     * if it fails, this attribute is 'null'
     *
     * finally, a response is returned containing the login-token ContextAttribute (if authentication succeeded)
     */
    public Object login(Request request, Response response){
        String username = request.queryParamOrDefault("username","");
        String password = request.queryParamOrDefault("password","");
        ContextAttribute token = iotsearServer.authenticate(username,password);
        JSONObject json = new JSONObject();
        if(token!=null){
            json.put("login-successful","true");
            json.put("token", encoderDecoder.toJSON(token));
        }else{
            json.put("login-successful","false");
        }
        System.out.println(json.toJSONString());
        return json;
    }

    /**
     * This API is used by the (Mock)NFC context source
     * First, the timestamp and its HMAC are decoded from the URL
     * next, they are verified, and a return message is created based on
     * the success of the verification
     */
    public Object nfcChallenge(Request request, Response response){
        String ts = request.queryParamOrDefault("ts","");
        String mac = request.queryParamOrDefault("mac","");
        JSONObject json = new JSONObject();
        if(DoorAccess.verifyAccess(ts,mac)){
            json.put("request-granted","true");
            json.put("challenge",DoorAccess.generateToken());
        }else{
            json.put("request-granted","false");
        }
        System.out.println(json.toJSONString());
        return json;
    }

    /**
     * Handle authorization requests
     *
     * first, the policy target is decoded from the request
     * - resource and action are encoded in the URL parameters
     * - subject is encoded in the provided context attribute
     *
     * next, the actual policy enforcement is deferred to the iotsearServer object
     *
     * finally, a response is returned based on the policyenforcement result (ALLOW/DENY)
     */
    public Object authorizeRequest(Request request, Response response){
        response.type("application/json");
        String resource = request.params(":resource");
        String action = request.params("action");
        ContextAttribute attribute = encoderDecoder.decode(request.bodyAsBytes());
        System.out.println("received access request: "+attribute.getSubject().getIdentifier()+", "+resource+", "+action);

        AuthorizationPolicy.PolicyEffect effect = iotsearServer.authorize(resource, action, attribute);

        JSONObject json = new JSONObject();
        switch (effect){
            case ALLOW:
                json.put("request-granted","true");
                break;
            case DENY:
                json.put("request-granted","false");
                break;
        }
        System.out.println(json.toJSONString());
        return json;
    }



    private synchronized void printReceivedContext(ContextAttribute attribute) {
        System.out.println("========================Attribute processed========================");
        System.out.println(attribute.getType()+":"+ attribute.getValue());
        if(attribute.getSubject()!=null)
            System.out.println("with subject: "+ attribute.getSubject().getIdentifier());
        AuthenticityProof authenticityProof = attribute.getAuthenticityProofFromMetadata();
        if(authenticityProof !=null)
            System.out.println("with authenticy proof "+ authenticityProof.getProviderID() +"(valid: "+authenticityProof.verify()+")");
        OwnershipProof ownershipProof = attribute.getOwnershipProofFromMetadata();
        if(ownershipProof !=null)
            System.out.println("with ownership proof"+ ownershipProof.getProviderID() +"(valid: "+ownershipProof.verify()+")");
        System.out.println("===================================================================");
    }
}
