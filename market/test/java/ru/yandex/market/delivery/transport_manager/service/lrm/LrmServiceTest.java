package ru.yandex.market.delivery.transport_manager.service.lrm;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class LrmServiceTest {
    LrmService service = new LrmService(null);

    @Test
    public void getLrmWithNoBoxes() {
        Assertions.assertEquals(Collections.emptyList(), service.searchByBoxes(Collections.emptyList()));
    }

    @Test
    public void getLrmWithNull() {
        Assertions.assertEquals(Collections.emptyList(), service.searchByBoxes(null));
    }

    @Test
    public void getLrmWithNonEmptyCollection() {
        ReturnsApi mock = Mockito.mock(ReturnsApi.class);
        service = new LrmService(mock);
        service.searchByBoxes(Collections.singletonList("BOX"));
        verify(mock).searchReturns(any());
    }

}
