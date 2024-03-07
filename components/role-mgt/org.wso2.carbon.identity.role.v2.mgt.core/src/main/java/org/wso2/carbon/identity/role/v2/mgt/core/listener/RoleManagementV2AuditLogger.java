/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.role.v2.mgt.core.listener;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.role.v2.mgt.core.listener.utils.ListenerUtils;
import org.wso2.carbon.identity.role.v2.mgt.core.model.IdpGroup;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Permission;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.model.UserBasicInfo;
import org.wso2.carbon.utils.AuditLog;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ADDED_GROUPS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ADDED_IDP_GROUPS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ADDED_PERMISSIONS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ADD_ROLE_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.AUDIENCE_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.DELETED_GROUPS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.DELETED_IDP_GROUPS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.DELETED_PERMISSIONS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.DELETED_USERS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.DELETE_ROLE_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.GET_ROLES_OF_USER_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.GET_USERS_OF_ROLE_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.GROUPS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.NEW_USERS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.PERMISSIONS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ROLES_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.ROLE_NAME_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.UPDATE_GROUPS_OF_ROLE_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.UPDATE_IDP_GROUPS_OF_ROLES_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.UPDATE_PERMISSIONS_OF_ROLES_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.UPDATE_ROLE_NAME_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.UPDATE_USERS_OF_ROLE_ACTION;
import static org.wso2.carbon.identity.central.log.mgt.utils.LogConstants.UserManagement.USERS_FIELD;
import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.isEnableV2AuditLogs;
import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.jsonObjectToMap;
import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.triggerAuditLogEvent;
import static org.wso2.carbon.identity.role.v2.mgt.core.listener.utils.ListenerUtils.getInitiatorId;

/**
 * This v2 audit logger logs the RoleManagement related operations.
 */
public class RoleManagementV2AuditLogger extends AbstractRoleManagementListener {

    @Override
    public int getExecutionOrderId() {

        return 0;
    }

    @Override
    public int getDefaultOrderId() {

        return 0;
    }

    @Override
    public boolean isEnable() {
        return isEnableV2AuditLogs();
    }

    @Override
    public void postAddRole(RoleBasicInfo roleBasicInfo, String roleName, List<String> userList, List<String> groupList,
                            List<Permission> permissions, String audience, String audienceId, String tenantDomain) {

        if (isEnable()) {
            JSONObject data = new JSONObject();
            String roleId = roleBasicInfo.getId();
            data.put(ROLE_NAME_FIELD, roleName);
            data.put(AUDIENCE_FIELD, audience);
            if (ArrayUtils.isNotEmpty(permissions.toArray())) {
                ArrayList<String> permissionsList = new ArrayList<>();
                for (Permission permission : permissions) {
                    permissionsList.add(permission.getName());
                }
                data.put(PERMISSIONS_FIELD, new JSONArray(permissionsList));
            }
            if (ArrayUtils.isNotEmpty(userList.toArray())) {
                data.put(USERS_FIELD, new JSONArray(userList));
            }
            if (ArrayUtils.isNotEmpty(groupList.toArray())) {
                data.put(GROUPS_FIELD, new JSONArray(groupList));
            }
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), ADD_ROLE_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postUpdateRoleName(String roleId, String newRoleName, String tenantDomain) {

        JSONObject data = new JSONObject();
        data.put(ROLE_NAME_FIELD, newRoleName);
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), UPDATE_ROLE_NAME_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }


    @Override
    public void postDeleteRole(String roleId, String tenantDomain) {

        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), DELETE_ROLE_ACTION);
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postGetUserListOfRole(List<UserBasicInfo> userBasicInfoList, String roleId, String tenantDomain) {

        JSONObject data = new JSONObject();
        if (ArrayUtils.isNotEmpty(userBasicInfoList.toArray())) {
            ArrayList<String> usersList = new ArrayList<>();
            for (UserBasicInfo user : userBasicInfoList) {
                usersList.add(user.getId());
            }
            data.put(USERS_FIELD, new JSONArray(usersList));
        }
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), GET_USERS_OF_ROLE_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postUpdateUserListOfRole(String roleId, List<String> newUserIDList, List<String> deletedUserIDList,
                                         String tenantDomain) {

        JSONObject data = new JSONObject();
        if (ArrayUtils.isNotEmpty(newUserIDList.toArray())) {
            data.put(NEW_USERS_FIELD, new JSONArray(newUserIDList));
        }
        if (ArrayUtils.isNotEmpty(deletedUserIDList.toArray())) {
            data.put(DELETED_USERS_FIELD, new JSONArray(deletedUserIDList));
        }
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), UPDATE_USERS_OF_ROLE_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postUpdateGroupListOfRole(String roleId, List<String> newGroupIDList, List<String> deletedGroupIDList,
                                          String tenantDomain) {

        JSONObject data = new JSONObject();
        if (ArrayUtils.isNotEmpty(newGroupIDList.toArray())) {
            data.put(ADDED_GROUPS_FIELD, new JSONArray(newGroupIDList));
        }
        if (ArrayUtils.isNotEmpty(deletedGroupIDList.toArray())) {
            data.put(DELETED_GROUPS_FIELD, new JSONArray(deletedGroupIDList));
        }
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), UPDATE_GROUPS_OF_ROLE_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postUpdateIdpGroupListOfRole(String roleId, List<IdpGroup> newGroupIDList,
                                             List<IdpGroup> deletedGroupIDList, String tenantDomain) {

        JSONObject data = new JSONObject();
        if (ArrayUtils.isNotEmpty(newGroupIDList.toArray())) {
            ArrayList<String> newIdpGroupsList = new ArrayList<>();
            for (IdpGroup idpGroup : newGroupIDList) {
                newIdpGroupsList.add(idpGroup.getGroupId());
            }
            data.put(ADDED_IDP_GROUPS_FIELD, new JSONArray(newIdpGroupsList));
        }
        if (ArrayUtils.isNotEmpty(deletedGroupIDList.toArray())) {
            ArrayList<String> deletedIdpGroupList = new ArrayList<>();
            for (IdpGroup idpGroup : deletedGroupIDList) {
                deletedIdpGroupList.add(idpGroup.getGroupId());
            }
            data.put(DELETED_IDP_GROUPS_FIELD, new JSONArray(deletedIdpGroupList));
        }
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId,
                    LoggerUtils.Target.Role.name(), UPDATE_IDP_GROUPS_OF_ROLES_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }

    @Override
    public void postUpdatePermissionsForRole(String roleId, List<Permission> addedPermissions,
                                             List<Permission> deletedPermissions, String audience, String audienceId,
                                             String tenantDomain) {

        JSONObject data = new JSONObject();
        if (ArrayUtils.isNotEmpty(addedPermissions.toArray())) {
            ArrayList<String> addedPermissionsList = new ArrayList<>();
            for (Permission permission : addedPermissions) {
                addedPermissionsList.add(permission.getName());
            }
            data.put(ADDED_PERMISSIONS_FIELD, new JSONArray(addedPermissionsList));
        }
        if (ArrayUtils.isNotEmpty(deletedPermissions.toArray())) {
            ArrayList<String> deletedPermissionsList = new ArrayList<>();
            for (Permission permission : addedPermissions) {
                deletedPermissionsList.add(permission.getName());
            }
            data.put(DELETED_PERMISSIONS_FIELD, new JSONArray(deletedPermissionsList));
        }
        data.put(AUDIENCE_FIELD, audience);
        if (isEnable()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(), LoggerUtils.getInitiatorType(getInitiatorId()), roleId, LoggerUtils.Target.Role.name(),
                    UPDATE_PERMISSIONS_OF_ROLES_ACTION).data(jsonObjectToMap(data));
            triggerAuditLogEvent(auditLogBuilder);
        }
    }
}
