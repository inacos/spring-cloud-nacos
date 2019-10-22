package com.github.charlesvhe.eureka.sync;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liju2
 * @date 2019/10/10
 */
public class NacosInstanceCache {

    private Map<String, List<Instance>> instanceMap= new HashMap<>();

    void addCache(Instance nacosInstance) {
        String serviceName = nacosInstance.getServiceName();
        List<Instance> list = instanceMap.get(serviceName);
        if (list == null) {
            list = new ArrayList<>();
            instanceMap.put(serviceName, list);
        }
        list.add(nacosInstance);
    }

    void removeCache(Instance nacosInstance) {
        String serviceName = nacosInstance.getServiceName();
        List<Instance> list = instanceMap.get(serviceName);
        if (list == null) {
            return;
        }
        for (Instance item : list) {
            if (item.getIp().equals(nacosInstance.getIp()) && item.getPort() == nacosInstance.getPort()) {
                list.remove(item);
                return;
            }
        }
    }

    boolean containService(String serviceName) {
        List<Instance> list = instanceMap.get(serviceName);
        return list != null;
    }

    boolean containInstance(Instance nacosInstance) {
        List<Instance> list = instanceMap.get(nacosInstance.getServiceName());
        if (list == null) {
            return false;
        }
        for (Instance instance : list) {
            if (instance.getIp().equals(nacosInstance.getIp()) && instance.getPort() == nacosInstance.getPort()) {
                return true;
            }
        }
        return false;
    }
}
