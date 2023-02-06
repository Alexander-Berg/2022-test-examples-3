package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.time.Instant;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuGoldenItemsUpdateTaskMergeTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SSKU_KEY_1 = new ShopSkuKey(1, "1");
    private static final ShopSkuKey SSKU_KEY_2 = new ShopSkuKey(2, "2");
    private static final ShopSkuKey SSKU_KEY_3 = new ShopSkuKey(3, "3");

    @Test
    public void testConsequentMerge() {
        // given
        MasterData masterData11 = new MasterData().setShopSkuKey(SSKU_KEY_1).setCustomsCommodityCode("11");
        MasterData masterData12 = new MasterData().setShopSkuKey(SSKU_KEY_2).setCustomsCommodityCode("11");
        ReferenceItemWrapper referenceItemWrapper12 = referenceItem(SSKU_KEY_2, 12);
        ReferenceItemWrapper referenceItemWrapper13 = referenceItem(SSKU_KEY_3, 13);
        SskuGoldenParamValue paramValue112 = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(SSKU_KEY_1)
            .setMdmParamId(2)
            .setUpdatedTs(Instant.EPOCH)
            .setString("112");
        SskuGoldenParamValue paramValue113 = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(SSKU_KEY_1)
            .setMdmParamId(3)
            .setUpdatedTs(Instant.EPOCH)
            .setString("113");
        SskuGoldenParamValue paramValue122 = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(SSKU_KEY_2)
            .setMdmParamId(2)
            .setUpdatedTs(Instant.EPOCH)
            .setString("122");
        SskuGoldenParamValue.Key delete117 =
            new SskuGoldenParamValue.Key(SSKU_KEY_1.getSupplierId(), SSKU_KEY_1.getShopSku(), 7);
        SskuGoldenParamValue.Key delete139 =
            new SskuGoldenParamValue.Key(SSKU_KEY_3.getSupplierId(), SSKU_KEY_3.getShopSku(), 9);
        SskuGoldenItemsUpdateTask task1 = new SskuGoldenItemsUpdateTask(
            Stream.of(referenceItemWrapper12, referenceItemWrapper13)
                .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity())),
            Stream.of(masterData11, masterData12)
                .collect(Collectors.toMap(MasterData::getShopSkuKey, Function.identity())),
            Stream.of(paramValue112, paramValue122, paramValue113)
                .collect(Collectors.toMap(SskuGoldenParamValue::getGoldenKey, Function.identity())),
            Set.of(delete117, delete139)
        );

        MasterData masterData22 = new MasterData().setShopSkuKey(SSKU_KEY_2).setCustomsCommodityCode("22");
        MasterData masterData23 = new MasterData().setShopSkuKey(SSKU_KEY_3).setCustomsCommodityCode("23");
        ReferenceItemWrapper referenceItemWrapper21 = referenceItem(SSKU_KEY_1, 21);
        ReferenceItemWrapper referenceItemWrapper23 = referenceItem(SSKU_KEY_3, 23);
        SskuGoldenParamValue paramValue212 = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(SSKU_KEY_1)
            .setMdmParamId(2)
            .setUpdatedTs(Instant.EPOCH)
            .setString("212");
        SskuGoldenParamValue paramValue239 = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(SSKU_KEY_3)
            .setMdmParamId(9)
            .setUpdatedTs(Instant.EPOCH)
            .setString("239");
        SskuGoldenParamValue.Key delete213 =
            new SskuGoldenParamValue.Key(SSKU_KEY_1.getSupplierId(), SSKU_KEY_1.getShopSku(), 3);
        SskuGoldenParamValue.Key delete235 =
            new SskuGoldenParamValue.Key(SSKU_KEY_3.getSupplierId(), SSKU_KEY_3.getShopSku(), 5);
        SskuGoldenItemsUpdateTask task2 = new SskuGoldenItemsUpdateTask(
            Stream.of(referenceItemWrapper21, referenceItemWrapper23)
                .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity())),
            Stream.of(masterData22, masterData23)
                .collect(Collectors.toMap(MasterData::getShopSkuKey, Function.identity())),
            Stream.of(paramValue212, paramValue239)
                .collect(Collectors.toMap(SskuGoldenParamValue::getGoldenKey, Function.identity())),
            Set.of(delete213, delete235)
        );

        // when
        SskuGoldenItemsUpdateTask mergedTask = SskuGoldenItemsUpdateTask.consequentMerge(task1, task2);

        //then
        Assertions.assertThat(mergedTask.getReferenceItemsToInsertOrUpdate().values())
            .containsExactlyInAnyOrder(referenceItemWrapper21, referenceItemWrapper12, referenceItemWrapper23)
            .doesNotContain(referenceItemWrapper13); // overwritten by 23

        Assertions.assertThat(mergedTask.getMasterDataToInsertOrUpdate().values())
            .containsExactlyInAnyOrder(masterData11, masterData22, masterData23)
            .doesNotContain(masterData12); // overwritten by 22

        Assertions.assertThat(mergedTask.getGoldParamValuesToInsertOrUpdate().values())
            .containsExactlyInAnyOrder(paramValue212, paramValue122, paramValue239)
            .doesNotContain(paramValue112) // overwritten by 212
            .doesNotContain(paramValue113); // removed by delete213

        Assertions.assertThat(mergedTask.getGoldParamValuesToDelete())
            .containsExactlyInAnyOrder(delete117, delete213, delete235)
            .doesNotContain(delete139); // removed by paramValue239
    }

    private static ReferenceItemWrapper referenceItem(ShopSkuKey key, double value) {
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(value, value, value, value, null, null);
        MdmIrisPayload.Item item =
            ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER, shippingUnit);
        return new ReferenceItemWrapper(item);
    }
}
