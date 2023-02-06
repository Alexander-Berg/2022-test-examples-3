package ru.yandex.direct.grid.processing.service.goal;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaSegmentPreset;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.CREATION_OF_METRIKA_SEGMENTS_BY_PRESETS_ENABLED;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GoalGraphQLServiceSegmentPresetsTest {

    private static final MetrikaSegmentPreset PRESET_1 = new MetrikaSegmentPreset()
            .withPresetId(1)
            .withCounterId(1)
            .withDomain("ya.ru")
            .withName("Preset name 1");
    private static final MetrikaSegmentPreset PRESET_2 = new MetrikaSegmentPreset()
            .withPresetId(2)
            .withCounterId(1)
            .withDomain("ya.ru")
            .withName("Preset name 2");

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy:{login:\"%s\"}) {\n"
            + "     segmentPresets {\n"
            + "             presets {\n"
            + "             presetId\n"
            + "             counterId\n"
            + "             name\n"
            + "             domain\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    private Steps steps;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private MetrikaSegmentService metrikaSegmentService;

    private GridGraphQLContext context;
    private UserInfo userInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        var clientId = userInfo.getClientInfo().getClientId();
        steps.featureSteps().addClientFeature(clientId, CREATION_OF_METRIKA_SEGMENTS_BY_PRESETS_ENABLED, true);

        when(metrikaSegmentService.getSegmentPresets(clientId)).thenReturn(List.of(PRESET_1, PRESET_2));

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
    }

    @Test
    public void testGetSegmentPresets() {
        var query = String.format(QUERY_TEMPLATE, userInfo.getUser().getLogin());
        var result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();

        Map<Object, Object> data = result.getData();
        Map<Object, Object> presets = Map.of("presets", List.of(presetToMap(PRESET_1), presetToMap(PRESET_2)));
        Map<Object, Object> segmentPresets = Map.of("segmentPresets", presets);
        Map<Object, Object> expected = Map.of("client", segmentPresets);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private static Map<String, Object> presetToMap(MetrikaSegmentPreset preset) {
        return Map.of(
                "counterId", preset.getCounterId(),
                "presetId", preset.getPresetId(),
                "name", preset.getName(),
                "domain", preset.getDomain()
        );
    }
}
