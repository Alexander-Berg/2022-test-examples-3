package ru.yandex.market.logistics.management.domain.converter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.repository.LegalInfoRepository;

class LegalInfoConverterTest extends AbstractTest {

    private static final LegalInfoConverter CONVERTER = new LegalInfoConverter(
        new AddressConverter(Mappers.getMapper(AddressMapper.class)),
        Mockito.mock(LegalInfoRepository.class)
    );
    private static final LegalInfo EMPTY_MARKET_ID_LI = LegalInfo.newBuilder()
        .setRegistrationNumber("111")
        .setLegalName("")
        .setInn("")
        .setType("")
        .build();
    private static final LegalInfo FILLED_MARKET_ID_LI = LegalInfo.newBuilder()
        .setRegistrationNumber("111")
        .setLegalName("MarketID Name")
        .setType("ООО")
        .setInn("222")
        .build();
    private static final ru.yandex.market.logistics.management.domain.entity.LegalInfo EMPTY_LMS_LI =
        new ru.yandex.market.logistics.management.domain.entity.LegalInfo()
            .setId(1L)
            .setOgrn(333L);
    private static final ru.yandex.market.logistics.management.domain.entity.LegalInfo FILLED_LMS_LI =
        new ru.yandex.market.logistics.management.domain.entity.LegalInfo()
            .setId(1L)
            .setOgrn(333L)
            .setIncorporation("LMS Name")
            .setLegalForm("ИП")
            .setInn("444");

    @Test
    void enrichEmptyLegalInfoByEmptyOne() {
        softly.assertThat(CONVERTER.enrichLegalInfo(null, null)).isNull();

        softly.assertThat(CONVERTER.enrichLegalInfo(null, EMPTY_LMS_LI))
            .extracting(LegalInfo::getRegistrationNumber)
            .isEqualTo("333");

        softly.assertThat(CONVERTER.enrichLegalInfo(EMPTY_MARKET_ID_LI, null))
            .isEqualTo(EMPTY_MARKET_ID_LI);

        softly.assertThat(CONVERTER.enrichLegalInfo(EMPTY_MARKET_ID_LI, EMPTY_LMS_LI))
            .isEqualTo(EMPTY_MARKET_ID_LI);
    }

    @Test
    void enrichEmptyLegalInfoByFilledOne() {
        LegalInfo expectedForNull = LegalInfo.newBuilder()
            .setRegistrationNumber("333")
            .setLegalName("LMS Name")
            .setType("ИП")
            .setInn("444")
            .build();
        LegalInfo expectedForEmpty = LegalInfo.newBuilder()
            .setRegistrationNumber("111")
            .setLegalName("LMS Name")
            .setType("ИП")
            .setInn("444")
            .build();

        softly.assertThat(CONVERTER.enrichLegalInfo(null, FILLED_LMS_LI))
            .isEqualTo(expectedForNull);
        softly.assertThat(CONVERTER.enrichLegalInfo(EMPTY_MARKET_ID_LI, FILLED_LMS_LI))
            .isEqualTo(expectedForEmpty);
    }

    @Test
    void enrichFilledLegalInfoByEmptyOne() {
        softly.assertThat(CONVERTER.enrichLegalInfo(FILLED_MARKET_ID_LI, null))
            .isEqualTo(FILLED_MARKET_ID_LI);
        softly.assertThat(CONVERTER.enrichLegalInfo(FILLED_MARKET_ID_LI, EMPTY_LMS_LI))
            .isEqualTo(FILLED_MARKET_ID_LI);
    }

    @Test
    void enrichFilledLegalInfoByFilledOne() {
        softly.assertThat(CONVERTER.enrichLegalInfo(FILLED_MARKET_ID_LI, FILLED_LMS_LI))
            .isEqualTo(FILLED_MARKET_ID_LI);
    }
}
