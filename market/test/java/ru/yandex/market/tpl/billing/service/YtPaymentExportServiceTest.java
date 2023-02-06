package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.billing.dao.PaymentDao;
import ru.yandex.market.tpl.billing.model.PaymentMethod;
import ru.yandex.market.tpl.billing.model.yt.YtOnDeliveryPaymentDto;
import ru.yandex.market.tpl.billing.model.projection.OnDeliveryPaymentProjection;
import ru.yandex.market.tpl.billing.repository.PvzOrderRepository;
import ru.yandex.market.tpl.billing.service.yt.YtService;
import ru.yandex.market.tpl.billing.service.yt.exports.YtBillingExportService;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: Replace by the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82).
@ExtendWith(SpringExtension.class)
class YtPaymentExportServiceTest {
    private static final LocalDate DATE = LocalDate.of(2020, 10, 10);
    public static final OffsetDateTime FROM_DATETIME = DateTimeUtil.atStartOfDayWithOffset(DATE);
    public static final OffsetDateTime TO_DATETIME = DateTimeUtil.atEndOfDayWithOffset(DATE);
    public static final String FOLDER = "folder";
    private static final String FOLDER_NAME = null;

    @Mock
    private PvzOrderRepository pvzOrderRepository;
    @Mock
    private YtService ytService;


    @Test
    void exportForDate() {
        PaymentDao paymentDao = new PaymentDao(pvzOrderRepository);
        YtBillingExportService<YtOnDeliveryPaymentDto> ytPaymentExportService = new YtBillingExportService<>(
                ytService,
                paymentDao,
                FOLDER,
                FOLDER_NAME
        );
        List<YtOnDeliveryPaymentDto> orders = List.of(
                YtOnDeliveryPaymentDto.builder()
                        .orderId("EX-1893942")
                        .serviceEventTime("2021-01-18T10:15:30.000000+03:00")
                        .pickupPointId(100001L)
                        .balanceClientId("client-100000001")
                        .virtualAccountNumber("account-1001")
                        .inn("1003256219")
                        .paymentSum("372.5")
                        .paymentMethod("CARD")
                        .build()
        );

        OnDeliveryPaymentProjection projection = new OnDeliveryPaymentProjectionMock()
                .setMarketOrderId("EX-1893942")
                .setDeliveredAt(OffsetDateTime.parse("2021-01-18T10:15:30+03:00").toInstant())
                .setPickupPointId(100001L)
                .setBalanceClientId("client-100000001")
                .setVirtualAccountNumber("account-1001")
                .setInn("1003256219")
                .setPaymentSum(BigDecimal.valueOf(372.50))
                .setPaymentMethod(PaymentMethod.CARD);
        when(pvzOrderRepository.selectForPaymentsExport(FROM_DATETIME, TO_DATETIME)).thenReturn(List.of(projection));
        ytPaymentExportService.exportForDate(DATE, false);
        verify(ytService, times(1))
                .export(orders,
                        YtOnDeliveryPaymentDto.class,
                        null,
                        FOLDER,
                        YtService.getDateTableName(DATE),
                        false);
    }

    @Data
    @Accessors(chain = true)
    private static class OnDeliveryPaymentProjectionMock implements OnDeliveryPaymentProjection {
        String marketOrderId;
        String serviceEventTime;
        Long pickupPointId;
        String balanceClientId;
        String virtualAccountNumber;
        String inn;
        BigDecimal paymentSum;
        PaymentMethod paymentMethod;
        Instant deliveredAt;
        String payload;
    }
}
