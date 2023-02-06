package ru.yandex.direct.core.aggregatedstatuses;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;

import static org.junit.Assert.assertEquals;

public class AggregatedStatusDataMinimalTest {
    private static ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final String emptyJson = "{}";
    private static final String emptyAdGroupCountersJson = "{\"cnts\":{\"ads\":0,\"kws\":0,\"rets\":0}}";
    private static final String emptyCampaignCounters = "{\"cnts\":{\"grps\":0}}";

    @BeforeClass
    public static void prepareMapper() {
        objectMapper.registerModule(new Jdk8Module());
    }


    @Test
    public void keywordToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusAdData(null, (GdSelfStatusEnum) null));
        assertEquals("Serialized ok", emptyJson, json);
    }

    @Test
    public void adGroupToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusAdGroupData(null, null, null));
        assertEquals("Serialized ok", emptyJson, json);
    }

    @Test
    public void campaignToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusCampaignData(null, null, null));
        assertEquals("Serialized ok", emptyJson, json);
    }

    @Test
    public void emptyStatesToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusAdData(Collections.emptyList(), (GdSelfStatusEnum) null));
        assertEquals("Serialized ok", emptyJson, json);
    }

    @Test
    public void emptyAdGroupCountersToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusAdGroupData(null, new AdGroupCounters(), null));
        assertEquals("Serialized ok", emptyAdGroupCountersJson, json);
    }

    @Test
    public void emptyCampaignCountersToJson() throws IOException {
        String json = objectMapper.writeValueAsString(new AggregatedStatusCampaignData(null, new CampaignCounters(), null));
        assertEquals("Serialized ok", emptyCampaignCounters, json);
    }
}
