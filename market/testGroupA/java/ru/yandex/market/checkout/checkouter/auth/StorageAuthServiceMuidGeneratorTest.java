package ru.yandex.market.checkout.checkouter.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.auth.limit.AuthRateLimitsChecker;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class StorageAuthServiceMuidGeneratorTest {
    @Mock
    MuidStorage muidStorage;
    // Не используется тут, но используется внутри authService
    @Mock
    AuthRateLimitsChecker authRateLimitsChecker;
    @Mock
    CheckouterFeatureReader checkouterFeatureReader;

    @InjectMocks
    private StorageAuthService authService;

    @Test
    public void shouldUseStorageMuidGeneratorWhenPropertyIsFalse() {
        long expectedId = 1897394L;
        Mockito.reset(checkouterFeatureReader);
        Mockito.when(muidStorage.generate()).thenReturn(expectedId);

        long actualId = authService.generateNewMuid(mock(UserInfo.class), false);

        assertEquals(1L << 60 | expectedId, actualId);
    }
}
