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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.jdbc.SQLProcessor
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.util.ArrayList
import java.util.Iterator
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.model.ModelGroupReader

module = "EntitySQLProcessor.groovy"

sqlCommand = context.request.getParameter("sqlCommand")
selGroup = context.selGroup
userLoginId = context.userLogin?.userLoginId

resultMessage = ""
rs = null
columns = []
records = []
groups = []

if (!security.hasPermission("ENTITY_MAINT", session)) {
    Debug.logWarning("Denied raw SQL execution for user [${userLoginId}] on group [${selGroup}]: missing ENTITY_MAINT permission", module)
    context.groups = groups
    context.resultMessage = "Not executed: ENTITY_MAINT permission required"
    context.columns = columns
    context.records = records
    context.sqlCommand = null
    return
}

mgr = delegator.getModelGroupReader()
for (String group : mgr.getGroupNames(delegator.getDelegatorName())) groups.add(0,["group":group]) //use for list-option in widget drop-down

if (sqlCommand && selGroup) {
    Debug.logInfo("User [${userLoginId}] executing raw SQL on group [${selGroup}]: ${sqlCommand}", module)
    du = new SQLProcessor(delegator, delegator.getGroupHelperInfo(selGroup))
    try {
        if (sqlCommand.toUpperCase().startsWith("SELECT")) {
            rs = du.executeQuery(sqlCommand)
            if (rs) {
                rsmd = rs.getMetaData()
                numberOfColumns = rsmd.getColumnCount()
                for (i = 1; i <= numberOfColumns; i++) {
                    columns.add(rsmd.getColumnLabel(i))
                }
                rowLimitReached = false
                while (rs.next()) {
                    if (records.size() >= rowLimit) {
                        resultMessage = "Returned top $rowLimit rows."
                        rowLimitReached = true
                        break
                    }
                    record = []
                    for (i = 1; i <= numberOfColumns; i++) {
                        record.add(rs.getObject(i))
                    }
                    records.add(record)
                }
                resultMessage = "Returned " + (rowLimitReached? "top " + rowLimit : "" + records.size()) + " rows."
                rs.close()
            }
        } else {
            if (sqlCommand.toUpperCase().contains("SYSCS_UTIL.SYSCS_EXPORT_TABLE")
                    || sqlCommand.toUpperCase().contains("JSP")) {
                context.resultMessage = "Not executed for security reason"
                context.groups = groups
                context.columns = columns
                context.records = records
                context.sqlCommand = sqlCommand
                return
            }
            du.prepareStatement(sqlCommand)
            numOfAffectedRows = du.executeUpdate()
            resultMessage = "Affected $numOfAffectedRows rows."
        }
    } catch (Exception exc) {
        resultMessage = exc.getMessage()
    }
}
context.groups = groups
context.resultMessage = resultMessage
context.columns = columns
context.records = records
context.sqlCommand = sqlCommand // (see OFBIZ-6567)
