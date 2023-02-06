package ru.yandex.market.logistics.mqm.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class ClientReturnPlanFactUtilsKtTest {

    @DisplayName("Проверка фильтрации для подходящего закза")
    @Test
    fun successfulParseReturnId() {
        val returnId = parseReturnIdOrNull("VOZVRAT_SF_PS_123456")
        assertThat(returnId).isEqualTo(123456L);
    }

    @DisplayName("Проверка фильтрации для подходящего закза")
    @Test
    fun successfulParseReturnIdWithSpace() {
        val returnId = parseReturnIdOrNull("VOZVRAT_SF_PS_ 123456")
        assertThat(returnId).isEqualTo(123456L)
    }
}
