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
import org.apache.ofbiz.security.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.datafile.*

uiLabelMap = UtilProperties.getResourceBundleMap("WebtoolsUiLabels", locale)
messages = []
userLoginId = userLogin?.userLoginId

if (!security.hasPermission("DATAFILE_MAINT", session) || !security.hasPermission("ENTITY_MAINT", session)) {
    Debug.logWarning("Denied access to data file tools for userLogin [" + userLoginId + "]: DATAFILE_MAINT and ENTITY_MAINT permissions required", "ViewDataFile.groovy")
    context.messages = messages
    return
}

// Only paths inside ofbiz.home are accepted; remote URLs are not (see OFBIZ-12306).
String toOfbizHomePath(String location) {
    String ofbizHome = new File(System.getProperty("ofbiz.home")).getCanonicalPath()
    File file = new File(location)
    if (!file.isAbsolute()) {
        file = new File(ofbizHome, location)
    }
    String canonical = file.getCanonicalPath()
    if (!canonical.equals(ofbizHome) && !canonical.startsWith(ofbizHome + File.separator)) {
        Debug.logWarning("Rejected data file location outside ofbiz.home for userLogin [" + userLoginId + "]: " + location, "ViewDataFile.groovy")
        throw new IllegalArgumentException("File locations must be inside the OFBiz home directory")
    }
    return canonical
}

URL toLocalFileUrl(String location) {
    if (!location) {
        return null
    }
    if (UtilURL.fromUrlString(location)) {
        Debug.logWarning("Rejected URL data file location for userLogin [" + userLoginId + "]: " + location, "ViewDataFile.groovy")
        throw new IllegalArgumentException("Remote URLs are not accepted, only files inside the OFBiz home directory")
    }
    return UtilURL.fromFilename(toOfbizHomePath(location))
}

dataFileSave = request.getParameter("DATAFILE_SAVE")

entityXmlFileSave = request.getParameter("ENTITYXML_FILE_SAVE")

dataFileLoc = request.getParameter("DATAFILE_LOCATION")
definitionLoc = request.getParameter("DEFINITION_LOCATION")
definitionName = request.getParameter("DEFINITION_NAME")

if (request.getParameter("DATAFILE_IS_URL") != null || request.getParameter("DEFINITION_IS_URL") != null) {
    messages.add("Remote URLs are not accepted, only files inside the OFBiz home directory")
}

dataFileUrl = null
try {
    dataFileUrl = toLocalFileUrl(dataFileLoc)
}
catch (Exception e) {
    messages.add(e.getMessage())
}

definitionUrl = null
try {
    definitionUrl = toLocalFileUrl(definitionLoc)
}
catch (Exception e) {
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
    try {
        dataFileSave = toOfbizHomePath(dataFileSave)
        dataFile.writeDataFile(dataFileSave)
        messages.add(uiLabelMap.WebtoolsDataFileSavedTo + dataFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}

if (dataFile && entityXmlFileSave) {
    try {
        entityXmlFileSave = toOfbizHomePath(entityXmlFileSave)
        DataFile2EntityXml.writeToEntityXml(entityXmlFileSave, dataFile)
        messages.add(uiLabelMap.WebtoolsDataEntityFileSavedTo + entityXmlFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}
context.messages = messages
