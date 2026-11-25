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
package org.apache.ofbiz.widget.renderer.macro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.junit.Before;
import org.junit.Test;

import freemarker.core.Environment;
import freemarker.template.Template;

public class MacroCallTemplateCacheTest {
    @Before
    public void initialize() {
        System.setProperty("ofbiz.home", System.getProperty("user.dir"));
    }

    @Test
    public void sameMacroCallReusesParsedTemplate() throws Exception {
        Template first = MacroCallTemplateCache.getTemplate("<#macro m a>[${a}]</#macro><@m a=\"x\" />");
        Template second = MacroCallTemplateCache.getTemplate("<#macro m a>[${a}]</#macro><@m a=\"x\" />");
        assertSame(first, second);
    }

    @Test
    public void differentMacroCallsRenderTheirOwnOutput() throws Exception {
        Template x = MacroCallTemplateCache.getTemplate("<#macro m a>[${a}]</#macro><@m a=\"x\" />");
        Template y = MacroCallTemplateCache.getTemplate("<#macro m a>[${a}]</#macro><@m a=\"y\" />");
        assertNotSame(x, y);
        assertEquals("[x]", render(x));
        assertEquals("[y]", render(y));
        assertEquals("[x]", render(MacroCallTemplateCache.getTemplate("<#macro m a>[${a}]</#macro><@m a=\"x\" />")));
    }

    @Test
    public void cachedCallUsesTheMacroLibraryOfEachEnvironment() throws Exception {
        String call = "<@renderField value=\"v\" />";
        assertEquals("one:v", renderWithLibrary("<#macro renderField value>one:${value}</#macro>", call));
        assertEquals("two:v", renderWithLibrary("<#macro renderField value>two:${value}</#macro>", call));
    }

    @Test
    public void cacheKeysDoNotContainTheMacroCall() throws Exception {
        MacroCallTemplateCache.getTemplate("<#macro p v>${v}</#macro><@p v=\"s3cret-value\" />");
        for (Object key : UtilCache.findCache("widget.macro.call.template").getCacheLineKeys()) {
            assertFalse(key.toString().contains("s3cret-value"));
        }
    }

    private static String renderWithLibrary(String library, String call) throws Exception {
        StringWriter out = new StringWriter();
        Template libraryTemplate = new Template("library", new StringReader(library),
                FreeMarkerWorker.getDefaultOfbizConfig());
        Environment environment = FreeMarkerWorker.renderTemplate(libraryTemplate, new HashMap<String, Object>(), out);
        environment.include(MacroCallTemplateCache.getTemplate(call));
        return out.toString();
    }

    private static String render(Template template) throws Exception {
        StringWriter out = new StringWriter();
        template.process(new HashMap<String, Object>(), out);
        return out.toString();
    }
}
