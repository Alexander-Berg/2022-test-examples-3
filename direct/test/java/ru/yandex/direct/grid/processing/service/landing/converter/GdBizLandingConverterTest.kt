package ru.yandex.direct.grid.processing.service.landing.converter

import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class GdBizLandingConverterTest {

    companion object {

        @JvmStatic
        fun logoParamsFromLandlord(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x450",
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x450",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/%s",
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/landing_logo",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias",
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias"
            ),
        )

        @JvmStatic
        fun logoParamsToLandlord(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/orig",
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x450",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/landing_logo",
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/%s"
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias",
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias"
            ),
        )

        @JvmStatic
        fun coverParamsFromLandlord(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x1200",
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x1200",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/%s",
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/landing_background",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias",
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias"
            ),
        )

        @JvmStatic
        fun coverParamsToLandlord(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/orig",
                "https://avatars.mdst.yandex.net/get-direct/4699/Gi5OpoLQdzkcCgV21ROh-g/x1200",
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/landing_background",
                "https://avatars.mdst.yandex.net/get-maps-adv-crm/1398861/2a00000181809e/%s"
            ),
            Arguments.of(
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias",
                "https://avatars.mdst.yandex.net/get-another-namespace/11/c54df665b/some_alias"
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("logoParamsFromLandlord")
    fun test_patchImageUrlFromLandlord_logo(url: String?, expectedUrl: String?) {
        assertThat(patchImageUrlFromLandlord(url, LandingImageType.LOGO)).isEqualTo(expectedUrl)
    }

    @ParameterizedTest
    @MethodSource("logoParamsToLandlord")
    fun test_patchImageUrlToLandlord_logo(url: String?, expectedUrl: String?) {
        assertThat(patchImageUrlToLandlord(url, LandingImageType.LOGO)).isEqualTo(expectedUrl)
    }

    @ParameterizedTest
    @MethodSource("coverParamsFromLandlord")
    fun test_patchImageUrlFromLandlord_cover(url: String?, expectedUrl: String?) {
        assertThat(patchImageUrlFromLandlord(url, LandingImageType.COVER)).isEqualTo(expectedUrl)
    }

    @ParameterizedTest
    @MethodSource("coverParamsToLandlord")
    fun test_patchImageUrlToLandlord_cover(url: String?, expectedUrl: String?) {
        assertThat(patchImageUrlToLandlord(url, LandingImageType.COVER)).isEqualTo(expectedUrl)
    }
}
