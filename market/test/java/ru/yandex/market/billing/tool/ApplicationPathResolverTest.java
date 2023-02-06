package ru.yandex.market.billing.tool;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;

import ru.yandex.market.mbi.tool.ApplicationPathResolver;

public class ApplicationPathResolverTest {

    private static final String EXT_DATA_DIR = "ext.data.dir";
    private static final String OLD_PROPERTY = System.getProperty(EXT_DATA_DIR);

    @Test
    public void testGetterPathResolving() {
        System.setProperty(EXT_DATA_DIR, "/var/lib/yandex");

        String getterPathMbo = "/var/lib/yandex/market-data-getter/mbo_stuff/recent/tovar-tree.pb";
        Assert.assertEquals(
                getterPathMbo,
                ApplicationPathResolver.getGetterUrl(getterPathMbo, ApplicationPathResolver.MBI_BILLING_BUNDLE)
        );
        String getterPathAbo = "/var/lib/yandex/market-data-getter/abo/recent/";
        Assert.assertEquals(
                getterPathAbo,
                ApplicationPathResolver.getGetterUrl(getterPathAbo, ApplicationPathResolver.MBI_BILLING_BUNDLE)
        );

        String rtcExtDataDir = "/place/db/iss3/instances/18055_testing_market_mbi_billing_vla_qaoR3Jv6LfU/data-getter";
        System.setProperty(EXT_DATA_DIR, rtcExtDataDir);

        String getterRtcPath = rtcExtDataDir + "/mbi-billing/mbo_stuff/tovar-tree.pb";
        Assert.assertEquals(
                getterRtcPath,
                ApplicationPathResolver.getGetterUrl(getterPathMbo, ApplicationPathResolver.MBI_BILLING_BUNDLE)
        );
        getterRtcPath = rtcExtDataDir + "/mbi-billing/abo/";
        Assert.assertEquals(
                getterRtcPath,
                ApplicationPathResolver.getGetterUrl(getterPathAbo, ApplicationPathResolver.MBI_BILLING_BUNDLE)
        );
    }

    @Test
    public void testAutoconfigurationPathResolving() {
        System.setProperty(EXT_DATA_DIR, "/var/lib/yandex");

        String autoconfPath = "/etc/yandex/mbi-billing/banner";
        Assert.assertEquals(
                autoconfPath,
                ApplicationPathResolver.getAutoconfigurationsUrl(autoconfPath)
        );

        String rtcExtDataDir = "/place/db/iss3/instances/18055_testing_market_mbi_billing_vla_qaoR3Jv6LfU/data-getter";
        System.setProperty(EXT_DATA_DIR, rtcExtDataDir);
        String autoconfRtcPath = "/place/db/iss3/instances/" +
                "18055_testing_market_mbi_billing_vla_qaoR3Jv6LfU/mbi-billing/banner";
        Assert.assertEquals(
                autoconfRtcPath,
                ApplicationPathResolver.getAutoconfigurationsUrl(autoconfPath)
        );
    }

    @AfterEach
    private void restoreProperty() {
        if (OLD_PROPERTY != null) {
            System.setProperty(EXT_DATA_DIR, OLD_PROPERTY);
        } else {
            System.clearProperty(EXT_DATA_DIR);
        }
    }

}
