package ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmPath

class FlatAttributeTest {

    @Test
    fun `should return complex flat attribute title and internalName`() {
        // given
        val outer = mdmAttribute(internalName = "outer", ruTitle = "Внешний")
        val middle = mdmAttribute(internalName = "middle", ruTitle = "Средний")
        val inner = mdmAttribute(internalName = "inner", ruTitle = "Внутренний")
        val flatAttribute = FlatRichAttribute(listOf(outer, middle, inner), mapOf())

        // then
        flatAttribute.internalName() shouldBe "outer||middle||inner"
        flatAttribute.ruTitle() shouldBe "Внешний: Средний: Внутренний"
    }

    @Test
    fun `should return simple flat attribute title and internalName`() {
        // given
        val simple = mdmAttribute(internalName = "simple", ruTitle = "Внешний")
        val flatAttribute = FlatRichAttribute(listOf(simple), mapOf())

        // then
        flatAttribute.internalName() shouldBe "simple"
        flatAttribute.ruTitle() shouldBe "Внешний"
    }

    @Test
    fun `should return correct settings for flat attributes`() {
        // given
        val outer = mdmAttribute(internalName = "outer", ruTitle = "Внешний")
        val inner = mdmAttribute(internalName = "inner", ruTitle = "Внутренний")
        val correctPathForInner = MdmPath.fromAttributes(listOf(outer, inner))
        val wrongPathForInner = MdmPath.fromAttributes(listOf(outer))
        val settingsOnCorrectPath = commonParamViewSetting()
        val settingsOnWrongPath = commonParamViewSetting()

        val flatRichAttribute = FlatRichAttribute(
            listOf(outer, inner), mapOf(
                correctPathForInner to listOf(settingsOnCorrectPath),
                wrongPathForInner to listOf(settingsOnWrongPath),
            )
        )

        // then
        flatRichAttribute.getSettings() shouldContainExactly listOf(settingsOnCorrectPath)
    }
}
