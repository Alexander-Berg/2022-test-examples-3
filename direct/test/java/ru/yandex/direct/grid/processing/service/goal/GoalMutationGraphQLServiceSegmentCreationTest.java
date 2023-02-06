package ru.yandex.direct.grid.processing.service.goal;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaSegmentPreset;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaSegmentByPresets;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaSegmentsByPresetsPayload;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdPresetsByCounter;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.metrika.client.model.response.Segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.feature.FeatureName.CREATION_OF_METRIKA_SEGMENTS_BY_PRESETS_ENABLED;
import static ru.yandex.direct.grid.processing.service.goal.GoalDataConverter.toGdSegment;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GoalMutationGraphQLServiceSegmentCreationTest {

    private static final int DEFAULT_ID = 1;

    private static final MetrikaSegmentPreset PRESET_1 = new MetrikaSegmentPreset()
            .withPresetId(1)
            .withCounterId(DEFAULT_ID)
            .withDomain("ya.ru")
            .withName("Preset name 1");
    private static final MetrikaSegmentPreset PRESET_2 = new MetrikaSegmentPreset()
            .withPresetId(2)
            .withCounterId(DEFAULT_ID)
            .withDomain("ya.ru")
            .withName("Preset name 2");
    private static final Segment SEGMENT_1 = new Segment()
            .withId(1)
            .withCounterId(DEFAULT_ID)
            .withName("Preset name 1")
            .withExpression("ym:s:isNewUser=='Yes'");
    private static final Segment SEGMENT_2 = new Segment()
            .withId(2)
            .withCounterId(DEFAULT_ID)
            .withName("Preset name 2")
            .withExpression("ym:s:isNewUser=='No'");

    private final static String MUTATION_NAME = "createMetrikaSegmentByPreset";
    private final static String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult{\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    segments {\n"
            + "      id\n"
            + "      counterId\n"
            + "      name\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdMetrikaSegmentByPresets, GdMetrikaSegmentsByPresetsPayload>
            CREATE_SEGMENT_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                GdMetrikaSegmentByPresets.class, GdMetrikaSegmentsByPresetsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private MetrikaSegmentService metrikaSegmentService;

    @Autowired
    private Steps steps;

    private User user;

    @Before
    public void before() {
        var userInfo = steps.userSteps().createDefaultUser();
        user = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(user);
        steps.featureSteps().addClientFeature(user.getClientId(), CREATION_OF_METRIKA_SEGMENTS_BY_PRESETS_ENABLED, true);

        when(metrikaSegmentService.getSegmentPresets(userInfo.getClientId())).thenReturn(List.of(PRESET_1, PRESET_2));

        when(metrikaSegmentService.createMetrikaSegmentsByPresets(Map.of(DEFAULT_ID, List.of(PRESET_1, PRESET_2))))
                .thenReturn(List.of(SEGMENT_1, SEGMENT_2));
    }

    @Test
    public void testCreateSegmentByPreset() {
        var presetIds = List.of(PRESET_1.getPresetId(), PRESET_2.getPresetId());
        var input = new GdMetrikaSegmentByPresets()
                .withItems(List.of(new GdPresetsByCounter().withCounterId(DEFAULT_ID).withPresetIds(presetIds)));

        var result = processor.doMutationAndGetPayload(CREATE_SEGMENT_MUTATION, input, user);

        assertThat(result.getValidationResult()).isNull();

        var expectedSegment1 = toGdSegment(SEGMENT_1);
        var expectedSegment2 = toGdSegment(SEGMENT_2);
        assertThat(result.getSegments()).isEqualTo(List.of(expectedSegment1, expectedSegment2));
    }
}
