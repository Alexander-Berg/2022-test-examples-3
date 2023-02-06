package ru.yandex.direct.teststeps.configuration;

import com.google.common.collect.Ordering;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.Orderings;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import ru.yandex.direct.teststeps.swagger.SwaggerRedirectController;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Конфигурация Swagger-сервиса, который отдаёт описание API.
 * <p>
 * {@link SwaggerConfiguration Swagger конфигурация} состоит из статичного swagger-ui.html с обвязкой, раздаваемой через
 * webjar, и swagger-сервиса, описывающего API нашего {@link TestStepsAppConfiguration TestStepsApp}.
 * <p>
 * URL для swagger-сервиса кастомизируется через свойство "{@code springfox.documentation.swagger.v2.path}". Подробнее
 * <a href="springfox.documentation.swagger.v2.path">в документации</a>.
 * <p>
 * UI со статикой раздаётся по URL, указанному в конфигурации под ключом
 * "{@code springfox.documentation.swagger.ui.baseurl}".
 * Для того, чтобы это сделать, был использован подход из документации к Swagger'у:
 * <a href="http://springfox.github.io/springfox/docs/current/#q13">
 * Q. How does one configure swagger-ui for non-springboot applications?</a>
 *
 * @see SwaggerRedirectController
 */
@Configuration
@EnableSwagger2
@ComponentScan(
        basePackages = {
                "ru.yandex.direct.teststeps"
        }
)
public class SwaggerConfiguration implements WebMvcConfigurer {

    @Value("${springfox.documentation.swagger.ui.baseurl}")
    String swaggerUiBaseUrl;

    @Value("${springfox.documentation.swagger.v2.path}")
    String apiBaseUrl;

    @Value("${springfox.documentation.swagger.v2.host}")
    String host;

    @Value("${springfox.documentation.swagger.api.url}")
    String apiUrl;


    @SuppressWarnings("checkstyle:linelength")
    /*
     * <b>Комментарий по реализации</b>
     * <p>
     * При использовании конфигурации {@link Docket} по умолчанию к {@code operationId} добавляется магический
     * постфикс "_7". Это происходит из-за того, что Springfox swagger сначала для каждого из 8ми известных ему
     * HTTP-методов (полученных из {@link org.springframework.web.bind.annotation.RequestMethod#values()}) генерирует
     * по одному endpoint'у, которым даёт имена типа "auth", "auth_1", ..., "auth_7". Затем из них он выбирает тот,
     * который является первым по его default'ным правилам сортировки.
     * <p>
     * Мы разворачиваем default'ные правила сортировки однотипных операций, добавляя reverse при инициализации,
     * чтобы выбирался "auth", а не "auth_7"
     *
     * @see springfox.documentation.spi.service.contexts.Defaults#operationOrdering()
     * @see springfox.documentation.spring.web.readers.operation.ApiOperationReader#read(springfox.documentation.spi
     * .service.contexts.RequestMappingContext)
     * @see org.springframework.web.bind.annotation.RequestMethod
     */
    @Bean
    public Docket api() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .operationOrdering(Ordering.from(Orderings.positionComparator().reversed())
                        .compound(Orderings.nickNameComparator()));
        if (isNotEmpty(host)) {
            docket.host(host);
        }
        // документация доступна по http://localhost:8090/docs/
        return docket
                .pathMapping(apiUrl)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(swaggerUiBaseUrl + "/**")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(swaggerUiBaseUrl + apiBaseUrl, apiBaseUrl);
        registry.addRedirectViewController(swaggerUiBaseUrl + "", swaggerUiBaseUrl + "/swagger-ui.html");
        registry.addRedirectViewController(swaggerUiBaseUrl + "/", swaggerUiBaseUrl + "/swagger-ui.html");
    }
}
