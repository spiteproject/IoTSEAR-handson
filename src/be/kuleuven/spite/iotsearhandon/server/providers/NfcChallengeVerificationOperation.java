package be.kuleuven.spite.iotsearhandon.server.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.policy.PolicyConditionOperation;
import be.distrinet.spite.iotsear.policy.abstractFactories.PolicyConditionOperationFactory;
import be.kuleuven.spite.iotsearhandon.server.DoorAccess;
import org.pf4j.Extension;

import java.util.Map;

@Extension
public class NfcChallengeVerificationOperation extends PolicyConditionOperation implements PolicyConditionOperationFactory {
    @Override
    public String getProviderID() {
        return "handson:operation:nfc-verification";
    }

    @Override
    public boolean match(String source, String value, ContextAttribute attribute, AuthorizationPolicy policy) {
        if(source.equals("NFC"))
            return DoorAccess.checkToken(attribute.getValue());
        else return false;
    }

    @Override
    public PolicyConditionOperation createPolicyConditionOperation(Map<String, String> arguments) {
        return new NfcChallengeVerificationOperation();
    }
}
