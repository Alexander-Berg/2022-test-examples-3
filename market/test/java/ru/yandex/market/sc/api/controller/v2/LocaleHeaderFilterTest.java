package ru.yandex.market.sc.api.controller.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScApiControllerTest
class LocaleHeaderFilterTest {

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private MockMvc mockMvc;

    private Cell cell;

    @BeforeEach
    void init() {
        var sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        cell = testFactory.storedCell(sortingCenter, "bc1", CellType.BUFFER);
        testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).externalId("o1").build()
        );
    }

    @Test
    public void defaultLocale() {
        var caller = ScApiControllerCaller.createCaller(mockMvc);
        caller.sort("o1", cell)
                .andExpect(jsonPath("$.message", is("Посылка не на СЦ")));
    }

    @Test
    public void defaultLocaleLocaleNotFound() {
        var caller = ScApiControllerCaller.createCaller(mockMvc,
                ScApiControllerCaller.authWith(TestFactory.USER_UID_LONG, "fr"));
        caller.sort("o1", cell)
                .andExpect(jsonPath("$.message", is("Посылка не на СЦ")));
    }

    @Test
    public void enLocale() {
        var caller = ScApiControllerCaller.createCaller(mockMvc,
                ScApiControllerCaller.authWith(TestFactory.USER_UID_LONG, "en"));
        caller.sort("o1", cell)
                .andExpect(jsonPath("$.message", is("The package is not on the SC")));
    }

    @Test
    public void esLocale() {
        var caller = ScApiControllerCaller.createCaller(mockMvc,
                ScApiControllerCaller.authWith(TestFactory.USER_UID_LONG,
                        "es-CL, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"));
        caller.sort("o1", cell)
                .andExpect(jsonPath("$.message", is("Paquete no en SC")));
    }

}
