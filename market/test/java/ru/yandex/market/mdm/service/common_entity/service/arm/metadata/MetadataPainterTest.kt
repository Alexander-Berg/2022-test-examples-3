package ru.yandex.market.mdm.service.common_entity.service.arm.metadata

import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.service.common_entity.model.CommonParamUiSetting
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.MetadataPainter.Companion.DISPLAYED_IN_ARM_TABLE_VIEW_TYPE_ID
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.MetadataPainter.Companion.TABLE_DISPLAYED_RULE

class MetadataPainterTest {
    @Test
    fun `should correctly paint attribute with test rule`() {
        // given
        val testAttribute = mdmAttribute()
        val testSetting = commonParamViewSetting(commonViewTypeId = DISPLAYED_IN_ARM_TABLE_VIEW_TYPE_ID)
        val painter = MetadataPainter(listOf(TABLE_DISPLAYED_RULE))

        // when
        val paintedParam = painter.paint(testAttribute, listOf(testSetting))

        // then
        paintedParam.commonParamName shouldBe testAttribute.internalName

        paintedParam.displayed shouldBe true
        paintedParam.uiOrder shouldBe testSetting.uiOrder
        paintedParam.isFilterSupported shouldBe false
        paintedParam.uiSettings!! shouldHaveSingleElement CommonParamUiSetting.VISIBLE
    }
}
