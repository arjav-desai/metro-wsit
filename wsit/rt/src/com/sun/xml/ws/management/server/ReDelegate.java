/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.management.server;

import com.sun.xml.ws.api.management.EndpointCreationAttributes;
import com.sun.xml.ws.api.management.ConfigurationAPI;
import com.sun.xml.ws.api.management.InitParameters;
import com.sun.xml.ws.api.management.ManagedEndpoint;
import com.sun.xml.ws.api.server.DocumentAddressResolver;
import com.sun.xml.ws.api.server.PortAddressResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.management.ManagementConstants;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.privateutil.PolicyLogger;
import com.sun.xml.ws.policy.sourcemodel.attach.ExternalAttachmentsUnmarshaller;
import com.sun.xml.ws.resources.ManagementMessages;
import com.sun.xml.ws.server.EndpointFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
//import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
//import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author Fabian Ritzmann
 */
public class ReDelegate implements ConfigurationAPI {

    private static final PolicyLogger LOGGER = PolicyLogger.getLogger(ReDelegate.class);
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

//    public <T> void recreate(ManagedEndpoint<T> managedEndpoint,
//            EndpointCreationAttributes creationAttributes,
//            ClassLoader classLoader,
//            Reader newConfig) {
    @SuppressWarnings("unchecked")
    public <T> void recreate(InitParameters parameters) {
//            SDDocumentSource newWsdl) {
        final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader classLoader = parameters.get(ManagedEndpoint.CLASS_LOADER_PARAMETER_NAME, ClassLoader.class);
            Thread.currentThread().setContextClassLoader(classLoader);
//            final HashMap<String,SDDocumentSource> documents = new HashMap<String,SDDocumentSource>();
//            documents.put(newWsdl.getSystemId().toExternalForm(), newWsdl);
            final Reader newConfig = parameters.get(ManagementConstants.CONFIG_READER_PARAMETER_NAME, Reader.class);
            Map<URI, Policy> urnToPolicy = ExternalAttachmentsUnmarshaller.unmarshal(newConfig);

            final ManagedEndpoint<T> managedEndpoint = parameters.get(ManagedEndpoint.ENDPOINT_INSTANCE_PARAMETER_NAME, ManagedEndpoint.class);
            final EndpointCreationAttributes creationAttributes = parameters.get(ManagedEndpoint.CREATION_ATTRIBUTES_PARAMETER_NAME, EndpointCreationAttributes.class);
            WSEndpoint<T> delegate = recreateEndpoint(managedEndpoint, creationAttributes, urnToPolicy);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(ManagementMessages.WSM_0002_NEW_ENDPOINT_DELEGATE(delegate));
            }
            managedEndpoint.swapEndpointDelegate(delegate);

        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, ManagementMessages.WSM_0001_ENDPOINT_CREATION_FAILED(e), e);
            throw new WebServiceException(ManagementMessages.WSM_0001_ENDPOINT_CREATION_FAILED(e), e);
        } finally {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    private static <T> WSEndpoint<T> recreateEndpoint(WSEndpoint<T> endpoint,
            EndpointCreationAttributes creationAttributes,
            Map<URI, Policy> urnToPolicy) {
        final ServiceDefinition serviceDefinition = endpoint.getServiceDefinition();
        if (serviceDefinition == null) {
            throw new WebServiceException(ManagementMessages.WSM_0003_NO_SERVICE_DEFINITION());
        }
        for (SDDocument doc: serviceDefinition) {
            if (doc.isWSDL()) {
                replacePolicies(doc, urnToPolicy);
            }
        }

        final SDDocumentSource newWsdl = null;

        return EndpointFactory.createEndpoint(endpoint.getImplementationClass(),
                creationAttributes.isProcessHandlerAnnotation(),
                creationAttributes.getInvoker(),
                endpoint.getServiceName(),
                endpoint.getPortName(),
                endpoint.getContainer(),
                endpoint.getBinding(),
                newWsdl,
                null,
                creationAttributes.getEntityResolver(),
                creationAttributes.isTransportSynchronous());
    }

    private static void replacePolicies(SDDocument doc, Map<URI, Policy> urnToPolicy) {
        try {
            final StringWriter writer = new StringWriter();
            final XMLStreamWriter xmlWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(writer);
            doc.writeTo(new MockPortAddressResolver(), new MockDocumentAddressResolver(), xmlWriter);
            xmlWriter.flush();
            ManagementWSDLPatcher patcher = new ManagementWSDLPatcher(urnToPolicy);
            final StringReader reader = new StringReader(writer.getBuffer().toString());
            final XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);
            final StringWriter newWSDLWriter = new StringWriter();
            final XMLStreamWriter newWSDLXMLWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(newWSDLWriter);
            newWSDLXMLWriter.writeStartDocument();
            patcher.bridge(xmlReader, newWSDLXMLWriter);
            newWSDLXMLWriter.writeEndDocument();
            newWSDLXMLWriter.flush();
            System.out.println(newWSDLWriter.getBuffer());
        } catch (IOException ex) {
            throw LOGGER.logSevereException(new WebServiceException(""), ex);
        } catch (XMLStreamException ex) {
            throw LOGGER.logSevereException(new WebServiceException(""), ex);
        }
    }

    /**
     * We can return any address in this class because JAX-WS will later replace
     * it with a valid address. It is not possible to compute the correct port
     * address without receiving a GET request.
     */
    private static class MockPortAddressResolver extends PortAddressResolver {

        @Override
        public String getAddressFor(QName serviceName, String portName) {
            return "temporary address after web service reconfiguration";
        }
        
    }

    /**
     * We can return any address in this class because JAX-WS will later replace
     * it with a valid address. It is not possible to compute the correct port
     * address without receiving a GET request.
     */
    private static class MockDocumentAddressResolver implements DocumentAddressResolver {

        public String getRelativeAddressFor(SDDocument current, SDDocument referenced) {
            return referenced.getURL().toExternalForm();
        }
    }
    
}
