package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.outlet.OutletLegalInfo;
import ru.yandex.market.api.shop.OrganizationType;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

public class OutletLegalInfoParserTest {

    private OutletLegalInfoParser parser;

    @Before
    public void setUp() throws Exception {
        this.parser = new OutletLegalInfoParser();
    }

    @Test
    public void getParsed() {
        OutletLegalInfo info = parser.parse(ResourceHelpers.getResource("report_outlets__legalinfo.json"));

        assertEquals("23889841", info.getOutletId());
        assertEquals(OrganizationType.IP, info.getType());
        assertEquals("Лютик славный ИП", info.getName());
        assertEquals("304500116000157", info.getOgrn());
        assertEquals("г. Москва, Тестовая улица, д.10", info.getJuridicalAddress());
        assertEquals("г. Москва, Тверская улица, д. 16", info.getFactAddress());
    }
}
