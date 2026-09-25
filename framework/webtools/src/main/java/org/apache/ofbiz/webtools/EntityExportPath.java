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
package org.apache.ofbiz.webtools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * Resolves user supplied output locations for entity XML exports so that every export
 * is confined to the directory configured by <code>entityExportDir</code> in security.properties
 * (relative to <code>ofbiz.home</code>, default <code>runtime/output</code>).
 *
 * <p>The user supplied directory and file names are accepted only as single path components made of
 * letters, digits, '.', '_' and '-' (no separators, no leading dot), and the canonical result must stay
 * below the export base directory.</p>
 */
public final class EntityExportPath {

    public static final String module = EntityExportPath.class.getName();

    private static final Pattern SAFE_COMPONENT = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$");

    private EntityExportPath() { }

    /** The canonical export base directory. */
    public static File getBaseDir() throws IOException {
        String configured = UtilProperties.getPropertyValue("security", "entityExportDir", "runtime/output");
        return new File(System.getProperty("ofbiz.home"), configured).getCanonicalFile();
    }

    /** True when the value is a single, plain path component. */
    public static boolean isSafeComponent(String component) {
        return component != null && SAFE_COMPONENT.matcher(component).matches()
                && !".".equals(component) && !"..".equals(component);
    }

    /**
     * Resolves <code>outpath</code> as a sub-directory of the export base directory.
     * An empty <code>outpath</code> yields the base directory itself.
     * @return the canonical directory, or null when the value is not allowed
     */
    public static File resolveDir(String outpath) throws IOException {
        File baseDir = getBaseDir();
        if (outpath == null || outpath.isEmpty()) {
            return baseDir;
        }
        return resolveChild(baseDir, baseDir, outpath);
    }

    /**
     * Resolves <code>filename</code> as a file directly below <code>dir</code>, which must itself have been
     * produced by {@link #resolveDir(String)}.
     * @return the canonical file, or null when the value is not allowed
     */
    public static File resolveFile(File dir, String filename) throws IOException {
        if (dir == null) {
            return null;
        }
        return resolveChild(getBaseDir(), dir, filename);
    }

    private static File resolveChild(File baseDir, File parent, String component) throws IOException {
        if (!isSafeComponent(component)) {
            Debug.logWarning("Rejected entity export path component [" + component + "]", module);
            return null;
        }
        File resolved = new File(parent, component).getCanonicalFile();
        Path basePath = baseDir.toPath();
        if (!resolved.toPath().startsWith(basePath) || resolved.toPath().equals(basePath)) {
            Debug.logWarning("Rejected entity export path [" + resolved + "] outside of [" + baseDir + "]", module);
            return null;
        }
        return resolved;
    }
}
