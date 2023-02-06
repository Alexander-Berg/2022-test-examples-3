package ru.yandex.direct.core.entity.banner.type.href

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import ru.yandex.direct.common.db.PpcPropertiesSupport

internal class BannersUrlHelperTest {
    @Test
    internal fun extractHostFromHrefWithoutWwwOrNull_success_forTurboSiteHref() {
        val ppcPropertiesSupport = mock(PpcPropertiesSupport::class.java)
        val bannersUrlHelper = BannersUrlHelper(ppcPropertiesSupport)
        val actual = bannersUrlHelper.extractHostFromHrefWithoutWwwOrNull("v10-beauty-studio.turbo.site/brovi")
        assertThat(actual).isEqualTo("v10-beauty-studio.turbo.site")
    }
}
