package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.DataTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class SampleDataStartupV1 implements InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(SampleDataStartupV1.class);

    private static final DynamicBooleanProperty RUN = ArchaiusUtil.getBoolean("sample.setup");

    private static final String NAME = "sampleDataVersion1";

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    AccountDao accountDao;
    JsonMapper jsonMapper;

    @Override
    public void start() {
        if ( ! RUN.get() ) {
            return;
        }

        Data data = objectManager.findAny(Data.class, DATA.NAME, NAME);

        if ( data != null ) {
            return;
        }

        Account system = accountDao.getSystemAccount();
        if ( system == null ) {
            log.warn("Failed to find system account, not populating system data");
            return;
        }

        Map<String, Object> networkData;
        try {
            networkData = jsonMapper.readValue("{\"libvirt\":{\"network\":{\"source\":[{\"bridge\":\"docker0\"}],\"type\":\"bridge\"}}}");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<Object> toCreate = new ArrayList<Object>();

        Network network = createByUuid(Network.class, "unmanaged",
                NETWORK.ACCOUNT_ID, system.getId(),
                NETWORK.IS_PUBLIC, true,
                NETWORK.NAME, "Unmanaged Network",
                NETWORK.STATE, CommonStatesConstants.REQUESTED);

        toCreate.add(network);

        network = createByUuid(Network.class, "managed-docker0",
                NetworkConstants.FIELD_HOST_VNET_URI, "bridge://docker0",
                NetworkConstants.FIELD_DYNAMIC_CREATE_VNET, true,
                NETWORK.ACCOUNT_ID, system.getId(),
                NETWORK.IS_PUBLIC, true,
                NETWORK.KIND, NetworkConstants.KIND_HOSTONLY,
                NETWORK.NAME, "Managed Network on docker0",
                NETWORK.STATE, CommonStatesConstants.REQUESTED,
                NETWORK.DATA, networkData);

        toCreate.add(network);

        toCreate.add(createByUuid(Subnet.class, "docker0-subnet",
                SUBNET.ACCOUNT_ID, system.getId(),
                SUBNET.CIDR_SIZE, 24,
                SUBNET.END_ADDRESS, "10.42.0.250",
                SUBNET.KIND, "subnet",
                SUBNET.GATEWAY, "10.42.0.1",
                SUBNET.IS_PUBLIC, true,
                SUBNET.NAME, "Subnet for managed docker0",
                SUBNET.NETWORK_ADDRESS, "10.42.0.0",
                SUBNET.NETWORK_ID, network.getId(),
                SUBNET.START_ADDRESS, "10.42.0.2",
                SUBNET.STATE, CommonStatesConstants.REQUESTED));

        NetworkServiceProvider networkServiceProvider = createByUuid(NetworkServiceProvider.class, "docker0-agent-instance-provider",
                NETWORK_SERVICE_PROVIDER.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE_PROVIDER.KIND, NetworkServiceProviderConstants.KIND_AGENT_INSTANCE,
                NETWORK_SERVICE_PROVIDER.NAME, "Agent instance provider for managed docker0",
                NETWORK_SERVICE_PROVIDER.NETWORK_ID, network.getId(),
                NETWORK_SERVICE_PROVIDER.STATE, CommonStatesConstants.REQUESTED);

        toCreate.add(networkServiceProvider);

        toCreate.add(createByUuid(NetworkService.class, "docker0-dns-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_DNS,
                NETWORK_SERVICE.NAME, "DNS for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-dhcp-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_DHCP,
                NETWORK_SERVICE.NAME, "DHCP for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-link-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_LINK,
                NETWORK_SERVICE.NAME, "Instance links for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-ipsecTunnel-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_IPSEC_TUNNEL,
                NETWORK_SERVICE.NAME, "IPsec tunnels for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-port-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_PORT_SERVICE,
                NETWORK_SERVICE.NAME, "Ports service for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-host-nat-gateway-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_HOST_NAT_GATEWAY,
                NETWORK_SERVICE.NAME, "Host nat gateway service for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-metadata-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_METADATA,
                NETWORK_SERVICE.NAME, "Meta data service for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        for ( Object object : toCreate ) {
            try {
                processManager.executeStandardProcess(StandardProcess.CREATE, object, null);
            } catch ( ProcessCancelException e ) {
            }
        }

        objectManager.create(Data.class,
                DATA.NAME, NAME,
                DATA.VALUE, "true");
    }

    protected <T> T createByUuid(Class<T> type, String uuid, Object key, Object... values) {
        Map<Object,Object> inputProperties = CollectionUtils.asMap(key, values);
        inputProperties.put(ObjectMetaDataManager.UUID_FIELD, uuid);
        Map<String,Object> properties = objectManager.convertToPropertiesFor(type, inputProperties);

        T existing = objectManager.findAny(type, ObjectMetaDataManager.UUID_FIELD, uuid);
        if ( existing != null ) {
            objectManager.setFields(existing, properties);
            return existing;
        }

        return objectManager.create(type, properties);
    }

    @Override
    public void stop() {
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    @Inject
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
