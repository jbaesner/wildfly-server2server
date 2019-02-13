/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly;

import java.util.Properties;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class StartupSingletonBean {

    private final Logger logger = LoggerFactory.getLogger(StartupSingletonBean.class);
    // app-name/module-name/distinct-name/bean-name!bean-interface
    private final String EJB_JNDI_NAME_TEMPLATE =  "ejb:server2server/server2server-ejb/%s/SimpleRemoteBean!org.wildfly.SimpleRemote";

    // get some parameters provided by system properties
    String rocPort = System.getProperty("roc.port", "8080");
    String distinctName = System.getProperty("remote.distinct.name", "unknown");

    String url= "http-remoting://localhost:" + rocPort;
    String jndiName = String.format(EJB_JNDI_NAME_TEMPLATE, distinctName);

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void timer() {

        Context ctx = null;

        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            props.put(Context.PROVIDER_URL, url);
            ctx = new InitialContext(props);
            
            SimpleRemote bean = (SimpleRemote) ctx.lookup(jndiName);

            String destination = System.getProperty("jboss.node.name");
            String target = bean.logMessageAndReturnJBossNodeName(String.format("called from destination '%s'", destination));

            logger.info("called SimpleRemote from destination '{}' on target '{}'", destination, target);

        } catch (NamingException e) {
            logger.error("Unable to invoke SimpleRemoteBean", e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    logger.trace("Unable to close Context", e);
                }
                ctx = null;
            }
        }
    }
}
