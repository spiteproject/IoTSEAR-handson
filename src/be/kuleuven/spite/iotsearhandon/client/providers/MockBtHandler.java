package be.kuleuven.spite.iotsearhandon.client.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextHandler;
import org.json.simple.JSONObject;
import org.pf4j.Extension;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Extension
public class MockBtHandler extends ContextHandler {

    @Override
    public ContextHandler createContextHandler(JSONObject config) {
        return new MockBtHandler();
    }

    @Override
    public void enable() {
    }

    @Override
    public void retrieveWithListener(ContextProcessedListener listener) {
        ContextAttribute attribute = new ContextAttribute("BT",getMACAddress());
        super.handleContext(attribute);
        listener.onContextProcessed(attribute);
    }

    @Override
    public String getProviderID() {
        return "handson:context:handler:bt";
    }

    private String getMACAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null && isSuitableInterface(ni)) {
                    String[] hexadecimalFormat = new String[hardwareAddress.length];
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
                    }
                    return String.join(":", hexadecimalFormat);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * a suitable interface has an IPv4 address
     * @param ni
     * @return
     */
    private boolean isSuitableInterface(NetworkInterface ni){
        Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
        while(inetAddresses.hasMoreElements()) {
            String address = inetAddresses.nextElement().getHostAddress();
            String[] split = address.split("\\.");
            if(split.length == 4) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            System.out.println(new MockBtHandler().getMACAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
