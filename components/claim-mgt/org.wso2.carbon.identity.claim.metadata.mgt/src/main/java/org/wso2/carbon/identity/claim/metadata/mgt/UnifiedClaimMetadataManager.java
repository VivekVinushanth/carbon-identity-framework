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

package org.wso2.carbon.identity.claim.metadata.mgt;

import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataClientException;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.internal.ReadOnlyClaimMetadataManager;
import org.wso2.carbon.identity.claim.metadata.mgt.internal.ReadWriteClaimMetadataManager;
import org.wso2.carbon.identity.claim.metadata.mgt.model.AttributeMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.model.Claim;
import org.wso2.carbon.identity.claim.metadata.mgt.model.ClaimDialect;
import org.wso2.carbon.identity.claim.metadata.mgt.model.ExternalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.ErrorMessage.ERROR_CODE_NON_EXISTING_LOCAL_CLAIM_URI;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.ErrorMessage.ERROR_CODE_NO_DELETE_SYSTEM_CLAIM;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.ErrorMessage.ERROR_CODE_NO_DELETE_SYSTEM_DIALECT;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.ErrorMessage.ERROR_CODE_NO_RENAME_SYSTEM_DIALECT;

/**
 * Unified claim metadata manager.
 *
 * This class provides a unified view of claim metadata from the system default claim metadata manager and the
 * database-based claim metadata manager.
 */
public class UnifiedClaimMetadataManager implements ReadWriteClaimMetadataManager {

    private final ReadOnlyClaimMetadataManager systemDefaultClaimMetadataManager =
            new SystemDefaultClaimMetadataManager();
    private final ReadWriteClaimMetadataManager dbBasedClaimMetadataManager = new DBBasedClaimMetadataManager();

    /**
     * Get all claim dialects.
     *
     * @param tenantId Tenant ID.
     * @return List of claim dialects.
     * @throws ClaimMetadataException If an error occurs while retrieving claim dialects.
     */
    public List<ClaimDialect> getClaimDialects(int tenantId) throws ClaimMetadataException {

        List<ClaimDialect> claimDialectsInDB = this.dbBasedClaimMetadataManager.getClaimDialects(tenantId);
        List<ClaimDialect> claimDialectsInSystem = this.systemDefaultClaimMetadataManager.getClaimDialects(tenantId);
        Set<String> claimDialectURIsInDB = claimDialectsInDB.stream()
                .map(ClaimDialect::getClaimDialectURI)
                .collect(Collectors.toSet());

        List<ClaimDialect> allClaimDialects = new ArrayList<>(claimDialectsInDB);
        claimDialectsInSystem.stream()
                .filter(claimDialect -> !claimDialectURIsInDB.contains(claimDialect.getClaimDialectURI()))
                .forEach(allClaimDialects::add);
        return allClaimDialects;
    }

    /**
     * Get a claim dialect by URI.
     *
     * @param claimDialectURI Claim dialect URI.
     * @param tenantId        Tenant ID.
     * @return Claim dialect.
     * @throws ClaimMetadataException If an error occurs while retrieving claim dialect.
     */
    public Optional<ClaimDialect> getClaimDialect(String claimDialectURI, int tenantId) throws ClaimMetadataException {

        Optional<ClaimDialect> claimDialectInDB = this.dbBasedClaimMetadataManager.getClaimDialect(claimDialectURI, tenantId);
        if (claimDialectInDB.isPresent()) {
            return claimDialectInDB;
        }
        return this.systemDefaultClaimMetadataManager.getClaimDialect(claimDialectURI, tenantId);
    }

    /**
     * Add a claim dialect.
     *
     * @param claimDialect Claim dialect.
     * @param tenantId     Tenant ID.
     * @throws ClaimMetadataException If an error occurs while adding claim dialect.
     */
    public void addClaimDialect(ClaimDialect claimDialect, int tenantId) throws ClaimMetadataException {

        this.dbBasedClaimMetadataManager.addClaimDialect(claimDialect, tenantId);
    }

    /**
     * Rename a claim dialect.
     *
     * @param oldClaimDialect Old claim dialect.
     * @param newClaimDialect New claim dialect.
     * @param tenantId        Tenant ID.
     * @throws ClaimMetadataException If an error occurs while renaming claim dialect.
     */
    public void renameClaimDialect(ClaimDialect oldClaimDialect, ClaimDialect newClaimDialect, int tenantId)
            throws ClaimMetadataException {

        boolean isSystemDefaultClaimDialect = isSystemDefaultClaimDialect(oldClaimDialect.getClaimDialectURI(),
                tenantId);
        if (isSystemDefaultClaimDialect) {
            throw new ClaimMetadataClientException(ERROR_CODE_NO_RENAME_SYSTEM_DIALECT.getCode(),
                    String.format(ERROR_CODE_NO_RENAME_SYSTEM_DIALECT.getMessage(),
                            oldClaimDialect.getClaimDialectURI()));
        }

        this.dbBasedClaimMetadataManager.renameClaimDialect(oldClaimDialect, newClaimDialect, tenantId);
    }

    /**
     * Remove a claim dialect.
     *
     * @param claimDialect Claim dialect.
     * @param tenantId     Tenant ID.
     * @throws ClaimMetadataException If an error occurs while removing claim dialect.
     */
    public void removeClaimDialect(ClaimDialect claimDialect, int tenantId) throws ClaimMetadataException {

        boolean isSystemDefaultClaimDialect = isSystemDefaultClaimDialect(claimDialect.getClaimDialectURI(), tenantId);
        if (isSystemDefaultClaimDialect) {
            throw new ClaimMetadataClientException(ERROR_CODE_NO_DELETE_SYSTEM_DIALECT.getCode(),
                    String.format(ERROR_CODE_NO_DELETE_SYSTEM_DIALECT.getMessage(), claimDialect.getClaimDialectURI()));
        }

        this.dbBasedClaimMetadataManager.removeClaimDialect(claimDialect, tenantId);
    }

    /**
     * Get all local claims.
     *
     * @param tenantId Tenant ID.
     * @return List of local claims.
     * @throws ClaimMetadataException If an error occurs while retrieving local claims.
     */
    public List<LocalClaim> getLocalClaims(int tenantId) throws ClaimMetadataException {

        List<LocalClaim> localClaimsInSystem = this.systemDefaultClaimMetadataManager.getLocalClaims(tenantId);
        List<LocalClaim> localClaimsInDB = this.dbBasedClaimMetadataManager.getLocalClaims(tenantId);

        List<LocalClaim> allLocalClaims = new ArrayList<>(localClaimsInDB);
        localClaimsInSystem.forEach(systemClaim -> {
            Optional<LocalClaim> matchingClaimInDB = allLocalClaims.stream()
                    .filter(dbClaim -> dbClaim.getClaimURI().equals(systemClaim.getClaimURI()))
                    .findFirst();

            if (matchingClaimInDB.isPresent()) {
                matchingClaimInDB.get().setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
            } else {
                systemClaim.setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
                allLocalClaims.add(systemClaim);
            }
        });

        return allLocalClaims;
    }

    /**
     * Get a local claim by URI.
     *
     * @param localClaimURI Local claim URI.
     * @param tenantId      Tenant ID.
     * @return Local claim.
     * @throws ClaimMetadataException If an error occurs while retrieving local claim.
     */
    public Optional<LocalClaim> getLocalClaim(String localClaimURI, int tenantId) throws ClaimMetadataException {

        Optional<LocalClaim> localClaimInDB = this.dbBasedClaimMetadataManager.getLocalClaim(localClaimURI, tenantId);
        if (localClaimInDB.isPresent()) {
            if (isSystemDefaultLocalClaim(localClaimURI, tenantId)) {
                localClaimInDB.get().setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
            }
            return localClaimInDB;
        }
        Optional<LocalClaim> localClaimInSystem = this.systemDefaultClaimMetadataManager.getLocalClaim(localClaimURI, tenantId);
        if (localClaimInSystem.isPresent()) {
            localClaimInSystem.get().setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
            return localClaimInSystem;
        }
        return Optional.empty();
    }

    /**
     * Add a local claim.
     *
     * @param localClaim Local claim.
     * @param tenantId   Tenant ID.
     * @throws ClaimMetadataException If an error occurs while adding local claim.
     */
    public void addLocalClaim(LocalClaim localClaim, int tenantId) throws ClaimMetadataException {

        localClaim.getClaimProperties().remove(ClaimConstants.IS_SYSTEM_CLAIM);
        if (!isClaimDialectInDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId)) {
            addSystemDefaultDialectToDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId);
        }
        this.dbBasedClaimMetadataManager.addLocalClaim(localClaim, tenantId);
    }

    /**
     * Update a local claim.
     *
     * @param localClaim Local claim.
     * @param tenantId   Tenant ID.
     * @throws ClaimMetadataException If an error occurs while updating local claim.
     */
    public void updateLocalClaim(LocalClaim localClaim, int tenantId) throws ClaimMetadataException {

        localClaim.getClaimProperties().remove(ClaimConstants.IS_SYSTEM_CLAIM);
        if (isLocalClaimInDB(localClaim.getClaimURI(), tenantId)) {
            this.dbBasedClaimMetadataManager.updateLocalClaim(localClaim, tenantId);
        } else {
            this.addLocalClaim(localClaim, tenantId);
        }
    }

    /**
     * Update local claim mappings.
     *
     * @param localClaimList  List of local claims.
     * @param tenantId        Tenant ID.
     * @param userStoreDomain User store domain.
     * @throws ClaimMetadataException If an error occurs while updating local claim mappings.
     */
    public void updateLocalClaimMappings(List<LocalClaim> localClaimList, int tenantId, String userStoreDomain)
            throws ClaimMetadataException {

        if (localClaimList == null) {
            return;
        }
        if (!localClaimList.isEmpty() && !isClaimDialectInDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId)) {
            addSystemDefaultDialectToDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId);
        }

        Map<String, LocalClaim> localClaimMap = this.getLocalClaims(tenantId).stream()
                .collect(Collectors.toMap(LocalClaim::getClaimURI, localClaim -> localClaim));
        for (LocalClaim localClaim : localClaimList) {
            if (localClaimMap.get(localClaim.getClaimURI()) == null) {
                throw new ClaimMetadataClientException(ERROR_CODE_NON_EXISTING_LOCAL_CLAIM_URI.getCode(),
                        String.format(ERROR_CODE_NON_EXISTING_LOCAL_CLAIM_URI.getMessage(), localClaim.getClaimURI()));
            }
            List<AttributeMapping> missingMappedAttributes = localClaimMap.get(localClaim.getClaimURI())
                    .getMappedAttributes().stream()
                    .filter(mappedAttribute -> !mappedAttribute.getUserStoreDomain().equals(userStoreDomain))
                    .collect(Collectors.toList());
            localClaim.getMappedAttributes().addAll(missingMappedAttributes);
            localClaim.setClaimProperties(localClaimMap.get(localClaim.getClaimURI()).getClaimProperties());
        }
        this.dbBasedClaimMetadataManager.updateLocalClaimMappings(localClaimList, tenantId, userStoreDomain);
    }

    /**
     * Remove a local claim.
     *
     * @param localClaimURI Local claim URI.
     * @param tenantId      Tenant ID.
     * @throws ClaimMetadataException If an error occurs while removing local claim.
     */
    public void removeLocalClaim(String localClaimURI, int tenantId) throws ClaimMetadataException {

        boolean isSystemDefaultClaim = isSystemDefaultLocalClaim(localClaimURI, tenantId);
        if (isSystemDefaultClaim) {
            throw new ClaimMetadataClientException(ERROR_CODE_NO_DELETE_SYSTEM_CLAIM.getCode(),
                    String.format(ERROR_CODE_NO_DELETE_SYSTEM_CLAIM.getMessage(), localClaimURI));
        }

        this.dbBasedClaimMetadataManager.removeLocalClaim(localClaimURI, tenantId);
    }

    /**
     * Get all external claims.
     *
     * @param externalClaimDialectURI External claim dialect URI.
     * @param tenantId                Tenant ID.
     * @return List of external claims.
     * @throws ClaimMetadataException If an error occurs while retrieving external claims.
     */
    public List<ExternalClaim> getExternalClaims(String externalClaimDialectURI, int tenantId)
            throws ClaimMetadataException {

        List<ExternalClaim> externalClaimsInSystem = this.systemDefaultClaimMetadataManager.getExternalClaims(
                externalClaimDialectURI, tenantId);
        List<ExternalClaim> externalClaimsInDB = this.dbBasedClaimMetadataManager.getExternalClaims(
                externalClaimDialectURI, tenantId);

        Map<String, ExternalClaim> externalClaimsInDBMap = externalClaimsInDB.stream()
                .collect(Collectors.toMap(ExternalClaim::getClaimURI, claim -> claim));

        List<ExternalClaim> allExternalClaims = new ArrayList<>();
        for (ExternalClaim externalClaimInSystem : externalClaimsInSystem) {
            ExternalClaim matchingClaimInDB = externalClaimsInDBMap.get(externalClaimInSystem.getClaimURI());
            if (matchingClaimInDB != null) {
                matchingClaimInDB.setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
                allExternalClaims.add(matchingClaimInDB);
                externalClaimsInDBMap.remove(externalClaimInSystem.getClaimURI());
            } else {
                externalClaimInSystem.setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
                allExternalClaims.add(externalClaimInSystem);
            }
        }
        allExternalClaims.addAll(externalClaimsInDBMap.values());
        return allExternalClaims;
    }

    /**
     * Get an external claim by URI.
     *
     * @param externalClaimDialectURI External claim dialect URI.
     * @param claimURI                Claim URI.
     * @param tenantId                Tenant ID.
     * @return External claim.
     * @throws ClaimMetadataException If an error occurs while retrieving external claim.
     */
    public Optional<ExternalClaim> getExternalClaim(String externalClaimDialectURI, String claimURI, int tenantId)
            throws ClaimMetadataException {

        Optional<ExternalClaim> externalClaim = this.dbBasedClaimMetadataManager.getExternalClaim(
                externalClaimDialectURI, claimURI, tenantId);
        if (externalClaim.isPresent()) {
            if (isSystemDefaultExternalClaim(externalClaimDialectURI, claimURI, tenantId)) {
                externalClaim.get().setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
            }
            return externalClaim;
        }
        Optional<ExternalClaim> externalClaimInSystem = this.systemDefaultClaimMetadataManager.getExternalClaim(
                externalClaimDialectURI, claimURI, tenantId);
        if (externalClaimInSystem.isPresent()) {
            externalClaimInSystem.get().setClaimProperty(ClaimConstants.IS_SYSTEM_CLAIM, Boolean.TRUE.toString());
            return externalClaimInSystem;
        }
        return Optional.empty();
    }

    /**
     * Add an external claim.
     *
     * @param externalClaim External claim.
     * @param tenantId      Tenant ID.
     * @throws ClaimMetadataException If an error occurs while adding external claim.
     */
    public void addExternalClaim(ExternalClaim externalClaim, int tenantId)
            throws ClaimMetadataException {

        externalClaim.getClaimProperties().remove(ClaimConstants.IS_SYSTEM_CLAIM);
        if (!isClaimDialectInDB(externalClaim.getClaimDialectURI(), tenantId)) {
            addSystemDefaultDialectToDB(externalClaim.getClaimDialectURI(), tenantId);
        }
        if (!isLocalClaimInDB(externalClaim.getMappedLocalClaim(), tenantId)) {
            addSystemDefaultLocalClaimToDB(externalClaim.getMappedLocalClaim(), tenantId);
        }
        this.dbBasedClaimMetadataManager.addExternalClaim(externalClaim, tenantId);
    }

    /**
     * Update an external claim.
     *
     * @param externalClaim External claim.
     * @param tenantId      Tenant ID.
     * @throws ClaimMetadataException If an error occurs while updating external claim.
     */
    public void updateExternalClaim(ExternalClaim externalClaim, int tenantId)
            throws ClaimMetadataException {

        externalClaim.getClaimProperties().remove(ClaimConstants.IS_SYSTEM_CLAIM);
        if (!isLocalClaimInDB(externalClaim.getMappedLocalClaim(), tenantId)) {
            addSystemDefaultLocalClaimToDB(externalClaim.getMappedLocalClaim(), tenantId);
        }
        if (isExternalClaimInDB(externalClaim.getClaimURI(), externalClaim.getClaimDialectURI(), tenantId)) {
            this.dbBasedClaimMetadataManager.updateExternalClaim(externalClaim, tenantId);
        } else {
            this.addExternalClaim(externalClaim, tenantId);
        }
    }

    /**
     * Remove an external claim.
     *
     * @param externalClaimDialectURI External claim dialect URI.
     * @param externalClaimURI        External claim URI.
     * @param tenantId                Tenant ID.
     * @throws ClaimMetadataException If an error occurs while removing external claim.
     */
    public void removeExternalClaim(String externalClaimDialectURI, String externalClaimURI, int tenantId)
            throws ClaimMetadataException {

        boolean isSystemDefaultClaim = isSystemDefaultExternalClaim(externalClaimDialectURI, externalClaimURI, tenantId);
        if (isSystemDefaultClaim) {
            throw new ClaimMetadataClientException(ERROR_CODE_NO_DELETE_SYSTEM_CLAIM.getCode(),
                    String.format(ERROR_CODE_NO_DELETE_SYSTEM_CLAIM.getMessage(), externalClaimURI));
        }

        this.dbBasedClaimMetadataManager.removeExternalClaim(externalClaimDialectURI, externalClaimURI, tenantId);
    }

    /**
     * Check whether any external claim maps to a given local claim.
     * @param localClaimURI Local claim URI.
     * @param tenantId      Tenant ID.
     * @return True if the local claim is mapped.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    public boolean isMappedLocalClaim(String localClaimURI, int tenantId) throws ClaimMetadataException {

        List<ClaimDialect> claimDialects = this.getClaimDialects(tenantId);

        for (ClaimDialect claimDialect : claimDialects) {
            if (ClaimConstants.LOCAL_CLAIM_DIALECT_URI.equals(claimDialect.getClaimDialectURI())) {
                continue;
            }
            List<ExternalClaim> externalClaims = getExternalClaims(claimDialect.getClaimDialectURI(), tenantId);
            for (ExternalClaim externalClaim : externalClaims) {
                if (externalClaim.getMappedLocalClaim().equals(localClaimURI)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove mapped user store attributes of a user store domain.
     * @param tenantId        Tenant ID.
     * @param userstoreDomain User Store Domain name.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    public void removeClaimMappingAttributes(int tenantId, String userstoreDomain) throws ClaimMetadataException {

        this.dbBasedClaimMetadataManager.removeClaimMappingAttributes(tenantId, userstoreDomain);
    }

    /**
     * Remove all claim dialects.
     * @param tenantId  Tenant ID.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    public void removeAllClaimDialects(int tenantId) throws ClaimMetadataException {

        this.dbBasedClaimMetadataManager.removeAllClaimDialects(tenantId);
    }

    /**
     * Get all external claims mapped to a local claim.
     * @param localClaimURI Local claim URI.
     * @param tenantId      Tenant ID.
     * @return List of mapped external claims.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    public List<Claim> getMappedExternalClaims(String localClaimURI, int tenantId) throws ClaimMetadataException {

        List<Claim> mappedExternalClaims = new ArrayList<>();
        List<ClaimDialect> claimDialects = getClaimDialects(tenantId);
        for (ClaimDialect claimDialect : claimDialects) {
            if (ClaimConstants.LOCAL_CLAIM_DIALECT_URI.equals(claimDialect.getClaimDialectURI())) {
                continue;
            }
            List<ExternalClaim> externalClaimsInDialect = getExternalClaims(claimDialect.getClaimDialectURI(),
                    tenantId);
            for (ExternalClaim externalClaim : externalClaimsInDialect) {
                if (externalClaim.getMappedLocalClaim().equals(localClaimURI)) {
                    mappedExternalClaims.add(externalClaim);
                }
            }
        }
        return mappedExternalClaims;
    }

    /**
     * Check whether a local claim is mapped within a dialect.
     * @param mappedLocalClaim         Mapped local claim.
     * @param externalClaimDialectURI  External claim dialect URI.
     * @param tenantId                 Tenant ID.
     * @return True if the local claim is mapped.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    public boolean isLocalClaimMappedWithinDialect(String mappedLocalClaim, String externalClaimDialectURI,
                                                   int tenantId) throws ClaimMetadataException {

        return getExternalClaims(externalClaimDialectURI, tenantId).stream()
                .anyMatch(externalClaim -> externalClaim.getMappedLocalClaim().equals(mappedLocalClaim));
    }

    /**
     * Check whether a claim dialect is a system default claim dialect.
     * @param claimDialectURI Claim dialect URI.
     * @param tenantId        Tenant ID.
     * @return True if the claim dialect is a system default claim dialect.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    private boolean isSystemDefaultClaimDialect(String claimDialectURI, int tenantId) throws ClaimMetadataException {

        return this.systemDefaultClaimMetadataManager.getClaimDialect(claimDialectURI, tenantId).isPresent();
    }

    /**
     * Check whether a local claim is a system default local claim.
     * @param localClaimURI Local claim URI.
     * @param tenantId      Tenant ID.
     * @return True if the local claim is a system default local claim.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    private boolean isSystemDefaultLocalClaim(String localClaimURI, int tenantId) throws ClaimMetadataException {

        return this.systemDefaultClaimMetadataManager.getLocalClaims(tenantId).stream()
                .anyMatch(localClaim -> localClaim.getClaimURI().equals(localClaimURI));
    }

    /**
     * Check whether an external claim is a system default external claim.
     * @param externalClaimDialectURI External claim dialect URI.
     * @param externalClaimURI        External claim URI.
     * @param tenantId                Tenant ID.
     * @return True if the external claim is a system default external claim.
     * @throws ClaimMetadataException if an error occurs during the operation.
     */
    private boolean isSystemDefaultExternalClaim(String externalClaimDialectURI, String externalClaimURI, int tenantId)
            throws ClaimMetadataException {

        return this.systemDefaultClaimMetadataManager.getExternalClaims(externalClaimDialectURI,tenantId).stream()
                .anyMatch(externalClaim -> externalClaim.getClaimURI().equals(externalClaimURI));
    }

    private boolean isClaimDialectInDB(String claimDialectURI, int tenantId) throws ClaimMetadataException {

        return this.dbBasedClaimMetadataManager.getClaimDialect(claimDialectURI, tenantId).isPresent();
    }

    private boolean isLocalClaimInDB(String localClaimURI, int tenantId) throws ClaimMetadataException {

        return this.dbBasedClaimMetadataManager.getLocalClaim(localClaimURI, tenantId).isPresent();
    }

    private boolean isExternalClaimInDB(String claimURI, String claimDialectURI, int tenantId)
            throws ClaimMetadataException {

        return this.dbBasedClaimMetadataManager.getExternalClaim(claimDialectURI, claimURI, tenantId).isPresent();
    }

    private void addSystemDefaultDialectToDB(String claimDialectURI, int tenantId) throws ClaimMetadataException {

        Optional<ClaimDialect> claimDialectInSystem = this.systemDefaultClaimMetadataManager
                .getClaimDialect(claimDialectURI, tenantId);
        if (claimDialectInSystem.isPresent()) {
            this.dbBasedClaimMetadataManager.addClaimDialect(claimDialectInSystem.get(), tenantId);
        }
    }

    private void addSystemDefaultLocalClaimToDB(String claimURI, int tenantId)
            throws ClaimMetadataException {

        boolean isClaimDialectInDB = isClaimDialectInDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId);
        if (!isClaimDialectInDB) {
            addSystemDefaultDialectToDB(ClaimConstants.LOCAL_CLAIM_DIALECT_URI, tenantId);
        }
        Optional<LocalClaim> claimInSystem = this.systemDefaultClaimMetadataManager.getLocalClaim(claimURI, tenantId);
        if (claimInSystem.isPresent()) {
            this.dbBasedClaimMetadataManager.addLocalClaim(claimInSystem.get(), tenantId);
        }
    }
}
