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

import org.apache.ofbiz.entity.GenericValue

/*
 * ================================================================
 * ProductPromoCond Services
 * ================================================================
 */

/**
 * Create a ProductPromoCond
 */
def createProductPromoCond() {
    GenericValue newEntity = makeValue("ProductPromoCond")
    newEntity.setNonPKFields(parameters)
    if (parameters.carrierShipmentMethod) {
        newEntity.otherValue = parameters.carrierShipmentMethod
    }
    newEntity.setPKFields(parameters)
    delegator.setNextSubSeqId(newEntity, "productPromoCondSeqId", 2, 1)
    Map result = success()
    result.productPromoCondSeqId = newEntity.productPromoCondSeqId
    newEntity.create()
    return result
}

/**
 * Update a ProductPromoCond
 */
def updateProductPromoCond() {
    GenericValue lookedUpValue = from("ProductPromoCond").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    if (parameters.carrierShipmentMethod) {
        lookedUpValue.otherValue = parameters.carrierShipmentMethod
    }
    lookedUpValue.store()
    return success()
}
