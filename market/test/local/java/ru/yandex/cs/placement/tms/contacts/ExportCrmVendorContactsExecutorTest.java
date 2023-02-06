package ru.yandex.cs.placement.tms.contacts;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.placement.tms.contacts.service.CrmVendorContactUploadService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.matchers.XmlArgMatcher;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/contacts/ExportCrmVendorContactsExecutorTest/before.csv",
        dataSource = "vendorDataSource"
)
public class ExportCrmVendorContactsExecutorTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private ExportCrmVendorContactsExecutor exportCrmVendorContactsExecutor;
    @Autowired
    private CrmVendorContactUploadService crmVendorContactUploadService;

    @Test
    void doJob() {
        exportCrmVendorContactsExecutor.doJob(null);
        String expectedXml = "" +
                "<contacts>" +
                "  <contact>" +
                "    <vendorId>100</vendorId>" +
                "    <vendorName>Test vendor 100</vendorName>" +
                "    <brandId>100</brandId>" +
                "    <brandName>Vasya-Brand</brandName>" +
                "    <managerLogin>kolya</managerLogin>" +
                "    <contactName>Vasya</contactName>" +
                "    <phone>+79876543210</phone>" +
                "    <email>vasya@yandex.ru</email>" +
                "    <company>Test company 100</company>" +
                "    <brandUrl>vasya-site.info</brandUrl>" +
                "  </contact>" +
                "  <contact>" +
                "    <vendorId>200</vendorId>" +
                "    <vendorName>Test vendor 200</vendorName>" +
                "    <brandId>200</brandId>" +
                "    <brandName>Petya-brand</brandName>" +
                "    <managerLogin/>" +
                "    <contactName>Petya</contactName>" +
                "    <phone>+70123456789</phone>" +
                "    <email>petya@yandex.ru</email>" +
                "    <company>Test company 200</company>" +
                "    <brandUrl>petya-site.info</brandUrl>" +
                "  </contact>" +
                "</contacts>";

        Mockito.verify(crmVendorContactUploadService, Mockito.times(1))
                .uploadCrmVendorContacts(ArgumentMatchers.argThat(new XmlArgMatcher(expectedXml)));
    }
}
