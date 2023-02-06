package ru.yandex.direct.core.entity.landing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.landing.model.BizContactType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BizContactValueFormatterTest {

    fun phoneInputs() = listOf(
            "8 913 700 90 90" to "89137009090",
            "+7 913 700 90 90" to "79137009090",
            "+7 (913) 700 90 90" to "79137009090",
            "7-(913)-700-90-90" to "79137009090",
            "some.ru" to null,
            "wa.me/9991112233" to null,
            "t.me/jhgj" to null,
            "798876" to null,
            "79991112233me" to null,
    )

    fun telegramInputs() = listOf(
            "tiger" to "https://t.me/tiger",
            "t.me/tiger" to "https://t.me/tiger",
            "t.me/tiger123" to "https://t.me/tiger123",
            "http://telegram.me/tiger123" to "https://telegram.me/tiger123",
            "https://t.me/joinchat/RmilNPaI10tDdmOr" to "https://t.me/joinchat/RmilNPaI10tDdmOr",
            "http://t.me/+RmilNPaI10tDdmOr" to "https://t.me/+RmilNPaI10tDdmOr",
            "telegram.me/joinchat/RmilNPaI10tDdmOr" to "https://telegram.me/joinchat/RmilNPaI10tDdmOr",
            null to null,
            "" to null,
            "some.ru" to null,
            "http://some.ru" to null,
            "https://t.me" to null,
            "https://t.me/qwerty/sji" to null,
            "wa.me/79991112233" to null,
            "viber.click/79991112233" to null,
            "vk.com/public1" to null,
            "twitter.com/mytw" to null,
    )

    fun vkInputs() = listOf(
            "vk.com/public1" to "https://vk.com/public1",
            "http://www.vk.com/public1" to "https://vk.com/public1",
            "https://vk.me/23423fsds" to "https://vk.me/23423fsds",
            "public1" to "https://vk.com/public1",
            "vk.cc/r34r3d0" to "https://vk.cc/r34r3d0",
            null to null,
            "" to null,
            "vk.com" to null,
            "viber.click/tiger" to null,
            "wa.me/79991112233" to null,
            "t.me/tiger" to null,
            "twitter.com/mytw" to null,
    )

    fun whatsappInputs() = listOf(
            "79991112233" to "https://wa.me/79991112233",
            "7 (999) 111-22-33" to "https://wa.me/79991112233",
            "8-999-111-22-33" to "https://wa.me/79991112233",
            "79991112233" to "https://wa.me/79991112233",
            "http://www.wa.me/79991112233" to "https://wa.me/79991112233",
            "wa.me/79991112233" to "https://wa.me/79991112233",
            "http://api.whatsapp.com/send?phone=79991112233" to "https://api.whatsapp.com/send?phone=79991112233",
            "https://chat.whatsapp.com/invite/FoI7CCZwyS0HJmITGfZ4Qm" to "https://chat.whatsapp.com/invite/FoI7CCZwyS0HJmITGfZ4Qm",
            "chat.whatsapp.com/FoI7CCZwyS0HJmITGfZ4Qm" to "https://chat.whatsapp.com/FoI7CCZwyS0HJmITGfZ4Qm",
            null to null,
            "" to null,
            "some.ru" to null,
            "https://api.whatsapp.com/79624252299" to null,
            "https://chat.whatsapp.com/send?phone=79991112233" to null,
            "https://chat.whatsapp.com/some/FoI7CCZwyS0HJmITGfZ4Qm" to null,
            "https://wa.me/tiger" to null,
            "t.me/qwerty" to null,
            "tiger" to null,
            "wa.me/tiger" to null,
            "https://wa.me" to null,
            "api.whatsapp.com/79624252299" to null,
            "chat.whatsapp.com/send?phone=79991112233" to null,
            "chat.whatsapp.com/some/FoI7CCZwyS0HJmITGfZ4Qm" to null,
            "viber.click/79991112233" to null,
            "vk.com/public1" to null,
            "twitter.com/mytw" to null,
    )

    fun viberInputs() = listOf(
            "79991112233" to "https://viber.click/79991112233",
            "viber.click/79991112233" to "https://viber.click/79991112233",
            null to null,
            "" to null,
            "t.me/qwerty" to null,
            "https://viber.click/tiger" to null,
            "some.ru" to null,
            "tiger" to null,
            "79933" to null,
            "viber.click" to null,
            "viber.click/tiger" to null,
            "viber.click/7999" to null,
            "wa.me/79991112233" to null,
            "t.me/tiger" to null,
            "vk.com/public1" to null,
            "twitter.com/mytw" to null,
    )

    @ParameterizedTest
    @MethodSource("phoneInputs")
    fun test_phoneInputs(input: Pair<String, String>) {
        assertThat(BizContactValueFormatter.formatPhone(input.first)).isEqualTo(input.second)
    }

    @ParameterizedTest
    @MethodSource("telegramInputs")
    fun test_telegramInputs(input: Pair<String, String>) {
        assertThat(BizContactValueFormatter.formatMessenger(input.first, BizContactType.TELEGRAM)).isEqualTo(input.second)
    }

    @ParameterizedTest
    @MethodSource("vkInputs")
    fun test_vkInputs(input: Pair<String, String>) {
        assertThat(BizContactValueFormatter.formatMessenger(input.first, BizContactType.VKONTAKTE)).isEqualTo(input.second)
    }

    @ParameterizedTest
    @MethodSource("whatsappInputs")
    fun test_whatsappInputs(input: Pair<String, String>) {
        assertThat(BizContactValueFormatter.formatMessenger(input.first, BizContactType.WHATSAPP)).isEqualTo(input.second)
    }

    @ParameterizedTest
    @MethodSource("viberInputs")
    fun test_viberInputs(input: Pair<String, String>) {
        assertThat(BizContactValueFormatter.formatMessenger(input.first, BizContactType.VIBER)).isEqualTo(input.second)
    }

}
