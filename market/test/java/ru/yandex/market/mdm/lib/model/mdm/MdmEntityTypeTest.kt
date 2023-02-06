package ru.yandex.market.mdm.lib.model.mdm

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmEntityType

class MdmEntityTypeTest {

    @Test
    fun `should create the same entity after toBuilder transformation`() {
        // given
        val mdmEntityType = mdmEntityType()

        // when
        val builder = mdmEntityType.toBuilder()
        val rebuiltMdmEntityType = builder.build()

        // then
        rebuiltMdmEntityType shouldBe mdmEntityType
    }
}
