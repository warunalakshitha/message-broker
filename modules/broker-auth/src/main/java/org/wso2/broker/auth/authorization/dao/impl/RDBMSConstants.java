/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.auth.authorization.dao.impl;

/**
 * Constants related to permission store database.
 */
final class RDBMSConstants {

    private RDBMSConstants() {
    }

    // Resource related sql queries
    static final String PS_INSERT_RESOURCE_PERMISSION = "INSERT INTO MB_RESOURCE_PERMISSION (RESOURCE_GROUP_NAME, "
            + "RESOURCE_NAME, USER_GROUP_ID, PERMISSION) VALUES(?, ?, ?, ?)";

    static final String PS_UPDATE_RESOURCE_PERMISSION = "UPDATE MB_RESOURCE_PERMISSION SET PERMISSION = ? WHERE"
            + " RESOURCE_GROUP_NAME = ? AND RESOURCE_NAME=? AND USER_GROUP_ID=?";

    static final String PS_DELETE_RESOURCE_PERMISSION = "DELETE FROM MB_RESOURCE_PERMISSION WHERE "
            + "RESOURCE_GROUP_NAME = ? AND RESOURCE_NAME= ? ";

    static final String PS_SELECT_RESOURCE_PERMISSION =
            "SELECT USER_GROUP_ID, PERMISSION FROM MB_RESOURCE_PERMISSION WHERE RESOURCE_GROUP_NAME= ? AND "
                    + "RESOURCE_NAME = ?";

    static final String PS_SELECT_RESOURCE_PERMISSION_BY_GROUP =
            "SELECT RESOURCE_NAME, USER_GROUP_ID, PERMISSION FROM MB_RESOURCE_PERMISSION WHERE RESOURCE_GROUP_NAME = "
                    + "? ORDER BY RESOURCE_NAME";

    static final String PS_SELECT_RESOURCE_PERMISSION_ALL =
            "SELECT RESOURCE_GROUP_NAME, RESOURCE_NAME, USER_GROUP_ID, PERMISSION FROM MB_RESOURCE_PERMISSION ORDER "
                    + "BY RESOURCE_GROUP_NAME, RESOURCE_NAME";

    // Resource group related sql queries
    static final String PS_INSERT_RESOURCE_GROUP_PERMISSION = "INSERT INTO MB_RESOURCE_GROUP_PERMISSION "
            + "(RESOURCE_GROUP_NAME, USER_GROUP_ID, PERMISSION) VALUES(?, ?, ?)";

    static final String PS_UPDATE_RESOURCE_GROUP_PERMISSION = "UPDATE MB_RESOURCE_GROUP_PERMISSION SET PERMISSION = ? "
            + "WHERE RESOURCE_GROUP_NAME = ? AND USER_GROUP_ID= ?";

    static final String PS_DELETE_RESOURCE_GROUP_PERMISSION = "DELETE FROM MB_RESOURCE_GROUP_PERMISSION WHERE "
            + "RESOURCE_GROUP_NAME = ?";

    static final String PS_SELECT_RESOURCE_GROUP_PERMISSION = "SELECT USER_GROUP_ID, PERMISSION FROM "
            + "MB_RESOURCE_GROUP_PERMISSION  WHERE RESOURCE_GROUP_NAME = ?";

    static final String PS_SELECT_RESOURCE_GROUP_PERMISSION_ALL = "SELECT RESOURCE_GROUP_NAME, USER_GROUP_ID, "
            + "PERMISSION FROM MB_RESOURCE_GROUP_PERMISSION";
}
