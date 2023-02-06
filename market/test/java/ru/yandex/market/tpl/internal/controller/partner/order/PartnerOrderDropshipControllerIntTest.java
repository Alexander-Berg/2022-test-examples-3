package ru.yandex.market.tpl.internal.controller.partner.order;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerOrderDropshipControllerIntTest extends BaseTplIntWebTest {

    private final MovementGenerator movementGenerator;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    @BeforeEach
    void setUp() {
        LocalDate now = LocalDate.now(clock);

        var user = testUserHelper.findOrCreateUser(UID);

        var company = testUserHelper.findOrCreateSuperCompany();

        var userShift = testUserHelper.createEmptyShift(user, now);

        var movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .pallets(123)
                .build()
        );
        var collectDropshipTask = testDataFactory.addDropshipTask(userShift.getId(), movement);

    }

    @SneakyThrows
    @Test
    void shouldReturnReassignActionForMovement() {
        mockMvc.perform(get("/internal/partner/orders")
                        .header(COMPANY_HEADER, -1L))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].actions[*].type").value(Matchers.hasItem("REASSIGN")))
        ;
    }

}
