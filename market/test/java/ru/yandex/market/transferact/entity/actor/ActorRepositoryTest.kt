package ru.yandex.market.transferact.entity.actor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest

const val actorExternalId = "actorExternalId"

class ActorRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Test
    fun `When findByActorId then return actor`() {
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val actorId = ActorId(actorExternalId)
        actorId.actorType = actorType
        val actor = ActorEntity()
        actor.actorId = actorId
        actorRepository.save(actor)

        val found = actorRepository.findByActorId(actorId)

        assertThat(found).isNotNull
        assertThat(found?.actorId?.actorType).isEqualTo(actorType)
        assertThat(found?.actorId?.externalId).isEqualTo(actorExternalId)
    }

}
