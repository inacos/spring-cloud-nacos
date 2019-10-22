package com.github.charlesvhe.eureka.sync;

import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author liju2
 * @date 2019/10/10
 */
@Component
public class NacosEventListener implements EventListener {

    @Autowired
    private PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    @Override
    public void onEvent(Event event) {
        if (event instanceof NamingEvent) {
            NamingEvent namingEvent = (NamingEvent) event;
            List<Instance> instances = namingEvent.getInstances();
            String serviceName = namingEvent.getServiceName().substring(namingEvent.getServiceName().lastIndexOf('@') + 1).toUpperCase();
            List<Application> applications = peerAwareInstanceRegistry.getSortedApplications();

            List<InstanceInfo> instanceInfos = new ArrayList<>();
            Optional<Application> optional = applications.stream().filter(application ->
                    application.getName().equals(serviceName)).findFirst();
            if (optional.isPresent()) {
                Application application = optional.get();
                instanceInfos = application.getInstances();
            }
            for (InstanceInfo instanceInfo : instanceInfos) {
                Optional<Instance> find = instances.stream().filter(instance ->
                        instance.getIp().equals(instanceInfo.getIPAddr()) && instance.getPort() == instanceInfo.getPort()).findFirst();
                if (find.isPresent()) {
                    // 更新服务状态
                    Instance target = find.get();
                    if (isFromEureka(target)) {
                        continue;
                    }
                    setStatus(instanceInfo, target.isEnabled());
                } else {
                    // 下线服务
                    deregister(instanceInfo);
                }
            }
            for (Instance instance : instances) {
                if (isFromEureka(instance)) {
                    continue;
                }
                Optional<InstanceInfo> find = instanceInfos.stream().filter(instanceInfo ->
                        instance.getIp().equals(instanceInfo.getIPAddr()) && instance.getPort() == instanceInfo.getPort()).findFirst();
                if (!find.isPresent()) {
                    // 上线服务
                    register(instance);
                }
            }
        }
    }

    private void setStatus(InstanceInfo instanceInfo, boolean up) {
        InstanceInfo.InstanceStatus status;
        if (up) {
            status = InstanceInfo.InstanceStatus.UP;
        } else {
            status = InstanceInfo.InstanceStatus.DOWN;
        }
        peerAwareInstanceRegistry.statusUpdate(instanceInfo.getAppName(), instanceInfo.getId(), status,
                instanceInfo.getLastDirtyTimestamp() + "", true);
    }

    private void deregister(InstanceInfo instanceInfo) {
        peerAwareInstanceRegistry.cancel(instanceInfo.getAppName(), instanceInfo.getInstanceId(), true);
    }

    private void register(Instance instance) {
        String appName = instance.getServiceName().substring(instance.getServiceName().lastIndexOf('@') + 1);
        String discoveryClient = instance.getMetadata().get(ProxyConstants.METADATA_DISCOVERY_CLIENT);
        if (StringUtils.isEmpty(discoveryClient)) {
            Map<String, String> metadata = new HashMap<>(instance.getMetadata());
            metadata.put(ProxyConstants.METADATA_DISCOVERY_CLIENT, ProxyConstants.NACOS_VALUE);
            peerAwareInstanceRegistry.register(InstanceInfo.Builder.newBuilder()
                    .setAppName(appName.toLowerCase())
                    .setIPAddr(instance.getIp())
                    .setPort(instance.getPort())
                    .setInstanceId(String.format("%s:%s:%s", appName, instance.getIp(), instance.getPort()))
                    .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
                    .setMetadata(metadata)
                    .build(), true);
        }
    }

    private boolean isFromEureka(Instance instance) {
        String discoveryClient = instance.getMetadata().get(ProxyConstants.METADATA_DISCOVERY_CLIENT);
        return !StringUtils.isEmpty(discoveryClient) && discoveryClient.equals(ProxyConstants.EUREKA_VALUE);
    }

}
