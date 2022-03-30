package be.kuleuven.spite.iotsearhandon.server;

import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.model.Subject;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextSource;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.policy.PolicyTarget;
import be.distrinet.spite.iotsear.systemProviders.context.source.DummySource;
import org.json.simple.JSONObject;
import org.pf4j.Extension;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;

@Extension
public class IoTSEARServer extends DummySource {
    private static final String LOGINFILE = "./login-credentials.txt";
    SecureRandom random;
    private IoTSEAR iotsear = IoTSEAR.getInstance();

    public IoTSEARServer(){
        random = new SecureRandom();
        insertCalendarContext();
    }

    /**
     * Authenticate login credentials
     *
     * if the credentials are authentic (tested by the 'verifyLogin' method, a login-token is created
     * the login-token is a ContextAttribute which has a randomly generated string as value, and the
     * login-username as subject. Furthermore, the source of this token is set to 'this' and a timestamp is added
     * Finally, the login-token is inserted in IoTSEAR's context store before it is returned
     *
     * if the login-credentials are not authentic, the return value is 'null'
     */
    public ContextAttribute authenticate(String username, String password){
        if(verifyLogin(username,password)){
            String token = generateToken();
            ContextAttribute authenticationContext = new ContextAttribute("sessiontoken",token);
            authenticationContext.setSubject(new Subject(username));
            authenticationContext.setSource(this);
            authenticationContext.setTimestamp(System.currentTimeMillis());
            iotsear.getContextStore().store(authenticationContext);
            return authenticationContext;
        }else{
            return null;
        }
    }

    /**
     * perform policy enforcement
     *
     * First, the context provided context attribute is inserted into IoTSEAR's context store
     * Next, the PolicyTarget is created, which consist out of the 'subject' 'resource' and 'action' triple.
     * While 'resource' and 'action' are provided as argument,
     * the 'subject' is obtained through the provided ContextAttribute
     * - attribute.getSubject().getIdentifier()
     *
     * next, the policy enforcement is triggered using IoTSEAR's PolicyEngine
     * - iotsear.getPolicyEngine()
     * - PolicyEngine.enforce(PolicyTarget target)
     * which returns a PolicyEffect (ALLOW/DENY)
     */
    public AuthorizationPolicy.PolicyEffect authorize(String resource, String action, ContextAttribute attribute) {
        iotsear.getContextStore().store(attribute);
        PolicyTarget target = new PolicyTarget(attribute.getSubject().getIdentifier(), resource, action);
        AuthorizationPolicy.PolicyEffect effect = iotsear.getPolicyEngine().enforce(target);
        return effect;
    }


    /**
     * check the LOGINFILE for the provided credentials
     */
    private boolean verifyLogin(String user, String pw){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(LOGINFILE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] split = line.split(":");
            if(split[0].equals(user) && split[1].equals(pw))
                return true;
        }
        return false;
    }

    /**
     * generate a 32byte random string
     */
    private String generateToken(){

        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getEncoder().encodeToString(tokenBytes);
    }

    /**
     * insert the schedule as a Context Attribute
     */
    private void insertCalendarContext(){
        ContextAttribute attribute = new ContextAttribute("calendar#02.124","./schedule.json");
        attribute.setSource(this);
        attribute.setTimestamp(System.currentTimeMillis());
        IoTSEAR.getInstance().getContextStore().store(attribute);
    }

    //////////CONTEXT SOURCE METHODS////////////
    @Override
    public String getIdentifier() {
        return "iotsear-server";
    }

    @Override
    public String getProviderID() {
        return "handson:server";
    }
    @Override
    public ContextSource createSource(JSONObject json) {
        return this;
    }


}
