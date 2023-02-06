package ru.yandex.market.sc.internal.controller.partner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerSortingCenterControllerTest {
    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    void getSortingCenterQr() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/sorting-center/qr",
                        sortingCenter.getPartnerId()
                )
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getSortingCenter2FaAuthUserQr() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/sorting-center/password",
                        sortingCenter.getPartnerId()
                )
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getSortingCenterAuthUserQr() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.REQUIRE_VALIDATE_SC_ID, true);
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/sorting-center/password",
                        sortingCenter.getPartnerId()
                )
        ).andExpect(status().isOk())
        .andExpect(content().string("{\"password\":\"" + sortingCenter.getId() + "\"}"));
    }
}
