package ru.yandex.market.partner.mvc.security;

import java.lang.management.ManagementFactory;
import java.util.StringJoiner;

import javax.management.ObjectName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.sec.JavaSecFunctionalTest;
import ru.yandex.market.security.SecManager;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SecurityControllerTest extends JavaSecFunctionalTest {

    @Autowired
    @Qualifier("secManager")
    SecManager contextSecManager;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(contextSecManager.canDo(anyString(), any())).thenAnswer(
                invocation -> secManager.canDo(invocation.getArgument(0), invocation.getArgument(1))
        );

        when(contextSecManager.hasAuthority(anyString(), anyString(), any())).thenAnswer(
                invocation -> secManager.hasAuthority(invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2))
        );
    }

    @Test
    void testGetAllowedActions() throws Exception {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getAllowedActions?format=json&id={campaignId}&_user_id={userId}&ADDED_INFO={actions}",
                6, 555, new StringJoiner(",")
                        .add("enableFeature@POST")
                        .add("/dco-enabled")
                        .toString());
        JsonTestUtil.assertEquals(response,
                //language=json
                "[\n" +
                        "    {\n" +
                        "      \"name\": \"enableFeature@POST\"\n" +
                        "    }\n" +
                        "  ]"
        );

        /*
            Проверяем, что метрики соломона не поломались. Проверяем по двум показателям:
            1. ThreadPoolObserver захватил getAllowedActionsExecutor и там есть атрибут TotalThreads
            2. После обращения к ручке выше TotalThreads не 0 - ее обработал нужный нам executor.
         */
        int totalThreads = getExecutorTotalThreadsFromJmx("coconCheckerExecutorObserver");
        assertTrue(totalThreads > 0);
    }

    @Test
    void testGetAllowedParams() throws Exception {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getAllowedParams?format=json&id={campaignId}&_user_id={userId}&paramTypes={actions}",
                2, 555, new StringJoiner(",")
                        .add("2")
                        .add("128")
                        .toString());

        JsonTestUtil.assertEquals(response,
                //language=json
                "[\n" +
                        "    {\n" +
                        "      \"name\": \"manageParamUpdate\",\n" +
                        "      \"entityId\": \"2\"\n" +
                        "    }," +
                        "    {\n" +
                        "      \"name\": \"manageParamUpdate\",\n" +
                        "      \"entityId\": \"128\"\n" +
                        "    }" +
                        "  ]"
        );

        /*
            Проверяем, что метрики соломона не поломались. Проверяем по двум показателям:
            1. ThreadPoolObserver захватил getAllowedParamsExecutor и там есть атрибут TotalThreads
            2. После обращения к ручке выше TotalThreads не 0 - ее обработал нужный нам executor.
         */
        int totalThreads = getExecutorTotalThreadsFromJmx("getAllowedParamsExecutorObserver");
        assertTrue(totalThreads > 0);
    }

    private static int getExecutorTotalThreadsFromJmx(String name) throws Exception {
        return (int) ManagementFactory.getPlatformMBeanServer().getAttribute(
                ObjectName.getInstance("ru.yandex.common.util.jmx:name=" + name + ",type=ThreadPoolObserver"),
                "TotalThreads"
        );
    }

}
