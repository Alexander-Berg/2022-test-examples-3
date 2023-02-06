package ru.yandex.market.checkout.checkouter.returns;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnsDao;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnCapacityStorageTest {

    private final Clock clock = Clock.systemDefaultZone();
    @Mock
    private CheckouterFeatureReader featureReader;
    @Mock
    private ReturnsDao returnsDao;

    private Map<LocalDate, Integer> storage;
    private ReturnCapacityStorage capacityStorage;
    private static int maxReturnCount = 2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(featureReader.getInteger(IntegerFeatureType.RETURN_MAX_CAPACITY)).thenReturn(maxReturnCount);
        storage = new HashMap<>();
        Mockito.when(returnsDao.getCourierReturnCapacityInfo()).thenReturn(storage);
        capacityStorage = new ReturnCapacityStorage(featureReader, returnsDao, clock);
    }

    @Test()
    @DisplayName("На дату нет созданных возвратов - true")
    void trueWhenDateNotExist() {
        ReturnCapacityStorage.ReturnCapacityInfo storageInfo = capacityStorage.getInfo();
        assertThat(storageInfo.isCapacityAvailable(1)).isTrue();
    }

    @Test
    @DisplayName("На дату есть возвраты, но их мало - true")
    void trueWhenMinReturnCount() {
        storage.put(LocalDate.now().plusDays(2), 1);
        ReturnCapacityStorage.ReturnCapacityInfo storageInfo = capacityStorage.getInfo();
        assertThat(storageInfo.isCapacityAvailable(2)).isTrue();
    }

    @Test
    @DisplayName("На дату есть возвраты, и их максимальное количество - false")
    void falseWhenMaxReturnCount() {
        storage.put(LocalDate.now().plusDays(2), maxReturnCount);
        ReturnCapacityStorage.ReturnCapacityInfo storageInfo = capacityStorage.getInfo();
        assertThat(storageInfo.isCapacityAvailable(2)).isFalse();
    }
}
