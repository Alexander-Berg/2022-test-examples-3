package ru.yandex.direct.inventori;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionResponse;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CampaignPredictionResponseTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Parameterized.Parameter(0)
    public String filename;

    @Parameterized.Parameter(1)
    public Class<? extends CampaignPredictionResponse> finalClass;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"responses/normal.json", CampaignPredictionAvailableResponse.class},
                new Object[]{"responses/reach_less_than.json", CampaignPredictionLowReachResponse.class}
        );
    }

    @Test
    public void testDeserialize() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream(filename);
        CampaignPredictionResponse response = mapper.readerFor(CampaignPredictionResponse.class).readValue(in);
        assertTrue(finalClass.isAssignableFrom(response.getClass()));
    }
}
