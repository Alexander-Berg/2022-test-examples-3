package ru.yandex.market.sc.core.domain.transfer_act;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.route.model.PendingTransferActsDto;
import ru.yandex.market.sc.core.domain.route.model.TransferActDiscrepancyDetailsDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OnlineTransferActServiceTest {

    private OnlineTransferActService onlineTransferActServiceUnderTest;
    private final TransferApi transferApi =
            Mockito.mock(TransferApi.class);
    private final OnlineTransferActSignService onlineTransferActSignService =
            Mockito.mock(OnlineTransferActSignService.class);
    private final OnlineTransferActPlaceQueryService onlineTransferActPlaceQueryService =
            Mockito.mock(OnlineTransferActPlaceQueryService.class);
    private final SortingCenterPropertySource sortingCenterPropertySource =
            Mockito.mock(SortingCenterPropertySource.class);

    @BeforeEach
    void init() {
        onlineTransferActServiceUnderTest = new OnlineTransferActService(
                transferApi,
                onlineTransferActPlaceQueryService,
                onlineTransferActSignService,
                sortingCenterPropertySource
        );
    }

    @Test
    void getPendingTransferActs() {
        SortingCenter sortingCenter = TestFactory.sortingCenter(123L);
        String sortingCenterYandexId = sortingCenter.getYandexId();
        Mockito.when(transferApi.pendingTransfersGet(sortingCenterYandexId))
                .thenReturn(List.of(
                        OnlineTransferActTestFactory.createPendingTransferDto(
                                "1",
                                LocalDate.of(2021, Month.DECEMBER, 19),
                                sortingCenter.getYandexId(),
                                "456"
                        ),
                        OnlineTransferActTestFactory.createPendingTransferDto(
                                "2",
                                LocalDate.of(2021, Month.DECEMBER, 20),
                                sortingCenter.getYandexId(),
                                "567"
                        )
                ));
        Mockito.when(sortingCenterPropertySource.supportOnlineTransferAct(sortingCenter.getId()))
                .thenReturn(true);
        PendingTransferActsDto pendingTransferActs =
                onlineTransferActServiceUnderTest.getPendingTransferActs(sortingCenter);

        assertThat(pendingTransferActs.pendingTransferActs())
                .containsExactlyInAnyOrder(
                        new PendingTransferActsDto.PendingTransferActDto(
                                "456",
                                LocalDate.of(2021, Month.DECEMBER, 19),
                                "1"
                        ),
                        new PendingTransferActsDto.PendingTransferActDto(
                                "567",
                                LocalDate.of(2021, Month.DECEMBER, 20),
                                "2"
                        )
                );
    }

    @Test
    void getRouteTransferActDetails() {
        SortingCenter sortingCenter = TestFactory.sortingCenter(123L);
        TransferDto transferDto = OnlineTransferActTestFactory.createTransferDto(
                "1",
                LocalDate.of(2021, Month.DECEMBER, 19),
                sortingCenter.getYandexId(),
                "789",
                List.of("received1", "received2"),
                List.of("skipped1", "skipped2")
        );
        Mockito.when(transferApi.transferIdGet("1"))
                .thenReturn(transferDto);
        Mockito.when(onlineTransferActPlaceQueryService.getOrdersWithPlaces(
                sortingCenter,
                Set.of("skipped1", "skipped2")
        )).thenReturn(List.of(
                new OnlineTransferActPlaceQueryService.PlaceDetails(
                        "skipped1",
                        null,
                        ScOrderState.SORTED,
                        "cell1"
                ),
                new OnlineTransferActPlaceQueryService.PlaceDetails(
                        "skipped2",
                        "2-1",
                        ScOrderState.SORTED,
                        "cell1"
                ),
                new OnlineTransferActPlaceQueryService.PlaceDetails(
                        "skipped2",
                        "2-2",
                        ScOrderState.SORTED,
                        "cell2"
                )
        ));
        TransferActDiscrepancyDetailsDto transferActDetails =
                onlineTransferActServiceUnderTest.getRouteTransferActDetails(sortingCenter, "1");
        assertThat(transferActDetails).isEqualTo(new TransferActDiscrepancyDetailsDto(
                "1",
                List.of(
                        new TransferActDiscrepancyDetailsDto.Discrepancy(
                                "skipped1",
                                null,
                                ScOrderState.SORTED,
                                "cell1",
                                TransferActDiscrepancyDetailsDto.Discrepancy.Type.REMOVED
                        ),
                        new TransferActDiscrepancyDetailsDto.Discrepancy(
                                "skipped2",
                                "2-1",
                                ScOrderState.SORTED,
                                "cell1",
                                TransferActDiscrepancyDetailsDto.Discrepancy.Type.REMOVED
                        ),
                        new TransferActDiscrepancyDetailsDto.Discrepancy(
                                "skipped2",
                                "2-2",
                                ScOrderState.SORTED,
                                "cell2",
                                TransferActDiscrepancyDetailsDto.Discrepancy.Type.REMOVED
                        )
                )
        ));
    }

    @Test
    void signOnlineTransferAct() {
        SortingCenter sortingCenter = TestFactory.sortingCenter(123L);
        TransferDto transferDto = OnlineTransferActTestFactory.createTransferDto(
                "1",
                LocalDate.of(2021, Month.DECEMBER, 19),
                sortingCenter.getYandexId(),
                "123",
                List.of("order1"),
                List.of()
        );
        Mockito.when(transferApi.transferIdGet("1"))
                .thenReturn(transferDto);
        onlineTransferActServiceUnderTest.signOnlineTransferAct(sortingCenter, "1");
        Mockito.verify(onlineTransferActSignService)
                .sign(sortingCenter, transferDto);
    }

}
