package ru.yandex.market.api.server.context;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.Environment;

public class DcHostTest {

    @Test
    public void parseYpDefault() {
        Assert.assertEquals(DcHost.DEFAULT_DC_HOST.toString(), DcHost.parseYp(".man.yp-c.yandex.net").toString());
    }

    @Test
    public void parseGencfgDefault() {
        Assert.assertEquals(DcHost.DEFAULT_DC_HOST.toString(), DcHost.parseGencfg(".gencfg-c.yandex.net").toString());
    }

    @Test
    public void parseYpTestManOk() {
        ApplicationContextHolder.setEnvironment(Environment.TESTING);
        Assert.assertEquals("mrnswqs4bbgbdieset", DcHost.parse("rnswqs4bbgbdiese.man.yp-c.yandex.net").toString());
    }

    @Test
    public void parseYpProdSasOk() {
        ApplicationContextHolder.setEnvironment(Environment.PRODUCTION);
        Assert.assertEquals("htesting-market-content-api-sas-1x", DcHost.parse("testing-market-content-api-sas-1.sas.yp-c.yandex.net").toString());
    }

    @Test
    public void parseYpLocalVlaOk() {
        ApplicationContextHolder.setEnvironment(Environment.LOCAL);
        Assert.assertEquals("vrnswqs4bbgbdiesel", DcHost.parse("rnswqs4bbgbdiese.vla.yp-c.yandex.net").toString());
    }

    @Test
    public void parseYpLocalIvaOk() {
        ApplicationContextHolder.setEnvironment(Environment.LOCAL);
        Assert.assertEquals("ernswqs4bbgbdiesel", DcHost.parse("rnswqs4bbgbdiese.iva.yp-c.yandex.net").toString());
    }

    @Test
    public void parseGencfgTestManOk() {
        ApplicationContextHolder.setEnvironment(Environment.TESTING);
        Assert.assertEquals("m0771t", DcHost.parse("man3-0771-c08-man-market-prod--1c4-19634.gencfg-c.yandex.net").toString());
    }

    @Test
    public void parseGencfgProdSasOk() {
        ApplicationContextHolder.setEnvironment(Environment.PRODUCTION);
        Assert.assertEquals("h0771x", DcHost.parse("sas-0771-c08-man-market-prod--1c4-19634.gencfg-c.yandex.net").toString());
    }

    @Test
    public void parseGencfgLocalVlaOk() {
        ApplicationContextHolder.setEnvironment(Environment.LOCAL);
        Assert.assertEquals("v0771l", DcHost.parse("vla3-0771-c08-man-market-prod--1c4-19634.gencfg-c.yandex.net").toString());
    }

    @Test
    public void parseGencfgLocalIvaOk() {
        ApplicationContextHolder.setEnvironment(Environment.LOCAL);
        Assert.assertEquals("e0771l", DcHost.parse("iva-0771-c08-man-market-prod--1c4-19634.gencfg-c.yandex.net").toString());
    }

}
