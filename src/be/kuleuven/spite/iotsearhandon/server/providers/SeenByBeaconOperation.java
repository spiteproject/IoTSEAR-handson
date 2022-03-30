package be.kuleuven.spite.iotsearhandon.server.providers;

import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.ContextSource;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.policy.PolicyConditionOperation;
import be.distrinet.spite.iotsear.policy.abstractFactories.PolicyConditionOperationFactory;
import be.distrinet.spite.iotsear.systemProviders.darc.verifiers.FreshnessVerifier1m;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Extension
public class SeenByBeaconOperation extends PolicyConditionOperation implements PolicyConditionOperationFactory {


    @Override
    public String getProviderID() {
        return "handson:operation:seen-by-beacon";
    }

    @Override
    public boolean match(String source, String value, ContextAttribute attribute, AuthorizationPolicy policy) {
        //get latest beacon
        ContextAttribute beacon = getBeacon(value);
        if(beacon == null)return false;
        if(!new FreshnessVerifier1m().verify(beacon,null)) return false;
        return beacon.getValue().contains(attribute.getValue());
    }

    @Override
    public PolicyConditionOperation createPolicyConditionOperation(Map<String, String> arguments) {
        return new SeenByBeaconOperation();
    }

    private ContextAttribute getBeacon(String beaconID){
        ContextSource source = IoTSEAR.getInstance().getContextManager().getSource(beaconID);
        if(source == null) return null;
        if(!source.getSourceType().equals("BTSensor")) return null;
        //find most recent beacon
        List<ContextAttribute> bySource = IoTSEAR.getInstance().getContextStore().findBySource(beaconID);
        List<ContextAttribute> attributes = bySource
                                                .stream()
                                                .sorted((att1,att2)->Long.compare(att2.getTimestamp(),att1.getTimestamp()))
                                                .collect(Collectors.toList());
        return attributes.size()>0?attributes.get(0):null;
    }
}
