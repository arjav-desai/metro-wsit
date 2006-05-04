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

package com.sun.xml.ws.policy.jaxws;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.io.File;
import javax.servlet.ServletContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.SAXException;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver.Parser;
import com.sun.xml.ws.policy.PolicyException;

/**
 * Reads a policy configuration file and returns the WSDL model generated from it.
 */
public final class PolicyConfigParser {
    
    private static final String FILE_PREFIX="wsit-";
    private static final String FILE_POSTFIX=".xml";
    private static final String CONFIG_FILE_NAME="wsit.xml";

    /**
     * Reads a WSDL file from META-INF or WEB-INF/wsit.xml, parses it
     * and returns a WSDLModel.
     *
     * @param container May hold the servlet context if run inside a web container
     * @return A WSDLModel populated from the WSDL file
     */
    public static WSDLModel parse(Container container) throws PolicyException {
        try {
            WSDLModel model = null;
            XMLStreamBuffer buffer = null;
            ServletContext context = container.getSPI(ServletContext.class);
            if (context != null) {
                buffer = loadFromContext(context);
            }
            else {
                // We are not running inside a web container, load file from META-INF
                buffer = loadFromClasspath(CONFIG_FILE_NAME);
            }
            if (buffer != null) {
                model = parse(buffer);
            }
            return model;
        } catch (XMLStreamBufferException ex) {
            throw new PolicyException(ex);
        } catch (XMLStreamException ex) {
            throw new PolicyException(ex);
        }
    }
    
    
    /**
     * Reads WSDL from an XMLStreamBuffer, parses it
     * and returns a WSDLModel.
     *
     * @param buffer The XMLStreamBuffer from which WSDL is parsed
     * @return A WSDLModel populated from the WSDL input
     */
    public static WSDLModel parse(XMLStreamBuffer buffer) throws PolicyException {
        try {
            WSDLModel model = null;
            URL systemId = new URL("http://example.org/wsit");
            SDDocumentSource doc = SDDocumentSource.create(systemId, buffer);
            Parser parser =  new Parser(doc);
            model = RuntimeWSDLParser.parse(parser, new Resolver(),
                new WSDLParserExtension[] { new PolicyWSDLParserExtension() } );
            return model;
        } catch (XMLStreamException ex) {
            throw new PolicyException(ex);
        } catch (IOException ex) {
            throw new PolicyException(ex);
        } catch (SAXException ex) {
            throw new PolicyException(ex);
        }
    }
    

    private static XMLStreamBuffer loadFromClasspath(String filename)
        throws XMLStreamException, XMLStreamBufferException {
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream input = null;
        if (cl == null) {
            input = ClassLoader.getSystemResourceAsStream(filename);
        }
        else {
            input = cl.getResourceAsStream(filename);
        }
        XMLStreamBuffer buffer = null;
        if (input != null) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            buffer = XMLStreamBuffer.createNewBufferFromXMLStreamReader(reader);
        }
        return buffer;
    }

    
    private static XMLStreamBuffer loadFromContext(ServletContext context)
        throws XMLStreamException, XMLStreamBufferException {
        
        XMLStreamBuffer buffer = null;
        InputStream input = context.getResourceAsStream("/WEB-INF/" + CONFIG_FILE_NAME);
        if (input != null) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            buffer = XMLStreamBuffer.createNewBufferFromXMLStreamReader(reader);
        }
        return buffer;
    }
    
    
    private static final class Resolver implements XMLEntityResolver {
        
        public Parser resolveEntity(String string, String string0) {
            throw new UnsupportedOperationException("Can not resolve XML entities");
        }
    }
}
