package com.github.charlesvhe.config;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

/**
 * @author liju2
 * @date 2019/10/18
 */
public class NacosEnvironmentRepository implements EnvironmentRepository {

    private ConfigService configService;

    private ConfigProxyProperties configProxyProperties;

    public NacosEnvironmentRepository(ConfigService configService, ConfigProxyProperties configProxyProperties) {
        this.configService = configService;
        this.configProxyProperties = configProxyProperties;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        try {
            Environment environment = new Environment(application, new String[]{profile}, label, null, null);
            addPropertySource("application", environment);
            addPropertySource(application, environment);
            return environment;
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    private void addPropertySource(String application, Environment environment) throws NacosException {
        String dataId = application + ".yml";
        String config = configService.getConfig(dataId, Constants.DEFAULT_GROUP, 3000);
        if (StringUtils.hasText(config)) {
            YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
            yamlPropertiesFactoryBean.setResources(new ByteArrayResource(config.getBytes()));
            environment.add(new PropertySource(dataId, yamlPropertiesFactoryBean.getObject()));
        }
    }
}
