package ru.yandex.market.ff.service.implementation;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeatureToggleServiceImplTest {

    @Test
    void is5thRegistryTypeAllowed() {
        ConcreteEnvironmentParamService cps = mock(ConcreteEnvironmentParamService.class);
        FeatureToggleServiceImpl ftService = new FeatureToggleServiceImpl(cps);

        when(cps.additionalSupplyPartnersEnabled()).thenReturn(Set.of(123L, 0L));
        when(cps.additionalSupplyWarehousesEnabled()).thenReturn(Set.of(456L, 9L));
        when(cps.supplyTypesMigratedTo5thRegistryType()).thenReturn(Set.of(RequestType.SUPPLY));

        Assertions.assertTrue(ftService.is5thRegistryTypeAllowed(123L, 456L, RequestType.SUPPLY));
        Assertions.assertFalse(ftService.is5thRegistryTypeAllowed(22L, 456L, RequestType.SUPPLY));
        Assertions.assertFalse(ftService.is5thRegistryTypeAllowed(123L, 456L, RequestType.X_DOC_PARTNER_SUPPLY_TO_FF));
        Assertions.assertFalse(ftService.is5thRegistryTypeAllowed(123L, 456L, null));
    }
}
