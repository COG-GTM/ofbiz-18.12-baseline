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

import java.io.IOException;
import java.io.StringReader;

import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;

import freemarker.template.Template;

/**
 * Parsed FreeMarker templates for the macro call strings built by the macro renderers.
 * A template only depends on its source text, so identical calls reuse one parse.
 */
final class MacroCallTemplateCache {

    private static final UtilCache<String, Template> TEMPLATES =
            UtilCache.createUtilCache("widget.macro.call.template", 10000, 0, true);

    private MacroCallTemplateCache() { }

    static Template getTemplate(String macro) throws IOException {
        Template template = TEMPLATES.get(macro);
        if (template == null) {
            template = new Template("macroCall", new StringReader(macro), FreeMarkerWorker.getDefaultOfbizConfig());
            template = TEMPLATES.putIfAbsentAndGet(macro, template);
        }
        return template;
    }
}
