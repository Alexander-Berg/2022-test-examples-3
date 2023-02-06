package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class TarifficatorUpdateTagsStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;
    private static final long TARIFF_ID = 3L;

    @MockBean
    private TarifficatorClient tarifficatorClient;

    private final TarifficatorUpdateTagsStageHandler tarifficatorUpdateTagsStageHandler;

    @Test
    void setupTariffTags() {
        when(tarifficatorClient.searchTariffs(any()))
                .thenReturn(List.of(TariffDto.builder().id(TARIFF_ID).build()));

        tarifficatorUpdateTagsStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(tarifficatorClient, times(1))
                .updateTags(TARIFF_ID, TarifficatorUpdateTagsStageHandler.BERU_TAG_NAMES);
    }

}
