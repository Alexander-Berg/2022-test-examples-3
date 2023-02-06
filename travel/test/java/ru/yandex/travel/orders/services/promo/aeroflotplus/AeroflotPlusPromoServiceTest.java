package ru.yandex.travel.orders.services.promo.aeroflotplus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.promo.AeroflotPlusPromoInfo;
import ru.yandex.avia.booking.promo.AeroflotPlusPromoInfo.PlusCode;
import ru.yandex.avia.booking.promo.AviaPromoCampaignsInfo;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.entities.promo.aeroflotplus.AeroflotPlusPromoCode;
import ru.yandex.travel.orders.mocks.AeroflotMocks;
import ru.yandex.travel.orders.repository.promo.aeroflotplus.AeroflotPlusPromoCodeRepository;
import ru.yandex.travel.orders.services.email.SendEmailParams;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.workflow.single_operation.SingleOperationService;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AeroflotPlusPromoServiceTest {
    private AeroflotPlusPromoProperties properties;
    private AeroflotPlusPromoCodeRepository repository;
    private SingleOperationService operations;
    private SettableClock clock;
    private AeroflotPlusPromoService service;

    private List<AeroflotPlusPromoCode> latestFreeCodes;
    private SendEmailParams latestAsyncEmail;

    @Before
    public void init() {
        properties = AeroflotPlusPromoProperties.builder()
                .emailCampaign("aeroflotPlusCampaign")
                .startsAt(Instant.parse("2021-06-01T00:00:00Z"))
                .endsAt(Instant.parse("2021-07-01T00:00:00Z"))
                .build();
        repository = Mockito.mock(AeroflotPlusPromoCodeRepository.class);
        operations = Mockito.mock(SingleOperationService.class);
        clock = new SettableClock();
        service = new AeroflotPlusPromoService(properties, repository, operations, clock);

        clock.setCurrentTime(Instant.parse("2021-06-15T00:00:00Z"));
        when(repository.findFreeCodes(anyInt())).thenAnswer(inv -> {
           int n = inv.getArgument(0);
           latestFreeCodes = Stream.iterate(0, i -> i < n, i -> i + 1) // 0 .. n-1
                   .map(i -> AeroflotPlusPromoCode.builder().code("code" + i).plusPoints(1_000).build())
                   .collect(toList());
           return latestFreeCodes;
        });
        when(operations.runOperation(any(), any(), any())).thenAnswer(inv -> {
            latestAsyncEmail = inv.getArgument(2);
            return UUID.randomUUID();
        });

        latestFreeCodes = null;
        latestAsyncEmail = null;
    }

    @Test
    public void testEnabledDates() {
        AeroflotOrder order = testOrder(promo(1_000));

        clock.setCurrentTime(Instant.parse("2021-05-15T00:00:00Z"));
        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());

        clock.setCurrentTime(Instant.parse("2021-06-15T00:00:00Z"));
        service.registerConfirmedOrder(order);
        verify(operations, times(1)).runOperation(any(), any(), any());
        Mockito.clearInvocations(operations);

        clock.setCurrentTime(Instant.parse("2021-07-15T00:00:00Z"));
        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());

        properties.setStartsAt(clock.instant());
        properties.setEndsAt(properties.getStartsAt());
        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());
    }

    @Test
    public void testEnabledPromoInfo() {
        AeroflotOrder order = testOrder(null);

        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());

        order = testOrder(promo(1_000));
        service.registerConfirmedOrder(order);
        verify(operations, times(1)).runOperation(any(), any(), any());
        Mockito.clearInvocations(operations);

        order = testOrder(promo());
        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());

        order = testOrder(null);
        order.getAeroflotOrderItem().getPayload().setPromoCampaignsInfo(null);
        service.registerConfirmedOrder(order);
        verify(operations, never()).runOperation(any(), any(), any());
    }

    @Test
    public void testAssignedCodes() {
        //AeroflotOrder order = testOrder(promo(1_000, 1_000));
        AeroflotOrder order = testOrder(promo(1_000));
        AeroflotPlusPromoInfo promo = order.getAeroflotOrderItem().getPayload()
                .getPromoCampaignsInfo().getPlusPromo2021();
        assertThat(order.getId()).isNotNull();
        assertThat(promo.getEmailTaskId()).isNull();

        service.registerConfirmedOrder(order);
        //assertThat(latestFreeCodes).hasSize(2).allSatisfy(code -> {
        assertThat(latestFreeCodes).hasSize(1).allSatisfy(code -> {
            assertThat(code.getUsedAt()).isEqualTo(clock.instant());
            assertThat(code.getOrderId()).isEqualTo(order.getId());
        });

        assertThat(promo.getEmailTaskId()).isNotNull();
        assertThat(promo.getPlusCodes().get(0).getCode()).isEqualTo("code0");
        //assertThat(promo.getPlusCodes().get(1).getCode()).isEqualTo("code1");
    }

    @Test
    public void testAssignedPointsMismatch() {
        AeroflotOrder order = testOrder(promo(1_000));
        when(repository.findFreeCodes(1)).thenReturn(List.of(AeroflotPlusPromoCode.builder().plusPoints(42).build()));

        assertThatThrownBy(() -> service.registerConfirmedOrder(order))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected amount of points per code");
    }

    @Test
    public void testAsyncEmail() {
        AeroflotOrder order = testOrder(promo(1_000));
        assertThat(order.getId()).isNotNull();
        assertThat(order.getEmail()).isNotEmpty();

        service.registerConfirmedOrder(order);

        assertThat(latestAsyncEmail.getCampaignId()).isEqualTo("aeroflotPlusCampaign");

        assertThat(latestAsyncEmail.getTargets()).hasSize(1);
        assertThat(latestAsyncEmail.getTargets().get(0).getEmail()).isEqualTo(order.getEmail());

        assertThat(latestAsyncEmail.getArguments()).isEqualTo(SendEmailParams.wrapArgs(Map.of("code", "code0")));

        assertThat(latestAsyncEmail.getContextEntityId()).isEqualTo(order.getId());
    }

    private AeroflotOrder testOrder(AeroflotPlusPromoInfo promo) {
        AeroflotOrder order = AeroflotMocks.testOrder();
        order.getAeroflotOrderItem().getPayload().setPromoCampaignsInfo(AviaPromoCampaignsInfo.builder()
                .plusPromo2021(promo)
                .build());
        return order;
    }

    private AeroflotPlusPromoInfo promo(Integer... plusPoints) {
        return AeroflotPlusPromoInfo.builder()
                .enabled(plusPoints != null && plusPoints.length > 0)
                .plusCodes(plusPoints == null ? List.of() : Arrays.stream(plusPoints)
                        .map(p -> PlusCode.builder().points(p).build())
                        .collect(toList()))
                .build();
    }
}
