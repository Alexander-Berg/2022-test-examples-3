package ru.yandex.market.sc.internal.controller.manual;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
public class ManualDropOffControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestFactory testFactory;

    SortingCenter oldSortingCenter;
    SortingCenter newSortingCenter;

    @BeforeEach
    void init() {
        oldSortingCenter = testFactory.storedSortingCenter(1);
        newSortingCenter = testFactory.storedSortingCenter(2);

        testFactory.setSortingCenterProperty(oldSortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(newSortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, "true");
    }

    @SneakyThrows
    @Test
    void moveScOkTest() {
        mockMvc.perform(post("/manual/dropOff/moveSc")
                        .param("oldPartnerId", oldSortingCenter.getPartnerId())
                        .param("newPartnerId", newSortingCenter.getPartnerId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @SneakyThrows
    @Test
    void moveScIsNotDropOffTest() {
        testFactory.setSortingCenterProperty(newSortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, "false");

        mockMvc.perform(post("/manual/dropOff/moveSc")
                        .param("oldPartnerId", oldSortingCenter.getPartnerId())
                        .param("newPartnerId", newSortingCenter.getPartnerId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }
}
