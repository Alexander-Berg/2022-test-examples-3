package ru.yandex.market.wrap.infor.service.health;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.wrap.infor.configuration.property.WarehouseMapProperties;
import ru.yandex.market.wrap.infor.configuration.property.WarehouseProperties;
import ru.yandex.market.wrap.infor.repository.InconsistentLifetimeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class InconsistentSkuLifetimeServiceTest {

    private static final String TOKEN = "TOKEN";
    private static final String ANOTHER_TOKEN = "ANOTHER_TOKEN";
    private static final String OK_MESSAGE = "147=0,171=0,172=0";
    private static final String CRIT_MESSAGE = "147=0,171=1,172=0";

    private InconsistentLifetimeRepository repository;
    private TokenContextHolder holder;
    private WarehouseMapProperties properties;

    @BeforeEach
    void initialize() {
        repository = Mockito.mock(InconsistentLifetimeRepository.class);
        holder = Mockito.mock(TokenContextHolder.class);
        properties = new WarehouseMapProperties(
            ImmutableMap.of(
                "147", new WarehouseProperties(TOKEN, "147", null, null, null),
                "171", new WarehouseProperties(TOKEN, "171", null, null, null),
                "172", new WarehouseProperties(TOKEN, "172", null, null, null)
            ));
    }

    @Test
    void shouldReturnOkAfterCreation() {
        InconsistentSkuLifetimeService service = new InconsistentSkuLifetimeService(properties, holder, repository);

        CheckResult result = service.getLastCheckStatus();

        assertEquals(CheckResult.Level.OK, result.getLevel());
        assertTrue(result.getMessage().isEmpty());
        verifyZeroInteractions(repository);
        verifyZeroInteractions(holder);
    }

    @Test
    void shouldReturnOkWithMessageWhenInitializedAndAllSkuLifetimesConsistent() {
        InconsistentSkuLifetimeService service = new InconsistentSkuLifetimeService(properties, holder, repository);
        when(repository.countSkuWithInconsistentLifetimes()).thenReturn(0);

        service.updateCache();
        CheckResult result = service.getLastCheckStatus();

        assertEquals(CheckResult.Level.OK, result.getLevel());
        assertEquals(OK_MESSAGE, result.getMessage());
        verify(holder, times(3)).setToken(TOKEN);
        verify(repository, times(3)).countSkuWithInconsistentLifetimes();
        verify(holder, times(3)).clearToken();
    }

    @Test
    void shouldReturnCritWithMessageWhenInitializedAndHasInconsistency() {
        WarehouseMapProperties props = new WarehouseMapProperties(
            ImmutableMap.of(
                "147", new WarehouseProperties(TOKEN, "147", null, null, null),
                "171", new WarehouseProperties(ANOTHER_TOKEN, "171", null, null, null),
                "172", new WarehouseProperties(TOKEN, "172", null, null, null)
            ));
        InconsistentSkuLifetimeService service =
            new InconsistentSkuLifetimeService(props, holder, repository);
        when(repository.countSkuWithInconsistentLifetimes()).thenReturn(0, 1, 0);

        service.updateCache();
        CheckResult result = service.getLastCheckStatus();

        assertEquals(CheckResult.Level.CRITICAL, result.getLevel());
        assertEquals(CRIT_MESSAGE, result.getMessage());
        InOrder inOrder = Mockito.inOrder(holder);
        inOrder.verify(holder).setToken(TOKEN);
        inOrder.verify(holder).setToken(ANOTHER_TOKEN);
        inOrder.verify(holder).setToken(TOKEN);
        verify(repository, times(3)).countSkuWithInconsistentLifetimes();
        verify(holder, times(3)).clearToken();
    }

}
