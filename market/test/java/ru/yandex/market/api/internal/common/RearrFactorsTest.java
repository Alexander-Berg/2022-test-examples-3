package ru.yandex.market.api.internal.common;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 17.07.2020
 */
public class RearrFactorsTest extends UnitTestBase {

    @Test
    public void noFlags() {
        RearrFactors rearrFactors = new RearrFactors("");
        Assert.assertTrue(rearrFactors.getRearrFactors().isEmpty());
    }

    @Test
    public void hasSomeFlags() {
        RearrFactors rearrFactors = new RearrFactors("key1=value1;key2=value2;key3=value3");
        Assert.assertEquals(3, rearrFactors.getRearrFactors().size());
    }

    @Test
    public void noApiFlags() {
        RearrFactors rearrFactors = new RearrFactors("key1=value1;key2=value2;key3=value3");
        Assert.assertTrue(rearrFactors.getRearrFactors(RearrFactor.API_FLAG_PREFIX).isEmpty());
        Assert.assertFalse(rearrFactors.getRawRearrFactors().isEmpty());
    }

    @Test
    public void multipleFlags() {
        String rawRearrFactors = ";;;;market_capi_test=value;market_capi_test=second;" +
                "market_capi_empty=;" +
                "market_capi_no_value;" +
                "market_capi_key_value=1=2;" +
                "key1=value1;;;key2=value2;key3=value3;;;;;";

        RearrFactors rearrFactors = new RearrFactors(rawRearrFactors);

        Map<String, String> apiRearrFactors = rearrFactors.getRearrFactors(RearrFactor.API_FLAG_PREFIX);
        Assert.assertEquals(4, apiRearrFactors.size());
        Assert.assertFalse(apiRearrFactors.isEmpty());
        Assert.assertEquals("value", apiRearrFactors.get("market_capi_test"));
        Assert.assertTrue(apiRearrFactors.containsKey("market_capi_empty"));
        Assert.assertTrue(apiRearrFactors.containsKey("market_capi_no_value"));
        Assert.assertEquals("1=2", apiRearrFactors.get("market_capi_key_value"));
    }
}
