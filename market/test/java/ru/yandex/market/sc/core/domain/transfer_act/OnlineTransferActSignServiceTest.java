package ru.yandex.market.sc.core.domain.transfer_act;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.transferact.client.api.SignatureApi;
import ru.yandex.market.tpl.common.transferact.client.model.SignatureDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OnlineTransferActSignServiceTest {

    private OnlineTransferActSignService onlineTransferActSignServiceUnderTest;
    private final SignatureApi signatureApi =
            Mockito.mock(SignatureApi.class);
    private final OnlineTransferActRouteQueryService onlineTransferActRouteQueryService =
            Mockito.mock(OnlineTransferActRouteQueryService.class);

    @BeforeEach
    void init() {
        onlineTransferActSignServiceUnderTest = new OnlineTransferActSignService(
                signatureApi,
                onlineTransferActRouteQueryService
        );
    }

    @Test
    void sign() {
        SortingCenter sortingCenter = TestFactory.sortingCenter(123L);
        TransferDto transferDto = OnlineTransferActTestFactory.createTransferDto(
                "1",
                LocalDate.of(2021, Month.DECEMBER, 19),
                sortingCenter.getYandexId(),
                "123",
                List.of("order1"),
                List.of()
        );
        Mockito.when(onlineTransferActRouteQueryService.getLastStockmanSignatureData(
                sortingCenter,
                transferDto
        )).thenReturn(new OnlineTransferActRouteQueryService.SignatureData(
                456L,
                "Vasiliy"
        ));
        onlineTransferActSignServiceUnderTest.sign(sortingCenter, transferDto);

        SignatureDto signatureDto = new SignatureDto();
        signatureDto.setSignerId("456");
        signatureDto.setSignerName("Vasiliy");
        signatureDto.setSignatureData("b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0");
        Mockito.verify(signatureApi)
                .transferTransferIdSignaturePut("1", signatureDto);
    }
}
