package ru.yandex.market.mboc.common.honestmark;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HonestMarkDepartmentServiceTest {


    private HonestMarkDepartmentsLoader honestMarkDepartmentsLoader;
    private HonestMarkDepartmentService honestMarkDepartmentService;

    private static final long bootsDepId = 3L;
    private static final long bootsDepHid1 = 101L;
    private static final long bootsDepHid2 = 102L;
    private static final Set<Long> bootsDepHids = Set.of(bootsDepHid1, bootsDepHid2);

    @Before
    public void setUp() throws Exception {
        honestMarkDepartmentsLoader = Mockito.mock(HonestMarkDepartmentsLoader.class);
        honestMarkDepartmentService = new HonestMarkDepartmentService(honestMarkDepartmentsLoader);
        Mockito.when(honestMarkDepartmentsLoader.getAllGroupsWithCategories())
            .thenReturn(Map.of(bootsDepId, bootsDepHids));
    }

    @Test
    public void testGetDepartmentByNameOther() {
        Optional<HonestMarkDepartment> department = honestMarkDepartmentService
            .getDepartmentByName(HonestMarkDepartment.OTHER.getName());
        Assertions.assertThat(department).isPresent();
        Assertions.assertThat(department.get()).isEqualTo(HonestMarkDepartment.OTHER);
    }

    @Test
    public void testGetDepartmentByIdOther() {
        Optional<HonestMarkDepartment> department = honestMarkDepartmentService
            .getDepartmentById(HonestMarkDepartment.OTHER.getId());
        Assertions.assertThat(department).isPresent();
        Assertions.assertThat(department.get()).isEqualTo(HonestMarkDepartment.OTHER);
    }

    @Test
    public void testGetDepartmentByNameUnexistingName() {
        Optional<HonestMarkDepartment> department = honestMarkDepartmentService.getDepartmentByName("unexisting_name");
        Assertions.assertThat(department).isNotPresent();
    }

    @Test
    public void testGetDepartmentByNameOk() {
        final String bootsName = "boots";
        final long bootsId = 3L;
        final Set<Long> categories = Set.of(1L, 2L, 3L);
        Mockito.when(honestMarkDepartmentsLoader.getAllGroupsWithCategories()).thenReturn(Map.of(bootsId, categories));
        Optional<HonestMarkDepartment> department = honestMarkDepartmentService.getDepartmentByName(bootsName);
        Assertions.assertThat(department).isPresent();
        HonestMarkDepartment honestMarkDepartment = department.get();
        Assertions.assertThat(honestMarkDepartment.getId()).isEqualTo(bootsId);
        Assertions.assertThat(honestMarkDepartment.getName()).isEqualTo(bootsName);
        Assertions.assertThat(honestMarkDepartment.getCategories()).isEqualTo(categories);
    }

    @Test
    public void testGetDepartmentByNameIdNotFound() {
        final String bootsName = "boots";
        Mockito.when(honestMarkDepartmentsLoader.getAllGroupsWithCategories()).thenReturn(Map.of());
        Optional<HonestMarkDepartment> department = honestMarkDepartmentService.getDepartmentByName(bootsName);
        Assertions.assertThat(department).isNotPresent();
    }

}
