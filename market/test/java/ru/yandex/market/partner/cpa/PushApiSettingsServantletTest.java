package ru.yandex.market.partner.cpa;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тест управления настройками push-api
 *
 * @author stani on 04.07.18.
 */
@DbUnitDataSet(before = "PushApiSettingsServantletTest.before.csv")
public class PushApiSettingsServantletTest extends FunctionalTest {

    @Autowired
    PushApi pushApi;

    @Test
    void testGetShopPushApiSettings() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/pushApiSettings?id={campaignId}&format=json", 101L);
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/cpa/getShopPushApiSettings.json");
    }

    @Test
    void testGetSupplierPushApiSettings() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/pushApiSettings?id={campaignId}&format=json", 201L);
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/cpa/getSupplierPushApiSettings.json");
    }

    @Test
    @DbUnitDataSet(after = "SupplierUpdatePushApiSettingsServantletTest.after.csv")
    void testUpdateSupplierPushApiSettings() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl +
                        "/pushApiSettings?a=u&id={campaignId}&format=json&pr_api_url=https://ololo-supplier.url" +
                        "&pr_auth_type=HEADER&pr_sha1=23410DAA&pr_data_type=JSON", 201L);
        JsonTestUtil.assertEquals(response, "[\"OK\"]");
    }

    @Test
    @DbUnitDataSet(after = "SupplierInsPushApiSettingsServantletTest.after.csv")
    void testInsertSupplierPushApiSettings() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl +
                        "/pushApiSettings?a=u&id={campaignId}&format=json&pr_api_url=https://ololo-supplier.url" +
                        "&pr_auth_type=HEADER&pr_sha1=23410daf&pr_data_type=JSON", 202L);
        JsonTestUtil.assertEquals(response, "[\"OK\"]");
    }

    @Test
    @DbUnitDataSet(after = "SupplierUpdatePushApiSettingsServantletTest.noDataType.after.csv")
    void testInsertUpdateSupplierPushApiSettingsWithoutDataType() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl +
                        "/pushApiSettings?a=u&id={campaignId}&format=json&pr_api_url=https://ololo-supplier.url" +
                        "&pr_auth_type=HEADER&pr_sha1=23410DAA" +
                        "&sbx_api_url=https://ololo-supplier-sbx.url&sbx_auth_type=URL&sbx_sha1=23410DAD", 202L);
        JsonTestUtil.assertEquals(response, "[\"OK\"]");

        ArgumentCaptor<Settings> settingsCaptor = ArgumentCaptor.forClass(Settings.class);
        verify(pushApi, times(2)).settings(
                eq(3L),
                settingsCaptor.capture(),
                anyBoolean()
        );

        List<Settings> sentSettings = settingsCaptor.getAllValues();

        //noinspection unchecked
        assertThat(sentSettings, containsInAnyOrder(
                MbiMatchers.transformedBy(this::cleanUp, samePropertyValuesAs(
                        new Settings("https://ololo-supplier.url", "token3", DataType.JSON, AuthType.HEADER))),
                MbiMatchers.transformedBy(this::cleanUp, samePropertyValuesAs(
                        new Settings("https://ololo-supplier-sbx.url", "token3", DataType.JSON, AuthType.URL)))));

    }

    Settings cleanUp(Settings settings) {
        return new Settings(
                settings.getUrlPrefix(),
                settings.getAuthToken(),
                settings.getDataType(),
                settings.getAuthType()
        );
    }

}
