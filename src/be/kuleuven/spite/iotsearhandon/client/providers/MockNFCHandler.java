package be.kuleuven.spite.iotsearhandon.client.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextHandler;
import be.distrinet.spite.iotsear.crypto.SignatureSPI;
import be.distrinet.spite.iotsear.systemProviders.crypto.SHA256WithHMAC;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pf4j.Extension;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Extension
public class MockNFCHandler extends ContextHandler {
    private SignatureSPI signer;
    HttpClient client;

    public MockNFCHandler(){
    }
    public MockNFCHandler(String secretKey){
        client = HttpClient.newHttpClient();
        signer = new SHA256WithHMAC(secretKey);
    }

    @Override
    public ContextHandler createContextHandler(JSONObject config) {
        return new MockNFCHandler(config.get("secret-key").toString());
    }

    @Override
    public void enable() {
    }

    @Override
    public void retrieveWithListener(ContextProcessedListener listener) {
        String ts = System.currentTimeMillis() + "";
        try {
            String mac = this.signer.sign(ts);
            String challenge = doChallengeRequest(ts,mac);

            ContextAttribute attribute = new ContextAttribute("NFC",challenge);
            super.handleContext(attribute);
            listener.onContextProcessed(attribute);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getProviderID() {
        return "handson:context:handler:nfc";
    }


    private String doChallengeRequest(String ts, String mac) throws ParseException, IOException, InterruptedException {
        String url = "http://localhost:4567/nfcChallenge?ts="+ts+"&mac="+ URLEncoder.encode(mac, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.body());
        if(Boolean.parseBoolean(json.get("request-granted").toString()))
            return json.get("challenge").toString();
        else return "";
    }
}
