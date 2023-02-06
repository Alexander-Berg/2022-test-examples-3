package ru.yandex.market.replenishment.autoorder.model.mapper;

import java.time.LocalDate;

import org.junit.Test;

import ru.yandex.market.replenishment.autoorder.api.dto.DepartmentQuotaDTO;
import ru.yandex.market.replenishment.autoorder.dto.QuotaDto;

import static org.junit.Assert.assertEquals;

public class DepartmentQuotaDTOMapperTest {

    private final DepartmentQuotaDTOMapper mapper = new DepartmentQuotaDTOMapper();

    @Test
    public void mapTest() {
        DepartmentQuotaDTO actual = mapper.map(buildQuotaDto());
        DepartmentQuotaDTO expected = buildDepartmentQuotaDTO();
        assertEquals(expected, actual);
    }

    private DepartmentQuotaDTO buildDepartmentQuotaDTO() {
        return new DepartmentQuotaDTO(1L,
            2L,
            "nameW",
            3L,
            "nameD",
            1000L,
            null,
            LocalDate.parse("2020-03-03"));
    }

    private QuotaDto buildQuotaDto() {
        QuotaDto quotaDto = new QuotaDto();
        quotaDto.setId(1L);
        quotaDto.setWarehouse("nameW");
        quotaDto.setWarehouseId(2L);
        quotaDto.setDepartment("nameD");
        quotaDto.setDepartmentId(3L);
        quotaDto.setQuantity(1000L);
        quotaDto.setDate(LocalDate.parse("2020-03-03"));
        return quotaDto;
    }

}
