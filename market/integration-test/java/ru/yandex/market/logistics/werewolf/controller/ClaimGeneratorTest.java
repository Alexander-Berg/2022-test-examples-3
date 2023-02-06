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
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.ClaimDataBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.ContractorInfo;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.ContractorInfo.ContractorInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.CustomerInfo;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.CustomerInfo.CustomerInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.Order;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimData.Order.OrderBuilder;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Генерация претензии от Маркета к организации-перевозчику")
public class ClaimGeneratorTest extends AbstractDocumentGeneratorTest {

    @Test
    @DisplayName("Успешная генерация претензии в формате HTML")
    void generateClaimSuccess() throws Exception {
        performAndDispatch(
            "controller/documents/request/claim.json",
            request -> request.accept(MediaType.TEXT_HTML)
        )
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent("controller/documents/response/claim.html")));
    }

    @Test
    @DisplayName("Успешная генерация претензии без адреса исполнителя в формате HTML")
    void generateClaimWithoutContractorAddressSuccess() throws Exception {
        performAndDispatch(
            "controller/documents/request/claim_without_address.json",
            request -> request.accept(MediaType.TEXT_HTML)
        )
            .andExpect(status().isOk())
            .andExpect(content().string(
                extractFileContent("controller/documents/response/claim_without_address.html"))
            );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("validationSource")
    @DisplayName("Валидация входных параметров")
    void validation(
        Consumer<ClaimDataBuilder> builderConsumer,
        String field,
        String message
    ) throws Exception {
        ClaimDataBuilder claimDataBuilder = claimDataBuilder();
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
                validationContractor(),
                validationCustomer(),
                validationOrder()
            )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimDataBuilder>, String, String>> validationCommon() {
        return Stream.of(
            Triple.of(
                c -> c.contractorInfo(null),
                "contractorInfo",
                "must not be null"
            ),
            Triple.of(
                c -> c.id(null),
                "id",
                "must not be blank"
            ),
            Triple.of(
                c -> c.date(null),
                "date",
                "must not be null"
            ),
            Triple.of(
                c -> c.agreement(null),
                "agreement",
                "must not be blank"
            ),
            Triple.of(
                c -> c.agreementDate(null),
                "agreementDate",
                "must not be null"
            ),
            Triple.of(
                c -> c.orders(null),
                "orders",
                "must not be empty"
            ),
            Triple.of(
                c -> c.orders(Collections.singletonList(null)),
                "orders[0]",
                "must not be null"
            ),
            Triple.of(
                c -> c.amount(null),
                "amount",
                "must not be null"
            ),
            Triple.of(
                c -> c.customerInfo(null),
                "customerInfo",
                "must not be null"
            ),
            Triple.of(
                c -> c.manager(null),
                "manager",
                "must not be blank"
            ),
            Triple.of(
                c -> c.proxyDate(null),
                "proxyDate",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimDataBuilder>, String, String>> validationContractor() {
        return Stream.<Triple<UnaryOperator<ContractorInfoBuilder>, String, String>>of(
                Triple.of(
                    c -> c.incorporation(null),
                    "contractorInfo.incorporation",
                    "must not be blank"
                )
            )
            .map(t -> Triple.of(
                c -> c.contractorInfo(t.getLeft().apply(contractorInfoBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            ));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimDataBuilder>, String, String>> validationCustomer() {
        return Stream.<Triple<UnaryOperator<CustomerInfoBuilder>, String, String>>of(
                Triple.of(
                    c -> c.incorporation(null),
                    "customerInfo.incorporation",
                    "must not be blank"
                ),
                Triple.of(
                    c -> c.ogrn(null),
                    "customerInfo.ogrn",
                    "must not be blank"
                ),
                Triple.of(
                    c -> c.address(null),
                    "customerInfo.address",
                    "must not be blank"
                ),
                Triple.of(
                    c -> c.inn(null),
                    "customerInfo.inn",
                    "must not be blank"
                ),
                Triple.of(
                    c -> c.kpp(null),
                    "customerInfo.kpp",
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
            )
            .map(t -> Triple.of(
                c -> c.customerInfo(t.getLeft().apply(customerInfoBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            ));
    }

    @Nonnull
    private static Stream<Triple<Consumer<ClaimDataBuilder>, String, String>> validationOrder() {
        return Stream.<Triple<UnaryOperator<OrderBuilder>, String, String>>of(
                Triple.of(
                    b -> b.externalId(null),
                    "orders[0].externalId",
                    "must not be blank"
                ),
                Triple.of(
                    b -> b.address(null),
                    "orders[0].address",
                    "must not be blank"
                ),
                Triple.of(
                    b -> b.shipmentDate(null),
                    "orders[0].shipmentDate",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.assessedValue(null),
                    "orders[0].assessedValue",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.comments(null),
                    "orders[0].comments",
                    "must not be blank"
                )
            )
            .map(t -> Triple.of(
                c -> c.orders(List.of(t.getLeft().apply(orderBuilder()).build())),
                t.getMiddle(),
                t.getRight()
            ));
    }

    @Nonnull
    @Override
    protected String defaultRequestBodyPath() {
        return "controller/documents/request/claim.json";
    }

    @Nonnull
    @Override
    protected String defaultHtmlResponseBodyPath() {
        return "controller/documents/response/claim.html";
    }

    @Nonnull
    @Override
    protected String defaultFilename() {
        return "CLAIM-AC-TEST-00000001";
    }

    @Nonnull
    @Override
    protected String requestPath() {
        return "/document/claim/generate";
    }

    @Nonnull
    @Override
    protected WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A4, PageOrientation.PORTRAIT);
    }

    @Nonnull
    private static ContractorInfoBuilder contractorInfoBuilder() {
        return ContractorInfo.builder()
            .incorporation("ООО ПЕРВАЯ ЭКСПЕДИЦИОННАЯ КОМПАНИЯ")
            .address("Россия, 109428, г. Москва, 1-ый Вязовский проезд, д.4, стр.19");
    }

    @Nonnull
    private static ClaimDataBuilder claimDataBuilder() {
        return ClaimData.builder()
            .contractorInfo(contractorInfoBuilder().build())
            .id("CLAIM-AC-TEST-00000001")
            .date(LocalDate.of(2021, 3, 16))
            .agreement("ББПКОХ14")
            .agreementDate(LocalDate.of(2020, 7, 1))
            .orders(List.of(orderBuilder().build()))
            .amount(BigDecimal.valueOf(1740))
            .customerInfo(customerInfoBuilder().build())
            .manager("О.В. Ларионова")
            .proxyDate(LocalDate.of(2019, 6, 20));
    }

    @Nonnull
    private static OrderBuilder orderBuilder() {
        return Order.builder()
            .externalId("33250421")
            .address("Россия, 109428, г. Москва, 1-ый Вязовский проезд, д.5, стр.19")
            .shipmentDate(LocalDate.of(2019, 6, 15))
            .assessedValue(BigDecimal.valueOf(1740))
            .comments("7 февраля 2021 г., Возвратный заказ на складе СЦ, однако на возврат заказ передан не был");
    }

    @Nonnull
    private static CustomerInfoBuilder customerInfoBuilder() {
        return CustomerInfo.builder()
            .incorporation("ООО Яндекс.Маркет")
            .ogrn("1167746491395")
            .address("119021, г. Москва, ул. Тимура Фрунзе, д. 11, строение 44, этаж 5")
            .inn("7704357909")
            .kpp("770401001")
            .account("40702810438000034726")
            .bankName("ПАО Сбербанк")
            .bik("044525225")
            .correspondentAccount("30101810400000000225");
    }
}
