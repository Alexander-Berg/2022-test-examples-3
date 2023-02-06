package ru.yandex.direct.teststeps.swagger;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;

import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import springfox.documentation.annotations.ApiIgnore;

import ru.yandex.direct.teststeps.configuration.SwaggerConfiguration;

import static com.google.common.io.Resources.getResource;

/**
 * Springfox Swagger сервис привязан к URL {@code /swagger-resource}
 * ({@link springfox.documentation.swagger.web.ApiResourceController}).
 * <p>
 * Чтобы вся документация была доступна через URL, указанный в конфигурации как
 * "{@code springfox.documentation.swagger.ui.baseurl}"
 * используем механизм forwarding'а Spring'а (
 * <a href="http://www.baeldung.com/spring-redirect-and-forward">подробнее</a>).
 *
 * @see SwaggerConfiguration
 */
@Controller
@ApiIgnore
@RequestMapping("${springfox.documentation.swagger.ui.baseurl}")
public class SwaggerRedirectController {
    /**
     * Возможно, эту константу стоит сделать final-переменной и тянуть из аннотации {@link RequestMapping} класса
     * {@link springfox.documentation.swagger.web.ApiResourceController}
     */
    public static final String SWAGGER_RESOURCES_MAPPING_PREFIX = "swagger-resources";

    private final String swaggerUiBaseUrl;
    private final String swaggerApiDocUrl;

    @Autowired
    public SwaggerRedirectController(@Value("${springfox.documentation.swagger.ui.baseurl}") String swaggerUiBaseUrl,
                                     @Value("${springfox.documentation.swagger.v2.path}") String swaggerApiDocUrl) {
        this.swaggerUiBaseUrl = swaggerUiBaseUrl;
        this.swaggerApiDocUrl = swaggerApiDocUrl;
    }

    @RequestMapping("/" + SWAGGER_RESOURCES_MAPPING_PREFIX + "/**")
    public ModelAndView redirectToSwagger(ModelMap model, HttpServletRequest req) {
        String servletPath = req.getServletPath();
        //Редирект на основе @PathVariable сделать не удалось, см. "https://jira.spring.io/browse/SPR-12546".
        String relativeSwaggerUrl =
                StringUtils.removeStart(req.getRequestURI(), req.getContextPath() + servletPath + swaggerUiBaseUrl);
        return new ModelAndView("forward:" + servletPath + relativeSwaggerUrl, model);
    }

    /**
     * JS код swagger-ui пытается искать API docs по относительному URL (/docs/docs/api в нашем случае)
     * Этот redirect осуществляет проброс этого запроса на правильный endpoint
     */
    @RequestMapping("${springfox.documentation.swagger.v2.path}")
    public ModelAndView redirectToDocsApi(ModelMap model, HttpServletRequest req) {
        return new ModelAndView("forward:" + req.getServletPath() + swaggerApiDocUrl, model);
    }

    /**
     * отвечает сгенеренной html, вместо стандартной swagger-ui.html из ресурсов springfox-swagger-ui
     * так же на страницу добавлен js код, который устанавливает хедер с csrf токеном
     */
    @RequestMapping(value = "/swagger-ui.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String swaggerPage() throws IOException {
        return Resources.toString(getResource("META-INF/resources/swagger-ui.tmpl"), Charset.defaultCharset());
    }
}
