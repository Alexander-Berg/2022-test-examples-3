package ru.yandex.market.transferact.entity.actor

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.common.util.exception.TransferActEntityNotFoundException
import ru.yandex.market.transferact.utils.TestOperationHelper

class ActorCommandServiceTest : AbstractTest() {

    @Autowired
    lateinit var actorCommandService: ActorCommandService

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    fun `When create actor and actorType not exist then throw exception`() {
        Assertions.assertThatThrownBy {
            actorCommandService.createActor(ActorCommand.Create(
                "externalId",
                ActorType(1, "MARKET_COURIER", "", "", null),
                "Vasiliy",
                "Vorobushek"
            ))
        }.isInstanceOf(TransferActEntityNotFoundException::class.java)
    }

    @Test
    fun `When create actor then return created actor`() {
        val actorTypeEntity = actorTypeRepository.save(ActorTypeEntity(name = name, apiKey = apiKey))

        val createdActor = actorCommandService.createActor(ActorCommand.Create(
            "externalId",
            ActorType(actorTypeEntity.id, actorTypeEntity.name, "", "", null),
            "Vasiliy",
            "Vorobushek"
        ))

        val actor = actorRepository.getByIdOrThrow(createdActor.id)
        assertThat(actor.actorId.externalId).isEqualTo("externalId")
        assertThat(actor.actorId.actorType).isEqualTo(actorTypeEntity)
    }

    @Test
    fun `When create actor with same actorId then return created actor`() {
        val actorTypeEntity = actorTypeRepository.save(ActorTypeEntity(name = name, apiKey = apiKey))

        val createdActor1 = actorCommandService.createActor(ActorCommand.Create(
            "externalId",
            ActorType(actorTypeEntity.id, actorTypeEntity.name, "", "", null),
            "Vasiliy",
            "Vorobushek"
        ))
        val createdActor2 = actorCommandService.createActor(ActorCommand.Create(
            "externalId",
            ActorType(actorTypeEntity.id, actorTypeEntity.name, "", "", null),
            "Vasilisa",
            "Teleport-G"
        ))

        assertThat(createdActor1).isEqualTo(createdActor2)
    }

}
