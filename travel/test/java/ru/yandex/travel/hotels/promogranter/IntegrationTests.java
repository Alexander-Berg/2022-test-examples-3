package ru.yandex.travel.hotels.promogranter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.cpa.tours.EEnvironment;
import ru.yandex.travel.hotels.promogranter.proto.TToursCpaRecord;
import ru.yandex.travel.hotels.promogranter.repositories.PlusTopupsRepository;
import ru.yandex.travel.hotels.promogranter.services.orders_client.OrchestratorClient;
import ru.yandex.travel.hotels.promogranter.services.tours_cpa.ToursCpaService;
import ru.yandex.travel.orders.yandex_plus.TGetPlusTopupStatusRsp;
import ru.yandex.travel.orders.yandex_plus.TSchedulePlusTopupRsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {}
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Slf4j
public class IntegrationTests {

    @Autowired
    private PlusTopupsRepository plusTopupsRepository;

    @MockBean
    private ToursCpaService toursCpaService;

    @MockBean
    private OrchestratorClient orchestratorClient;

    @Test
    public void testSimple() {
        var now = Instant.now();
        var orderAmount = 1000;
        var profitAmount = 100;
        var plusAmount = 50; // 0.05 * orderAmount

        when(toursCpaService.isReady()).thenReturn(true);
        when(toursCpaService.getAllTours()).thenReturn(List.of(buildSimpleCpaRecord(now, orderAmount, profitAmount)));
        when(orchestratorClient.schedulePlusTopup(any(), any())).thenReturn(CompletableFuture.completedFuture(TSchedulePlusTopupRsp.newBuilder()
                .setScheduled(true)
                .build()));
        when(orchestratorClient.getPlusTopupStatus(any(), any())).thenReturn(CompletableFuture.completedFuture(TGetPlusTopupStatusRsp.newBuilder()
                .setSchedulingStatus(TGetPlusTopupStatusRsp.EScheduledTopupStatus.STC_SUCCEED)
                .setTopupInfo(TGetPlusTopupStatusRsp.TTopupInfo.newBuilder()
                        .setAmount(plusAmount)
                        .setPurchaseToken("token")
                        .setFinishedAt(ProtoUtils.fromInstant(Instant.now()))
                        .setSucceed(true)
                        .build())
                .build()));

        waitForPredicateOrTimeout(() -> !plusTopupsRepository.getAll().isEmpty(), Duration.ofSeconds(10), "Wait for topup creation");
        waitForPredicateOrTimeout(() -> plusTopupsRepository.getAll().get(0).getScheduled(), Duration.ofSeconds(10), "Wait for schedule");
        waitForPredicateOrTimeout(() -> plusTopupsRepository.getAll().get(0).getFinished(), Duration.ofSeconds(10), "Wait for finish");

        assertThat(plusTopupsRepository.getAll()).isNotEmpty();
        var topup = plusTopupsRepository.getAll().get(0);
        assertThat(topup.getScheduled()).isTrue();
        assertThat(topup.getFinished()).isTrue();
        assertThat(topup.getTopupAmount().intValueExact()).isEqualTo(plusAmount);
    }

    private static TToursCpaRecord buildSimpleCpaRecord(Instant now, int orderAmount, int profitAmount) {
        var today = LocalDate.ofInstant(now, ZoneId.of("UTC"));
        return TToursCpaRecord.newBuilder()
                .setCategory("tours")
                .setPartnerName("leveltravel-whitelabel")
                .setCreatedAt(now.getEpochSecond())
                .setUpdatedAt(now.getEpochSecond())
                .setPartnerOrderId("111")
                .setStatus("confirmed")
                .setPartnerStatus("PAID")
                .setCheckIn(today.minusDays(10).toString())
                .setCheckOut(today.minusDays(5).toString())
                .setAdults(2)
                .setChildren(0)
                .setInfants(0)
                .setTravelOrderId("YA-" + UUID.randomUUID().toString())
                .setOrderAmount(orderAmount)
                .setProfitAmount(profitAmount)
                .setCurrencyCode("RUB")
                .setHasLabel(true)
                .setLabelUserIp("127.0.0.1")
                .setLabelPassportUid("123")
                .setLabelIsPlusUser(true)
                .setLabelLabelGenerationDate(today.toString())
                .setLabelEnvironment(EEnvironment.E_Testing)
                .build();
    }

    @SuppressWarnings("BusyWait")
    public static void waitForPredicateOrTimeout(Supplier<Boolean> predicate, Duration timeout,
                                                 String description) {
        long startWaitingTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startWaitingTime) < timeout.toMillis() && !predicate.get()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // preserving the status
                throw new RuntimeException(e);
            }
        }
        log.info("waitForPredicateOrTimeout has completed in {} ms with the {} result, check: {}",
                System.currentTimeMillis() - startWaitingTime, predicate.get() ? "OK" : "MISMATCH", description);
        assertThat(predicate.get()).isTrue();
    }
}
