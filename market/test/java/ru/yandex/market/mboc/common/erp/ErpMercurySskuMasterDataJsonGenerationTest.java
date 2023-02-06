package ru.yandex.market.mboc.common.erp;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.erp.model.ErpMercurySskuMasterData;

/**
 * @author dmserebr
 * @date 18/10/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpMercurySskuMasterDataJsonGenerationTest {
    @Test
    public void testEmptyJsonGeneration() {
        ErpMercurySskuMasterData data = new ErpMercurySskuMasterData();
        Assertions.assertThat(data.generateJsonString()).isEqualTo("{\"vetis_is_supervised\":true}");
    }

    @Test
    public void testJsonGeneration() {
        ErpMercurySskuMasterData data = new ErpMercurySskuMasterData();
        data.setBoxLengthInCm(100.0);
        data.setBoxWidthInCm(120.0);
        data.setBoxHeightInCm(140.0);
        data.setWeightGrossInGrams(42.0);
        data.setWeightNetInGrams(45.0);
        data.setVetisGuids(ImmutableList.of("guid1", "guid2"));
        data.setGtins(ImmutableList.of("1234", "9907934"));

        // note that WeightGrossInGrams should be ignored
        Assertions.assertThat(data.generateJsonString()).isEqualTo(
            "{\"vetis_is_supervised\":true, \"gross_width\":120.0, \"gross_length\":100.0, \"gross_height\":140.0, " +
                "\"net_weight\":45.0, \"guids\":[\"guid1\", \"guid2\"], \"gtins\":[\"1234\", \"9907934\"]}");
    }

    @Test
    public void testEmptyGuidAndGtinList() {
        ErpMercurySskuMasterData data = new ErpMercurySskuMasterData();
        data.setBoxLengthInCm(100.0);
        data.setBoxWidthInCm(120.0);
        data.setBoxHeightInCm(140.0);
        data.setHonestSign(true);
        data.setCustomsCommodityCode("1234567890");
        data.setVetisGuids(Collections.emptyList());
        data.setGtins(null); // same behaviour as empty list

        Assertions.assertThat(data.generateJsonString()).isEqualTo(
            "{\"vetis_is_supervised\":true, \"gross_width\":120.0, \"gross_length\":100.0, \"gross_height\":140.0, " +
                "\"tnved\":\"1234567890\", \"honest_sign\":true}");
    }
}
