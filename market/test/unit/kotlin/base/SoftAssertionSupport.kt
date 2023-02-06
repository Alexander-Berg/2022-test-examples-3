package ru.yandex.market.logistics.calendaring.base

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach

abstract class SoftAssertionSupport {
    protected val softly = SoftAssertions()

    @AfterEach
    fun triggerSoftAssertions() {
        softly.assertAll()
    }
}
