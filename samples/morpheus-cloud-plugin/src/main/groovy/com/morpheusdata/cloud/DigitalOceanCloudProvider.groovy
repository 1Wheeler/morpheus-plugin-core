package com.morpheusdata.cloud

import com.morpheusdata.core.BackupProvider
import com.morpheusdata.core.CloudProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.ProvisioningProvider
import com.morpheusdata.model.*
import com.morpheusdata.response.ServiceResponse
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.reactivex.observables.ConnectableObservable
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

@Slf4j
class DigitalOceanCloudProvider implements CloudProvider {
	Plugin plugin
	MorpheusContext morpheusContext
	DigitalOceanApiService apiService

	DigitalOceanCloudProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheusContext = context
		apiService = new DigitalOceanApiService()
	}

	@Override
	MorpheusContext getMorpheusContext() {
		return this.morpheusContext
	}

	@Override
	Plugin getPlugin() {
		return this.plugin
	}

	@Override
	String getProviderCode() {
		return 'digital-ocean-plugin'
	}

	@Override
	String getProviderName() {
		return 'Digital Ocean Plugin'
	}

	@Override
	Collection<OptionType> getOptionTypes() {
		OptionType ot1 = new OptionType(
				name: 'Username',
				code: 'do-username',
				fieldName: 'doUsername',
				displayOrder: 0,
				fieldLabel: 'Username',
				required: true,
				inputType: OptionType.InputType.TEXT
		)
		OptionType ot2 = new OptionType(
				name: 'API Key',
				code: 'do-api-key',
				fieldName: 'doApiKey',
				displayOrder: 1,
				fieldLabel: 'API Key',
				required: true,
				inputType: OptionType.InputType.PASSWORD
		)
		OptionType ot3 = new OptionType(
				name: 'Datacenter',
				code: 'do-datacenter',
				fieldName: 'datacenter',
				optionSource: 'loadDatacenters',
				displayOrder: 2,
				fieldLabel: 'Datacenter',
				required: true,
				inputType: OptionType.InputType.SELECT,
				dependsOn: 'do-api-key'
		)
		return [ot1, ot2, ot3]
	}

	@Override
	Collection<ComputeServerType> getComputeServerTypes() {
		//digital ocean
		def serverTypes = [
				new ComputeServerType(code: 'digitalOceanWindows2', name: 'DigitalOcean Windows Node', description: '', platform: PlatformType.windows, agentType: ComputeServerType.AgentType.guest,
						enabled: true, selectable: false, externalDelete: true, managed: true, controlPower: true, controlSuspend: false, creatable: false, computeService: 'digitalOceanComputeService',
						displayOrder: 17, hasAutomation: true, reconfigureSupported: true,
						containerHypervisor: true, bareMetalHost: false, vmHypervisor: false, guestVm: true,
				),

				new ComputeServerType(code: 'digitalOceanVm2', name: 'DigitalOcean VM Instance', description: '', platform: PlatformType.linux,
						enabled: true, selectable: false, externalDelete: true, managed: true, controlPower: true, controlSuspend: false, creatable: false, computeService: 'digitalOceanComputeService',
						displayOrder: 0, hasAutomation: true, reconfigureSupported: true,
						containerHypervisor: false, bareMetalHost: false, vmHypervisor: false, agentType: ComputeServerType.AgentType.guest, guestVm: true,
				),

				//docker
				new ComputeServerType(code: 'digitalOceanLinux2', name: 'DigitalOcean Docker Host', description: '', platform: PlatformType.linux,
						enabled: true, selectable: false, externalDelete: true, managed: true, controlPower: true, controlSuspend: false, creatable: true, computeService: 'digitalOceanComputeService',
						displayOrder: 16, hasAutomation: true, reconfigureSupported: true,
						containerHypervisor: true, bareMetalHost: false, vmHypervisor: false, agentType: ComputeServerType.AgentType.host, clusterType: ComputeServerType.ClusterType.docker,
						computeTypeCode: 'docker-host',
				),

				//kubernetes
				new ComputeServerType(code: 'digitalOceanKubeMaster2', name: 'Digital Ocean Kubernetes Master', description: '', platform: PlatformType.linux,
						reconfigureSupported: true, enabled: true, selectable: false, externalDelete: true, managed: true, controlPower: true, controlSuspend: true, creatable: true,
						supportsConsoleKeymap: true, computeService: 'digitalOceanComputeService', displayOrder: 10,
						hasAutomation: true, containerHypervisor: true, bareMetalHost: false, vmHypervisor: false, agentType: ComputeServerType.AgentType.host, clusterType: ComputeServerType.ClusterType.kubernetes,
						computeTypeCode: 'kube-master',
						optionTypes: [

						]
				),
				new ComputeServerType(code: 'digitalOceanKubeWorker2', name: 'Digital Ocean Kubernetes Worker', description: '', platform: PlatformType.linux,
						reconfigureSupported: true, enabled: true, selectable: false, externalDelete: true, managed: true, controlPower: true, controlSuspend: true, creatable: true,
						supportsConsoleKeymap: true, computeService: 'digitalOceanComputeService', displayOrder: 10,
						hasAutomation: true, containerHypervisor: true, bareMetalHost: false, vmHypervisor: false, agentType: ComputeServerType.AgentType.host, clusterType: ComputeServerType.ClusterType.kubernetes,
						computeTypeCode: 'kube-worker',
						optionTypes: [

						]
				),
				//unmanaged discovered type
				new ComputeServerType(code: 'digitalOceanUnmanaged', name: 'Digital Ocean VM', description: 'Digital Ocean VM', platform: PlatformType.none, agentType: ComputeServerType.AgentType.guest,
						enabled: true, selectable: false, externalDelete: true, managed: false, controlPower: true, controlSuspend: false, creatable: false, computeService: 'digitalOceanComputeService',
						displayOrder: 99, hasAutomation: false,
						containerHypervisor: false, bareMetalHost: false, vmHypervisor: false, managedServerType: 'digitalOceanVm2', guestVm: true, supportsConsoleKeymap: true
				)
		]

		return serverTypes
	}

	@Override
	Collection<ProvisioningProvider> getAvailableProvisioningProviders() {
		return plugin.getProvidersByType(ProvisioningProvider) as Collection<ProvisioningProvider>
	}

	@Override
	Collection<BackupProvider> getAvailableBackupProviders() {
		return plugin.getProvidersByType(BackupProvider) as Collection<BackupProvider>
	}

	@Override
	ProvisioningProvider getProvisioningProvider(String providerCode) {
		return getAvailableProvisioningProviders().find { it.providerCode == providerCode }
	}

	@Override
	ServiceResponse validate(Cloud zoneInfo) {
		log.debug "validating Cloud: ${zoneInfo.code}"
		if (!zoneInfo.configMap.datacenter) {
			return new ServiceResponse(success: false, msg: 'Choose a datacenter')
		}
		if (!zoneInfo.configMap.doUsername) {
			return new ServiceResponse(success: false, msg: 'Enter a username')
		}
		if (!zoneInfo.configMap.doApiKey) {
			return new ServiceResponse(success: false, msg: 'Enter your api key')
		}
		return new ServiceResponse(success: true)
	}

	@Override
	ServiceResponse initializeCloud(Cloud cloud) {
		ServiceResponse serviceResponse
		log.debug "Initializing Cloud: ${cloud.code}"
		log.debug "config: ${cloud.configMap}"
		String apiKey = cloud.configMap.doApiKey
		HttpGet accountGet = new HttpGet("${DigitalOceanApiService.DIGITAL_OCEAN_ENDPOINT}/v2/account")

		// check account
		def respMap = apiService.makeApiCall(accountGet, apiKey)
		if (respMap.resp.statusLine.statusCode == 200 && respMap.json.account.status == 'active') {
			serviceResponse = new ServiceResponse(success: true, content: respMap.json)

			loadDatacenters(cloud)
			cacheSizes(apiKey)
			cacheImages(cloud)

			KeyPair keyPair = morpheusContext.cloud.findOrGenerateKeyPair(cloud.account).blockingGet()
			if (keyPair) {
				KeyPair updatedKeyPair = findOrUploadKeypair(apiKey, keyPair.publicKey, keyPair.name)
				morpheusContext.cloud.updateKeyPair(updatedKeyPair, cloud)
			} else {
				log.debug "no morpheus keys found"
			}
		} else {
			serviceResponse = new ServiceResponse(success: false, msg: respMap.resp?.statusLine?.statusCode, content: respMap.json)
		}

		serviceResponse
	}

	@Override
	void refresh(Cloud cloudInfo) {
		log.debug "cloud refresh has run for ${cloudInfo.code}"
		cacheSizes(cloudInfo.configMap.doApiKey)
		loadDatacenters(cloudInfo)
		cacheImages(cloudInfo)
	}

	@Override
	void refreshDaily(Cloud cloudInfo) {
		log.debug "daily refresh run for ${cloudInfo.code}"
	}

	@Override
	ServiceResponse deleteCloud(Cloud cloudInfo) {
		return new ServiceResponse(success: true)
	}

	List<Map> loadDatacenters(def cloudInfo) {
		List datacenters = []
		log.debug "load datacenters for ${cloudInfo.code}"
		HttpGet http = new HttpGet("${DigitalOceanApiService.DIGITAL_OCEAN_ENDPOINT}/v2/regions")
		def respMap = apiService.makeApiCall(http, cloudInfo.configMap.doApiKey)
		respMap?.json?.regions?.each {
			datacenters << [value: it.slug, name: it.name, available: it.available]
		}
		// TODO cache these?
		datacenters
	}

	List<VirtualImage> listImages(Cloud cloudInfo, Boolean userImages) {
		log.debug "list ${userImages ? 'User' : 'OS'} Images"
		List<VirtualImage> virtualImages = []

		Map queryParams = [:]
		if (userImages) {
			queryParams.private = 'true'
		} else {
			queryParams.type = 'distribution'
		}
		List images = apiService.makePaginatedApiCall(cloudInfo.configMap.doApiKey, '/v2/images', 'images', queryParams)

		String imageCodeBase = "doplugin.image.${userImages ? 'user' : 'os'}"

		images.each {
			Map props = [
					name      : "${it.distribution} ${it.name}",
					externalId: it.id,
					code      : "${imageCodeBase}.${cloudInfo.code}.${it.id}",
					category  : "${imageCodeBase}.${cloudInfo.code}",
					imageType : ImageType.qcow2,
					platform  : it.distribution,
					isPublic  : it.public,
					minDisk   : it.min_disk_size,
					locations : it.regions
			]
			virtualImages << new VirtualImage(props)
		}
		virtualImages
	}

	def cacheImages(Cloud cloud) {
		List<VirtualImage> virtualImages = listImages(cloud, false)
		virtualImages += listImages(cloud, true)
		List externalIds = virtualImages.collect { it.externalId }
		def addList = []

		ConnectableObservable<VirtualImage> images = morpheusContext.virtualImage.listVirtualImages(cloud).publish()

		//remove
		images.filter { img -> return !externalIds.contains(img.externalId) }
				.buffer(50)
				.subscribe { morpheusContext.virtualImage.removeVirtualImage(it) }
		//update
		images.filter({ img -> externalIds.contains(img.externalId) })
				.map { VirtualImage img ->
					def match = virtualImages.find { it.externalId == img.externalId }
					virtualImages.remove(match)
					return img
				}
				.buffer(50)
				.doFinally {
					while (virtualImages.size() > 0) {
						List chunkedList = virtualImages.take(50)
						virtualImages = virtualImages.drop(50)
						morpheusContext.virtualImage.saveVirtualImage(chunkedList)
					}
				}
				.subscribe({ morpheusContext.virtualImage.updateVirtualImage(it) })
		//add
//		externalIds.each {
//			List<String> persistedExternalIds = []
//			images.map { it.externalId }.subscribe({ persistedExternalIds << it })
//			if (!persistedExternalIds.contains(it)) {
//				addList << it
//			}
//		}
//
//		morpheusContext.compute.saveVirtualImage(addList)
		images.connect()
	}

	def cacheSizes(String apiKey) {
		HttpGet sizesGet = new HttpGet("${DigitalOceanApiService.DIGITAL_OCEAN_ENDPOINT}/v2/sizes")
		Map respMap = apiService.makeApiCall(sizesGet, apiKey)
		List<ServicePlan> servicePlans = []
		respMap.json?.sizes?.each {
			def name = getNameForSize(it)
			def servicePlan = new ServicePlan(
					code: "doplugin.size.${it.slug}",
					provisionTypeCode: getProviderCode(),
					description: name,
					name: name,
					editable: false,
					externalId: it.slug,
					maxCores: it.vcpus,
					maxMemory: it.memory.toLong() * 1024l * 1024l, // MB
					maxStorage: it.disk.toLong() * 1024l * 1024l * 1024l, //GB
					sortOrder: it.disk.toLong(),
					price_monthly: it.price_monthly,
					price_hourly: it.price_hourly
			)
			servicePlans << servicePlan
		}

		morpheusContext.cloud.cachePlans(servicePlans)
	}

	KeyPair findOrUploadKeypair(String apiKey, String publicKey, String keyName) {
		keyName = keyName ?: 'morpheus_do_plugin_key'
		log.debug "find or update keypair for key $keyName"
		List keyList = apiService.makePaginatedApiCall(apiKey, '/v2/account/keys', 'ssh_keys', [:])
		log.debug "keylist: $keyList"
		def match = keyList.find { publicKey.startsWith(it.public_key) }
		log.debug("match: ${match} - list: ${keyList}")
		if (!match) {
			log.debug 'key not found in DO'
			HttpPost httpPost = new HttpPost("${DigitalOceanApiService.DIGITAL_OCEAN_ENDPOINT}/v2/account/keys")
			httpPost.entity = new StringEntity(JsonOutput.toJson([public_key: publicKey, name: keyName]))
			def respMap = apiService.makeApiCall(httpPost, apiKey)
			if (respMap.resp.statusLine.statusCode == 200) {
				match = new KeyPair(name: respMap.json.name, externalId: respMap.json.id, publicKey: respMap.json.public_key, publicFingerprint: respMap.json.fingerprint)
			} else {
				log.debug 'failed to add DO ssh key'
			}
			match = respMap.json
		}
		new KeyPair(name: match.name, externalId: match.id, publicKey: match.public_key, publicFingerprint: match.fingerprint)
	}

	private getNameForSize(sizeData) {
		def memoryName = sizeData.memory < 1000 ? "${sizeData.memory} MB" : "${sizeData.memory.div(1024l)} GB"
		"Plugin Droplet ${sizeData.vcpus} CPU, ${memoryName} Memory, ${sizeData.disk} GB Storage"
	}
}