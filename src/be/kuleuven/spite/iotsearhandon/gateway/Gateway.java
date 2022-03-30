package be.kuleuven.spite.iotsearhandon.gateway;


import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextEncoder;
import be.distrinet.spite.iotsear.core.model.context.ContextSource;
import be.distrinet.spite.iotsear.managers.ContextManager;
import be.distrinet.spite.iotsear.systemProviders.context.encoders.JsonStringEncoderDecoder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


import java.io.File;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gateway {
    private  String broker = "tcp://localhost:1883";
    ContextEncoder encoder = new JsonStringEncoderDecoder();
    private IoTSEAR iotsear;

    public Gateway(){
        initializeIoTSEAR();
        setupContextForwarding();
    }

    /**
     * Initialize iotsear
     * - set the iotsear field
     * - perform the 'configure' method on the iotsear object
     *      with argument (new File("PATH_TO_CONFIG"))
     */
    public void initializeIoTSEAR(){
        iotsear = IoTSEAR.getInstance();
        iotsear.configure(new File("./IoTSEARgateway.json"));
    }

    /**
     * The main responsibility of the gateway is to receive context attributes from the devices on the local network,
     * and forward them to the server through MQTT
     *
     * In order to do so, we add an 'onContextProcessedListener' to each of the available context sources
     * This will allow us to run some code after IoTSEAR has processed a newly created context attribute
     *
     * The code that the gateway will run will simply take the newly created context attribute and send
     * it to the server through MQTT
     */
    private void setupContextForwarding() {
        for(ContextSource source: ContextManager.getInstance().getSources()){
            source.addOnContextProcessedListener(attribute -> {
                try {

                    publishMQTTMessage(attribute);
                    System.out.println(attribute.getAuthenticityProofFromMetadata().verify());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void main(String... args) {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for(Handler h: handlers){
            h.setLevel(Level.WARNING);
        }

        Gateway gateway = new Gateway();
    }

    public void publishMQTTMessage(ContextAttribute attribute) throws MqttException {
        String clientId = ""+new Random().nextInt(20000);
        String topic = attribute.getSource().getMetaData("topic");
        MqttClient mqttClient = new MqttClient(this.broker, clientId, new MemoryPersistence());
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        mqttClient.connect(connOpts);
        MqttMessage message = new MqttMessage(encoder.encode(attribute));
        mqttClient.publish(topic,message);
        System.out.println("Published message on topic: "+topic);
        System.out.println(message);
        mqttClient.disconnect();
    }

}
