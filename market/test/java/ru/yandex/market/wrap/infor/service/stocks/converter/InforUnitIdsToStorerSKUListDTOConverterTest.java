package ru.yandex.market.wrap.infor.service.stocks.converter;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.StorerSKUListDTO;
import ru.yandex.market.wrap.infor.client.model.StorerSkuDTO;
import ru.yandex.market.wrap.infor.entity.InforUnitId;

class InforUnitIdsToStorerSKUListDTOConverterTest extends SoftAssertionSupport {

    private static final InforUnitId INFOR_UNIT_ID_1 = InforUnitId.of("SKU_1", 47L);
    private static final InforUnitId INFOR_UNIT_ID_2 = InforUnitId.of("SKU_2", 359L);

    private final InforUnitIdsToStorerSKUListDTOConverter converter = new InforUnitIdsToStorerSKUListDTOConverter();

    /**
     * Сценарий №1: тест на пустой список c UnitId
     */
    @Test
    void testOnEmptyList() {
        List<InforUnitId> unitIds = Collections.emptyList();
        StorerSKUListDTO body = converter.convert(unitIds);

        assertItems(unitIds, body);
    }


    /**
     * Сценарий №2: тест на одноэлементом списке UnitId
     */
    @Test
    void testOnSingleItemList() {
        List<InforUnitId> unitIds = Collections.singletonList(INFOR_UNIT_ID_1);
        StorerSKUListDTO body = converter.convert(unitIds);

        assertItems(unitIds, body);
    }

    /**
     * Сценарий №3: тест на списке из нескольких элементов UnitId
     */
    @Test
    void testOnMultiItemList() {
        List<InforUnitId> unitIds = ImmutableList.of(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2);
        StorerSKUListDTO body = converter.convert(unitIds);

        assertItems(unitIds, body);
    }

    /**
     * Сценарий №4: тест на списке из нескольких элеменетов с дубликатами
     */
    @Test
    void testOnListWithDuplicates() {
        List<InforUnitId> unitIds = ImmutableList.of(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2, INFOR_UNIT_ID_1, INFOR_UNIT_ID_2);
        StorerSKUListDTO body = converter.convert(unitIds);

        assertItems(unitIds, body);
    }

    private void assertItems(List<InforUnitId> inforUnitIds, StorerSKUListDTO body) {
        for (int i = 0; i < inforUnitIds.size(); i++) {
            StorerSkuDTO skuDto = body.getStorerSkuList().get(i);
            InforUnitId inforUnitId = inforUnitIds.get(i);

            softly.assertThat(skuDto.getSku()).isEqualTo(inforUnitId.getFormattedId());
            softly.assertThat(skuDto.getStorer()).isEqualTo(inforUnitId.getVendorIdAsString());
        }
    }

}
