package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.dto.InterWarehouseReplenishmentExcelDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.InterWarehouseReplenishment;
import ru.yandex.market.replenishment.autoorder.utils.converters.InterWarehouseFromExcelDtoConverter;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
@ActiveProfiles("unittest")
public class InterWarehouseFromExcelDtoConverterTest extends FunctionalTest {
    @Autowired
    InterWarehouseFromExcelDtoConverter interWarehouseFromExcelDtoConverter;

    @Test
    @DbUnitDataSet(before = "InterWarehouseFromExcelDtoConverterTest.before.csv")
    public void testConvert() {
        String login = "login";
        Map<String, Long> expectedMskus = Map.of(
                "000001.100", 100L,
                "000001.200", 200L
        );
        List<InterWarehouseReplenishmentExcelDTO> dtos = expectedMskus.keySet().stream()
                .map(ssku -> InterWarehouseReplenishmentExcelDTO.builder()
                        .ssku(ssku)
                        .supplierId(1L)
                        .supplierType(1)
                        .orderDate(LocalDate.of(2021, 2, 17))
                        .warehouseFrom(1L)
                        .warehouseTo(2L)
                        .adjustedPurchQty(1L)
                        .build()
                )
                .collect(Collectors.toList());
        Collection<InterWarehouseReplenishment> interWarehouseReplenishments =
                interWarehouseFromExcelDtoConverter.convert(dtos, login);

        assertThat(interWarehouseReplenishments, hasSize(2));
        assertTrue(interWarehouseReplenishments.stream()
                .anyMatch(item -> item != null && expectedMskus.get(item.getSsku()) == item.getMsku()));
    }
}
