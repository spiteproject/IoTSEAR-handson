package be.kuleuven.spite.iotsearhandon.gateway.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.pf4j.Extension;

import java.util.Random;


@Extension
public class MockHandler extends ContextHandler {
    public enum ValueType {
        STRING, ARRAY, SAMPLE
    }
    private ValueType outputType;
    private Object value;
    private double sigma;
    private int index = 0;
    private boolean repeat;
    private long time;
    private String contextType;

    @Override
    public ContextHandler createContextHandler(JSONObject config) {
        MockHandler handler = new MockHandler();
        handler.sigma = 0;
        if(config.containsKey("sigma")){
            handler.sigma = Double.parseDouble(config.get("sigma").toString());
        }
        handler.outputType = ValueType.STRING;
        String tmp = config.get("outputType").toString();
        if(tmp.equals("Array")){
            handler.outputType = ValueType.ARRAY;
        }else if(tmp.equals("Sample")){
            handler.outputType = ValueType.SAMPLE;
        }
        handler.value = config.get("value");
        handler.contextType = config.get("contextType").toString();
        handler.time = Long.parseLong(config.get("time").toString());
        handler.repeat = Boolean.parseBoolean(config.get("repeat").toString());
        return handler;
    }

    @Override
    public void enable() {
        Thread run = new Thread(()->{
            do {
                long currentTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - currentTime < this.time) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                super.handleContext(getValue());
            }while (repeat);
        });
        run.start();
    }

    @Override
    public void retrieveWithListener(ContextProcessedListener listener) {
        ContextAttribute attribute = getValue();
        super.handleContext(attribute);
        listener.onContextProcessed(attribute);
    }

    @Override
    public String getProviderID() {
        return "handson:context:handler:mock";
    }

    private ContextAttribute getValue(){
        String val = "";
        switch (this.outputType){
            case STRING:
                if(this.value instanceof JSONArray) val = ((JSONArray)this.value).toJSONString();
                val = this.value.toString();
                break;
            case ARRAY:
                JSONArray array = (JSONArray) this.value;
                val = array.get(this.index).toString();
                this.index = (this.index + 1) % array.size();
                break;
            case SAMPLE:
                val = ((Double)this.value) + this.sigma * (new Random().nextGaussian() - 0.5)+"";
                break;
        }
        ContextAttribute attribute = new ContextAttribute(this.contextType, val);
        return attribute;

    }
}
