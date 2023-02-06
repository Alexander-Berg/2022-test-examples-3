package ru.yandex.market.mdm.service.common_entity.model

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum

class CommonEntityTest {

    @Test
    fun `should add new param to existed CommonEntity`() {
        // given
        val existedEntity = CommonEntity(
            123L,
            CommonEntityType(
                CommonEntityTypeEnum.MDM_ENTITY_TYPE,
                commonParams = listOf(
                    CommonParam(
                        commonParamName = "some_attr",
                        commonParamValueType = CommonParamValueType.NUMERIC
                    )
                )
            ),
            listOf(
                CommonParamValue.byLong(
                    commonParamName = "some_attr",
                    value = 12
                )
            )
        )

        // when
        val newParam = CommonParam(
            commonParamName = "new_attr",
            commonParamValueType = CommonParamValueType.BOOLEAN
        )
        val newValue = CommonParamValue.byBoolean(
            commonParamName = "new_attr",
            value = true
        )
        val enrichedEntity = existedEntity.addCommonParamAndValue(newParam, newValue)

        // then
        enrichedEntity.commonEntityType.commonParams!!.size shouldBe 2
        enrichedEntity.commonEntityType.commonParams!! shouldContain newParam

        enrichedEntity.commonParamValues!!.size shouldBe 2
        enrichedEntity.commonParamValues!! shouldContain newValue
    }
}
