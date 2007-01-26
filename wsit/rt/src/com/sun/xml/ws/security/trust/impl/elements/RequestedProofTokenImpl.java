/*
 * $Id: RequestedProofTokenImpl.java,v 1.11 2007-01-26 05:57:46 jdg6688 Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.security.trust.impl.elements;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import com.sun.xml.ws.security.trust.WSTrustElementFactory;

import com.sun.xml.ws.security.trust.elements.str.SecurityTokenReference;

import java.net.URI;
import java.net.URISyntaxException;

import com.sun.xml.ws.security.trust.WSTrustConstants;
import com.sun.xml.ws.security.trust.impl.bindings.ObjectFactory;

import com.sun.xml.ws.security.trust.elements.BinarySecret;
import com.sun.xml.ws.security.trust.elements.RequestedProofToken;
import com.sun.xml.ws.security.trust.impl.bindings.RequestedProofTokenType;
import com.sun.xml.ws.security.trust.impl.bindings.BinarySecretType;

import com.sun.xml.ws.security.secext10.SecurityTokenReferenceType;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.xml.ws.security.trust.logging.LogDomainConstants;

import com.sun.istack.NotNull;

import com.sun.xml.ws.security.trust.logging.LogStringsMessages;

/**
 * @author Manveen Kaur
 */
public class RequestedProofTokenImpl extends RequestedProofTokenType implements RequestedProofToken {
    
    private static final Logger log =
            Logger.getLogger(
            LogDomainConstants.TRUST_IMPL_DOMAIN,
            LogDomainConstants.TRUST_IMPL_DOMAIN_BUNDLE);
    
    private String tokenType;
    private URI computedKey;
    private BinarySecret secret;
    private SecurityTokenReference str;
    
    public RequestedProofTokenImpl() {
        // empty constructor
    }
    
    public RequestedProofTokenImpl(String proofTokenType) {
        setProofTokenType(proofTokenType);
    }
    
    public RequestedProofTokenImpl(RequestedProofTokenType rptType){
        final JAXBElement obj = (JAXBElement)rptType.getAny();
        final String local = obj.getName().getLocalPart();
        if (local.equalsIgnoreCase("ComputedKey")) {
            setComputedKey(URI.create((String)obj.getValue()));
        }else if (local.equalsIgnoreCase("BinarySecret")){
            final BinarySecretType bsType = (BinarySecretType)obj.getValue();
            setBinarySecret(new BinarySecretImpl(bsType));
        } else{
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0019_INVALID_PROOF_TOKEN_TYPE(local));
            throw new RuntimeException(LogStringsMessages.WST_0019_INVALID_PROOF_TOKEN_TYPE(local));
        }
    }
    
    public String getProofTokenType() {
        return tokenType;
    }
    
    public final void setProofTokenType(@NotNull final String proofTokenType) {
        if (! (proofTokenType.equalsIgnoreCase(RequestedProofToken.BINARY_SECRET_TYPE)
        || proofTokenType.equalsIgnoreCase(RequestedProofToken.COMPUTED_KEY_TYPE)
        || proofTokenType.equalsIgnoreCase(RequestedProofToken.ENCRYPTED_KEY_TYPE)
        || proofTokenType.equalsIgnoreCase(RequestedProofToken.CUSTOM_TYPE)
        || proofTokenType.equalsIgnoreCase(RequestedProofToken.TOKEN_REF_TYPE)
        )) {
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0019_INVALID_PROOF_TOKEN_TYPE(proofTokenType));
            throw new RuntimeException(LogStringsMessages.WST_0019_INVALID_PROOF_TOKEN_TYPE(proofTokenType));
        }
        tokenType = proofTokenType;
    }
    
    public void setSecurityTokenReference(final SecurityTokenReference reference) {
        if (reference != null) {
            str = reference;
            final JAXBElement<SecurityTokenReferenceType> strElement=
                    (new com.sun.xml.ws.security.secext10.ObjectFactory()).createSecurityTokenReference((SecurityTokenReferenceType)reference);
            setAny(strElement);
        }
        setProofTokenType(RequestedProofToken.TOKEN_REF_TYPE);
    }
    
    public SecurityTokenReference getSecurityTokenReference() {
        return str;
    }
    
    public final void setComputedKey(@NotNull final URI computedKey) {
        
        if (computedKey != null) {
            final String ckString = computedKey.toString();
            if (!(ckString.equalsIgnoreCase(WSTrustConstants.CK_HASH) || (ckString.equalsIgnoreCase(WSTrustConstants.CK_PSHA1)))) {
                log.log(Level.SEVERE,
                        LogStringsMessages.WST_0028_INVALID_CK(ckString));
                throw new RuntimeException(LogStringsMessages.WST_0028_INVALID_CK(ckString));
            }
            this.computedKey = computedKey;
            final JAXBElement<String> ckElement=
                    (new ObjectFactory()).createComputedKey(computedKey.toString());
            setAny(ckElement);
        }
        setProofTokenType(RequestedProofToken.COMPUTED_KEY_TYPE);
    }
    
    public URI getComputedKey() {
        return computedKey;
    }
    
    public final void setBinarySecret(final BinarySecret secret) {
        if (secret != null) {
            this.secret = secret;
            final JAXBElement<BinarySecretType> bsElement=
                    (new ObjectFactory()).createBinarySecret((BinarySecretType)secret);
            setAny(bsElement);
        }
        setProofTokenType(RequestedProofToken.BINARY_SECRET_TYPE);
    }
    
    public BinarySecret getBinarySecret() {
        return secret;
    }
    
    public static RequestedProofTokenType fromElement(final org.w3c.dom.Element element)
    throws WSTrustException {
        try {
            final javax.xml.bind.Unmarshaller unmarshaller = WSTrustElementFactory.getContext().createUnmarshaller();
            return (RequestedProofTokenType)unmarshaller.unmarshal(element);
        } catch (JAXBException ex) {
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0021_ERROR_UNMARSHAL_DOM_ELEMENT(), ex);
            throw new WSTrustException(LogStringsMessages.WST_0021_ERROR_UNMARSHAL_DOM_ELEMENT(), ex);
        }
    }
    
}
