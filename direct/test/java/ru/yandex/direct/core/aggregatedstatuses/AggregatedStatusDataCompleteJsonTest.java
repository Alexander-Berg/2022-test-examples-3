package ru.yandex.direct.core.aggregatedstatuses;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.KeywordStatesEnum;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class AggregatedStatusDataCompleteJsonTest {
    static ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final String adgroupJson = "{\"cnts\":{\"ads\":4,\"b_s\":{\"RUN_OK\":1,\"PAUSE_OK\":1," +
            "\"STOP_CRIT\":1,\"DRAFT\":1},\"b_sts\":{\"SUSPENDED\":1,\"DRAFT\":1,\"REJECTED\":1}," +
            "\"kw_s\":{\"RUN_OK\":190,\"PAUSE_OK\":10},\"kw_sts\":{\"SUSPENDED\":10},\"kws\":200,\"rets\":0}," +
            "\"r\":[\"SUSPENDED_BY_USER\"],\"rr\":{\"COMMON\":[500,501],\"PERFORMANCE\":[501,502]},\"s\":\"PAUSE_OK\"," +
            "\"sts\":[\"DRAFT\",\"REJECTED\"]}";

    private static final String adJson = "{\"r\":[\"SUSPENDED_BY_USER\"],\"rr\":{\"COMMON\":[500,501]," +
            "\"PERFORMANCE\":[501,502]},\"s\":\"PAUSE_OK\",\"sts\":[\"ARCHIVED\",\"DRAFT\"]}";

    private static final String campaignJson = "{\"cnts\":{\"grps\":10,\"s\":{\"RUN_OK\":7,\"PAUSE_CRIT\":2," +
            "\"DRAFT\":1},\"sts\":{\"DRAFT\":1,\"REJECTED\":2}},\"r\":[\"SUSPENDED_BY_USER\"]," +
            "\"rr\":{\"COMMON\":[500,501],\"PERFORMANCE\":[501,502]},\"s\":\"PAUSE_OK\"," +
            "\"sts\":[\"SUSPENDED\",\"REJECTED\"]}";

    private static final SelfStatus selfStatus = new SelfStatus(GdSelfStatusEnum.PAUSE_OK,
            GdSelfStatusReason.SUSPENDED_BY_USER,
            Map.of(ModerationDiagType.COMMON, new LinkedHashSet<>(List.of(500L, 501L)),
                    ModerationDiagType.PERFORMANCE, new LinkedHashSet<>(List.of(501L, 502L))));

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String json;

    @Parameterized.Parameter(2)
    public Object object;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
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
    public void toJson() throws JsonProcessingException {
        String actualJson = objectMapper.writeValueAsString(object);
        assertEquals("Serialized ok", json, actualJson);
    }

    @Test
    public void fromJson() throws IOException {
        assertThat("Deserialized ok", object, beanDiffer(objectMapper.readValue(json, object.getClass())));
    }

    private static AggregatedStatusKeywordData getKeywordData() {
        return new AggregatedStatusKeywordData(
                new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER)
        );
    }

    private static AggregatedStatusAdData getAdData() {
        return new AggregatedStatusAdData(
                // Нужна коллекция, сохраняющая порядок, иначе тест будет флапать
                List.of(AdStatesEnum.ARCHIVED, AdStatesEnum.DRAFT),
                selfStatus
        );
    }

    private static AggregatedStatusAdGroupData getAdGroupData() {
        AdGroupCounters counters = new AdGroupCounters(
                4, 200, 0,
                Map.of(GdSelfStatusEnum.PAUSE_OK, 1, GdSelfStatusEnum.RUN_OK, 1, GdSelfStatusEnum.DRAFT, 1,
                        GdSelfStatusEnum.STOP_CRIT, 1),
                Map.of(AdStatesEnum.DRAFT, 1, AdStatesEnum.REJECTED, 1, AdStatesEnum.SUSPENDED, 1),
                Map.of(GdSelfStatusEnum.PAUSE_OK, 10, GdSelfStatusEnum.RUN_OK, 190),
                Map.of(KeywordStatesEnum.SUSPENDED, 10),
                Collections.emptyMap(), Collections.emptyMap());
        return new AggregatedStatusAdGroupData(
                List.of(AdGroupStatesEnum.DRAFT, AdGroupStatesEnum.REJECTED),
                counters,
                selfStatus
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
