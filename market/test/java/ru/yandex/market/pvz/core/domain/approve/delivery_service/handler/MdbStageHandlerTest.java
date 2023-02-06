package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.MdbDeliveryServiceCreator;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MdbStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private MdbDeliveryServiceCreator mdbDeliveryServiceCreator;

    private final MdbStageHandler mdbStageHandler;

    @Test
    void setupMdbMappings() {
        mdbStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(mdbDeliveryServiceCreator, times(1)).addMapping(DELIVERY_SERVICE_ID);
    }

    @Test
    void mdbInternalError() {
        doThrow(new HttpTemplateException(500, "")).when(mdbDeliveryServiceCreator).addMapping(anyLong());
        assertThatThrownBy(() -> mdbStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);
        verify(mdbDeliveryServiceCreator, times(1)).addMapping(DELIVERY_SERVICE_ID);
    }
}
