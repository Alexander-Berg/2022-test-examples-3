package ru.yandex.market.sc.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author hardlight
 */
@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortingCenterControllerTest {
    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    SortingCenter sortingCenter;
    User supportUser;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        supportUser = testFactory.getOrCreateSupportStoredUser(sortingCenter);
    }

    @Test
    @SneakyThrows
    void getSortingCenterList() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/sortingCenters/list")
                        .header("Authorization", "OAuth uid-" + supportUser.getUid())
        )
                .andExpect(status().isOk())
                .andExpect(content().json("[" + jsonSortingCenter(sortingCenter) + "]"));
    }

    private String jsonSortingCenter(SortingCenter sortingCenter) {
        return "{\"id\":" + sortingCenter.getId() +
                ",\"name\":\"" + sortingCenter.getScName() + "\"" +
                "}";
    }
}
