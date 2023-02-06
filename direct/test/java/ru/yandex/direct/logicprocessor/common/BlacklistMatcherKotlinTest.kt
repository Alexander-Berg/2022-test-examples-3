package ru.yandex.direct.logicprocessor.common

import com.fasterxml.jackson.annotation.JsonProperty
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.essblacklist.model.EssBlacklistItem
import ru.yandex.direct.ess.common.models.BaseLogicObject

data class BlacklistTestKotlinDataClass(
    @JsonProperty("str_alias")
    val str: String,
) : BaseLogicObject()

class BlacklistMatcherKotlinTest {

    @Test
    fun kotlinDataClassIsHandledCorrectly() {
        val matcher = BlacklistMatcher.create(
            listOf(EssBlacklistItem().withFilterSpec("{\"str_alias\": \"should match\"}")),
            BlacklistTestKotlinDataClass::class.java
        )
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(matcher.matches(BlacklistTestKotlinDataClass("should match"))).isTrue
            softly.assertThat(matcher.matches(BlacklistTestKotlinDataClass("should not match"))).isFalse
        }
    }
}
