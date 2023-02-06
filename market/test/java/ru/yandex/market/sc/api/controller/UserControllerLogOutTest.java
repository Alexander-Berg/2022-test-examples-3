package ru.yandex.market.sc.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mors741
 */
@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserControllerLogOutTest {
    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() throws JsonProcessingException {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter,
                SortingCenterPropertiesKey.LOG_OUT_IF_SAME_ACCOUNT_IN_USE,
                "true");
    }

    @Test
    void mainFlow() throws Exception {
        User user = testFactory.storedUser(sortingCenter, 100);

        performAndCheck("/api/users/check", user, "device_001", content().json("{}"));
        performAndCheck("/api/users/session", user, "device_001", content().json("{}"));

        performAndCheck("/api/users/check", user, "device_002", content().json("{}"));
        performAndCheck("/api/users/session", user, "device_002", content().json("{}"));

        performAndCheck("/api/users/session", user, "device_001", content().json("{\"invalidSession\":true}"));
    }

    @Test
    void newbie() throws Exception {
        User user = testFactory.storedUser(sortingCenter, 100, UserRole.NEWBIE_STOCKMAN);

        performAndCheck("/api/users/check", user, "device_001", content().json("{}"));
        performAndCheck("/api/users/session", user, "device_001", content().json("{}"));

        performAndCheck("/api/users/check", user, "device_002", content().json("{}"));
        performAndCheck("/api/users/session", user, "device_002", content().json("{}"));

        performAndCheck("/api/users/session", user, "device_001", content().json("{}"));
    }


    private void performAndCheck(
            String path,
            User user,
            String device_001,
            ResultMatcher contentMatcher
    ) throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get(path)
                                .header("Authorization", "OAuth uid-" + user.getUid())
                                .header("SC-Device-Id", device_001)
                )
                .andExpect(status().isOk())
                .andExpect(contentMatcher);
    }
}
