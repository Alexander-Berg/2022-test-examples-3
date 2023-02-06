package ru.yandex.travel.api.services.avia.td;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.travel.api.config.common.EncryptionConfigurationProperties;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.services.avia.td.promo.AviaTdAeroflotPlus2021Offer;

import static org.assertj.core.api.Assertions.assertThat;

public class AviaTdInfoExtractorTest {

    private final AviaTdInfoExtractor subject = new AviaTdInfoExtractor(
            new ApiTokenEncrypter(new EncryptionConfigurationProperties()));

    @Test
    public void testCorrectDataExtracted() throws IOException {
        JsonNode json = createJsonNode();
        AviaTdInfo result = subject.parseTdaemonInfo(json);
        assertThat(result.getPartnerCode()).isEqualTo("nemo-booking");
        assertThat(result.getPreliminaryPrice()).isNotNull();
        assertThat(result.getPreliminaryPrice().getCurrency().getCurrencyCode()).isEqualTo("RUB");
        assertThat(result.getPreliminaryPrice().getNumber().numberValueExact(BigDecimal.class)).isEqualTo(BigDecimal.valueOf(15475));
        assertThat(result.getPromoCampaigns().getPromo2020Ids()).isEqualTo(List.of(
                "1ADT-SVO.202104061450.IGT.SU.1078.R.RFOSLR",
                "1ADT-SVO.202104061450.IGT.SU.1078.R.RCORISLB",
                "1ADT-SVO.202104061450.IGT.SU.1078.R.RNORISLB"
        ));
    }

    @Test
    public void testCorrectDataExtracted_aeroflotPlusPromo2021() throws IOException {
        JsonNode json = createJsonNode();
        AviaTdInfo result = subject.parseTdaemonInfo(json);
        List<AviaTdAeroflotPlus2021Offer> offers = result.getPromoCampaigns().getAeroflotPlusPromo2021Offers();
        assertThat(offers).hasSize(2);
        assertThat(offers.get(0)).isEqualTo(AviaTdAeroflotPlus2021Offer.builder()
                .offerId("1ADT-SVO.202104061450.IGT.SU.1078.R.RFOSLR")
                .plusCodes(List.of(plus2021Code(1000), plus2021Code(2000)))
                .build());
        assertThat(offers.get(1)).isEqualTo(AviaTdAeroflotPlus2021Offer.builder()
                .offerId("1ADT-SVO.202104061450.IGT.SU.1078.R.RCORISLB")
                .plusCodes(List.of(plus2021Code(3000)))
                .build());
    }

    private JsonNode createJsonNode() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("avia/ticket_daemon/booking_info_by_token_sample.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(inputStream);
    }

    private AviaTdAeroflotPlus2021Offer.Code plus2021Code(int value) {
        return new AviaTdAeroflotPlus2021Offer.Code(value);
    }
}
