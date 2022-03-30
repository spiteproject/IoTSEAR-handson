package be.kuleuven.spite.iotsearhandon.client.providers;

import be.distrinet.spite.iotsear.IoTSEAR;
import be.distrinet.spite.iotsear.core.exceptions.InvalidSignatureException;
import be.distrinet.spite.iotsear.core.model.Subject;
import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;
import be.distrinet.spite.iotsear.core.model.context.proof.OwnershipProof;
import be.distrinet.spite.iotsear.systemProviders.crypto.SHA256WithHMAC;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class TokenKnowledgeProof extends OwnershipProof {
    SHA256WithHMAC signer = new SHA256WithHMAC();

    public TokenKnowledgeProof(){
        super();
    }

    public TokenKnowledgeProof(ContextAttribute attribute, String token) throws InvalidSignatureException {
        setAttribute(attribute);
        signer = new SHA256WithHMAC(token);
        proofString = signer.sign(getData());
    }

    @Override
    public OwnershipProof getOwnershipProof(String proofString, ContextAttribute attribute) {
        TokenKnowledgeProof proof = new TokenKnowledgeProof();
        String token = getToken(attribute.getSubject());
        if(token == null) return null;
        proof.signer = new SHA256WithHMAC(token);
        proof.proofString = proofString;
        proof.setAttribute(attribute);
        return proof;
    }

    @Override
    public OwnershipProof createOwnershipProof(ContextAttribute attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProviderID() {
        return "handson:context:proof:token-knowledge";
    }

    @Override
    protected boolean _verify() {
        try {
            return getProofString() != null && signer.verify(getData(), getProofString());
        } catch (InvalidSignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getData() {
        return getAttribute().getValue() + getAttribute().getTimestamp() + getSubject().getIdentifier();
    }

    private String getToken(final Subject subject){
        //find all attributes from source "iotsear-server" that have the given subject
        List<ContextAttribute> candidates = IoTSEAR.getInstance().getContextStore().find((ContextAttribute attribute)->{
            if(attribute.getSubject()!=null && attribute.getSource()!=null)
                return attribute.getSubject().getIdentifier().equals(subject.getIdentifier()) &&
                        attribute.getSource().getIdentifier().equals("iotsear-server");
            else return false;
        });
        //sort list with newest entries on top and return the value of the first element (if any)
        candidates.sort((ContextAttribute c1,ContextAttribute c2) -> Long.compare(c1.getTimestamp(),c2.getTimestamp())*-1);
        return (candidates.isEmpty())?null:candidates.get(0).getValue();
    }



}
