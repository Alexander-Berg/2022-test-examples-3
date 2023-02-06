package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.commonViewType

class CommonParamViewSettingProtoConverterTest {

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val commonViewType = commonViewType()
        val commonParamViewSetting = commonParamViewSetting(commonViewTypeId = commonViewType.mdmId)

        // when
        val proto = commonParamViewSetting.toProtoWith(commonViewType)
        val pojo = proto.toPojo()

        // then
        pojo shouldBe commonParamViewSetting

        // and
        proto.commonViewType shouldBe commonViewType.toProto()
    }
}
