package net.nacos.eureka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("spring.cloud.nacos.discovery")
public class NacosDiscoveryProperties extends Properties {
}
