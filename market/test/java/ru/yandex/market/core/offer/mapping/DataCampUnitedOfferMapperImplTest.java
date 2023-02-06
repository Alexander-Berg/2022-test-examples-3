package ru.yandex.market.core.offer.mapping;

import java.util.Optional;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampUnitedOffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.feed.offer.united.OfferContentInfo;
import ru.yandex.market.core.feed.offer.united.OfferSpecification;
import ru.yandex.market.core.feed.offer.united.OfferWeightDimensions;
import ru.yandex.market.core.feed.offer.united.UnitedOffer;
import ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil;

/**
 * Date: 20.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class DataCampUnitedOfferMapperImplTest extends FunctionalTest {

    @Autowired
    private DataCampUnitedOfferMapper unitedOfferMapper;

    @DisplayName("Проверяем заполнение при предложении из индексатора со всеми полями.")
    @Test
    void map_fullData_success() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.withAllFields.json",
                getClass()
        );
        DataCampOffer.Offer mergedOffer = DataCampUtil.mergeServiceOfferWithBasicOffer(unitedOffer, 123456);

        Assertions.assertThat(unitedOfferMapper.map(mergedOffer))
                .isEqualTo(UnitedOfferTestUtil.buildUnitedOffer(123456, "0516465165"));
    }

    @DisplayName("Проверяем заполнение при пустом предложении из индексатора.")
    @Test
    void map_emptyData_success() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.emptyFields.json",
                getClass()
        );
        DataCampOffer.Offer mergedOffer = DataCampUtil.mergeServiceOfferWithBasicOffer(unitedOffer, 123456);

        Assertions.assertThat(unitedOfferMapper.map(mergedOffer))
                .isEqualTo(UnitedOfferTestUtil.buildEmptyUnitedOffer(123456, "0516465165"));
    }

    @DisplayName("При отсутствии значения с весом в поле value_mg, парсим значение из поля grams")
    @Test
    void testParseWeightFromGramsField() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.weightGrams.json",
                getClass()
        );
        DataCampOffer.Offer mergedOffer = DataCampUtil.mergeServiceOfferWithBasicOffer(unitedOffer, 123456);

        long expectedWeight = 60000;
        Long weight = Optional.of(unitedOfferMapper.map(mergedOffer))
                .map(UnitedOffer::getContentInfo)
                .map(OfferContentInfo::getSpecification)
                .map(OfferSpecification::getWeightDimensions)
                .map(OfferWeightDimensions::getWeight)
                .orElse(null);
        Assertions.assertThat(weight)
                .isEqualTo(expectedWeight);
    }

    @Test
    @DisplayName("Мерж united-offer")
    void testMergeUnitedOffer() {
        DataCampUnitedOffer.UnitedOffer first = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.merge.first.json",
                getClass()
        );
        DataCampUnitedOffer.UnitedOffer second = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.merge.second.json",
                getClass()
        );
        DataCampUnitedOffer.UnitedOffer expected = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.merge.result.json",
                getClass()
        );

        DataCampUnitedOffer.UnitedOffer actual = DataCampUtil.mergeUnitedOffers(first, second);
        ProtoTestUtil.assertThat(actual)
                .isEqualTo(expected);
    }
}
