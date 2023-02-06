package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.ClaimData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

@DisplayName("Генерация претензии логистическому партнёру")
public class GetClaimTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная генерация в html формате")
    void getClaim() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.TEXT_HTML_VALUE))
                .path("document/claim/generate")
                .requestContentPath("request/claim.json")
                .responseContentPath("response/claim.html")
                .responseContentType(MediaType.TEXT_HTML)
                .build()
        );

        softly.assertThat(wwClient.generateClaim(createClaimData(), DocumentFormat.HTML))
            .isEqualTo(readFileIntoByteArray("response/claim.html"));
    }

    @Test
    @DisplayName("Ошибка генерации: DocumentFormat не поддерживается")
    void getClaimUnsupportedContent() {
        softly.assertThatThrownBy(
                () -> wwClient.generateClaim(createClaimData(), DocumentFormat.UNSUPPORTED)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported format: %s", DocumentFormat.UNSUPPORTED);
    }

    @Nonnull
    private ClaimData createClaimData() {
        return ClaimData.builder()
            .id("AC-TEST-00000001")
            .contractorInfo(
                ClaimData.ContractorInfo.builder()
                    .incorporation("ООО ПЕРВАЯ ЭКСПЕДИЦИОННАЯ КОМПАНИЯ")
                    .address("Россия, 109428, г. Москва, 1-ый Вязовский проезд, д.4, стр.19")
                    .build()
            )
            .date(LocalDate.of(2021, 3, 16))
            .agreement("ББПКОХ14")
            .agreementDate(LocalDate.of(2020, 7, 1))
            .orders(
                List.of(ClaimData.Order.builder()
                    .externalId("33250421")
                    .address("Россия, 109428, г. Москва, 1-ый Вязовский проезд, д.5, стр.19")
                    .shipmentDate(LocalDate.of(2019, 6, 15))
                    .assessedValue(BigDecimal.valueOf(1740))
                    .comments(
                        "7 февраля 2021 г., Возвратный заказ на складе СЦ, однако на возврат заказ передан не был"
                    )
                    .build())
            )
            .amount(BigDecimal.valueOf(1740))
            .customerInfo(
                ClaimData.CustomerInfo.builder()
                    .incorporation("ООО Яндекс.Маркет")
                    .ogrn("1167746491395")
                    .address("119021, г. Москва, ул. Тимура Фрунзе, д. 11, строение 44, этаж 5")
                    .inn("7704357909")
                    .kpp("770401001")
                    .account("40702810438000034726")
                    .bankName("ПАО Сбербанк")
                    .bik("044525225")
                    .correspondentAccount("30101810400000000225")
                    .build()
            )
            .manager("О.В. Ларионова")
            .proxyDate(LocalDate.of(2019, 6, 20))
            .build();
    }
}
