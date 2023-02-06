package ru.yandex.market.delivery.transport_manager.controller.lgw;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.les.trip.TripToYardLesProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorTicketDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorType;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackApiType;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StEntityErrorTicketService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/repository/movement/movement_test.xml")
class LgwMovementCallbackControllerTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovementMapper movementMapper;

    @Autowired
    private StEntityErrorTicketService stEntityErrorTicketService;

    @Autowired
    protected RegisterTrackProducer registerTrackProducer;

    @Autowired
    private TripToYardLesProducer tripToYardLesProducer;

    @Test
    void movementCreateSuccess() throws Exception {
        mockMvc.perform(
            put("/lgw/movement/TMM1/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/movement/movement_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        Movement movement = movementMapper.getById(1L);
        softly.assertThat(movement.getStatus()).isEqualTo(MovementStatus.LGW_CREATED);
        softly.assertThat(movement.getExternalId()).isEqualTo("666");
        softly.assertThat(movement.getIsTrackable()).isEqualTo(true);

        Mockito.verify(registerTrackProducer).produce(
            1L,
            ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType.MOVEMENT,
            "666",
            156L,
            RegisterTrackApiType.DS
        );
    }

    @Test
    void notTrackableMovementCreateSuccess() throws Exception {
        mockMvc.perform(
            put("/lgw/movement/TMM2/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/movement/movement_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        Movement movement = movementMapper.getById(2L);
        softly.assertThat(movement.getStatus()).isEqualTo(MovementStatus.LGW_CREATED);
        softly.assertThat(movement.getExternalId()).isEqualTo("666");
        softly.assertThat(movement.getIsTrackable()).isEqualTo(false);

        Mockito.verify(registerTrackProducer, Mockito.never()).produce(
            2L,
            ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType.MOVEMENT,
            "666",
            157L,
            RegisterTrackApiType.DS
        );
    }

    @Test
    void movementCreateError() throws Exception {
        mockMvc.perform(put("/lgw/movement/TMM1/error")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/lgw/movement/movement_error.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        Movement movement = movementMapper.getById(1L);
        softly.assertThat(movement.getStatus()).isEqualTo(MovementStatus.ERROR);
        verify(stEntityErrorTicketService).createErrorTicket(
            eq(EntityType.MOVEMENT),
            eq(1L),
            eq(new StartrekErrorTicketDto()
                .setErrorType(StartrekErrorType.MOVEMENT_ERROR)
                .setMessage("Ошибка создания / изменения перевозки: \n" +
                    "id в TM = 1; id партнера = 119, id перевозки у партнера = 666.\n" +
                    "Ссылка на LGW: localhost:3000/lgw/client-tasks?page=0&size=10&entityId=TMM1"))
        );
    }

    @DatabaseSetup(value = {
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml"
    }, type = DatabaseOperation.CLEAN_INSERT)
    @Test
    void assertTipToYardLesProducerInvoked() throws Exception {
        mockMvc.perform(
            put("/lgw/movement/TMM1/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/movement/movement_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(tripToYardLesProducer).enqueue(1L);
    }
}
