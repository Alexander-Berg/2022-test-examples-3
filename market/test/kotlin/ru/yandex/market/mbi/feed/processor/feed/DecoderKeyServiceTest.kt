package ru.yandex.market.mbi.feed.processor.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.feed.processor.FunctionalTest

internal class DecoderKeyServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var decoderKeyService: DecoderKeyService

    companion object {

        @JvmStatic
        fun arg() = listOf(
            Arguments.of("12432358300"),
            Arguments.of(""),
            Arguments.of("dsaf43tj3iofnh"),
            Arguments.of("dfsmfinu3479y832dr78y34hmf87ouw4pcy3789tywc0m489y9cw78n4cwy8sio8mu309qx8ig67827oqxy3rm83wn")
        )
    }

    @ParameterizedTest
    @MethodSource("arg")
    fun test(key: String) {
        val keyE = decoderKeyService.encode(key)
        assertThat(key).isNotEqualTo(keyE)
        val keyD = decoderKeyService.decode(keyE)
        assertThat(key).isEqualTo(keyD)
    }
}
