package ru.yandex.market.tpl.billing.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.HealthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class HealthControllerTest extends AbstractFunctionalTest {

    @Autowired
    HealthService healthService;

    @BeforeEach
    private void setupDate() {
        doReturn(healthService.checkPickupPointsImportedForAllOrders(LocalDate.of(2021, 10, 20)))
                .when(healthService).checkPickupPointsImportedForAllOrders(any(LocalDate.class));
        doReturn(healthService.checkAllShiftsHaveTariff(LocalDate.of(2021, 10, 20)))
                .when(healthService).checkAllShiftsHaveTariff(any(LocalDate.class));
    }

    @Test
    @DbUnitDataSet(before = "/controller/health/before/pickup_points_not_imported_for_all_orders.csv")
    void checkFailPickupPointsImportedForAllOrders() throws Exception {
        mockMvc.perform(get("/health/check-pickup-points-imported-for-all-orders"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Some orders have no imported pickup points. Following pickup points " +
                        "must be imported: [3, 5]"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/health/before/pickup_points_imported_for_all_orders.csv")
    void checkSuccessPickupPointsImportedForAllOrders() throws Exception {
        mockMvc.perform(get("/health/check-pickup-points-imported-for-all-orders"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/health/before/not_all_shifts_have_tariff.csv")
    void checkFailAllShiftsHaveTariff() throws Exception {
        mockMvc.perform(get("/health/check-all-shifts-have-tariff"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Following shifts don't have tariff: \n" +
                        "Date: 2021-11-10, sorting center: 12, count: 1, shifts: 1234"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/health/before/all_shifts_have_tariff.csv")
    void checkSuccessAllShiftsHaveTariff() throws Exception {
        mockMvc.perform(get("/health/check-all-shifts-have-tariff"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }
}
