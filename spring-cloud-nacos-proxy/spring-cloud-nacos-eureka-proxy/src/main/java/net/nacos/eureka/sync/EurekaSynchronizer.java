package net.nacos.eureka.sync;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class EurekaSynchronizer implements ApplicationListener {
    private static final Logger log = LoggerFactory.getLogger(EurekaSynchronizer.class);

    private NacosInstanceCache cache = new NacosInstanceCache();

    @Autowired
    private NamingService namingService;
    @Autowired
    private NamingMaintainService namingMaintainService;
    @Autowired
    private PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    @Override
    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof EurekaInstanceRegisteredEvent) {
            register((EurekaInstanceRegisteredEvent) e);
        } else if (e instanceof EurekaInstanceRenewedEvent) {
            setStatus((EurekaInstanceRenewedEvent) e);
        } else if (e instanceof EurekaInstanceCanceledEvent) {
            deregister((EurekaInstanceCanceledEvent) e);
        } else if (e instanceof EurekaRegistryAvailableEvent) {
            log.info("EurekaRegistryAvailableEvent");
        }
    }

    protected void setStatus(EurekaInstanceRenewedEvent evt) {
        InstanceInfo instanceInfo = evt.getInstanceInfo();
        if (instanceInfo == null || isFromNacos(instanceInfo)) {
            return;
        }

        Instance instance = getNacosInstanceFromEureka(instanceInfo);
        InstanceInfo.InstanceStatus status = evt.getInstanceInfo().getStatus();
        if (status.equals(InstanceInfo.InstanceStatus.UP) || status.equals(InstanceInfo.InstanceStatus.STARTING)) {
            instance.setEnabled(true);
        } else {
            instance.setEnabled(false);
        }
        if (!cache.containService(instanceInfo.getAppName())) {
            return;
        }
        try {
            namingMaintainService.updateInstance(instanceInfo.getAppName().toLowerCase(), instance);
        } catch (NacosException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected void deregister(EurekaInstanceCanceledEvent evt) {
        String serverId = evt.getServerId();
        String appName = evt.getAppName();

        List<Application> applicationList = peerAwareInstanceRegistry.getSortedApplications();
        Optional<Application> optional = applicationList.stream().filter(app -> app.getName().equals(appName)).findFirst();
        if (optional.isPresent()) {
            Application app = optional.get();
            InstanceInfo instanceInfo = app.getByInstanceId(serverId);
            Instance instance = getNacosInstanceFromEureka(instanceInfo);
            if (isFromNacos(instanceInfo)) {
                return;
            }
            try {
                namingService.deregisterInstance(instanceInfo.getAppName().toLowerCase(), Constants.DEFAULT_GROUP, instance);
                cache.removeCache(instance);
            } catch (NacosException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected void register(EurekaInstanceRegisteredEvent evt) {
        InstanceInfo instanceInfo = evt.getInstanceInfo();
        if (isFromNacos(instanceInfo)) {
            return;
        }
        Instance instance = getNacosInstanceFromEureka(instanceInfo);
        // 重复注册服务到nacos，会导致nacos启动多个服务心跳监测，导致服务无法下线
        if (cache.containInstance(instance)) {
            return;
        }
        try {
            namingService.registerInstance(instanceInfo.getAppName().toLowerCase(), Constants.DEFAULT_GROUP, instance);
            cache.addCache(instance);
        } catch (NacosException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected Instance getNacosInstanceFromEureka(InstanceInfo instanceInfo) {
        Map<String, String> metadata = new HashMap<>(instanceInfo.getMetadata());
        metadata.put(ProxyConstants.METADATA_DISCOVERY_CLIENT, ProxyConstants.EUREKA_VALUE);
        Instance instance = new Instance();
        instance.setServiceName(instanceInfo.getAppName());
        instance.setIp(instanceInfo.getIPAddr());
        instance.setPort(instanceInfo.getPort());
        instance.setWeight(1);
        instance.setClusterName(Constants.DEFAULT_CLUSTER_NAME);
        instance.setMetadata(metadata);

        return instance;
    }

    protected boolean isFromNacos(InstanceInfo instanceInfo) {
        String discoveryClient = instanceInfo.getMetadata().get(ProxyConstants.METADATA_DISCOVERY_CLIENT);
        return !StringUtils.isEmpty(discoveryClient) && discoveryClient.equals(ProxyConstants.NACOS_VALUE);
    }
}