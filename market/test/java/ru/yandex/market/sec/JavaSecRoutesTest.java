package ru.yandex.market.sec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.cocon.security.AllowedActionsManager;
import ru.yandex.market.security.data.Kampfer;
import ru.yandex.market.security.data.kampfer.EntityNotFoundException;
import ru.yandex.market.security.data.kampfer.PermissionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaSecRoutesTest extends JavaSecFunctionalTest {
    /**
     * Не более одного из этих прав может быть у ручки. Чтобы избежать множественных вызовов {@code ShopRolesChecker}.
     * Проверка на администраторов делается для любой не администраторской роли, см {@code CampaignRolesService#ADMIN_ROLES}.
     */
    private static final Set<String> UNIQUE_PERMISSIONS =
            Set.of("SHOP_ADMIN", "SHOP_TECHNICAL", "SHOP_OPERATOR", "SHOP_EVERYONE");

    @Autowired
    @Qualifier("mvcHandler")
    private Handler mvcHandler;

    @Autowired
    private AllowedActionsManager allowedActionsManager;

    /**
     * Проверка, что на все spring-mvc ручки (контроллеры) добавлены java-sec правила.
     * Этот тест НЕ покрывает servantlet'ы, с которыми сильно сложнее из-за CrudServantlet, у которых
     * нельзя декларативно определить, какие ручки действительно будут вызываться.
     */
    @DisplayName("На все MVC ручки добавлены правила в java-sec")
    @Test
    void allRoutesHasRules() throws ServletException {
        ServletHandler servletHandler = (ServletHandler) mvcHandler;
        WebApplicationContext wac = ((DispatcherServlet) servletHandler
                .getServlet("dispatcherServlet")
                .getServlet())
                .getWebApplicationContext();
        RequestMappingHandlerMapping handlerMapping = (RequestMappingHandlerMapping) wac
                .getBean("requestMappingHandlerMapping");
        Set<String> missingRules = findMissingRules(handlerMapping);
        assertTrue(missingRules.isEmpty(), missingRules::toString);
    }

    /**
     * Проверяет, что при проверке доступов к любой ручке не будет несколько раз вызываться {@code ShopRolesChecker}.
     */
    @Test
    void noMultipleShopsRoleChecker() {
        Kampfer kampfer = kampferFactory.getKampfer(DOMAIN, null);
        EntityNotFoundException entityNotFoundException = assertThrows(EntityNotFoundException.class, () -> {
            for (int i = 1; ; i++) {
                // проверяем, есть ли следующая операция, в противном случае EntityNotFoundException
                kampfer.operation().getOperation(i);
                List<PermissionModel> permissions = kampfer.permission().getPermissions(i);
                if (permissions.stream()
                        .map(PermissionModel::getAuthName)
                        .filter(UNIQUE_PERMISSIONS::contains)
                        .count() > 1) {
                    fail("Операция " + kampfer.operation().getOperation(i).getName() +
                            " имеет несколько проверок доступов к магазинам");
                }
            }
        });

        // на всякий пожарный проверим, что у нас больше 750 ручек
        assertThat(((Number) entityNotFoundException.getKey()).intValue())
                .isGreaterThan(750);
    }

    private Set<String> findMissingRules(final RequestMappingHandlerMapping handlerMapping) {
        Set<String> missingRules = new HashSet<>();
        Set<String> knownOperations = allowedActionsManager.getOperations();
        for (RequestMappingInfo mappingInfo : handlerMapping.getHandlerMethods().keySet()) {
            for (RequestMethod method : mappingInfo.getMethodsCondition().getMethods()) {
                for (String pattern : mappingInfo.getPatternsCondition().getPatterns()) {
                    if (ShouldSkipJavaSec.JAVA_SEC_INTERCEPTOR_IGNORED_EXACT_PATHS.contains(pattern)) {
                        continue;
                    }
                    //TODO use ActionNameProvider
                    String actionName = pattern.substring(1) + "@" + method.name();
                    if (authoritiesLoader.load(DOMAIN, actionName) == null
                            && !knownOperations.contains(actionName)) {
                        missingRules.add(actionName);
                    }
                }
            }
        }
        return missingRules;
    }

}

