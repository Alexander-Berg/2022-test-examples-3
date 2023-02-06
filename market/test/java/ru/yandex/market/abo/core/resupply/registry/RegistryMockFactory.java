package ru.yandex.market.abo.core.resupply.registry;

import static org.mockito.Mockito.mock;

public class RegistryMockFactory {

    private static RegistryValidationService registryValidationService;

    public static synchronized RegistryValidationService getRegistryValidationServiceMock() {
        if (registryValidationService == null) {
            registryValidationService = mock(RegistryValidationService.class);
        }
        return registryValidationService;
    }
}
