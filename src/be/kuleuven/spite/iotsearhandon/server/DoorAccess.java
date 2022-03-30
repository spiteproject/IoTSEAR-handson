package be.kuleuven.spite.iotsearhandon.server;

import be.distrinet.spite.iotsear.core.exceptions.InvalidSignatureException;
import be.distrinet.spite.iotsear.systemProviders.crypto.SHA256WithHMAC;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DoorAccess {
    private static SHA256WithHMAC signer = new SHA256WithHMAC("8IBaV6IsiDpC3r2pATjSlsy+O2MCI9zbJV+NDWh7j4Q=");
    private static Map<String,Long> tokenMap = new HashMap<>();
    private static SecureRandom rng = new SecureRandom();

    public static boolean verifyAccess(String ts, String mac){
        Long timestamp = Long.parseLong(ts);
        //deny if timestamp is too old
        if(System.currentTimeMillis() - 60000 > timestamp) return false;
        try {
            return signer.verify(ts,mac);
        } catch (InvalidSignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String generateToken(){
        byte[] r = new byte[32];
        rng.nextBytes(r);
        String token = Base64.getEncoder().encodeToString(r);
        tokenMap.put(token, System.currentTimeMillis());
        return token;
    }

    public static boolean checkToken(String token){
        return tokenMap.remove(token) != null;
    }
}
