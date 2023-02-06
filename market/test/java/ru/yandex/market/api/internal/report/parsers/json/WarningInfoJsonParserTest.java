package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;

import ru.yandex.market.api.ParseUtil;
import ru.yandex.market.api.domain.v2.WarningInfo;
import ru.yandex.market.api.integration.UnitTestBase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WarningInfoJsonParserTest extends UnitTestBase {

    @Test
    public void shouldParseFullText() {
        WarningInfo warningInfo = ParseUtil.parse(new WarningInfoJsonParser(true), "warning.json");

        assertThat(warningInfo.getCode(), is("adult"));
        assertThat(warningInfo.getText(), is("Возрастное ограничение полный текст"));
    }

    @Test
    public void shouldParseShortText() {
        WarningInfo warningInfo = ParseUtil.parse(new WarningInfoJsonParser(false), "warning.json");

        assertThat(warningInfo.getCode(), is("adult"));
        assertThat(warningInfo.getText(), is("Возрастное ограничение"));
    }
}
