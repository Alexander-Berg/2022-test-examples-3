package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.AudioNotificationEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.AudioNotificationMapper

class AudioNotificationMapperTest(@Autowired val mapper: AudioNotificationMapper) :
    AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/audio/persist/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/audio/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        mapper.persist(
            AudioNotificationEntity(
                clientId = 1,
                file = "eyJlcnJvcl9jb2RlIjoiVU5BVVRIT1JJWkVEIiwiZXJyb3JfbWVzc2FnZSI6InJwYyBlcnJvc" +
                    "jogY29kZSA9IFVuYXV0aGVudGljYXRlZCBkZXNjID0gVGhlIHRva2VuIGlzIGludmFsaWQifQ=="
            )
        )
    }
}
