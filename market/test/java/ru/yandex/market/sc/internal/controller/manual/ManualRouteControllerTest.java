package ru.yandex.market.sc.internal.controller.manual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * {@link ManualRouteController}
 */
@ScIntControllerTest
public class ManualRouteControllerTest {

    private static final long UID = 123L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedCell(sortingCenter);
    }

    @Test
    @Disabled // пока выпилил ручку - она мешалась сортаблам - нужно будет починить стрельбы, как будет время
    @DisplayName("Smoke test ручки, которая нужня для стрельб")
    void getRoutesByCourier() throws Exception {
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().sort().get();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/manual/routes/courier")
                        .param("scId", sortingCenter.getId().toString())
                        .param("routeType", RouteType.OUTGOING_COURIER.name())
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk());

    }


}
