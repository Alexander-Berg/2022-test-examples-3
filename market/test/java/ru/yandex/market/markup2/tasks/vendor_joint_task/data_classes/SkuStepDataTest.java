package ru.yandex.market.markup2.tasks.vendor_joint_task.data_classes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.vendor_sku_relevance.ParameterValue;
import ru.yandex.market.markup2.utils.JsonUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class SkuStepDataTest {

    private ObjectMapper mapper = JsonUtils.getMapper();

    @Test
    public void testDeserialization() throws IOException {
        URL json = Resources.getResource("fixtures/sku_step_data_fix1.json");
        SkuStepData data = mapper.readValue(json, SkuStepData.class);
        SkuStepData.SkuInfo skuInfo = data.getSkuList().iterator().next();
        assertNotNull(skuInfo.getDefiningParameters());
        assertNotNull(skuInfo.getPicUrls());
        assertEquals(228, skuInfo.getSkuId());
        List<ParameterValue> infoParameters = skuInfo.getInfoParameters();
        assertNotNull(infoParameters);
        assertTrue(infoParameters.isEmpty());
    }
}
