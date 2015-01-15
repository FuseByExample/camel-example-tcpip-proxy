/*
 * Copyright (C) Red Hat, Inc.
 * http://www.redhat.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fusesource.demo.router;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class CamelContextXmlTest extends CamelSpringTestSupport {
    @Produce
    protected ProducerTemplate inputEndpoint;

    @Test
    public void testCamelRoute() throws Exception {
        // Create 2 routes consuming from the tcp ports the route "route1" (inside camel-context.xml) writes into
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("mina:tcp://0.0.0.0:9001?textline=true&sync=true")
                        .routeId("controller1")
                        .setBody(groovy("request.getBody(java.lang.String).replaceFirst('</data>','<controller>Controller 1</controller></data>')"))
                        .log("Body = ${body}")
                        .transform(simple("${body}"));

                from("mina:tcp://0.0.0.0:9002?textline=true&sync=true")
                        .routeId("controller2")
                        .setBody(groovy("request.getBody(java.lang.String).replaceFirst('</data>','<controller>Controller 2</controller></data>')"))
                        .log("Body = ${body}")
                        .transform(simple("${body}"));
            }
        });

        final String request = "<data><stuff/></data>";
        final String expectedResponse1 = "<data><stuff/><controller>Controller 1</controller></data>";
        final String expectedResponse2 = "<data><stuff/><controller>Controller 2</controller></data>";

        Object response = inputEndpoint.requestBody("mina:tcp://0.0.0.0:9000?textline=true&sync=true", request);

        assertEquals("Incorrect response from controller 1", expectedResponse1, response);

        context().stopRoute("controller1");

        // in addition to stopping the route "controller1" let's remove it as well so that Camel graceful shutdown
        // (when test is tearing down) doesn't try to unbind the port 9001 again which is actually not bound anymore
        // (has already happened after the route stop). this could be confusing for the users:
        // (java.lang.IllegalArgumentException: Address not bound: /0.0.0.0:9001)
        context().removeRoute("controller1");

        // after the execution of the following line we see a stacktrace of MinaProducer failing to get the Mina IoSession
        // as the MinaConsumer of the route "controller1" is already gone. however as we've got a failover load balancer
        // (inside camel-context.xml) the MinaConsumer of other route "controller2" will just kick in automatically
        response = inputEndpoint.requestBody("mina:tcp://localhost:9000?textline=true&sync=true", request);

        assertEquals("Incorrect response from controller 2", expectedResponse2, response);
    }

    @Override
    protected ClassPathXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "META-INF/spring/camel-context.xml");
    }

}
