package ru.yandex.market.logistics.werewolf.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimFromPartner;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Генерация претензии от партнера к Маркету")
public class ClaimFromPartnerGeneratorTest extends AbstractDocumentGeneratorTest {

    @Test
    @DisplayName("Успешная генерация претензии в формате HTML")
    void generateClaimSuccess() throws Exception {
        performAndDispatch(
            "controller/documents/request/claim_from_partner.json",
            request -> request.accept(MediaType.TEXT_HTML)
        )
        .andExpect(status().isOk())
        .andExpect(content().string(extractFileContent("controller/documents/response/claim_from_partner.html")));
    }

    @Test
    @DisplayName("Успешная генерация претензии в формате HTML с пустыми телефоном, e-mail и КПП")
    void generateClaimSuccessEmptyContactsKpp() throws Exception {
        String expectedContentPath = "controller/documents/response/claim_from_partner_empty_contact_kpp.html";
        performAndDispatch(
                "controller/documents/request/claim_from_partner_empty_contact_kpp.json",
                request -> request.accept(MediaType.TEXT_HTML)
        )
        .andExpect(status().isOk())
        .andExpect(content().string(extractFileContent(expectedContentPath)));
    }

    @Nonnull
    @Override
    protected String defaultRequestBodyPath() {
        return "controller/documents/request/claim_from_partner.json";
    }

    @Nonnull
    @Override
    protected String defaultHtmlResponseBodyPath() {
        return "controller/documents/response/claim_from_partner.html";
    }

    @Nonnull
    @Override
    protected String defaultFilename() {
        return "CLAIM-FROM-PARTNER-566519-2021-11-21";
    }

    @Nonnull
    @Override
    protected String requestPath() {
        return "/document/claimFromPartner/generate";
    }

    @Nonnull
    @Override
    protected WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A4, PageOrientation.PORTRAIT);
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("validationSource")
    @DisplayName("Валидация входных параметров")
    void validationFullModel(
            Consumer<ClaimFromPartner.ClaimFromPartnerBuilder> builderConsumer,
            String field,
            String message
    ) throws Exception {
        ClaimFromPartner.ClaimFromPartnerBuilder claimDataBuilder = claimFromPartner();
        builderConsumer.accept(claimDataBuilder);
        performWithBody(
                objectMapper.writeValueAsString(claimDataBuilder.build()),
                request -> request.accept(MediaType.TEXT_HTML, APPLICATION_JSON_Q_09)
        )
                .andExpect(status().isBadRequest())
                .andExpect(fieldError(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.of(
                        validationCommon(),
                        validationCustomer(),
                        validationOrder(),
                        validationOrderItem()
                )
                .flatMap(Function.identity())
                .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimFromPartner.ClaimFromPartnerBuilder>,
            String, String>> validationCommon() {
        return Stream.of(
                Triple.of(
                        c -> c.partnerId(null),
                        "partnerId",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.customerInfo(null),
                        "customerInfo",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.agreementDate(null),
                        "agreementDate",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.agreement(null),
                        "agreement",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.claimDate(null),
                        "claimDate",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.startDate(null),
                        "startDate",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.endDate(null),
                        "endDate",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.fullPrice(null),
                        "fullPrice",
                        "must not be null"
                ),
                Triple.of(
                        c -> c.orders(Collections.singletonList(null)),
                        "orders[0]",
                        "must not be null"
                )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimFromPartner.ClaimFromPartnerBuilder>,
                          String, String>> validationCustomer() {
        return Stream.<Triple<UnaryOperator<ClaimFromPartner.CustomerInfo.CustomerInfoBuilder>, String, String>>of(
                Triple.of(
                        c -> c.incorporation(null),
                        "customerInfo.incorporation",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.jurAddress(null),
                        "customerInfo.jurAddress",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.factAddress(null),
                        "customerInfo.factAddress",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.inn(null),
                        "customerInfo.inn",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.account(null),
                        "customerInfo.account",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.bankName(null),
                        "customerInfo.bankName",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.bik(null),
                        "customerInfo.bik",
                        "must not be blank"
                ),
                Triple.of(
                        c -> c.correspondentAccount(null),
                        "customerInfo.correspondentAccount",
                        "must not be blank"
                )
        ).map(t -> Triple.of(
                c -> c.customerInfo(t.getLeft().apply(customerInfoBuilder()).build()),
                t.getMiddle(),
                t.getRight()
        ));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimFromPartner.ClaimFromPartnerBuilder>,
            String, String>> validationOrder() {
        return Stream.<Triple<UnaryOperator<ClaimFromPartner.Order.OrderBuilder>, String, String>>of(
                        Triple.of(
                                b -> b.orderId(null),
                                "orders[0].orderId",
                                "must not be null"
                        ),
                        Triple.of(
                                b -> b.orderPrice(null),
                                "orders[0].orderPrice",
                                "must not be null"
                        ),
                        Triple.of(
                                c -> c.orderItems(Collections.singletonList(null)),
                                "orders[0].orderItems[0]",
                                "must not be null"
                        )
                )
                .map(t -> Triple.of(
                        c -> c.orders(List.of(t.getLeft().apply(orderBuilder()).build())),
                        t.getMiddle(),
                        t.getRight()
                ));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimFromPartner.ClaimFromPartnerBuilder>,
                          String, String>> validationOrderItem() {
        return Stream.<Triple<UnaryOperator<ClaimFromPartner.Order.OrderBuilder>, String, String>>of(
                        Triple.of(
                                b -> b.orderItems(List.of(orderItemBuilder().orderId(null).build())),
                                "orders[0].orderItems[0].orderId",
                                "must not be null"
                        ),
                        Triple.of(
                                b -> b.orderItems(List.of(orderItemBuilder().shopSku(null).build())),
                                "orders[0].orderItems[0].shopSku",
                                "must not be blank"
                        ),
                        Triple.of(
                                b -> b.orderItems(List.of(orderItemBuilder().itemCount(null).build())),
                                "orders[0].orderItems[0].itemCount",
                                "must not be null"
                        ),
                        Triple.of(
                                b -> b.orderItems(List.of(orderItemBuilder().itemPrice(null).build())),
                                "orders[0].orderItems[0].itemPrice",
                                "must not be null"
                        )
                )
                .map(t -> Triple.of(
                        c -> c.orders(List.of(t.getLeft().apply(orderBuilder()).build())),
                        t.getMiddle(),
                        t.getRight()
                ));
    }

    @Nonnull
    private static ClaimFromPartner.ClaimFromPartnerBuilder claimFromPartner() {
        return ClaimFromPartner.builder()
                               .partnerId(566519L)
                               .customerInfo(customerInfoBuilder().build())
                               .claimDate(LocalDate.of(2021, 11, 21))
                               .agreement("951480/20")
                .agreementDate(LocalDate.of(2021, 3, 16))
                .startDate(LocalDate.of(2021, 8, 23))
                .endDate(LocalDate.of(2021, 9, 1))
                .fullPrice(new BigDecimal("80.7"))
                .orders(List.of(orderBuilder().build()));
    }

    @Nonnull
    private static ClaimFromPartner.CustomerInfo.CustomerInfoBuilder customerInfoBuilder() {
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
                .account("88888888810090004064");
    }

    @Nonnull
    private static ClaimFromPartner.OrderItem.OrderItemBuilder orderItemBuilder() {
        return ClaimFromPartner.OrderItem.builder().shopSku("shop-unic-0001").itemCount(2L)
                .itemPrice(new BigDecimal("10")).orderId(2180290L);
    }

    @Nonnull
    private static ClaimFromPartner.Order.OrderBuilder orderBuilder() {
        return ClaimFromPartner.Order.builder().orderId(2180293L).orderPrice(new BigDecimal("20.2"))
                .orderItems(List.of(orderItemBuilder().build()));
    }
}
