/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;

public class ActiveMQConnectionFactoryTest extends CombinationTestSupport {
    
    public void testUseURIToSetUseClientIDPrefixOnConnectionFactory() throws URISyntaxException, JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?jms.clientIDPrefix=Cheese");
        assertEquals("Cheese", cf.getClientIDPrefix());

        ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();
        try {
            connection.start();

            String clientID = connection.getClientID();
            log.info("Got client ID: " + clientID);

            assertTrue("should start with Cheese! but was: " + clientID, clientID.startsWith("Cheese"));
        }
        finally {
            connection.close();
        }
    }
    
    public void testUseURIToSetOptionsOnConnectionFactory() throws URISyntaxException, JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?jms.useAsyncSend=true");
        assertTrue(cf.isUseAsyncSend());
        // the broker url have been adjusted.
        assertEquals("vm://localhost", cf.getBrokerURL());
        
        cf = new ActiveMQConnectionFactory("vm://localhost?jms.useAsyncSend=false");
        assertFalse(cf.isUseAsyncSend());
        // the broker url have been adjusted.
        assertEquals("vm://localhost", cf.getBrokerURL());

        cf = new ActiveMQConnectionFactory("vm:(broker:()/localhost)?jms.useAsyncSend=true");
        assertTrue(cf.isUseAsyncSend());
        // the broker url have been adjusted.
        assertEquals("vm:(broker:()/localhost)", cf.getBrokerURL());
    }

    public void testCreateVMConnectionWithEmbdeddBroker() throws URISyntaxException, JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        // Make sure the broker is not created until the connection is instantiated.
        assertNull( BrokerRegistry.getInstance().lookup("localhost") );        
        Connection connection = cf.createConnection();
        // This should create the connection.
        assertNotNull(connection);
        // Verify the broker was created.
        assertNotNull( BrokerRegistry.getInstance().lookup("localhost") );
        connection.close();
        // Verify the broker was destroyed.
        assertNull( BrokerRegistry.getInstance().lookup("localhost") );
    }
    
    public void testGetBrokerName() throws URISyntaxException, JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();
        connection.start();
        
        String brokerName = connection.getBrokerName();
        log.info("Got broker name: " + brokerName);
        
        assertNotNull("No broker name available!", brokerName);
        connection.close();
    }
    
    public void testCreateTcpConnectionUsingAllocatedPort() throws Exception {
        assertCreateConnection("tcp://localhost:0?wireFormat.tcpNoDelayEnabled=true");
    }
    public void testCreateTcpConnectionUsingKnownPort() throws Exception {
        assertCreateConnection("tcp://localhost:61610?wireFormat.tcpNoDelayEnabled=true");
    }
    
    protected void assertCreateConnection(String uri) throws Exception {
        // Start up a broker with a tcp connector.
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        TransportConnector connector = broker.addConnector(uri);
        broker.start();
        
        URI temp = new URI(uri);
        //URI connectURI = connector.getServer().getConnectURI();
        // TODO this sometimes fails when using the actual local host name
        URI currentURI = connector.getServer().getConnectURI();

        // sometimes the actual host name doesn't work in this test case
        // e.g. on OS X so lets use the original details but just use the actual port
        URI connectURI = new URI(temp.getScheme(), temp.getUserInfo(), temp.getHost(), currentURI.getPort(), temp.getPath(), temp.getQuery(), temp.getFragment());
        
        
        log.info("connection URI is: " + connectURI);
        
        // This should create the connection.
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(connectURI);
        Connection connection = cf.createConnection();
        assertNotNull(connection);
        connection.close();
        
        broker.stop();
    }
    
}
