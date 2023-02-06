package ru.yandex.market.mboc.common.honestmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class HonestMarkDepartmentsLoadersHolderTest {

    private static final int REFRESH_TIMEOUT_HOURS = 1;

    ScheduledExecutorService scheduledExecutorService;

    @Spy
    public HonestMarkDepartmentsLoader honestMarkDepartmentExceptionService = new HonestMarkDepartmentsLoader() {
        @Override
        public Map<Long, Set<Long>> getAllGroupsWithCategories() {
            throw new UnsupportedOperationException();
        }
    };

    @Spy
    public HonestMarkDepartmentsLoader honestMarkDepartmentOkService = new HonestMarkDepartmentsLoader() {
        @Override
        public Map<Long, Set<Long>> getAllGroupsWithCategories() {
            Map<Long, Set<Long>> result = new HashMap<>();
            result.put(1L, new HashSet<>(Arrays.asList(1L, 2L)));
            result.put(2L, new HashSet<>(Arrays.asList(3L, 4L)));
            return result;
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .build()
        );

    }

    @Test
    public void whenFirstOkShouldReturnGroups() {
        HonestMarkDepartmentsLoader honestMarkDepartmentServicesHolder = new HonestMarkDepartmentsLoadersHolder(
            Arrays.asList(
                honestMarkDepartmentOkService,
                honestMarkDepartmentExceptionService
            ),
            scheduledExecutorService, REFRESH_TIMEOUT_HOURS
        );
        Map<Long, Set<Long>> allGroupsWithCategories = honestMarkDepartmentServicesHolder.getAllGroupsWithCategories();
        Assertions.assertThat(allGroupsWithCategories).isNotEmpty();
        Mockito.verify(honestMarkDepartmentOkService, Mockito.times(1)).getAllGroupsWithCategories();
        Mockito.verify(honestMarkDepartmentExceptionService, Mockito.times(0)).getAllGroupsWithCategories();
    }

    @Test
    public void whenFirstThrowsAndSecondOkShouldLoadFromNextSource() {
        HonestMarkDepartmentsLoader honestMarkDepartmentServicesHolder = new HonestMarkDepartmentsLoadersHolder(
            Arrays.asList(
                honestMarkDepartmentExceptionService,
                honestMarkDepartmentOkService
            ),
            scheduledExecutorService, REFRESH_TIMEOUT_HOURS
        );
        Map<Long, Set<Long>> allGroupsWithCategories = honestMarkDepartmentServicesHolder.getAllGroupsWithCategories();
        Assertions.assertThat(allGroupsWithCategories).isNotEmpty();
        Mockito.verify(honestMarkDepartmentOkService, Mockito.times(1)).getAllGroupsWithCategories();
        Mockito.verify(honestMarkDepartmentExceptionService, Mockito.times(1)).getAllGroupsWithCategories();
    }

    @Test(expected = RuntimeException.class)
    public void whenBothThrowsShouldThrow() {
        HonestMarkDepartmentsLoader honestMarkDepartmentServicesHolder = new HonestMarkDepartmentsLoadersHolder(
            Arrays.asList(
                honestMarkDepartmentExceptionService,
                honestMarkDepartmentExceptionService
            ),
            scheduledExecutorService, REFRESH_TIMEOUT_HOURS
        );
        Map<Long, Set<Long>> allGroupsWithCategories = honestMarkDepartmentServicesHolder.getAllGroupsWithCategories();
        Assertions.assertThat(allGroupsWithCategories).isNotEmpty();
        Mockito.verify(honestMarkDepartmentExceptionService, Mockito.times(2)).getAllGroupsWithCategories();
    }
}
