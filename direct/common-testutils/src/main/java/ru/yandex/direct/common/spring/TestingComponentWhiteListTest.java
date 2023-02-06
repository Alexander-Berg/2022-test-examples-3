package ru.yandex.direct.common.spring;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNotNull;

@RunWith(Parameterized.class)
public class TestingComponentWhiteListTest {
    private static final String PACKAGE_NAME = "ru.yandex.direct";

    @Parameterized.Parameter
    public String testingComponentClass;

    /**
     * Аннотация {@link TestingComponent} не должна применяться для сервисов,
     * которые в итоге хочется дать <b>внешним</b> пользователям.
     * Если разрабатываешь новую продуктовую функциональность, то использовать её не стоит.
     */
    private static final Set<String> TESTING_COMPONENT_WHITE_LIST = Set.of(
            "ru.yandex.direct.jobs.DebugJobRunner",
            "ru.yandex.direct.jobs.campaign.CleanTestingCampaignsJob",
            "ru.yandex.direct.jobs.campaign.repository.CleanTestingCampaignsRepository",
            "ru.yandex.direct.intapi.entity.recommendation.controller.RecommendationController",
            "ru.yandex.direct.intapi.entity.recommendation.controller.service.RecommendationInsertService",
            "ru.yandex.direct.web.core.swagger.SwaggerRedirectController",
            "ru.yandex.direct.web.entity.admin.DefectController",
            "ru.yandex.direct.web.entity.deal.controller.DealTestController",
            "ru.yandex.direct.web.entity.test.TestController",
            "ru.yandex.direct.internaltools.tools.idm.tool.AddIdmGroupTool",
            "ru.yandex.direct.internaltools.tools.idm.tool.ManageIdmGroupMembersTool",
            "ru.yandex.direct.internaltools.tools.idm.tool.ManageIdmGroupRolesTool",
            "ru.yandex.direct.internaltools.tools.trackingphone.UnlinkTelephonyTool",
            "ru.yandex.direct.jobs.DummyTestingComponent",
            "ru.yandex.direct.common.DummyTestingComponent"
    );

    @Parameterized.Parameters(name = "{0}")
    public static List<String> parametersForCheckTestingComponentAreKnown() {
        var items = new Reflections(PACKAGE_NAME).getTypesAnnotatedWith(TestingComponent.class).stream()
                .map(Class::getName)
                .collect(Collectors.toList());
        if (items.isEmpty()) {
//         Небольшой хак, чтобы аркадийный CI не ругался, что тест NOT_LAUNCHED
//         Проблема в том, что Parameterized-тест без children в isTest() зачем-то возвращает true
            items.add(null);
        }
        return items;
    }

    @Before
    public void setUp() {
        assumeNotNull(testingComponentClass);
    }

    @Test
    public void checkTestingComponentAreKnown() {
        assertThat(testingComponentClass).isIn(TESTING_COMPONENT_WHITE_LIST);
    }
}
