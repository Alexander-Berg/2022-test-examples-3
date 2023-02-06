package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;

@DisplayName("Массовая генерация ярлыков для заказов")
class GetLabelsForOrdersTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная генерация")
    void getLabelsForOrders() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.APPLICATION_PDF_VALUE))
                .path("document/label/generate")
                .requestContentPath("request/labels.json")
                .responseContentPath("response/app.pdf")
                .responseContentType(MediaType.APPLICATION_PDF)
                .build()
        )
            .andExpect(queryParam("pageSize", PageSize.A4.name()));

        List<LabelInfo> data = List.of(
            LabelInfo.builder()
                .platformClientId(3L)
                .barcode("order-barcode")
                .sortingCenter(
                    LabelInfo.PartnerInfo.builder()
                        .legalName("sorting-center-legal-name")
                        .readableName("sorting-center-readable-name")
                        .build()
                )
                .deliveryService(
                    LabelInfo.PartnerInfo.builder()
                        .legalName("delivery-service-legal-name")
                        .readableName("delivery-service-readable-name")
                        .build()
                )
                .place(
                    LabelInfo.PlaceInfo.builder()
                        .externalId("place-external-id")
                        .placeNumber(1)
                        .placesCount(2)
                        .weight(BigDecimal.valueOf(2.3))
                        .build()
                )
                .recipient(
                    LabelInfo.RecipientInfo.builder()
                        .firstName("Алексей")
                        .middleName("Алексеевич")
                        .lastName("Алексеев")
                        .phoneNumber("+12345678910")
                        .build()
                )
                .address(
                    LabelInfo.AddressInfo.builder()
                        .country("Россия")
                        .locality("Москва")
                        .street("Ленина")
                        .house("1/1")
                        .zipCode("123321")
                        .build()
                )
                .shipmentDate(LocalDate.parse("2020-06-06"))
                .seller(
                    LabelInfo.SellerInfo.builder()
                        .number("seller-barcode")
                        .legalName("seller-legal-name")
                        .readableName("seller-readable-name")
                        .build()
                )
                .build()
        );

        softly.assertThat(
            wwClient.generateLabels(
                data,
                DocumentFormat.PDF,
                PageSize.A4
            )
        )
            .isEqualTo(readFileIntoByteArray("response/app.pdf"));
    }
}
