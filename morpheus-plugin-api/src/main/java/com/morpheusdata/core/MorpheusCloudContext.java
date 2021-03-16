package com.morpheusdata.core;

import com.morpheusdata.model.*;
import com.morpheusdata.model.projection.NetworkDomainIdentityProjection;
import com.morpheusdata.model.projection.ReferenceDataSyncProjection;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface MorpheusCloudContext {

	void updateZoneStatus(Cloud cloud, String status, String message, Date syncDate);

	/**
	 *	Get the ssh credentials associated with an account
	 * @param account to lookup
	 * @return Morpheus KeyPair
	 */
	Single<KeyPair> findOrGenerateKeyPair(Account account);

	/**
	 *	Update Morpheus with an external reference to the KeyPair in your Cloud API.
	 * @param keyPair that was updated
	 * @param cloud associated with the credentials
	 * @return void
	 */
	Single<Void> updateKeyPair(KeyPair keyPair, Cloud cloud);

	Single<Container> getContainerById(Long id);

	Single<Cloud> getCloudById(Long id);

	Single<Map> buildContainerUserGroups(Account account, VirtualImage virtualImage, List<UserGroup> userGroups, User user, Map opts);

	Single<ReferenceData> save(ReferenceData referenceData);
	Single<ReferenceData> save(ReferenceData referenceData, Boolean flush);

	Single<ReferenceData> save(ReferenceData referenceData, Cloud cloud, String category);
	Single<Boolean> saveAll(List<ReferenceData> referenceData);
	Single<Boolean> saveAll(List<ReferenceData> referenceData, Cloud cloud, String category);
	Single<Boolean> removeMissingReferenceDataByIds(List<Long> longs);
	Observable<ReferenceDataSyncProjection> listReferenceDataByCategory(Cloud cloud, String code);
	Single<ReferenceData> findReferenceDataByExternalId(String externalId);
	Single<ReferenceData> listReferenceDataByExternalIds(List<Long> externalIds);
	Single<List<ReferenceData>> findReferenceDataByCategory(Cloud cloud, String category);

	Single<ComputeZonePool> save(ComputeZonePool pool, Cloud cloud, String category);
//	Single<Void> cacheResourcePools(List<ComputeZonePool> pools, Cloud cloud, String category);
	Single<List<ComputeZonePool>> readResourcePools(Cloud cloud, String category);

	Single<Void> updatePowerState(Long id, String state);

	Single<Void> updateInstanceStatus(List<Long> ids, Instance.Status status);

	Single<List<Long>> getStoppedContainerInstanceIds(Long containerId);

	Single<Instance> getInstance(ComputeServer server);

	Single<Container> getContainer(ComputeServer server);
}
