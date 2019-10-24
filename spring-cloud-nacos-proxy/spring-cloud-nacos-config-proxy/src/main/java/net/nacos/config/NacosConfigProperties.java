package net.nacos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("spring.cloud.nacos.config")
public class NacosConfigProperties extends Properties {
}
