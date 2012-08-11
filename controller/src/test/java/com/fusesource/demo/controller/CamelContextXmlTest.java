/*
 * Copyright 2012 FuseSource
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.fusesource.demo.controller;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlTest extends CamelSpringTestSupport {
    // Templates to send to input endpoints
    @Produce
    protected ProducerTemplate inputEndpoint;

    @Test
    public void testCamelRoute() throws Exception {
        String body = "<data><stuff/></data>\n";

        String response = inputEndpoint.requestBody("mina:tcp://localhost:9001?textline=true&sync=true", body, java.lang.String.class);

        assertEquals("Incorrect Response", "<data><stuff/><controller>Controller 1</controller></data>", response);
    }

    @Override
    protected ClassPathXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "META-INF/spring/camel-context.xml");
    }

}
