package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.location.TestLocationService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.LocationData;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class LocationControllerTest extends BasePlannerWebTest {

    private static final long DELIVERY_SERVICE_ID = 123L;

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final TestLocationService testLocationService;

    private Run run;

    @BeforeEach
    void setUp() {
        Run run = runGenerator.generate(b -> b.deliveryServiceId(DELIVERY_SERVICE_ID));
        User user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();

        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        testLocationService.saveLocation(user, LocationData.builder()
                .userShiftId(userShift.getId())
                .longitude(new BigDecimal("12.34"))
                .latitude(new BigDecimal("23.45"))
                .build());

        testUserHelper.openShift(user, userShift.getId());
    }

    @SneakyThrows
    @Test
    void shouldGetLocationsByDeliveryServiceId() {
        mockMvc.perform(get("/internal/coordinates")
                .param("deliveryServiceIds", String.valueOf(DELIVERY_SERVICE_ID))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)));

    }

    @SneakyThrows
    @Test
    void shouldGetLocationsNewByDeliveryServiceId() {
        mockMvc.perform(get("/internal/locations")
                .param("deliveryServiceIds", String.valueOf(DELIVERY_SERVICE_ID))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)));

    }


}
