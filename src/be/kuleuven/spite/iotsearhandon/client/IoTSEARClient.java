package be.kuleuven.spite.iotsearhandon.client;

import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextSource;
import be.distrinet.spite.iotsear.core.model.context.proof.OwnershipProof;
import be.distrinet.spite.iotsear.systemProviders.context.encoders.JsonStringEncoderDecoder;
import be.kuleuven.spite.iotsearhandon.client.providers.TokenKnowledgeProof;

import javax.swing.*;
import java.io.File;

public class IoTSEARClient {

    //Object containing the values of the username and password GUI fields
    private Fields usernamePassword;
    private Client window;
    private ContextAttribute nfcContext;
    private ContextAttribute loginToken;

    private RequestHelper requestHelper = new RequestHelper("localhost",4567);

    //IoTSEAR main object
    private IoTSEAR iotsear;

    public IoTSEARClient(Client window) {
        this.usernamePassword = window.fields;
        this.window = window;
        initializeIoTSEAR();
    }

    /**
     * Initialize iotsear
     * - set the iotsear field
     * - perform the 'configure' method on the iotsear object
     *      with argument (new File("PATH_TO_CONFIG"))
     */
    public void initializeIoTSEAR(){
        iotsear = IoTSEAR.getInstance();
        iotsear.configure(new File("./IoTSEARclient.json"));
    }


    /**
     * Perform the login using the requestHelper
     * This will call the login REST API and, if successful return a login context-attribute
     * if not successful, it returns null
     *
     * Finally, this method updates the GUI based on the success of the login
     */
    public void login() throws Exception {
        loginToken = requestHelper.doLoginRequest(usernamePassword.getUsername(),usernamePassword.getPassword());
        if(loginToken!=null){//Authentication succeeded
            this.window.setLoggedIn(true);
            showDialog("Login Successful!");
        }else{
            this.window.setLoggedIn(false);
            showDialog("Login Failed");
        }
    }

    /**
     * First obtain the ContextSource of the NFC reader
     * - use iotsear.getContextManager().getSource with the identifier of the NFC reader as argument (see IoTSEARclient.json)
     * Then obtain the NFC context Attribute using the ContextSource.retrieveContext method
     *
     * Once the ContextAttribute is received, set the field 'nfcContext' to this NFC ContextAttribute
     * Next, set the subject of 'nfcContext' to the subject of 'loginToken'
     * - this.nfcContext.setSubject(...)
     * - this.loginToken.getSubject()
     *
     * Finally, the GUI is updated based on the success of the operation
     */
    public void retrieveNfcContext(){
        ContextSource nfc = iotsear.getContextManager().getSource("phone-nfc");
        nfc.retrieveContext((ContextAttribute attribute)->{
            this.nfcContext = attribute;
            nfcContext.setSubject(this.loginToken.getSubject());

            printContextAttribute(nfcContext);
            window.nfcRetrieved(nfcContext);
        });
    }

    /**
     * First, create a new TokenKnowledgeProof of the nfcContext using the loginToken's value
     * - OwnershipProof proof = new TokenKnowledgeProof(...)
     * - The first argument should be the nfcContext object
     * - The second argument is the value of the loginToken
     * in doing so, a signature of the nfcContext attribute using the login token is created
     *
     * next, attach the proof to the nfcContext using the 'setOwnershipProof' method
     *
     * Finally, the access request is made to the REST API using the requestHelper.doDoorRequest method
     * Set the method arguments:
     * - The first argument is the resource, and can be found in './door-policy.json'
     * - The second argument is the action, and can also be found in './door-policy.json'
     * - The third argument is the nfcContext attribute
     */
    public void getDoorAccess(){
        boolean open = false;
        try {
            OwnershipProof proof = new TokenKnowledgeProof(nfcContext, this.loginToken.getValue());
            nfcContext.setOwnershipProof(proof);

            printContextAttribute(nfcContext);

            open = requestHelper.doDoorRequest("02.124:lock", "open", this.nfcContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        window.doorOpened(open);
    }

    /**
     * First obtain the ContextSource of the BT device
     * - use iotsear.getContextManager().getSource with the identifier of the BT device as argument (see IoTSEARclient.json)
     * Then obtain the ContextAttribute using the ContextSource.retrieveContext method
     *
     * Next, create a new TokenKnowledgeProof of the attribute using the loginToken's value
     * - OwnershipProof proof = new TokenKnowledgeProof(...)
     * - The first argument should be the 'attribute' object
     * - The second argument is the value of the loginToken
     * in doing so, a signature of the attribute using the login token is created
     *
     * next, attach the proof to the attribute using the 'setOwnershipProof' method
     *
     * Finally, the access request is made to the REST API using the requestHelper.doRequest method
     * Set the method arguments:
     * - The first argument is the resource
     * - The second argument is the action
     * - The third argument is the ContextAttribute 'attribute'
     */
    public void getFileAccess(String resource, String action){
        ContextSource source = iotsear.getContextManager().getSource("phone-bt");
        source.retrieveContext(attribute -> {
            try {
                attribute.setSubject(loginToken.getSubject());
                OwnershipProof proof = new TokenKnowledgeProof(attribute, this.loginToken.getValue());
                attribute.setOwnershipProof(proof);

                printContextAttribute(attribute);

                boolean success = requestHelper.doFileAccessRequest(resource, action, attribute);
                window.fileRequested(success, action);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void printContextAttribute(ContextAttribute attribute) {
        System.out.println(new String(new JsonStringEncoderDecoder().encode(attribute)));
    }

    private void showDialog(String message){
        JOptionPane.showMessageDialog(null, message);
    }

}
