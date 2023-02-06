package ru.yandex.direct.core.entity.uac.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.uac.converter.GroupRetargetingConditionConverter.toGroupRetargetingCondition

class GroupRetargetingConditionConverterTest{
    @Test
    fun setRetCondIdTest() {
        val res = toGroupRetargetingCondition(1L,
            RetargetingCondition().withType(ConditionType.interests).withName("name") as RetargetingCondition,
            mapOf(1L to listOf(RetargetingCondition().withId(7L) as RetargetingCondition)),
            emptyMap(),
        )
        assertThat(res?.id).isEqualTo(7L)
        assertThat(res?.name).isEqualTo("name")
        assertThat(res?.type).isEqualTo(ConditionType.interests)
    }

    @Test
    fun setRetCondMultipleGroupTest() {
        val retargetingCondition = RetargetingCondition()
            .withType(ConditionType.interests)
            .withName("name") as RetargetingCondition
        val existedRetargetingConditions = mapOf(
            1L to listOf(RetargetingCondition().withId(7L) as RetargetingCondition),
            2L to listOf(RetargetingCondition().withId(9L) as RetargetingCondition),
        )

        val res1 = toGroupRetargetingCondition(1L, retargetingCondition, existedRetargetingConditions, emptyMap())
        val res2 = toGroupRetargetingCondition(2L, retargetingCondition, existedRetargetingConditions, emptyMap())
        assertThat(res1?.id).isEqualTo(7L)
        assertThat(res2?.id).isEqualTo(9L)
    }
}
