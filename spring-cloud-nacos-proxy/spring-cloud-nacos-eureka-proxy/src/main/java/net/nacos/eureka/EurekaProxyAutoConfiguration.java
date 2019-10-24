package net.nacos.eureka;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import net.nacos.eureka.sync.EurekaSynchronizer;
import net.nacos.eureka.sync.NacosSynchronizer;
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
@EnableConfigurationProperties(NacosDiscoveryProperties.class)
@AutoConfigureAfter({AutoServiceRegistrationConfiguration.class,
        AutoServiceRegistrationAutoConfiguration.class, PeerAwareInstanceRegistry.class})
@Import({EurekaSynchronizer.class, NacosSynchronizer.class})
public class EurekaProxyAutoConfiguration {

    @Bean
    public NamingService namingService(NacosDiscoveryProperties nacosDiscoveryProperties) throws NacosException {
        return NacosFactory.createNamingService(nacosDiscoveryProperties);
    }

    @Bean
    public NamingMaintainService namingMaintainService(NacosDiscoveryProperties nacosDiscoveryProperties) throws NacosException {
        return NacosFactory.createMaintainService(nacosDiscoveryProperties);
    }

}
