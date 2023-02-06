package ru.yandex.market.transferact.entity.actor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.utils.TestOperationHelper

class ActorQueryServiceTest : AbstractTest() {
    @Autowired
    lateinit var actorQueryService: ActorQueryService

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `When getActorByTypeAndExternalId then return actor`() {
        val actor = testOperationHelper.createActor()
        val actorType = actor.actorId.actorType

        val actorProjection = Actor(
            actor.id,
            actor.name,
            actor.companyName,
            actor.actorId.externalId,
            ActorType(actorType.id, actorType.name, actorType.apiKey, actorType.signatureRequestCallbackUrl, actorType.tvmClientId)
        )

        val found = actorQueryService.getActorByTypeAndExternalId(actorType.id, actor.actorId.externalId)

        assertThat(found).isNotNull
        assertThat(found).isEqualTo(actorProjection)
    }

    @Test
    fun `When getActorByTypeAndExternalId throw exception`() {
        val actorType = ActorTypeEntity(name = "test", apiKey = "test")
        actorTypeRepository.save(actorType)
        assertThat(actorQueryService.getActorByTypeAndExternalId(actorType.id, "123")).isNull()
    }
}
