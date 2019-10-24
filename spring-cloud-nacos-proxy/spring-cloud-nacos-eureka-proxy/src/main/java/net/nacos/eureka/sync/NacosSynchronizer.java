package net.nacos.eureka.sync;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author liju2
 * @date 2019/10/10
 */
@Component
public class NacosSynchronizer {
    private static final Logger log = LoggerFactory.getLogger(NacosSynchronizer.class);

    @Autowired
    private NamingService namingService;
    @Autowired
    private NacosEventListener listener;

    @PostConstruct
    public void init() {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread t = new Thread(runnable);
            t.setName(NacosSynchronizer.class.getName());
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                syncService();
            } catch (Throwable e) {
                log.error("synchronize service error", e);
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void syncService() throws Exception {
        ListView<String> serviceList = namingService.getServicesOfServer(1, 1000);
        for (String service : serviceList.getData()) {
            List<ServiceInfo> list = namingService.getSubscribeServices();
            Optional<ServiceInfo> optional = list.stream().filter(serviceInfo -> serviceInfo.getName().equals(service)).findFirst();
            if (!optional.isPresent()) {
                namingService.subscribe(service, listener);
            }
        }

    }
}
