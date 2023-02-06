package ru.yandex.market.ff.service.lrm;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class LrmServiceTest {
    LrmService service = new LrmService(null, null);

    @Test
    public void getLrmWithNoBoxes() {
        Assertions.assertEquals(Collections.emptyList(), service.searchByBoxes(Set.of()));
    }

    @Test
    public void getLrmWithNull() {
        Assertions.assertEquals(Collections.emptyList(), service.searchByBoxes(null));
    }

    @Test
    public void getLrmWithNonEmptyCollection() {
        ReturnsApi mock = Mockito.mock(ReturnsApi.class);
        ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);
        service = new LrmService(mock, paramService);
        service.searchByBoxes(Set.of("BOX"));
        verify(mock).searchReturns(any());
    }

}
