package ru.yandex.market.logistics.yard.facade.uuid

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.uuid_version.ServiceUUIDDecorator
import java.util.*

class ServiceUUIDDecoratorTest(@Autowired val serviceUUIDDecorator: ServiceUUIDDecorator) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/uuid/service/before.xml"])
    fun getFullByIdsWithUrlsTest() {
        val uuids = listOf(UUID.fromString("9d98d902-688b-4c45-864c-ba3d44318f07"))
        val fullByIdsWithUrls = serviceUUIDDecorator.getFullByIdsWithUrls(uuids)
        assertions().assertThat(fullByIdsWithUrls).isNotNull

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/uuid/service/before.xml"])
    fun getByUUIDTest() {
        val service = serviceUUIDDecorator.getByUUID(UUID.fromString("9d98d902-688b-4c45-864c-ba3d44318f07"))
        assertions().assertThat(service).isNotNull
    }

}
