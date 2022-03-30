package be.kuleuven.spite.iotsearhandon.client.providers;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.policy.AuthorizationPolicy;
import be.distrinet.spite.iotsear.policy.PolicyConditionVerifier;
import be.distrinet.spite.iotsear.policy.abstractFactories.PolicyConditionVerifierFactory;
import org.pf4j.Extension;

@Extension
public class SubjectOwnershipVerifier extends PolicyConditionVerifier implements PolicyConditionVerifierFactory {
    @Override
    public String getProviderID() {
        return "hands-on:verifier:subject-ownership";
    }

    @Override
    public boolean verify(ContextAttribute attribute, AuthorizationPolicy policy) {
        if(attribute.getSubject() == null) return false;
        if(!policy.getPepTarget().getSubject().equals(attribute.getSubject().getIdentifier())) return false;
        if(attribute.getOwnershipProofFromMetadata() == null)return false;
        return attribute.getOwnershipProofFromMetadata().verify();
    }

    @Override
    public PolicyConditionVerifier createPolicyConditionVerifier() {
        return new SubjectOwnershipVerifier();
    }
}
