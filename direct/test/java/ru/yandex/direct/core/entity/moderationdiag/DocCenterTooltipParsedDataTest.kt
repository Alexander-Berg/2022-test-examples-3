package ru.yandex.direct.core.entity.moderationdiag

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.direct.utils.fromJson

class DocCenterTooltipParsedDataTest {
    @Test
    fun useSelectorFromJsonIfExist() {
        val result = DocCenterTooltipParsedData(fromJson("{\"content\": \"<h1>123</h1><span>456</span>\", \"selector\": {\".title\": \"<h1>title</h1>\"}}"))
        assertThat(result.title(), equalTo("<h1>title</h1>"))
        assertThat(result.content(), equalTo("<h1>123</h1><span>456</span>"))
    }

    @Test
    fun findH1IfNotExist() {
        val result = DocCenterTooltipParsedData(fromJson("{\"content\": \"<h1 class=\\\"doc" +
            "-c-title doc-c-topictitle1 doc-c-headers doc-c-headers_mod_h1\\\" id=\\\"ariaid-title1\\\" " +
            "data-help-title=\\\"1\\\">Страница перехода не отображается</h1><span>456</span>\", \"selector\": {}}"))

        assertThat(result.title(), equalTo("<h1 class=\"doc" +
            "-c-title doc-c-topictitle1 doc-c-headers doc-c-headers_mod_h1\" id=\"ariaid-title1\" " +
            "data-help-title=\"1\">Страница перехода не отображается</h1>"))
        assertThat(result.content(), equalTo("<h1 class=\"doc" +
            "-c-title doc-c-topictitle1 doc-c-headers doc-c-headers_mod_h1\" id=\"ariaid-title1\" " +
            "data-help-title=\"1\">Страница перехода не отображается</h1><span>456</span>"))
    }

    @Test
    fun emptyIfh1NotExistAndSelectorNotExist() {
        val result = DocCenterTooltipParsedData(fromJson("{\"content\": \"<span>456</span>\"}"))
        assertThat(result.title(), equalTo(""))
        assertThat(result.content(), equalTo("<span>456</span>"))
    }

    @Test
    fun correctJsonFix() {
        val result = DocCenterTooltipParsedData(fromJson("{\"content\": \"<h1>title</h1><span>456</span>\", \"selector\": {\".title\": \"\"}}"))
        assertThat(result.title(), equalTo("<h1>title</h1>"))
        assertThat(result.content(), equalTo("<h1>title</h1><span>456</span>"))
        assertThat(result.json(), equalTo("{\"content\":\"<h1>title</h1><span>456</span>\",\"selector\":{\".title\":\"<h1>title</h1>\"}}"))
    }
}
