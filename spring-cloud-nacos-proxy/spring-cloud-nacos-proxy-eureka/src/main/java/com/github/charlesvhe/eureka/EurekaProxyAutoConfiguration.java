package com.github.charlesvhe.eureka;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.github.charlesvhe.eureka.sync.EurekaSynchronizer;
import com.github.charlesvhe.eureka.sync.NacosSynchronizer;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author liju2
 * @date 2019/10/10
 */
@Configuration
@EnableConfigurationProperties({EurekaProxyProperties.class})
@AutoConfigureAfter({AutoServiceRegistrationConfiguration.class,
        AutoServiceRegistrationAutoConfiguration.class, PeerAwareInstanceRegistry.class})
@Import({EurekaSynchronizer.class, NacosSynchronizer.class})
public class EurekaProxyAutoConfiguration {

    @Bean
    public NamingService namingService(EurekaProxyProperties proxyProperties) throws NacosException {
        String serverAddr = proxyProperties.getServerAddr();
        return NacosFactory.createNamingService(serverAddr);
    }

    @Bean
    public NamingMaintainService namingMaintainService(EurekaProxyProperties proxyProperties) throws NacosException {
        String serverAddr = proxyProperties.getServerAddr();
        return NacosFactory.createMaintainService(serverAddr);
    }

}
