/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.webapp.control;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ViewMap;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class ConfigXMLReaderTests {
    private static final String CONTROLLER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<site-conf xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns=\"http://ofbiz.apache.org/Site-Conf\""
            + " xsi:schemaLocation=\"http://ofbiz.apache.org/Site-Conf http://ofbiz.apache.org/dtds/site-conf.xsd\">"
            + "<request-map uri=\"publicRequest\">"
            + "<security https=\"false\" auth=\"false\"/>"
            + "<response name=\"success\" type=\"view\" value=\"publicView\"/>"
            + "</request-map>"
            + "<request-map uri=\"implicitRequest\">"
            + "<security https=\"true\"/>"
            + "<response name=\"success\" type=\"view\" value=\"implicitView\"/>"
            + "</request-map>"
            + "<request-map uri=\"protectedRequest\">"
            + "<security https=\"true\" auth=\"true\"/>"
            + "<response name=\"success\" type=\"view\" value=\"protectedView\"/>"
            + "</request-map>"
            + "<view-map name=\"publicView\" type=\"screen\" page=\"component://test/widget/Screens.xml#public\""
            + " auth=\"false\"/>"
            + "<view-map name=\"implicitView\" type=\"screen\" page=\"component://test/widget/Screens.xml#implicit\"/>"
            + "<view-map name=\"protectedView\" type=\"screen\" page=\"component://test/widget/Screens.xml#protected\""
            + " auth=\"true\"/>"
            + "</site-conf>";

    private Map<String, RequestMap> requestMaps;
    private Map<String, ViewMap> viewMaps;

    @Before
    public void setUp() throws Exception {
        Element root = UtilXml.readXmlDocument(
                new ByteArrayInputStream(CONTROLLER.getBytes(StandardCharsets.UTF_8)), true, "ConfigXMLReaderTests")
                .getDocumentElement();
        requestMaps = new HashMap<>();
        for (Element element : UtilXml.childElementList(root, "request-map")) {
            RequestMap requestMap = new RequestMap(element);
            requestMaps.put(requestMap.uri, requestMap);
        }
        viewMaps = new HashMap<>();
        for (Element element : UtilXml.childElementList(root, "view-map")) {
            ViewMap viewMap = new ViewMap(element);
            viewMaps.put(viewMap.name, viewMap);
        }
    }

    @Test
    public void viewMapRequiresAuthByDefault() {
        assertTrue(viewMaps.get("implicitView").securityAuth);
    }

    @Test
    public void viewMapHonorsExplicitAuth() {
        assertTrue(viewMaps.get("protectedView").securityAuth);
        assertFalse(viewMaps.get("publicView").securityAuth);
    }

    @Test
    public void requestMapSecurityRequiresAuthByDefault() {
        assertTrue(requestMaps.get("implicitRequest").securityAuth);
    }

    @Test
    public void requestMapSecurityHonorsExplicitAuth() {
        assertTrue(requestMaps.get("protectedRequest").securityAuth);
        assertFalse(requestMaps.get("publicRequest").securityAuth);
    }
}
