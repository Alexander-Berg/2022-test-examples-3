package ru.yandex.market.logistics.yard.facade.uuid

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.uuid_version.CapacityUUIDDecorator
import java.util.*

class CapacityUUIDDecoratorTest(@Autowired val capacityUUIDDecorator: CapacityUUIDDecorator) :
    AbstractSecurityMockedContextualTest() {


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/uuid/capacity/before.xml"])
    fun getByUUIDsTest() {
        val uuids = listOf(UUID.fromString("b0ae452c-45fd-4a3c-853c-d93a5b340ecf"))
        val byUUIDs = capacityUUIDDecorator.getByUUIDs(uuids)
        assertions().assertThat(byUUIDs).isNotNull
    }

}
