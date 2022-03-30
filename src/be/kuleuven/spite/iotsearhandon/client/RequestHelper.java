package be.kuleuven.spite.iotsearhandon.client;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.systemProviders.context.encoders.JsonStringEncoderDecoder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RequestHelper {
    private final String uri;
    private HttpClient client;
    private JsonStringEncoderDecoder encoderDecoder = new JsonStringEncoderDecoder();

    public RequestHelper(String url, int port){
        //test if url/port are well formed
        uri = URI.create("http://"+url+":"+port).toString();
        client = HttpClient.newHttpClient();
    }

    public ContextAttribute doLoginRequest(String username, String password) throws Exception {
        URI loginURI = new URIBuilder(uri+"/login").addQueryParameter("username",username).addQueryParameter("password",password).getUri();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(loginURI)
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.body());
        if(Boolean.parseBoolean(json.get("login-successful").toString()))
            return encoderDecoder.fromJSON((JSONObject) json.get("token"));
        else return null;
    }

    public boolean doDoorRequest(String resource, String action, ContextAttribute attribute) throws Exception{
        URI loginURI = new URIBuilder(uri+"/door").addPathParameter(resource).addPathParameter(action).getUri();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(loginURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encoderDecoder.encode(attribute)))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.body());
        return Boolean.parseBoolean(json.get("request-granted").toString());
    }

    public boolean doFileAccessRequest(String resource, String action, ContextAttribute attribute) throws Exception{
        URI loginURI = new URIBuilder(uri+"/fileshare").addPathParameter(resource).addPathParameter(action).getUri();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(loginURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encoderDecoder.encode(attribute)))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.body());
        return Boolean.parseBoolean(json.get("request-granted").toString());
    }


    private class URIBuilder{
        private String uri;

        public URIBuilder(String uri) {
            this.uri = uri;
        }

        public URIBuilder addQueryParameter(String name, String value){
            StringBuilder uriString = new StringBuilder(uri);
            if(!uriString.toString().contains("?")){
                uriString.append("?");
            }else{
                uriString.append("&");
            }
            uriString.append(escapeString(name)).append("=").append(escapeString(value));
            uri = uriString.toString();
            return this;
        }
        public URIBuilder addPathParameter(String parameter){
            StringBuilder uriString = new StringBuilder(uri);
            if(!uriString.toString().endsWith("/")){
                uriString.append("/");
            }
            uriString.append(escapeString(parameter));
            uri = uriString.toString();
            return this;
        }
        public URI getUri(){
            return URI.create(this.uri);
        }
        private String escapeString(String s){
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }
}
