package ru.yandex.direct.core.aggregatedstatuses;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class AggregatedStatusDataMultiReasonsTest {
    static ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final String adgroupJson = "{\"cnts\":{\"ads\":4,\"b_s\":{\"DRAFT\":1,\"RUN_OK\":1,\"PAUSE_OK\":1," +
            "\"STOP_CRIT\":1},\"b_sts\":{\"SUSPENDED\":1,\"DRAFT\":1,\"REJECTED\":1},\"kw_s\":{\"RUN_OK\":190," +
            "\"PAUSE_OK\":10},\"kws\":200},\"fields\":{\"status_active\":\"YES\"," +
            "\"status_empty\":\"No\"},\"r\":[\"SUSPENDED_BY_USER\"],\"s\":\"PAUSE_OK\",\"sts\":[\"DRAFT\",\"REJECTED\"]}";

    private static final String keywordJson = "{\"fields\":{\"status_active\":\"YES\",\"status_empty\":\"No\"}," +
            "\"r\":[\"SUSPENDED_BY_USER\"],\"s\":\"PAUSE_OK\",\"sts\":[\"SUSPENDED\",\"REJECTED\"]}";

    private static final String adJson = "{\"fields\":{\"status_active\":\"YES\",\"status_empty\":\"No\"}," +
            "\"r\":[\"SUSPENDED_BY_USER\"],\"s\":\"PAUSE_OK\",\"sts\":[\"ARCHIVED\",\"DRAFT\"]}";

    private static final String campaignJson = "{\"cnts\":{\"grps\":10,\"s\":{\"DRAFT\":1,\"RUN_OK\":7," +
            "\"PAUSE_CRIT\":2},\"sts\":{\"DRAFT\":1,\"REJECTED\":2}},\"fields\":{\"status_active\":\"YES\"," +
            "\"status_empty\":\"No\"},\"r\":[\"SUSPENDED_BY_USER\"],\"s\":\"PAUSE_OK\",\"sts\":[\"SUSPENDED\"," +
            "\"REJECTED\"]}";

    private static SelfStatus selfStatus = new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER);

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String json;

    @Parameterized.Parameter(2)
    public Object object;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {"keyword", keywordJson, getKeywordData()},
                {"campaign", campaignJson, getCampaignData()},
                {"adgroup", adgroupJson, getAdGroupData()},
                {"ad", adJson, getAdData()},
        });
    }

    @BeforeClass
    public static void prepareMapper() {
        objectMapper.registerModule(new Jdk8Module());
    }

    @Test
    public void fromJson() throws IOException {
        Object o = objectMapper.readValue(json, object.getClass());
        assertThat("Deserialized ok", object, beanDiffer(o));
    }

    private static AggregatedStatusAdData getKeywordData() {
        return new AggregatedStatusAdData(
                List.of(AdStatesEnum.SUSPENDED, AdStatesEnum.REJECTED),
                new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER)
        );
    }

    private static AggregatedStatusAdData getAdData() {
        return new AggregatedStatusAdData(
                // Нужна колекция сохраняющая порядок иначе тест будет флапать
                List.of(AdStatesEnum.ARCHIVED, AdStatesEnum.DRAFT),
                new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER)
        );
    }

    private static AggregatedStatusAdGroupData getAdGroupData() {
        AdGroupCounters counters = new AdGroupCounters(
                4, 200, 0,
                Map.of(GdSelfStatusEnum.PAUSE_OK, 1, GdSelfStatusEnum.RUN_OK, 1, GdSelfStatusEnum.DRAFT, 1, GdSelfStatusEnum.STOP_CRIT, 1),
                Map.of(AdStatesEnum.DRAFT,1, AdStatesEnum.REJECTED, 1, AdStatesEnum.SUSPENDED, 1),
                Map.of(GdSelfStatusEnum.PAUSE_OK, 10, GdSelfStatusEnum.RUN_OK, 190),
                Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap());
        return new AggregatedStatusAdGroupData(
                List.of(AdGroupStatesEnum.DRAFT, AdGroupStatesEnum.REJECTED), counters,
                selfStatus.getStatus(), selfStatus.getReasons()
        );
    }

    private static AggregatedStatusCampaignData getCampaignData() {
        CampaignCounters counters = new CampaignCounters(
                10,
                Map.of(GdSelfStatusEnum.DRAFT, 1, GdSelfStatusEnum.RUN_OK, 7, GdSelfStatusEnum.PAUSE_CRIT, 2),
                Map.of(AdGroupStatesEnum.DRAFT, 1, AdGroupStatesEnum.REJECTED, 2));
        return new AggregatedStatusCampaignData(
                List.of(CampaignStatesEnum.SUSPENDED, CampaignStatesEnum.REJECTED),
                counters,
                selfStatus
        );
    }
}
