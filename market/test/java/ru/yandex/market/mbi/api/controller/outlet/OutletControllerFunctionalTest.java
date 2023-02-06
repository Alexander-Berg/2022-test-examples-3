package ru.yandex.market.mbi.api.controller.outlet;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.cutoff.CutoffNotificationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletLegalInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.outlets.Address;
import ru.yandex.market.mbi.api.client.entity.outlets.GeoInfo;
import ru.yandex.market.mbi.api.client.entity.outlets.LegalInfo;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber;
import ru.yandex.market.mbi.api.client.entity.outlets.ScheduleLine;
import ru.yandex.market.mbi.api.client.entity.outlets2.CloseOutletRequestDTO;
import ru.yandex.market.mbi.api.client.entity.outlets2.ClosedOutletBulkDTO;
import ru.yandex.market.mbi.api.client.entity.outlets2.ClosedOutletDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Функциональные тесты для ручек работы с аутлетами.
 *
 * @author Vadim Lyalin
 */
class OutletControllerFunctionalTest extends FunctionalTest {

    /**
     * Поиск без условий.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListAllOutletsV2() {
        PagedOutletsDTO outletsDTO = mbiApiClient.getOutletsV2(null, null, null, null, null, null, null, null, null);
        assertEquals(7, outletsDTO.getOutlets().size());
        OutletInfoDTO expected = new OutletInfoDTO(expectedOutletInfo());
        ReflectionAssert.assertReflectionEquals(expected, outletsDTO.getOutlets().iterator().next());
    }

    // Тест проверяет корректную работу пейджера на контроллере
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testPagerXML() {
        String url = "http://localhost:" + port + "/outlets/v2?page=2&size=2";
        ResponseEntity<String> actual = FunctionalTestHelper.get(url);

        String expected =
                // language=xml
                "<outlets>\n" +
                        "    <pager>\n" +
                        "        <total>7</total>\n" +
                        "        <from>3</from>\n" +
                        "        <to>4</to>\n" +
                        "        <page-size>2</page-size>\n" +
                        "        <pages-count>4</pages-count>\n" +
                        "        <current-page>2</current-page>\n" +
                        "    </pager>\n" +
                        "    <outlets-info>\n" +
                        "        <outlet-info id=\"3\" shop-outlet-id=\"3\" datasource-id=\"1\" type=\"RETAIL\" name=\"ларёк\" status=\"UNKNOWN\" is-main=\"false\">\n" +
                        "            <address city=\"Москва\"/>\n" +
                        "            <phones/>\n" +
                        "            <emails/>\n" +
                        "            <schedule id=\"-1\">\n" +
                        "                <lines/>\n" +
                        "            </schedule>\n" +
                        "            <geo-info>\n" +
                        "                <gps-coords>\n" +
                        "                    <lon>40.55</lon>\n" +
                        "                    <lat>60.77</lat>\n" +
                        "                </gps-coords>\n" +
                        "            </geo-info>\n" +
                        "            <delivery-rules/>\n" +
                        "        </outlet-info>\n" +
                        "        <outlet-info id=\"4\" shop-outlet-id=\"4\" datasource-id=\"1\" type=\"RETAIL\" name=\"ларёк\" status=\"AT_MODERATION\" is-main=\"false\">\n" +
                        "            <address city=\"Москва\"/>\n" +
                        "            <phones/>\n" +
                        "            <emails/>\n" +
                        "            <schedule id=\"-1\">\n" +
                        "                <lines/>\n" +
                        "            </schedule>\n" +
                        "            <geo-info>\n" +
                        "                <gps-coords>\n" +
                        "                    <lon>40.55</lon>\n" +
                        "                    <lat>60.77</lat>\n" +
                        "                </gps-coords>\n" +
                        "            </geo-info>\n" +
                        "            <delivery-rules/>\n" +
                        "        </outlet-info>\n" +
                        "    </outlets-info>\n" +
                        "</outlets>";

        MbiAsserts.assertXmlEquals(expected, actual.getBody());
    }

    // Тест проверяет правильную выдачу ручки контроллера, выдающей все аутлеты
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTestXML.before.csv")
    void testListAllOutletsFullXML() {
        String url = "http://localhost:" + port + "/outlets/v2";
        ResponseEntity<String> actual = FunctionalTestHelper.get(url);
        String expected = StringTestUtil.getString(getClass(), "testListAllOutletsFullXML.xml");
        MbiAsserts.assertXmlEquals(expected, actual.getBody());
    }


    /**
     * Поиск торговых точек для магазина.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListOutletsByShopV2() {
        PagedOutletsDTO outletsDTO = mbiApiClient.getOutletsV2(1L, null, null, null, null, null, null, null, null);
        assertEquals(5, outletsDTO.getOutlets().size());
        OutletInfoDTO expected = new OutletInfoDTO(expectedOutletInfo());
        ReflectionAssert.assertReflectionEquals(expected, outletsDTO.getOutlets().iterator().next());
    }


    /**
     * Поиск ПВЗ для поставщика.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListOutletsBySupplierV2() {
        PagedOutletsDTO outletsDTO = mbiApiClient.getOutletsV2(4L, null, null, null, null, null, null, null, null);
        assertEquals(1, outletsDTO.getOutlets().size());

        OutletInfo expectedOutletInfo = new OutletInfo(8, 4, OutletType.MIXED, "ларёк", false, "8");
        expectedOutletInfo.setStatus(OutletStatus.AT_MODERATION);
        expectedOutletInfo.addPhone(ru.yandex.market.core.outlet.PhoneNumber.builder().setCountry("+7")
                .setCity("495").setNumber("1234567").setPhoneType(PhoneType.FAX).build());
        expectedOutletInfo.setSchedule(new Schedule(1, Collections.singletonList(
                new ru.yandex.market.core.schedule.ScheduleLine(
                        ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek.MONDAY, 1, 1, 1))));
        expectedOutletInfo.setAddress(new ru.yandex.market.core.outlet.Address.Builder()
                .setCity("Москва")
                .build());
        expectedOutletInfo.setGeoInfo(new ru.yandex.market.core.outlet.GeoInfo(new Coordinates(40.55, 60.77), null));

        OutletInfoDTO expected = new OutletInfoDTO(expectedOutletInfo);
        ReflectionAssert.assertReflectionEquals(expected, outletsDTO.getOutlets().iterator().next());
    }

    /**
     * Поиск торговых точек по фильтру.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListFilteredOutletsV2() {
        PagedOutletsDTO outletsDTO = mbiApiClient.getOutletsV2(1L, 1L, null, OutletType.RETAIL, null,
                OutletStatus.AT_MODERATION, null, null, null);
        assertEquals(1, outletsDTO.getOutlets().size());
        OutletInfoDTO expected = new OutletInfoDTO(expectedOutletInfo());
        ReflectionAssert.assertReflectionEquals(expected, outletsDTO.getOutlets().iterator().next());
    }


    /**
     * Поиск торговых точек по улице.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListOutletsByStreetV2() {
        PagedOutletsDTO outletsDTO = mbiApiClient.getOutletsV2(null, null, "Толстого", null, null, null, null, null, null);
        OutletInfoDTO expected = new OutletInfoDTO(expectedOutletInfo());
        ReflectionAssert.assertReflectionEquals(expected, outletsDTO.getOutlets().iterator().next());
    }


    /**
     * Поиск инлетов по сервису доставки.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListInletsByDs() {
        List<Outlet> inlets = mbiApiClient.getInletsV2(106L);
        assertEquals(1, inlets.size());
        assertEquals(7, inlets.iterator().next().getId());
    }

    /**
     * Поиск инлетов по сервису доставки с пустым результатом.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testListNoInletsByDs() {
        List<Outlet> inlets = mbiApiClient.getInletsV2(1111L);
        MatcherAssert.assertThat(inlets, Matchers.notNullValue());
        assertEquals(0, inlets.size());
    }

    /**
     * Тест на корректное закрытие аутлета V2.
     */
    @Test
    @DbUnitDataSet(
            before = "OutletFunctionalTest.before.csv",
            after = "OutletControllerFunctionalTest.closeOutlet.after.csv"
    )
    void testCloseOutletV2() {
        ClosedOutletDTO expectedResponseV2 = new ClosedOutletDTO(
                1,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.SENT,
                null
        );

        ClosedOutletBulkDTO responseV2 = mbiApiClient.closeOutletsV2(Collections.singletonList(1L), 123, 105, "msg",
                "subject", "body");
        MatcherAssert.assertThat(responseV2.getItems(), Matchers.contains(expectedResponseV2));
    }

    /**
     * Тест на закрытие уже закрытого аутлета V2.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testCloseClosedOutletV2() {
        ClosedOutletDTO expectedResponseV2 = new ClosedOutletDTO(
                5,
                CutoffActionStatus.DONT_NEED,
                CutoffNotificationStatus.NOT_SENT,
                "Outlet is already closed"
        );

        ClosedOutletBulkDTO responseV2 = mbiApiClient.closeOutletsV2(Collections.singletonList(5L), 123, 105, "msg",
                "subject", "body");
        MatcherAssert.assertThat(responseV2.getItems(), Matchers.contains(expectedResponseV2));
    }

    /**
     * Тест на получение несуществующего аутлета.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testGetNonExistedOutlet() {
        MatcherAssert.assertThat(
                mbiApiClient.getOutlet(0, false),
                Matchers.nullValue()
        );
    }

    /**
     * Тест на получение не удаленного аутлета.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testGetOutlet() {
        Outlet outlet = mbiApiClient.getOutlet(1, false);
        ReflectionAssert.assertReflectionEquals(expectedOutlet(), outlet);

        outlet = mbiApiClient.getOutlet(1, true);
        ReflectionAssert.assertReflectionEquals(expectedOutlet(), outlet);
    }

    /**
     * Тест на получение удаленного аутлета.
     */
    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void testGetDeletedOutlet() {
        Outlet outlet = mbiApiClient.getOutlet(9, false);
        Assertions.assertNull(outlet);

        outlet = mbiApiClient.getOutlet(9, true);

        Address address = new Address("Москва", "Ул. Льва Толстого", null, null, null, null, null, null, null);
        final Outlet expected = new Outlet(9, "ларёк", null, null, address, new GeoInfo("40.55,60.77", null), null, null, null, null);
        ReflectionAssert.assertReflectionEquals(expected, outlet);
    }

    private Outlet expectedOutlet() {
        Address address = new Address("Москва", "Ул. Льва Толстого", null, null, null, null, null, null, null);
        return new Outlet(1, "ларёк", null, null, address, new GeoInfo("40.55,60.77", null), null,
                Collections.singletonList(new PhoneNumber("+7", "495", "1234567", null, null, PhoneType.FAX)),
                Collections.singletonList(new ScheduleLine(ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek.MONDAY,
                        1, 1, 1)), new LegalInfo(OrganizationType.OOO, "Novostroy", "12346",
                "Novosibirsk ul Chehova 1", "Novosibirsk ul Chehova 2"));
    }

    private OutletInfo expectedOutletInfo() {
        OutletInfo expectedOutletInfo = new OutletInfo(1, 1, OutletType.RETAIL, "ларёк", false, "1");
        expectedOutletInfo.setStatus(OutletStatus.AT_MODERATION);
        expectedOutletInfo.addPhone(ru.yandex.market.core.outlet.PhoneNumber.builder()
                .setCountry("+7")
                .setCity("495")
                .setNumber("1234567")
                .setPhoneType(PhoneType.FAX)
                .build());
        expectedOutletInfo.setSchedule(new Schedule(1, Collections.singletonList(
                new ru.yandex.market.core.schedule.ScheduleLine(
                        ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek.MONDAY, 1, 1, 1))));
        expectedOutletInfo.setAddress(new ru.yandex.market.core.outlet.Address.Builder()
                .setCity("Москва")
                .setStreet("Ул. Льва Толстого")
                .build());
        expectedOutletInfo.setGeoInfo(new ru.yandex.market.core.outlet.GeoInfo(new Coordinates(40.55, 60.77), null));
        expectedOutletInfo.setLegalInfo(new OutletLegalInfo.Builder()
                .setOutletId(1L)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName("Novostroy")
                .setRegistrationNumber("12346")
                .setJuridicalAddress("Novosibirsk ul Chehova 1")
                .setFactAddress("Novosibirsk ul Chehova 2")
                .build());
        return expectedOutletInfo;
    }

    @Test
    @DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
    void validTest() {
        CloseOutletRequestDTO closeOutletRequestDTO = new CloseOutletRequestDTO(
                ImmutableList.of(5L),
                123L,
                105,
                "msg",
                "subject",
                "body"
        );

        Assertions.assertEquals((Integer) 105, closeOutletRequestDTO.getTemplateId());
        Assertions.assertEquals("msg", closeOutletRequestDTO.getMessage());
        Assertions.assertEquals("subject", closeOutletRequestDTO.getSubject());

        String url = "http://localhost:" + port + "/outlets/close/v2";
        String requestXML =
                // language=xml
                "<close-outlet-request uid=\"123\" template-id=\"105\" message=\"msg\" subject=\"subject\" body=\"body\">\n" +
                        "    <outlet-id>5</outlet-id>\n" +
                        "</close-outlet-request>";
        String actual = FunctionalTestHelper.postForXml(url, requestXML);

        String expected =
                // language=xml
                "<closed-outlets><closed-outlet outlet-id=\"5\" status=\"DONT_NEED\" " +
                        "notification-status=\"NOT_SENT\" error=\"Outlet is already closed\"/></closed-outlets>";

        MbiAsserts.assertXmlEquals(expected, actual);
    }

    @Test
    void changeTest() {
        CloseOutletRequestDTO closeOutletRequestDTO = new CloseOutletRequestDTO(
                ImmutableList.of(1L, 2L),
                1L,
                null,
                null,
                null,
                "body"
        );

        Assertions.assertEquals((Integer) 157, closeOutletRequestDTO.getTemplateId());
        Assertions.assertEquals("", closeOutletRequestDTO.getMessage());
        Assertions.assertEquals("", closeOutletRequestDTO.getSubject());
    }

    @Test
    void nullListTest() {

        String url = "http://localhost:" + port + "/outlets/close/v2";
        String requestXML =
                // language=xml
                "<close-outlet-request uid=\"1\" template-id=\"3\" message=\"message\" subject=\"subject\" body=\"body\"/>";

        RuntimeException runtimeException = Assertions.assertThrows(
                RuntimeException.class,
                () -> FunctionalTestHelper.postForXml(url, requestXML)
        );
        Assertions.assertEquals("400 Bad Request", runtimeException.getMessage());
    }

    @Test
    void nullBodyTest() {

        String url = "http://localhost:" + port + "/outlets/close/v2";
        String requestXML =
                // language=xml
                "<close-outlet-request uid=\"1\" template-id=\"3\" message=\"message\" subject=\"subject\">\n" +
                        "    <outlet-id>1</outlet-id>\n" +
                        "    <outlet-id>2</outlet-id>\n" +
                        "</close-outlet-request>";

        RuntimeException runtimeException = Assertions.assertThrows(
                RuntimeException.class,
                () -> FunctionalTestHelper.postForXml(url, requestXML)
        );
        Assertions.assertEquals("400 Bad Request", runtimeException.getMessage());
    }

    @Test
    void emptyListTest() {

        String url = "http://localhost:" + port + "/outlets/close/v2";
        String requestXML =
                // language=xml
                "<close-outlet-request uid=\"1\" template-id=\"3\" message=\"message\" subject=\"subject\" body=\"body\"/>";

        RuntimeException runtimeException = Assertions.assertThrows(
                RuntimeException.class,
                () -> FunctionalTestHelper.postForXml(url, requestXML)
        );
        Assertions.assertEquals("400 Bad Request", runtimeException.getMessage());
    }
}
