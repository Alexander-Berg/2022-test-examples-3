package ru.yandex.market.logistics.utilizer.service.system;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.domain.entity.SystemProperty;
import ru.yandex.market.logistics.utilizer.repo.SystemPropertyJpaRepository;
import ru.yandex.market.logistics.utilizer.service.system.keys.SystemPropertySetIntegerKey;
import ru.yandex.market.logistics.utilizer.service.system.keys.SystemPropertySetLongKey;

public class SystemPropertyServiceUnitTest extends SoftAssertionSupport {

    private SystemPropertyService systemPropertyService;
    private SystemPropertyJpaRepository systemPropertyRepository;

    @BeforeEach
    public void init() {
        systemPropertyRepository = Mockito.mock(SystemPropertyJpaRepository.class);
        systemPropertyService = new SystemPropertyService(systemPropertyRepository);
        Mockito.when(systemPropertyRepository
                .findByName(SystemPropertySetLongKey.VENDORS_TO_FINALIZE_CYCLE_FOR.name()))
                .thenReturn(Optional.of(createSystemProperty(" 123, 456   ")));
        Mockito.when(systemPropertyRepository
                .findByName(SystemPropertySetLongKey.VENDORS_TO_CREATE_TRANSFER_FOR.name()))
                .thenReturn(Optional.of(createSystemProperty(" ")));
    }

    @Test
    public void systemPropertySetLongForNotEmptyString() {
        Set<Long> actual = systemPropertyService.getProperty(SystemPropertySetLongKey.VENDORS_TO_FINALIZE_CYCLE_FOR);
        softly.assertThat(actual).containsExactlyInAnyOrder(123L, 456L);
    }

    @Test
    public void systemPropertySetLongForEmptyString() {
        Set<Long> actual = systemPropertyService.getProperty(SystemPropertySetLongKey.VENDORS_TO_CREATE_TRANSFER_FOR);
        softly.assertThat(actual).isEmpty();
    }

    @Test
    public void systemPropertySetIntegerForNotEmptyString() {
        Mockito.when(systemPropertyRepository
                .findByName(SystemPropertySetIntegerKey.WAREHOUSES_FOR_UTILIZATION.name()))
                .thenReturn(Optional.of(createSystemProperty(" 123, 456   ")));
        Set<Integer> actual = systemPropertyService.getProperty(SystemPropertySetIntegerKey.WAREHOUSES_FOR_UTILIZATION);
        softly.assertThat(actual).containsExactlyInAnyOrder(123, 456);
    }

    @Test
    public void systemPropertySetIntegerForEmptyString() {
        Mockito.when(systemPropertyRepository
                .findByName(SystemPropertySetIntegerKey.WAREHOUSES_FOR_UTILIZATION.name()))
                .thenReturn(Optional.of(createSystemProperty("")));
        Set<Integer> actual = systemPropertyService.getProperty(SystemPropertySetIntegerKey.WAREHOUSES_FOR_UTILIZATION);
        softly.assertThat(actual).isEmpty();
    }

    private SystemProperty createSystemProperty(String value) {
        SystemProperty systemProperty = new SystemProperty();
        systemProperty.setValue(value);
        return systemProperty;
    }
}
