package ru.yandex.market.pers.address.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 22.04.2021
 */
@Configuration
@EnableSwagger2WebMvc
@Profile("!test")
public class SwaggerConfig {
}
