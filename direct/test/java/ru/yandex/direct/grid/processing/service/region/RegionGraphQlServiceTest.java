package ru.yandex.direct.grid.processing.service.region;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.geo.model.GeoRegion;
import ru.yandex.direct.core.entity.geo.model.GeoRegionType;
import ru.yandex.direct.core.entity.geo.model.GeoRegionWithAdRegion;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;

@GridProcessingTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class RegionGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = "{\n" +
            "  regionByText(text: \"%s\") {\n" +
            "    id,\n" +
            "    type,\n" +
            "    nameEn,\n" +
            "    nameRu,\n" +
            "    nameTr,\n" +
            "    nameUa,\n" +
            "    parent\n" +
            "    adRegion {\n" +
            "      id,\n" +
            "      type,\n" +
            "      nameEn,\n" +
            "      nameRu,\n" +
            "      nameTr,\n" +
            "      nameUa,\n" +
            "      parent\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private RegionDataService regionDataService;

    private ClientInfo clientInfo;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser());
    }

    @Test
    public void positive() {
        var commercialRegion = new GeoRegion()
                .withId(213L)
                .withName("Москва")
                .withEname("Moscow")
                .withTrname("Moskova")
                .withUaname("Мiсква")
                .withType(GeoRegionType.CITY)
                .withParentId(3L);
        var region = new GeoRegionWithAdRegion()
                .withId(213L)
                .withName("Москва")
                .withEname("Moscow")
                .withTrname("Moskova")
                .withUaname("Мiсква")
                .withType(GeoRegionType.CITY)
                .withParentId(3L)
                .withAdRegion(commercialRegion);

        given(regionDataService.searchRegions(eq("Москва"))).willReturn(singletonList(region));

        var query = String.format(QUERY_TEMPLATE, "Москва");

        var result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();

        var expected = Map.of(
                "regionByText", singletonList(
                        Map.of(
                                "id", 213L,
                                "nameRu", "Москва",
                                "nameEn", "Moscow",
                                "nameTr", "Moskova",
                                "nameUa", "Мiсква",
                                "type", 6,
                                "parent", 3L,
                                "adRegion", Map.of(
                                        "id", 213L,
                                        "nameRu", "Москва",
                                        "nameEn", "Moscow",
                                        "nameTr", "Moskova",
                                        "nameUa", "Мiсква",
                                        "type", 6,
                                        "parent", 3L
                                )
                        )
                )
        );

        assertThat(data, beanDiffer(expected));
    }

}
