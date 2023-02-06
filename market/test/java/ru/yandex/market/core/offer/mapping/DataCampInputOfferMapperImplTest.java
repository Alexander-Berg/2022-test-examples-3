package ru.yandex.market.core.offer.mapping;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.proto.indexer.v2.BlueAssortment;

import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildEmptyUnitedOfferFromInputOffer;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildUnitedOfferFromInputOffer;

/**
 * Date: 27.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class DataCampInputOfferMapperImplTest extends FunctionalTest {

    @Autowired
    private DataCampInputOfferMapper inputOfferMapper;

    @DisplayName("Проверяем заполнение при предложении из индексатора со всеми полями.")
    @Test
    void map_fullData_success() {
        BlueAssortment.InputOffer inputOffer = ProtoTestUtil.getProtoMessageByJson(
                BlueAssortment.InputOffer.class,
                "proto/DataCampInputOfferMapper/InputOffer.withAllFields.json",
                getClass()
        );

        Assertions.assertThat(inputOfferMapper.map(inputOffer))
                .isEqualTo(buildUnitedOfferFromInputOffer("offer3"));
    }

    @DisplayName("Проверяем заполнение при пустом предложении из индексатора.")
    @Test
    void map_emptyData_success() {
        BlueAssortment.InputOffer inputOffer = ProtoTestUtil.getProtoMessageByJson(
                BlueAssortment.InputOffer.class,
                "proto/DataCampInputOfferMapper/InputOffer.emptyFields.json",
                getClass()
        );

        Assertions.assertThat(inputOfferMapper.map(inputOffer))
                .isEqualTo(buildEmptyUnitedOfferFromInputOffer());
    }
}
