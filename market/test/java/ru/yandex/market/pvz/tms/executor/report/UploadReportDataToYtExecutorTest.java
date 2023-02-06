package ru.yandex.market.pvz.tms.executor.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.Report;
import ru.yandex.market.pvz.core.domain.report.ReportCommandService;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.tms.executor.report.producer.model.PaidOrderYtModel;
import ru.yandex.market.pvz.tms.test.TransactionlessEmbeddedDbTmsTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})

@TransactionlessEmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UploadReportDataToYtExecutorTest {

    private final TestableClock clock;

    private final UploadReportDataToYtExecutor executor;
    private final ReportCommandService reportCommandService;
    private final ReportGenerator reportGenerator;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final Yt hahn;
    private final Yt arnold;

    private Map<Yt, ArgumentCaptor<Iterator<?>>> clusters;

    @MockBean
    private ReportService reportService;

    @BeforeEach
    void setup() {
        clusters = Map.of(
                hahn, ArgumentCaptor.forClass(Iterator.class),
                arnold, ArgumentCaptor.forClass(Iterator.class)
        );

        for (Yt cluster : clusters.keySet()) {
            when(cluster.cypress().exists(any(YPath.class))).thenReturn(false);
            when(cluster.cypress()
                    .get(any(), anyCollection())
                    .getAttributeOrThrow(anyString())
                    .stringValue()
            ).thenReturn("mounted");

            YtTables tables = cluster.tables();
            doNothing().when(tables).insertRows(
                    any(), anyBoolean(), anyBoolean(),
                    any(YTableEntryType.class),
                    clusters.get(cluster).capture()
            );
        }

    }

    @Test
    void test() {

        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        partner = legalPartnerFactory
                .forceApprove(partner.getId(), LocalDate.of(2019, 9, 1));

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(partner)
                        .build());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime deliveryDate = LocalDateTime.of(2020, 9, 3, 0, 0, 0);
        clock.setFixed(deliveryDate.toInstant(zone), zone);

        orderFactory.deliverOrderCompletely(order.getId(), OrderDeliveryType.PASSPORT, null);

        LocalDate from = LocalDate.of(2020, 9, 1);
        LocalDate to = LocalDate.of(2020, 9, 30);

        Report paidOrderReport = reportGenerator.generate(partner.getId(), PvzReportType.PAID_ORDER, from, to);
        reportCommandService.markPaid(paidOrderReport.getId(), Instant.now(), "123", to);

        executor.doRealJob(null);

        for (ArgumentCaptor<?> captor : clusters.values()) {
            List<Class<?>> uploadedClasses = (List) captor.getAllValues().stream()
                    .flatMap(arg -> Streams.stream((Iterator) arg))
                    .map(Object::getClass)
                    .collect(Collectors.toList());

            assertThat(uploadedClasses).containsExactlyInAnyOrderElementsOf(List.of(
                    PaidOrderYtModel.class
            ));
        }

        reportCommandService.markPaid(paidOrderReport.getId(), Instant.now(), "123", LocalDate.now(), true);
        executor.doRealJob(null);

        for (ArgumentCaptor<?> captor : clusters.values()) {
            List<Class<?>> uploadedClasses = (List) captor.getAllValues().stream()
                    .flatMap(arg -> Streams.stream((Iterator) arg))
                    .map(Object::getClass)
                    .collect(Collectors.toList());

            assertThat(uploadedClasses).containsExactlyInAnyOrderElementsOf(List.of(
                    PaidOrderYtModel.class
            ));
        }
    }

}
