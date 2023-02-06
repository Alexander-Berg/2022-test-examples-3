package ru.yandex.market.transferact.entity.actor

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.common.util.exception.TransferActEntityNotFoundException

class ActorTypeQueryServiceTest : AbstractTest() {

    @Autowired
    lateinit var actorTypeQueryService: ActorTypeQueryService

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository


    @Test
    fun `When getActorTypeByApiKey then return actorTypeProjection`() {
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)

        val actorTypeProjection = actorTypeQueryService.getActorTypeByApiKey(apiKey)

        assertThat(actorTypeProjection).isNotNull
        assertThat(actorTypeProjection.name).isEqualTo(name)
        assertThat(actorTypeProjection.apiKey).isEqualTo(apiKey)
    }

    @Test
    fun `When getActorTypeByApiKey throw exception`() {
        assertThatThrownBy {
            actorTypeQueryService.getActorTypeByApiKey(apiKey)
        }.isInstanceOf(TransferActEntityNotFoundException::class.java)
    }

}
