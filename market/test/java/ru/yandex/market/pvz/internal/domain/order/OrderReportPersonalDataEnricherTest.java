package ru.yandex.market.pvz.internal.domain.order;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.core.domain.order.model.OrderReport;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class OrderReportPersonalDataEnricherTest {

    @Mock
    private PersonalExternalService personalExternalService;

    @Test
    void enrichOrders() {
        when(personalExternalService.getMultiTypePersonalByIds(List.of(
                Pair.of("1", CommonTypeEnum.FULL_NAME),
                Pair.of("2", CommonTypeEnum.PHONE),
                Pair.of("3", CommonTypeEnum.FULL_NAME),
                Pair.of("4", CommonTypeEnum.PHONE),
                Pair.of("5", CommonTypeEnum.FULL_NAME),
                Pair.of("6", CommonTypeEnum.PHONE)))
        ).thenReturn(List.of(
                new MultiTypeRetrieveResponseItem()
                        .id("1")
                        .type(CommonTypeEnum.FULL_NAME)
                        .value(new CommonType().fullName(new FullName().forename("Пупкин").surname("Василий"))),
                new MultiTypeRetrieveResponseItem()
                        .id("2")
                        .type(CommonTypeEnum.PHONE)
                        .value(new CommonType().phone("+71112223342")),
                new MultiTypeRetrieveResponseItem()
                        .id("3")
                        .type(CommonTypeEnum.FULL_NAME)
                        .value(new CommonType().fullName(new FullName().forename("Петров").surname("Иван"))),
                new MultiTypeRetrieveResponseItem()
                        .id("4")
                        .type(CommonTypeEnum.PHONE)
                        .value(new CommonType().phone("+71112223344")),
                new MultiTypeRetrieveResponseItem()
                        .id("5")
                        .type(CommonTypeEnum.FULL_NAME)
                        .value(new CommonType().fullName(new FullName().forename("Сидоров").surname("Семен"))),
                new MultiTypeRetrieveResponseItem()
                        .id("6")
                        .type(CommonTypeEnum.PHONE)
                        .value(new CommonType().phone("+71112223346"))
        ));

        Page<OrderReport> orders = new PageImpl<>(List.of(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("3")
                        .recipientPhoneId("4")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("5")
                        .build(),
                OrderReport.builder()
                        .recipientPhoneId("6")
                        .build(),
                OrderReport.builder().build()
        ));

        var enricher = new OrderReportPersonalDataEnricher(personalExternalService);

        enricher.enrich(orders);

        assertThat(orders).containsExactly(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .recipientName("Василий Пупкин")
                        .recipientPhone("+71112223342")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("3")
                        .recipientPhoneId("4")
                        .recipientName("Иван Петров")
                        .recipientPhone("+71112223344")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("5")
                        .recipientName("Семен Сидоров")
                        .build(),
                OrderReport.builder()
                        .recipientPhoneId("6")
                        .recipientPhone("+71112223346")
                        .build(),
                OrderReport.builder().build()
        );
    }

    @Test
    void enrichSeveralOrdersOfOneRecipient() {
        when(personalExternalService.getMultiTypePersonalByIds(List.of(
                Pair.of("1", CommonTypeEnum.FULL_NAME),
                Pair.of("2", CommonTypeEnum.PHONE),
                Pair.of("1", CommonTypeEnum.FULL_NAME),
                Pair.of("2", CommonTypeEnum.PHONE)))
        ).thenReturn(List.of(
                new MultiTypeRetrieveResponseItem()
                        .id("1")
                        .type(CommonTypeEnum.FULL_NAME)
                        .value(new CommonType().fullName(new FullName().forename("Пупкин").surname("Василий"))),
                new MultiTypeRetrieveResponseItem()
                        .id("2")
                        .type(CommonTypeEnum.PHONE)
                        .value(new CommonType().phone("+71112223342"))
        ));

        Page<OrderReport> orders = new PageImpl<>(List.of(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .build()
        ));

        var enricher = new OrderReportPersonalDataEnricher(personalExternalService);

        enricher.enrich(orders);

        assertThat(orders).containsExactly(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .recipientName("Василий Пупкин")
                        .recipientPhone("+71112223342")
                        .build(),
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .recipientName("Василий Пупкин")
                        .recipientPhone("+71112223342")
                        .build()
        );
    }

    @Test
    void enrichOrderWithAbsentDataInPersonal() {
        when(personalExternalService.getMultiTypePersonalByIds(List.of(
                Pair.of("1", CommonTypeEnum.FULL_NAME),
                Pair.of("2", CommonTypeEnum.PHONE)))
        ).thenReturn(List.of());

        Page<OrderReport> orders = new PageImpl<>(List.of(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .build()
        ));

        var enricher = new OrderReportPersonalDataEnricher(personalExternalService);

        enricher.enrich(orders);

        assertThat(orders).containsExactly(
                OrderReport.builder()
                        .recipientFullnameId("1")
                        .recipientPhoneId("2")
                        .build()
        );
    }
}
