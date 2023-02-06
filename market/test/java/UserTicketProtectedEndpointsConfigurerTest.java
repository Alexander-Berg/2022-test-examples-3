package ru.yandex.market.javaframework.tvm;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import ru.yandex.market.javaframework.tvm.configurers.tvm.UserTicketProtectedEndpointsConfigurer;
import ru.yandex.market.javaframework.yamlproperties.openapi.paths.OpenApiPaths;
import ru.yandex.market.javaframework.yamlproperties.openapi.paths.OperationSettings;
import ru.yandex.market.javaframework.yamlproperties.openapi.paths.PathSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserTicketProtectedEndpointsConfigurerTest {

    @Test
    public void checkUserTicket_True_Test() {
        final String path = "/test";
        final String method = HttpMethod.POST.name();

        final OperationSettings operationSettings = new OperationSettings();
        operationSettings.setCheckUserTicket(true);

        final PathSettings pathSettings = new PathSettings();
        pathSettings.put(method, operationSettings);

        final OpenApiPaths openApiPaths = new OpenApiPaths();
        openApiPaths.put(path, pathSettings);

        final UserTicketProtectedEndpointsConfigurer configurer =
            new UserTicketProtectedEndpointsConfigurer(openApiPaths);

        assertEquals(Set.of(new AntPathRequestMatcher(path, method)), configurer.getUserTicketProtectedPaths());
    }

    @Test
    public void checkUserTicket_False_Test() {
        final String path = "/test";
        final String method = HttpMethod.POST.name();

        final OperationSettings operationSettings = new OperationSettings();
        operationSettings.setDisableSecurity(true);

        final PathSettings pathSettings = new PathSettings();
        pathSettings.put(method, operationSettings);

        final OpenApiPaths openApiPaths = new OpenApiPaths();
        openApiPaths.put(path, pathSettings);

        final UserTicketProtectedEndpointsConfigurer configurer =
            new UserTicketProtectedEndpointsConfigurer(openApiPaths);

        assertNull(configurer.getUserTicketProtectedPaths());
    }
}
