package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.ClaimFromPartner;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

@DisplayName("Генерация претензии от партнёра маркетплейса Яндекс Маркету")
public class GetClaimFromPartnerTest extends AbstractClientTest {
    @Test
    @DisplayName("Успешная генерация в html формате")
    void getClaim() {
        prepareMockRequest(
                MockRequestUtils.MockRequest.builder()
                        .requestMethod(HttpMethod.PUT)
                        .header("accept", List.of("application/json; q=0.9", MediaType.TEXT_HTML_VALUE))
                        .path("document/claimFromPartner/generate")
                        .requestContentPath("request/claim_from_partner.json")
                        .responseContentPath("response/claim_from_partner.html")
                        .responseContentType(MediaType.TEXT_HTML)
                        .build()
        );

        softly.assertThat(wwClient.generateClaimFromPartner(createClaimData(), DocumentFormat.HTML))
                .isEqualTo(readFileIntoByteArray("response/claim_from_partner.html"));
    }

    @Test
    @DisplayName("Ошибка генерации: DocumentFormat не поддерживается")
    void getClaimUnsupportedContent() {
        softly.assertThatThrownBy(
                        () -> wwClient.generateClaimFromPartner(createClaimData(), DocumentFormat.UNSUPPORTED)
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported format: %s", DocumentFormat.UNSUPPORTED);
    }

    private ClaimFromPartner createClaimData() {
        return ClaimFromPartner.builder()
                .partnerId(566519L)
                .customerInfo(createCustomerInfo())
                .claimDate(LocalDate.of(2021, 11, 21))
                .agreement("951480/21")
                .agreementDate(LocalDate.of(2021, 3, 16))
                .startDate(LocalDate.of(2021, 8, 23))
                .endDate(LocalDate.of(2021, 9, 1))
                .fullPrice(new BigDecimal("80.7"))
                .orders(createOrders()).build();
    }

    private ClaimFromPartner.CustomerInfo createCustomerInfo() {
        return ClaimFromPartner.CustomerInfo.builder()
                .email("test-email@domain.ru")
                .phone("+7 1111110990")
                .incorporation("ООО \"Атлантик Сити\"")
                .kpp("222201001")
                .bik("044525974")
                .inn("3337352333")
                .correspondentAccount("00000000000000000700")
                .bankName("ПАО СБЕРБАНК")
                .jurAddress("628408,Ханты-Мансийский автономный округ-Югра,Сургут г,Республики ул,73/1")
                .factAddress("109518,Москва,проспект Мира 5 к4,этаж 3")
                .account("88888888810090004064")
                .build();
    }

    @Nonnull
    private List<ClaimFromPartner.OrderItem> createOrderItems(long orderId, int iterations) {
        return IntStream.range(0, iterations).mapToObj(i ->
                    ClaimFromPartner.OrderItem.builder().itemCount(2L).itemPrice(new BigDecimal("10.1"))
                            .shopSku("shop-sku-" + orderId + "-unic-000" + i).orderId(orderId).build()
        ).collect(Collectors.toList());
    }

    @Nonnull
    private List<ClaimFromPartner.Order> createOrders() {
        return List.of(
                ClaimFromPartner.Order.builder().orderId(2180293L).orderPrice(new BigDecimal("20.2"))
                        .orderItems(createOrderItems(2180293L, 2)).build(),
                ClaimFromPartner.Order.builder().orderId(2180284L).orderPrice(new BigDecimal("30.3"))
                        .orderItems(createOrderItems(2180284L, 3)).build(),
                ClaimFromPartner.Order.builder().orderId(2180295L).orderPrice(new BigDecimal("10.1"))
                        .orderItems(createOrderItems(2180295L, 1)).build()
        );
    }
}
