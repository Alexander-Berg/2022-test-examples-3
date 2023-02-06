package ru.yandex.market.mdm.lib.model.mdm

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmEntity
import ru.yandex.market.mdm.fixtures.randomId

class MdmEntityTest {

    @Test
    fun `should recreate entity from builder`() {
        // given
        val mdmAttributeValues = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(int64 = 155)))
        val mdmEntity = mdmEntity(values = listOf(mdmAttributeValues))

        // when
        val builder = mdmEntity.toBuilder()
        val recreatedEntity = builder.build()

        // then
        recreatedEntity shouldBe mdmEntity
    }
}
