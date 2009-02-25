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

package com.sun.xml.ws.rx.policy.assertion;

import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.SimpleAssertion;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import com.sun.xml.ws.rx.rm.ReliableMessagingFeatureBuilder;
import java.util.Collection;
import javax.xml.namespace.QName;

/**
 * Assertion which replaces inactivity timeout attribute of WS-RMP v1.0 RMAssertion.
 * The same assertion is used by .Net framework which could simplify the interoperability.
 * 
 * <pre>
 * <netrmp:InactivityTimeout Milliseconds="600000" xmlns:netrmp="http://schemas.microsoft.com/ws-rx/wsrmp/200702"/> 
 * </pre>
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class InactivityTimeoutAssertion extends SimpleAssertion implements RmAssertionTranslator {
    public static final QName NAME = AssertionNamespace.MICROSOFT_200702.getQName("InactivityTimeout");
    private static final QName MILISECONDS_ATTRIBUTE_QNAME = new QName("", "Milliseconds");    

    private static AssertionInstantiator instantiator = new AssertionInstantiator() {
        public PolicyAssertion newInstance(AssertionData data, Collection<PolicyAssertion> assertionParameters, AssertionSet nestedAlternative){
            return new InactivityTimeoutAssertion(data, assertionParameters);
        }
    };
    
    public static AssertionInstantiator getInstantiator() {
        return instantiator;
    }

    private final long timeout;
    
    public InactivityTimeoutAssertion(AssertionData data, Collection<? extends PolicyAssertion> assertionParameters) {
        super(data, assertionParameters);
        
        timeout = Long.parseLong(data.getAttributeValue(MILISECONDS_ATTRIBUTE_QNAME));
    }
   
    public long getTimeout() {
        return timeout;
    }

    public ReliableMessagingFeatureBuilder update(ReliableMessagingFeatureBuilder builder) {
        return builder.sequenceInactivityTimeout(timeout);
    }
    
    

}