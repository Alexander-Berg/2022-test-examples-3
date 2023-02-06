package ru.yandex.direct.core.entity.uac

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.UacCommonUtils.getHrefWithTrackingParams

@RunWith(JUnitParamsRunner::class)
class UacHrefWithTrackingParamsTest {
    fun parameters() = listOf(
        listOf("invalid//href", "a=b&c=d", "invalid//href"),
        listOf("no-protocol.site", "a=b&c=d", "no-protocol.site"), // href без протокола не считается валидным
        listOf("https://no-tracking-params.site", null, "https://no-tracking-params.site"),
        listOf("https://simple.site", "a=b", "https://simple.site?a=b"),
        listOf("https://simple.site/#page_anchor", "a=b", "https://simple.site/?a=b#page_anchor"),
        listOf("https://empty-path.site/", "a=b", "https://empty-path.site/?a=b"),
        listOf("https://long-path.site/a/b/c", "a=b", "https://long-path.site/a/b/c?a=b"),
        listOf("https://merge-params.site/path?a=b", "c=d", "https://merge-params.site/path?c=d&a=b"),
        listOf("https://merge-and-replace-params.site/path?a=b&c=d&e", "a=f&e&c=d", "https://merge-and-replace-params.site/path?a=f&e&c=d"),
        listOf("https://no-value-param.site/", "param1=val&ue1", "https://no-value-param.site/?param1=val&ue1"),
        listOf("https://encoded-value-param.site", "param1=val%26ue1", "https://encoded-value-param.site?param1=val%26ue1"),
        listOf("https://merge-withancor-params.site/path?a=b#ancor", "c=d", "https://merge-withancor-params.site/path?c=d&a=b#ancor")
    )

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("check merging tracking params '{1}' to href '{0}'")
    fun testGetHrefWithTrackingParams(href: String, trackingParams: String?, hrefWithTrackingParams: String) {
        assertThat(getHrefWithTrackingParams(href, trackingParams)).isEqualTo(hrefWithTrackingParams);
    }
}
