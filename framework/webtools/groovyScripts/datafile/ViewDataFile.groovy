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

if (!security.hasPermission("DATAFILE_MAINT", session) || !security.hasPermission("ENTITY_MAINT", session)) {
    Debug.logWarning("Denied access to data file tools for userLogin [" + (userLogin?.userLoginId) + "]: DATAFILE_MAINT and ENTITY_MAINT permissions required", "ViewDataFile.groovy")
    context.messages = messages
    return
}

boolean isInternalAddress(InetAddress addr) {
    if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
            || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
        return true
    }
    // IPv6 unique local addresses (fc00::/7) are not covered by isSiteLocalAddress
    if (addr instanceof Inet6Address && (addr.getAddress()[0] & 0xfe) == 0xfc) {
        return true
    }
    return false
}

URL toRemoteUrl(String location) {
    URL url = new URL(location)
    if (!(url.getProtocol() in ["http", "https"]) || !url.getHost()) {
        throw new MalformedURLException("Only http and https URLs with a host are allowed: " + location)
    }
    if (url.getUserInfo()) {
        throw new MalformedURLException("URLs with embedded credentials are not allowed: " + location)
    }
    InetAddress[] addresses
    try {
        addresses = InetAddress.getAllByName(url.getHost())
    } catch (UnknownHostException e) {
        throw new MalformedURLException("Unable to resolve host: " + url.getHost())
    }
    for (InetAddress addr : addresses) {
        if (isInternalAddress(addr)) {
            throw new MalformedURLException("URLs resolving to internal or reserved addresses are not allowed: " + url.getHost())
        }
    }
    return url
}

URL toLocalFileUrl(String location) {
    if (!location) {
        return null
    }
    String ofbizHome = new File(System.getProperty("ofbiz.home")).getCanonicalPath()
    String canonical = new File(location).getCanonicalPath()
    if (!canonical.equals(ofbizHome) && !canonical.startsWith(ofbizHome + File.separator)) {
        throw new MalformedURLException("File locations must be inside the OFBiz home directory: " + location)
    }
    return UtilURL.fromFilename(canonical)
}

dataFileSave = request.getParameter("DATAFILE_SAVE")

entityXmlFileSave = request.getParameter("ENTITYXML_FILE_SAVE")

dataFileLoc = request.getParameter("DATAFILE_LOCATION")
definitionLoc = request.getParameter("DEFINITION_LOCATION")
definitionName = request.getParameter("DEFINITION_NAME")
dataFileIsUrl = null != request.getParameter("DATAFILE_IS_URL")
definitionIsUrl = null != request.getParameter("DEFINITION_IS_URL")

dataFileUrl = null
try {
    dataFileUrl = dataFileIsUrl ? (dataFileLoc ? toRemoteUrl(dataFileLoc) : null) : toLocalFileUrl(dataFileLoc)
}
catch (java.net.MalformedURLException e) {
    messages.add(e.getMessage())
}

definitionUrl = null
try {
    definitionUrl = definitionIsUrl ? (definitionLoc ? toRemoteUrl(definitionLoc) : null) : toLocalFileUrl(definitionLoc)
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
    try {
        dataFile.writeDataFile(dataFileSave)
        messages.add(uiLabelMap.WebtoolsDataFileSavedTo + dataFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}

if (dataFile && entityXmlFileSave) {
    try {
        //dataFile.writeDataFile(entityXmlFileSave)
        DataFile2EntityXml.writeToEntityXml(entityXmlFileSave, dataFile)
        messages.add(uiLabelMap.WebtoolsDataEntityFileSavedTo + entityXmlFileSave)
    }
    catch (Exception e) {
        messages.add(e.getMessage())
    }
}
context.messages = messages
