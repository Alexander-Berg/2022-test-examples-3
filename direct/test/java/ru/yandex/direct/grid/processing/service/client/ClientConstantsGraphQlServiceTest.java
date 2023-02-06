package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.BRANDSAFETY_ADDITIONAL_CATEGORIES;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientConstantsGraphQlServiceTest {

    private final static String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    clientConstants {\n" +
            "      allowedBrandSafetyCategoriesByClient {\n" +
            "        id,\n" +
            "        name,\n" +
            "        description\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private TestCryptaSegmentRepository repository;

    private UserInfo userWithFeature;
    private UserInfo userWithoutFeature;

    @Before
    public void setUp() {
        setLocale(Locale.forLanguageTag("ru"));

        userWithFeature = userSteps.createUser(generateNewUser());
        userWithoutFeature = userSteps.createUser(generateNewUser());
        featureSteps.addClientFeature(userWithFeature.getClientId(), BRANDSAFETY_ADDITIONAL_CATEGORIES, true);

        var goal = new Goal()
                .withId(4_294_967_297L)
                .withType(GoalType.BRANDSAFETY)
                .withName("Test")
                .withTankerNameKey("brandsafety_adult_name")
                .withTankerDescriptionKey("brandsafety_adult_description");

        var featureGoal = new Goal()
                .withId(4_294_967_302L) // Under feature
                .withType(GoalType.BRANDSAFETY)
                .withName("Under feature")
                .withTankerNameKey("brandsafety_politics_name")
                .withTankerDescriptionKey("brandsafety_politics_description");

        repository.addAll(Set.of((Goal) goal, (Goal) featureGoal));
    }

    @Test
    public void shouldNotRespondByCategoryUnderFeatureForUserHasNoThisFeature() {
        var context = ContextHelper.buildContext(userWithoutFeature.getUser());
        var query = String.format(QUERY_TEMPLATE, userWithoutFeature.getUser().getLogin());
        var expected = Map.of(
                "client",
                Map.of(
                        "clientConstants",
                        Map.of(
                                "allowedBrandSafetyCategoriesByClient",
                                Collections.singletonList(
                                        Map.of(
                                                "id", 4_294_967_297L,
                                                "name", "Взрослый контент",
                                                "description", "Контент, демонстрирующий наготу, явное сексуальное поведение, материалы о сексе. Сексуальный и эротический смысл такого контента является основным."
                                        )
                                )
                        )
                )
        );

        var result = processor.processQuery(null, query, null, context);
        var data = result.getData();

        assertThat(data).is(matchedBy(beanDiffer(expected)));

    }

    @Test
    public void shouldSendAllCategoriesWhenClientHasFeature() {
        var context = ContextHelper.buildContext(userWithFeature.getUser());
        var query = String.format(QUERY_TEMPLATE, userWithFeature.getUser().getLogin());
        var expected = Map.of(
                "client",
                Map.of(
                        "clientConstants",
                        Map.of(
                                "allowedBrandSafetyCategoriesByClient",
                                List.of(
                                        Map.of(
                                                "id", 4_294_967_297L,
                                                "name", "Взрослый контент",
                                                "description", "Контент, демонстрирующий наготу, явное сексуальное поведение, материалы о сексе. Сексуальный и эротический смысл такого контента является основным."
                                        ),
                                        Map.of(
                                                "id", 4_294_967_302L,
                                                "name", "Политика",
                                                "description", "Современная политика или упоминания политиков, политическая агитация, расследования, международные дипломатические и военные конфликты."
                                        )
                                )
                        )
                )
        );

        var result = processor.processQuery(null, query, null, context);
        var data = result.getData();

        assertThat(data).is(matchedBy(beanDiffer(expected)));

    }
}
