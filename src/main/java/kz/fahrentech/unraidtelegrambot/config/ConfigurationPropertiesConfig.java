package kz.fahrentech.unraidtelegrambot.config;

import kz.fahrentech.unraidtelegrambot.telegram.TelegramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AppProperties.class, TelegramProperties.class})
public class ConfigurationPropertiesConfig {
}
