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

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EntryWeigher;

import freemarker.template.Template;

/**
 * Parsed FreeMarker templates for the macro call strings built by the macro renderers.
 * A template only depends on its source text, so identical calls reuse one parse.
 * Calls can carry rendered values (login keys), so this is a private LRU map, not a UtilCache
 * listed in Cache Maintenance; it is bounded by the total length of the calls. Password fields
 * and very long calls are parsed without being cached.
 */
final class MacroCallTemplateCache {

    private static final long DEFAULT_MAX_TOTAL_CHARS = 5_000_000L;
    private static final long CONFIGURED_MAX_TOTAL_CHARS =
            UtilProperties.getPropertyAsLong("widget", "widget.macro.call.cache.maxChars", DEFAULT_MAX_TOTAL_CHARS);
    static final long MAX_TOTAL_CHARS =
            CONFIGURED_MAX_TOTAL_CHARS > 0 ? CONFIGURED_MAX_TOTAL_CHARS : DEFAULT_MAX_TOTAL_CHARS;
    static final int MAX_CACHED_CALL_CHARS = 20_000;

    private static final ConcurrentLinkedHashMap<String, Template> TEMPLATES =
            new ConcurrentLinkedHashMap.Builder<String, Template>()
            .maximumWeightedCapacity(MAX_TOTAL_CHARS)
            .weigher(new EntryWeigher<String, Template>() {
                @Override
                public int weightOf(String macro, Template template) {
                    return Math.max(1, macro.length());
                }
            })
            .build();

    private MacroCallTemplateCache() { }

    static Template getTemplate(String macro) throws IOException {
        if (macro.length() > MAX_CACHED_CALL_CHARS || macro.startsWith("<@renderPasswordField ")) {
            return parse(macro);
        }
        Template template = TEMPLATES.get(macro);
        if (template == null) {
            template = parse(macro);
            Template existing = TEMPLATES.putIfAbsent(macro, template);
            if (existing != null) {
                template = existing;
            }
        }
        return template;
    }

    private static Template parse(String macro) throws IOException {
        return new Template("macroCall", new StringReader(macro), FreeMarkerWorker.getDefaultOfbizConfig());
    }

    static boolean contains(String macro) {
        return TEMPLATES.containsKey(macro);
    }

    static long weightedSize() {
        return TEMPLATES.weightedSize();
    }
}
