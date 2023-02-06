package ru.yandex.market.tpl.internal.controller.partner;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;
import ru.yandex.market.tpl.internal.controller.TplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@TplIntWebTest
public class PartnerDropshipControllerIntTest extends BaseTplIntWebTest {
    private static final long UID = 123456L;
    private static final long UID2 = 2234562L;

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository userShiftRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    private UserShift userShift;

    private User user2;
    private UserShift userShift2;

    private CollectDropshipTask collectDropshipTask;

    @BeforeEach
    void setUp() {
        var localDate = LocalDate.now(clock);

        var user = testUserHelper.findOrCreateUser(UID);

        var company = testUserHelper.findOrCreateSuperCompany();

        userShift = testUserHelper.createEmptyShift(user, localDate);

        user2 = testUserHelper.findOrCreateUser(UID2);
        userShift2 = testUserHelper.createEmptyShift(user2, localDate);

        var movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .pallets(123)
                .build()
        );
        collectDropshipTask = testDataFactory.addDropshipTask(userShift.getId(), movement);
    }

    @Test
    @SneakyThrows
    void shouldReturnPallets() {
        var movement = movementRepository.findByIdOrThrow(collectDropshipTask.getMovementId());

        mockMvc.perform(get("/internal/partner/dropships/{externalId}", movement.getExternalId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, -1L)
                )
                .andExpect(jsonPath("$.delivery.pallets").exists())
                .andExpect(jsonPath("$.delivery.pallets").value(123));
    }

    @Test
    void shouldGetReassignAction() throws Exception {
        var movement = movementRepository.findByIdOrThrow(collectDropshipTask.getMovementId());

        mockMvc.perform(get("/internal/partner/dropships/{movementId}", movement.getExternalId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, -1L)
                )
                .andExpect(jsonPath("$.actions").exists())
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.actions[*].type").value(Matchers.hasItem("REASSIGN")))
        ;

    }

    @Test
    void shouldReassignCollectDropshipTaskToAnotherCourier() throws Exception {
        mockMvc.perform(post("/internal/partner/v2/orders/reassign")
                        .param("movementIds", String.valueOf(collectDropshipTask.getMovementId()))
                        .param("courierTo", String.valueOf(user2.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, -1L)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        userShift2 = userShiftRepository.findByIdOrThrow(userShift2.getId());

        var oldTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
        Assertions.assertThat(oldTask.getStatus())
                .isEqualTo(CollectDropshipTaskStatus.CANCELLED);

        var newTask = userShift2.streamCollectDropshipTasks().findFirst().orElseThrow();
        Assertions.assertThat(newTask.getStatus())
                .isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
    }
}
