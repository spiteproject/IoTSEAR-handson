package be.kuleuven.spite.iotsearhandon.server.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.policy.PolicyConditionOperation;
import be.distrinet.spite.iotsear.policy.abstractFactories.PolicyConditionOperationFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pf4j.Extension;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Extension
public class IsScheduledOperation extends PolicyConditionOperation implements PolicyConditionOperationFactory {

    @Override
    public String getProviderID() {
        return "handson:operation:is-scheduled";
    }

    @Override
    public boolean match(String source, String value, ContextAttribute attribute, AuthorizationPolicy policy) {
        JSONObject schedule = parseSchedule(attribute.getValue());
        if(schedule.isEmpty()) return false;
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String date = dateFormat.format(now);
        JSONObject daySchedule = (JSONObject) schedule.get(date);
        if(daySchedule == null) return false;
        for(Object o:daySchedule.keySet()){
            if(daySchedule.get(o).toString().equals(value)){
                String[] timeslot = o.toString().split("-");
                if(isInTimeslot(now,timeslot[0],timeslot[1])){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public PolicyConditionOperation createPolicyConditionOperation(Map<String, String> arguments) {
        return new IsScheduledOperation();
    }

    private boolean isInTimeslot(Date date, String left, String right){
        String[] l = left.split(":");
        String[] r = right.split(":");

        Calendar calLeft = Calendar.getInstance();
        calLeft.set(Calendar.HOUR_OF_DAY,Integer.parseInt(l[0]));
        calLeft.set(Calendar.MINUTE,Integer.parseInt(l[1]));

        Calendar calRight = Calendar.getInstance();
        calRight.set(Calendar.HOUR_OF_DAY,Integer.parseInt(r[0]));
        calRight.set(Calendar.MINUTE,Integer.parseInt(r[1]));

        return date.compareTo(calLeft.getTime()) != date.compareTo(calRight.getTime());
    }

    private JSONObject parseSchedule(String path){
        JSONObject json = new JSONObject();
        JSONParser parser = new JSONParser();
        try {
            Reader fileReader = new FileReader(path);
            json = (JSONObject) parser.parse(fileReader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return json;
    }


}
