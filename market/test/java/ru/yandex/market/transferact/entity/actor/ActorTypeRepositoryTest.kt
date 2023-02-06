package ru.yandex.market.transferact.entity.actor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest

const val apiKey = "apiKey"
const val name = "name"

class ActorTypeRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Test
    fun `When findByApiKey then return ActorType`() {
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val found = actorTypeRepository.findByApiKey(apiKey)
        assertThat(found).isNotNull
        assertThat(found?.name).isEqualTo(name)
        assertThat(found?.apiKey).isEqualTo(apiKey)
    }

}
