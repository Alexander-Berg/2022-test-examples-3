package ru.yandex.avia.booking.partners.gateways.aeroflot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotRequestContext;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.SearchData;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.MULTIPLE_OFFERS_ANOTHER_ID;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.MULTIPLE_OFFERS_MAIN_ID;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.SINGLE_COMPLEX_OFFER_ID;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3MultipleTariffs;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3SingleTariff;

class AeroflotGatewayResolveVariantTestInfo {
    private final AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("url_not_needed");

    @Test
    void resolveVariant_single() {
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThat(variant.getOffer().getId()).isEqualTo(SINGLE_COMPLEX_OFFER_ID);

        SearchData searchData = variant.getSearchData();
        assertThat(searchData.getQid())
                .isEqualTo("180208-232505-325.ticket.plane.c213_c2_2018-03-21_None_economy_1_0_0_ru.ru");
        assertThat(searchData.getExternalBookingUrl())
                .isEqualTo("http://yandex.mlsd.ru/flights__from_meta?flight_id=146980020001&external_subject_id=10208");
        var searchParams = searchData.getSearchParams();
        assertThat(searchParams.getNationalVersion()).isEqualTo("ru");
        assertThat(searchParams.getLang()).isEqualTo("ru-lang");
        assertThat(searchParams.getKlass()).isEqualTo("economy");
        assertThat(searchParams.getPointFrom()).isEqualTo("c213");
        assertThat(searchParams.getPointTo()).isEqualTo("c2");

        AeroflotRequestContext context = variant.getContext();
        assertThat(context.getCabinType()).isEqualTo(3);
        assertThat(context.getCountryCode()).isEqualTo("RU");
        assertThat(context.getLanguage()).isEqualTo("RU");
        assertThat(context.getResponseId()).isEqualTo(
                "MOW.20201002.KHV-KHV.20201012.MOW_2020-09-24T12:42:59.655628");

        // the converted variant data is tested in AeroflotNdcApiV3CompatibilityConverterTest
    }

    @Test
    void resolveVariant_multipleOffers_jsonTrimmer() {
        JsonNode variantData = loadSampleTdRequestNdcV3MultipleTariffs();
        String variantAsJson = variantData.toString();
        assertThat(variantAsJson.length()).isGreaterThan(29_000);

        AeroflotVariant variant = gateway.resolveVariantInfo(variantData);
        assertThat(variant.getOffer().getId()).isEqualTo(MULTIPLE_OFFERS_MAIN_ID);
        // some redundant offers have been filtered out
        assertThat(variant.getAllTariffs()).hasSize(3);
        assertThat(variantData.toString()).isEqualTo(variantAsJson);

        AeroflotVariant variant2 = gateway.resolveVariantInfoAndOptimizeJson(variantData);
        assertThat(variant2).isEqualTo(variant);
        // the filtered variant data was store back into the json node
        assertThat(variantData.toString()).isNotEqualTo(variantAsJson);
        assertThat(variantData.toString().length()).isLessThan(10_000);

        AeroflotVariant variant3 = gateway.resolveVariantInfoAndOptimizeJson(variantData);
        // consecutive reads aren't affected
        assertThat(variant3).isEqualTo(variant);
    }

    @Test
    void resolveVariant_selectedOfferChanged() {
        JsonNode request = loadSampleTdRequestNdcV3MultipleTariffs();
        AeroflotVariant v2 = gateway.resolveVariantInfo(request);
        assertThat(v2.getOffer().getId()).isEqualTo(MULTIPLE_OFFERS_MAIN_ID);
        assertThat(v2.getOffer().getTotalPrice()).isEqualTo(Money.of(3755, "RUB"));

        // user has re-selected another tariff
        ((ObjectNode) request.at("/order_data/booking_info")).put("OfferId", MULTIPLE_OFFERS_ANOTHER_ID);
        AeroflotVariant v3 = gateway.resolveVariantInfo(request);
        assertThat(v3.getOffer().getId()).isEqualTo(MULTIPLE_OFFERS_ANOTHER_ID);
        assertThat(v3.getOffer().getTotalPrice()).isEqualTo(Money.of(6155, "RUB"));
    }
}
