package net.nacos.config;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class NacosEnvironmentRepository implements EnvironmentRepository {

    private ConfigService configService;
    private NacosConfigProperties nacosConfigProperties;

    public NacosEnvironmentRepository(ConfigService configService, NacosConfigProperties nacosConfigProperties) {
        this.configService = configService;
        this.nacosConfigProperties = nacosConfigProperties;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        try {
            // 兼容spring cloud config配置 暂不支持label
            Environment environment = new Environment(application, new String[]{profile}, label, null, null);
            addPropertySource("application", environment);
            addPropertySource("application" + "-" + profile, environment);
            addPropertySource(application, environment);
            addPropertySource(application + "-" + profile, environment);
            return environment;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addPropertySource(String application, Environment environment) throws NacosException, IOException {
        String fileExtension = nacosConfigProperties.getProperty("fileExtension", "properties");

        String dataId = application + "." + fileExtension;
        String config = configService.getConfig(dataId, Constants.DEFAULT_GROUP, 3000);
        if (StringUtils.hasText(config)) {
            // 解析不同文件格式
            if ("yml".equals(fileExtension)) {
                YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
                yamlPropertiesFactoryBean.setResources(new ByteArrayResource(config.getBytes()));
                environment.add(new PropertySource(dataId, yamlPropertiesFactoryBean.getObject()));
            } else {
                Properties properties = new Properties();
                properties.load(new StringReader(config));
                environment.add(new PropertySource(dataId, properties));
            }
        }
    }
}
