package ru.yandex.market.tpl.api.controller.api.task;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.PhotoRequirementType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoutePointControllerIntTest extends BaseApiIntTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;

    private User user;
    private UserShift userShift;
    private long routePointId;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now());
        CollectDropshipTask collectDropshipTask = testDataFactory.addDropshipTask(userShift.getId());
        routePointId = collectDropshipTask.getRoutePoint().getId();
    }

    @SneakyThrows

    @Test
    void shouldReturnTakePhotoDto() {
        mockMvc.perform(get("/api/route-points/{id}", routePointId)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.tasks[0].takePhoto.photoRequired").value(PhotoRequirementType.OPTIONAL.name())
                ));
    }
}
