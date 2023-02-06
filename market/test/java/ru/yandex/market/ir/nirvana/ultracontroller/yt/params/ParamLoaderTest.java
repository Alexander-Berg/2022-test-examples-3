package ru.yandex.market.ir.nirvana.ultracontroller.yt.params;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@SuppressWarnings("CheckStyle")
public class ParamLoaderTest {
    @Test
    public void computeEnrichedOfferFields() throws Exception {
        AppParams params = AppParams.load("src/test/resources/uc_config.json");
        EnrichedOfferFields enrichedOfferFields = params.getEnrichedOfferFields();

        List<EnrichedOffer2YtField> fields = ParamLoader.computeEnrichedOfferFields(enrichedOfferFields);
        Assert.assertEquals(3, fields.size());

        Assert.assertEquals(150, params.getHeavyCommandsRetries());
    }
}
