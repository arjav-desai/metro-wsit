/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
/*
 * AsymmetricBindingTest.java
 * JUnit based test
 *
 * Created on August 24, 2006, 12:26 AM
 */

package com.sun.xml.ws.security.impl.policy;

import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.sourcemodel.PolicyModelTranslator;
import com.sun.xml.ws.policy.sourcemodel.PolicyModelUnmarshaller;
import com.sun.xml.ws.policy.sourcemodel.PolicySourceModel;
import com.sun.xml.ws.security.policy.AlgorithmSuiteValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

import junit.framework.*;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.NestedPolicy;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import com.sun.xml.ws.security.policy.AlgorithmSuite;
import com.sun.xml.ws.security.policy.MessageLayout;
import com.sun.xml.ws.security.policy.Token;
import com.sun.xml.ws.security.policy.SecurityAssertionValidator;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import static com.sun.xml.ws.security.impl.policy.Constants.*;

/**
 *
 * @author Mayank.Mishra@SUN.com
 */
public class AsymmetricBindingTest extends TestCase {
    
    public AsymmetricBindingTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(AsymmetricBindingTest.class);
        
        return suite;
    }
    
    private PolicySourceModel unmarshalPolicyResource(String resource) throws PolicyException, IOException {
        Reader reader = getResourceReader(resource);
        PolicySourceModel model = PolicyModelUnmarshaller.getXmlUnmarshaller().unmarshalModel(reader);
        reader.close();
        return model;
    }
    
    private Reader getResourceReader(String resourceName) {
        return new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName));
    }
    
    public Policy unmarshalPolicy(String xmlFile)throws Exception{
        PolicySourceModel model =  unmarshalPolicyResource(
                xmlFile);
        Policy mbp = PolicyModelTranslator.getTranslator().translate(model);
        return mbp;
        
    }
    
    public void testAsymmerticBinding1() throws Exception {
        String fileName="security/AsymmetricBindingAssertion1.xml";
        Policy policy = unmarshalPolicy(fileName);
        Iterator <AssertionSet> itr = policy.iterator();
        if(itr.hasNext()) {
            AssertionSet as = itr.next();
            for(PolicyAssertion assertion : as) {
                assertEquals("Invalid assertion", "AsymmetricBinding",assertion.getName().getLocalPart());
                AsymmetricBinding asb = (AsymmetricBinding)assertion;
                
                X509Token tkn1 = (X509Token)asb.getInitiatorToken();
                assertTrue(tkn1.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V1TOKEN10));
                
                X509Token tkn2 = (X509Token)asb.getRecipientToken();
                assertTrue(tkn2.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V3TOKEN11));
                
                assertFalse("Tokens are Protected", asb.getTokenProtection());
                
                AlgorithmSuite aSuite = asb.getAlgorithmSuite();
                assertEquals("Unmatched Algorithm",aSuite.getEncryptionAlgorithm(), AlgorithmSuiteValue.TripleDesRsa15.getEncAlgorithm());
                
                assertTrue(asb.isIncludeTimeStamp());
                
                assertFalse("Signature is Encrypted", asb.getSignatureProtection());
                
                assertFalse(asb.isSignContent());
            }
        }
    }
    
    
    public void testAsymmerticBinding2() throws Exception {
        String fileName="security/AsymmetricBindingAssertion2.xml";
        Policy policy = unmarshalPolicy(fileName);
        Iterator <AssertionSet> itr = policy.iterator();
        if(itr.hasNext()) {
            AssertionSet as = itr.next();
            for(PolicyAssertion assertion : as) {
                assertEquals("Invalid assertion", "AsymmetricBinding",assertion.getName().getLocalPart());
                AsymmetricBinding asb = (AsymmetricBinding)assertion;
                
                X509Token tkn1 = (X509Token)asb.getInitiatorToken();
                assertTrue(tkn1.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V1TOKEN10));
                
                X509Token tkn2 = (X509Token)asb.getRecipientToken();
                assertTrue(tkn2.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V3TOKEN11));
                
                AlgorithmSuite aSuite = asb.getAlgorithmSuite();
                assertEquals("Unmatched Algorithm",aSuite.getEncryptionAlgorithm(), AlgorithmSuiteValue.TripleDesRsa15.getEncAlgorithm());
                
                assertTrue(asb.isIncludeTimeStamp());
                
                assertTrue("Signature is not Encrypted", asb.getSignatureProtection());
                
                assertTrue("Tokens are not protected", asb.getTokenProtection());
                
                assertEquals("SIGN_ENCRYPT is the protection order",com.sun.xml.ws.security.impl.policy.AsymmetricBinding.ENCRYPT_SIGN,asb.getProtectionOrder());
            }
        }
    }
    
    
    public void testAsymmerticCR6420594() throws Exception {
        String fileName="security/AsymmetricCR.xml";
        Policy policy = unmarshalPolicy(fileName);
        Iterator <AssertionSet> itr = policy.iterator();
        if(itr.hasNext()) {
            AssertionSet as = itr.next();
            for(PolicyAssertion assertion : as) {
                assertEquals("Invalid assertion", "AsymmetricBinding",assertion.getName().getLocalPart());
                AsymmetricBinding asb = (AsymmetricBinding)assertion;
                
                X509Token tkn1 = (X509Token)asb.getInitiatorToken();
                assertTrue(tkn1.getIncludeToken().equals(tkn1.getSecurityPolicyVersion().includeTokenAlways));
                assertTrue(tkn1.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V3TOKEN10));
                
                X509Token tkn2 = (X509Token)asb.getRecipientToken();
                assertTrue(tkn2.getIncludeToken().equals(tkn2.getSecurityPolicyVersion().includeTokenAlways));
                assertTrue(tkn2.getTokenType().equals(com.sun.xml.ws.security.impl.policy.X509Token.WSSX509V3TOKEN10));
            }
        }
    }
    
}
