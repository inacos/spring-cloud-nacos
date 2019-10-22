package com.github.charlesvhe.config;

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
@EnableConfigurationProperties({ConfigProxyProperties.class})
@ConditionalOnClass(EnableConfigServer.class)
@AutoConfigureBefore(ConfigServerAutoConfiguration.class)
public class ConfigProxyAutoConfiguration {

    @Bean
    public ConfigService nacosConfigService(ConfigProxyProperties configProxyProperties) throws NacosException {
        String serverAddr = configProxyProperties.getServerAddr();
        return NacosFactory.createConfigService(serverAddr);
    }

    @Bean
    public EnvironmentRepository nacosEnvironmentRepository(ConfigService configService, ConfigProxyProperties configProxyProperties) {
        return new NacosEnvironmentRepository(configService, configProxyProperties);
    }

}
