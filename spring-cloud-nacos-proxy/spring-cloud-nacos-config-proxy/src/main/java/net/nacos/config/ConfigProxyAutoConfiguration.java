package net.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NacosConfigProperties.class)
@ConditionalOnClass({EnableConfigServer.class, ConfigService.class})
@AutoConfigureBefore(ConfigServerAutoConfiguration.class)
public class ConfigProxyAutoConfiguration {

    @Bean
    public ConfigService nacosConfigService(NacosConfigProperties nacosConfigProperties) throws NacosException {
        return NacosFactory.createConfigService(nacosConfigProperties);
    }

    @Bean
    public EnvironmentRepository nacosEnvironmentRepository(ConfigService configService, NacosConfigProperties nacosConfigProperties) {
        return new NacosEnvironmentRepository(configService, nacosConfigProperties);
    }

}
