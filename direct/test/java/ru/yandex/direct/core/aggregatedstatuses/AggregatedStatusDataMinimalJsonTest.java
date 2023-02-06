package ru.yandex.direct.core.aggregatedstatuses;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;

import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class AggregatedStatusDataMinimalJsonTest {
    private static ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final String json = "{}";
    private static final String jsonFields = "{\"fields\": {}}";
    private static final String jsonStates = "{\"sts\": []}";

    private static final List<GdSelfStatusReason> noReasons = null;

    @BeforeClass
    public static void prepareMapper() {
        objectMapper.registerModule(new Jdk8Module());
    }

    @Test
    public void keywordFromJson() throws IOException {
        AggregatedStatusAdData data = objectMapper.readValue(json,
                AggregatedStatusAdData.class);
        assumeTrue(data.getStates() != null);
        assumeTrue(data.getStates().isEmpty());
        assumeTrue(data.getStatus().isEmpty());
        AggregatedStatusAdData expectedData = new AggregatedStatusAdData(null, null, noReasons);
        assertThat("Deserialized ok", expectedData, beanDiffer(data));
    }

    @Test
    public void adGroupFromJson() throws IOException {
        AggregatedStatusAdGroupData data = objectMapper.readValue(json,
                AggregatedStatusAdGroupData.class);
        AggregatedStatusAdGroupData expectedData = new AggregatedStatusAdGroupData(null, new AdGroupCounters(), null, noReasons);
        assertThat("Deserialized ok", expectedData, beanDiffer(data));
    }

    @Test
    public void campaignFromJson() throws IOException {
        AggregatedStatusCampaignData data = objectMapper.readValue(json,
                AggregatedStatusCampaignData.class);
        AggregatedStatusCampaignData expectedData = new AggregatedStatusCampaignData( null, new CampaignCounters(), null, noReasons);
        assertThat("Deserialized ok", expectedData, beanDiffer(data));
    }

    @Test
    public void fieldsFromJson() throws IOException {
        AggregatedStatusAdData data = objectMapper.readValue(jsonFields,
                AggregatedStatusAdData.class);
        AggregatedStatusAdData expectedData = new AggregatedStatusAdData(null, null, noReasons);
        assertThat("Deserialized ok", expectedData, beanDiffer(data));
    }

    @Test
    public void statesFromJson() throws IOException {
        AggregatedStatusAdData data = objectMapper.readValue(jsonStates,
                AggregatedStatusAdData.class);
        AggregatedStatusAdData expectedData = new AggregatedStatusAdData(Collections.emptyList(), null, noReasons);
        assertThat("Deserialized ok", expectedData, beanDiffer(data));
    }
}
