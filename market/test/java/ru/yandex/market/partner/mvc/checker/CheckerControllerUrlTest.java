package ru.yandex.market.partner.mvc.checker;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.partner.test.context.FunctionalTest;

class CheckerControllerUrlTest extends FunctionalTest {

    private static final String UNSUPPORTED_PATH_VARIABLES_REGEXP_GROUP = Stream.of("id", "supplierId", "supplier_id")
            .map(s -> String.format("\\{%s\\}", s))
            .collect(Collectors.joining("|", "(", ")"));

    @Autowired
    private Handler mvcHandler;

    /**
     * В Партнерке id можно называть только campaignId (или campaign_id)
     * Но во всех ручках campaignId как id не используется, поэтому добавил простой тест,
     * который не даст создать ручку с id как параметр.
     * <p>
     * Также нельзя создавать ручку с параметром supplier_id, supplierId
     */
    @Test
    @DisplayName("У всех MVC ручках path variable содержит допустимое название")
    void urlWithUnsupportedPathVariablesTest() throws ServletException {
        ServletHandler servletHandler = (ServletHandler) mvcHandler;
        WebApplicationContext wac = ((DispatcherServlet) servletHandler
                .getServlet("dispatcherServlet")
                .getServlet())
                .getWebApplicationContext();

        String urlsWithId = ((RequestMappingHandlerMapping) wac.getBean("requestMappingHandlerMapping"))
                .getHandlerMethods()
                .keySet()
                .stream()
                .map(RequestMappingInfo::getPatternsCondition)
                .map(PatternsRequestCondition::getPatterns)
                .flatMap(Collection::stream)
                .filter(s -> Pattern
                        .compile("\\/" + UNSUPPORTED_PATH_VARIABLES_REGEXP_GROUP + "(\\/|$)", Pattern.CASE_INSENSITIVE)
                        .matcher(s)
                        .find()
                ).collect(Collectors.joining(", "));

        Assertions.assertTrue(urlsWithId.isEmpty(), () -> "Urls with unsupported path variables " + urlsWithId);
    }
}
