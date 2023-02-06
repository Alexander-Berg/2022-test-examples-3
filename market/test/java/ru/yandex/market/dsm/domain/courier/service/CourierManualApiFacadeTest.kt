package ru.yandex.market.dsm.domain.courier.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.config.props.SelfemployedTrackerProperties
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.mj.generated.server.model.CourierTicketActionDto
import ru.yandex.mj.generated.server.model.CourierTicketActionTypeDto
import ru.yandex.startrek.client.Transitions
import ru.yandex.startrek.client.model.IssueUpdate

class CourierManualApiFacadeTest : AbstractTest() {
    @Autowired
    private lateinit var courierManualApiFacade: CourierManualApiFacade

    @Autowired
    private lateinit var transitions: Transitions

    @Autowired
    private lateinit var selfemployedTrackerProperties: SelfemployedTrackerProperties

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Test
    fun doTicketAction() {
        val courier = courierTestFactory.generateCourier()
        val dto = CourierTicketActionDto()
        dto.id = courier.id
        dto.actionType = CourierTicketActionTypeDto.CREATED

        courierManualApiFacade.doTicketAction(dto)
        Mockito.verify(transitions, Mockito.times(1)).execute(
            courier.createTicket,
            selfemployedTrackerProperties.statusKeyCreated
        )
        Mockito.reset(transitions)

        dto.actionType = CourierTicketActionTypeDto.READY_TO_GO
        courierManualApiFacade.doTicketAction(dto)
        Mockito.verify(transitions, Mockito.times(1)).execute(
            courier.createTicket,
            selfemployedTrackerProperties.statusKeyReadyToGo
        )
        Mockito.reset(transitions)

        dto.actionType = CourierTicketActionTypeDto.CLOSE
        courierManualApiFacade.doTicketAction(dto)
        Mockito.verify(transitions, Mockito.times(1)).execute(
            Mockito.eq(courier.createTicket),
            Mockito.eq(selfemployedTrackerProperties.statusKeyClose),
            Mockito.any<IssueUpdate>()
        )
    }
}
