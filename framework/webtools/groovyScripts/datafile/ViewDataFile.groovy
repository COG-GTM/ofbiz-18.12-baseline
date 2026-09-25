/*
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
 */

import java.util.*
import java.net.*
import java.nio.file.Files
import java.util.regex.Pattern
import org.apache.ofbiz.security.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.datafile.*

final String MODULE = "ViewDataFile.groovy"

uiLabelMap = UtilProperties.getResourceBundleMap("WebtoolsUiLabels", locale)
messages = []

if (!security.hasPermission("DATAFILE_MAINT", session)) {
    messages.add(uiLabelMap.WebtoolsPermissionError)
    context.messages = messages
    return
}

// Output files are only ever written under this directory; the request supplies a bare file name, never a path.
final String outputDirName = "runtime/output/datafile"
final Pattern safeFileName = Pattern.compile('^[A-Za-z0-9_-]{1,200}(\\.[A-Za-z0-9]{1,10})?$')

// Validates fileName, runs writer(File) against it inside the output directory and returns the
// path (relative to ofbiz.home) that was written, or null after adding an error message.
saveOutputFile = { String fileName, Closure writer ->
    if (!fileName || !safeFileName.matcher(fileName).matches()) {
        messages.add(uiLabelMap.WebtoolsDataFileInvalidSaveName)
        return null
    }
    try {
        File outputDir = new File(System.getProperty("ofbiz.home"), outputDirName)
        if (Files.isSymbolicLink(outputDir.toPath())) {
            throw new IOException("Output directory is a symbolic link")
        }
        Files.createDirectories(outputDir.toPath())
        File canonicalDir = outputDir.getCanonicalFile()
        File outFile = new File(canonicalDir, fileName)
        if (!canonicalDir.equals(outFile.getCanonicalFile().getParentFile()) || Files.isSymbolicLink(outFile.toPath())) {
            throw new IOException("Refusing to write outside the output directory")
        }
        writer(outFile)
        return outputDirName + "/" + outFile.getName()
    }
    catch (Exception e) {
        Debug.logError(e, "Error writing " + outputDirName + "/" + fileName, MODULE)
        messages.add(uiLabelMap.WebtoolsDataFileSaveError)
        return null
    }
}

dataFileSave = request.getParameter("DATAFILE_SAVE")

entityXmlFileSave = request.getParameter("ENTITYXML_FILE_SAVE")

dataFileLoc = request.getParameter("DATAFILE_LOCATION")
definitionLoc = request.getParameter("DEFINITION_LOCATION")
definitionName = request.getParameter("DEFINITION_NAME")
dataFileIsUrl = null != request.getParameter("DATAFILE_IS_URL")
definitionIsUrl = null != request.getParameter("DEFINITION_IS_URL")

try {
    dataFileUrl = dataFileIsUrl?new URL(dataFileLoc):UtilURL.fromFilename(dataFileLoc)
}
catch (java.net.MalformedURLException e) {
    messages.add(e.getMessage())
}

try {
    definitionUrl = definitionIsUrl?new URL(definitionLoc):UtilURL.fromFilename(definitionLoc)
}
catch (java.net.MalformedURLException e) {
    messages.add(e.getMessage())
}

definitionNames = null
if (definitionUrl) {
    try {
        ModelDataFileReader reader = ModelDataFileReader.getModelDataFileReader(definitionUrl)
        if (reader) {
            definitionNames = ((Collection)reader.getDataFileNames()).iterator()
            context.put("definitionNames", definitionNames)
        }
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}

dataFile = null
if (dataFileUrl && definitionUrl && definitionNames) {
    try {
        dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definitionName)
        context.put("dataFile", dataFile)
    }
    catch (Exception e) {
        messages.add(e.toString()); Debug.log(e)
    }
}

if (dataFile) {
    modelDataFile = dataFile.getModelDataFile()
    context.put("modelDataFile", modelDataFile)
}

if (dataFile && dataFileSave) {
    savedPath = saveOutputFile(dataFileSave) { File outFile -> dataFile.writeDataFile(outFile.getPath()) }
    if (savedPath) {
        messages.add(uiLabelMap.WebtoolsDataFileSavedTo + savedPath)
    }
}

if (dataFile && entityXmlFileSave) {
    savedPath = saveOutputFile(entityXmlFileSave) { File outFile -> DataFile2EntityXml.writeToEntityXml(outFile.getPath(), dataFile) }
    if (savedPath) {
        messages.add(uiLabelMap.WebtoolsDataEntityFileSavedTo + savedPath)
    }
}
context.messages = messages
