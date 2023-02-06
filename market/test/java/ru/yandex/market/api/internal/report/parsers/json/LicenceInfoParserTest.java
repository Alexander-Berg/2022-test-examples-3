package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.LicenceInfo;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 05.06.2019
 */
public class LicenceInfoParserTest {
    private LicenceInfoParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new LicenceInfoParser();
    }

    @Test
    public void shouldParseInfo() {
        LicenceInfo info = parser.parse(ResourceHelpers.getResource("report_outlets__legalinfo_licence.json"));

        Assert.assertEquals("11ЛИЦ3434901", info.getNumber());
        Assert.assertEquals("1 мая 2019 г.", info.getStartDate());
        Assert.assertEquals("30 июня 2019 г.", info.getEndDate());
    }
}
