package ru.yandex.direct.core.entity.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SocialNetworkAccountDomainUtilsTest {

    fun instagramAccountUrls() = listOf(
            Arguments.of("https://instagram.com/mylogin", "mylogin"),
            Arguments.of("http://instagram.com/mylogin333", "mylogin333"),
            Arguments.of("https://instagram.com/mylogin/", "mylogin"),
            Arguments.of("https://instagram.com/mylogin/?a=b&c=d", "mylogin"),
            Arguments.of("https://instagram.com/mylogin?a=f/", "mylogin"),
            Arguments.of("https://www.instagram.com/mylogin", "mylogin"),
            Arguments.of("https://instagram.com/mylogin.2", "mylogin.2"),
            Arguments.of("https://m.instagram.com/mylogin3", "mylogin3"),
            Arguments.of("https://instagr.am/mylogin_4", "mylogin_4"),
            Arguments.of("https://instagram.com/mylogin".uppercase(), "mylogin"),
    )

    fun nonInstagramAccountUrls() = listOf(
            null,
            "",
            " ",
            "https://instagram.com/",
            "https://ya.ru",
            "https://instagram.ru/mylogin",
            "https://instagr.amm/mylogin2/",
    )

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("instagramAccountUrls")
    fun test_instagram_url(url: String, expectedUsername: String) {
        val username = SocialNetworkAccountDomainUtils.extractInstagramUsername(url)
        Assertions.assertThat(username).isEqualTo(expectedUsername)
    }

    @ParameterizedTest
    @MethodSource("nonInstagramAccountUrls")
    fun test_non_instagram_url(url: String?) {
        val username = SocialNetworkAccountDomainUtils.extractInstagramUsername(url)
        Assertions.assertThat(username).isNull()
    }
}
