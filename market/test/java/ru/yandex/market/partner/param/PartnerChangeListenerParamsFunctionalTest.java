package ru.yandex.market.partner.param;

import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.mbo.model.PartnerChangeLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "PartnerChangeEventListenerFunctionalTest.before.csv")
class PartnerChangeListenerParamsFunctionalTest extends FunctionalTest {

    @Autowired
    @Qualifier("mboPartnerExportLogbrokerService")
    LogbrokerService mboPartnerExportLogbrokerService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    @DbUnitDataSet(
            before = "PartnerChangeEventListenerFunctionalTest.manageParam.push.before.csv",
            after = "PartnerChangeEventListenerFunctionalTest.manageParam.push.after.csv"
    )
    void testIsPushPartnerEventChange() {
        doReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK")))
                .when(dataCampShopClient).changeSchema(anyLong(), any(), any());
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=IS_PUSH_PARTNER&value=REAL", 22L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/pushPartnerParam.json");
        assertLBEvent();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerChangeEventListenerFunctionalTest.manageParam.unitedCatalog.before.csv",
            after = "PartnerChangeEventListenerFunctionalTest.manageParam.unitedCatalog.after.csv"
    )
    void tesUnitedCatalogEventChange() {
        doReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK")))
                .when(dataCampShopClient).changeSchema(anyLong(), any(), any());
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=UNITED_CATALOG_STATUS&value=SUCCESS", 22L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/unitedCatalogParam.json");
        assertLBEvent();
    }

    private void assertLBEvent() {
        var recordCaptor = ArgumentCaptor.forClass(PartnerChangeLogbrokerEvent.class);
        verify(mboPartnerExportLogbrokerService, times(1)).publishEvent(recordCaptor.capture());
        var record = recordCaptor.getValue();
        assertThat(record.getPayload().getIsPush()).as("is_push must be true").isTrue();
        assertThat(record.getPayload().getIsUnitedCatalog()).as( "is_united_catalog must be true").isTrue();
        assertThat(record.getPayload().getIsIgnoreStocks()).as( "is_ignore_stocks must be true").isTrue();
    }
}
