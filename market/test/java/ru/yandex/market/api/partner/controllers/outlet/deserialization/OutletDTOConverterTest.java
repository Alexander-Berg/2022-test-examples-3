package ru.yandex.market.api.partner.controllers.outlet.deserialization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.api.partner.apisupport.ApiInvalidRequestException;
import ru.yandex.market.api.partner.apisupport.ErrorRestModel;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletAddressDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDeliveryRuleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletScheduleItemDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletStatusDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletTypeDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletWorkingScheduleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.converter.OutletDTOToOutletInfoConverter;
import ru.yandex.market.api.partner.controllers.outlet.model.converter.OutletInfoToOutletDTOConverter;
import ru.yandex.market.api.partner.controllers.region.model.Region;
import ru.yandex.market.api.partner.controllers.region.model.RegionDTO;
import ru.yandex.market.api.partner.controllers.util.CurrencyAndRegionHelper;
import ru.yandex.market.api.partner.controllers.util.OutletHelper;
import ru.yandex.market.api.partner.view.json.Names;
import ru.yandex.market.core.backa.persist.OutletExporter;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.moderation.ModerationInfo;
import ru.yandex.market.core.outlet.moderation.ModerationLevel;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleFactory;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Проверяет конвертацию {@link OutletDTO}  в {@link ru.yandex.market.core.outlet.OutletInfo} и обратно.
 */
@ExtendWith(MockitoExtension.class)
class OutletDTOConverterTest {
    @Mock
    private OutletExporter outletExporter;
    @Mock
    private CurrencyAndRegionHelper currencyAndRegionHelper;
    @Mock
    private GeoClient geoClient;

    @Mock
    private OutletInfoToOutletDTOConverter outletInfoToOutletDTOConverter;

    private OutletHelper outletHelper;

    @BeforeEach
    void setUp() {
        OutletDTOToOutletInfoConverter outletDTOToOutletInfoConverter = new OutletDTOToOutletInfoConverter(geoClient);

        outletHelper = new OutletHelper(currencyAndRegionHelper,
                outletDTOToOutletInfoConverter,
                outletInfoToOutletDTOConverter);
    }

    @Test
    void testMissedDeliveryRulesForDepotOrMixed() {
        final OutletDTO outletDTO = getOutletRequestWithoutDelivery();

        try {
            outletDTO.setDeliveryRules(null);
            final OutletInfo result = outletHelper.convertOutletDTOToInfo(outletDTO, getRegion(), 774, 174);
            fail(String.format("Outlet info (%s) should not be converted. Delivery rules were missed", result));
        } catch (final ApiInvalidRequestException ex) {
            final List<ErrorRestModel> errors = ex.getErrors();
            final ErrorRestModel error = Preconditions.checkNotNull(Iterables.getFirst(errors, null));
            final String message = error.getMessage();
            assertThat(message, containsString(Names.DeliveryRules.DELIVERY_RULES));
        }
    }

    /**
     * Простой случай конвертирования, когда все поля необходимы заполнены и все хорошо.
     */
    @Test
    void testSimpleConvert() throws ScheduleFactory.SchedulingException {
        //city не заполнено во входном запросе,
        // из того что пришло должны сетить только актуальные поля,
        // устаревшие - только на выход работают
        OutletDTO outletRequest = getOutletRequest();
        //конвертируем во внутреннюю модель
        OutletInfo result = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);
        OutletInfo expectedOutletInfo = getExpectedOutletInfo();
        Assertions.assertEquals(expectedOutletInfo, result, "Полученная и ожидаемая внутренняя модель не совпадают");

        when(outletExporter.getScheduleString(any())).thenReturn("пн.-чт. 09:00-20:30");
        //внутреннюю инфу дополненную инфой по службам доставки конвертируем в ответ
        expectedOutletInfo.setModerationInfo(new ModerationInfo("reason", ModerationLevel.NO_MODERATION));
        OutletDTO outletResponse = new OutletInfoToOutletDTOConverter(outletExporter).convert(expectedOutletInfo,
                getRegion());
        //ожидаем, что заполнятся:
        // city, id, status,shopOutletId,workingTime, reason, region - старые поля для обратной совместимости
        outletRequest.getOutletAddress().setCity(result.getAddress().getCity());
        outletRequest.setId(expectedOutletInfo.getId());
        outletRequest.setStatus(OutletStatusDTO.getByOutletStatus(expectedOutletInfo.getStatus()));
        outletRequest.setRegion(new RegionDTO(getRegion()));
        //для обратной совместимости
        outletRequest.setShopOutletId(expectedOutletInfo.getShopOutletId());
        outletRequest.setWorkingTime("пн.-чт. 09:00-20:30");

        outletRequest.getDeliveryRules().get(0).setDateSwitchHour(expectedOutletInfo.getDeliveryRules().get(0).getDateSwitchHour());
        outletRequest.getDeliveryRules().get(0).setPriceTo(expectedOutletInfo.getDeliveryRules().get(0).getPriceTo());
        outletRequest.getDeliveryRules().get(0).setShipperId(expectedOutletInfo.getDeliveryRules().get(0).getShipperId());
        outletRequest.getDeliveryRules().get(0).setShipperName(expectedOutletInfo.getDeliveryRules().get(0).getShipperName());
        outletRequest.getDeliveryRules().get(0).setShipperHumanReadbleId(expectedOutletInfo.getDeliveryRules().get(0).getShipperHumanReadableId());

        outletRequest.getDeliveryRules().get(0).setWorkInHoliday(outletRequest.getWorkingSchedule().isWorkInHoliday());//HumanReadbleId();
        Assertions.assertEquals(outletRequest, outletResponse, "Полученный и ожидаемый ответ не совпадают");
    }

    /**
     * Проверяем, что при конвертации в {@link OutletInfo} не будут использованы данные из deprecated полей.
     */
    @Test
    void testSimpleConvertWithDeprecatedPropertiesInRequest() throws ScheduleFactory.SchedulingException {
        //city не заполнено во входном запросе
        OutletDTO outletRequest = getOutletRequest();
        //заполняем поля, которые не должны участвовать в конвертации во внутреннюю модель
        fillOutProperties(outletRequest);

        OutletInfo result = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);
        OutletInfo expectedOutletInfo = getExpectedOutletInfo();
        Assertions.assertEquals(expectedOutletInfo, result);

    }

    private void fillOutProperties(OutletDTO outletRequest) {
        outletRequest.setId(666L);
        outletRequest.setStatus(OutletStatusDTO.FAILED);
        outletRequest.setShopOutletId("shopOutlet_Id");
        outletRequest.setWorkingTime("когда-нибудь");

        outletRequest.setModerationReason("из вне текст");

        RegionDTO regionDTO = new RegionDTO();
        regionDTO.setId(4444L);
        regionDTO.setName("random city");
        outletRequest.setRegion(regionDTO);
    }

    /**
     * Имя аутлета в запросе не заполнено.
     */
    @Test
    void testMissedName() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setName(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Тип аутлета в запросе не заполнен.
     */
    @Test
    void testMissedType() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setType(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Координаты аутлета в запросе не заполнены и не можем найти в geoClient.
     * Could not find GPS coordinates for outlet 0
     * (address: СПБ, Пискаревский проспект, д. 2, корпус 2, владение 3407)
     */
    @Test
    void testCouldNotFindGeoCoords() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setCoords(null);
        when(geoClient.findFirst(anyString())).thenReturn(Optional.empty());
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Координаты аутлета в запросе не заполнены и заполняются из geoClient.
     */
    @Test
    void testFindGeoCoords() throws ScheduleFactory.SchedulingException {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setCoords(null);
        GeoObject go = createSimpleGeoObject();
        when(geoClient.findFirst(anyString())).thenReturn(Optional.of(go));
        OutletInfo result = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);
        OutletInfo outletInfoExpected = getExpectedOutletInfo();
        outletInfoExpected.getGeoInfo().setGpsCoordinates(Coordinates.valueOf("37.586598 55.734035"));
        Assertions.assertEquals(outletInfoExpected, result);
    }

    private GeoObject createSimpleGeoObject() {
        return SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withPrecision(Precision.STREET).withKind(Kind.STREET).withPoint("37.586598 55.734035")
                        .build())
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder()
                                .withCountryName("Россия")
                                .withCountryCode("RU")
                                .build()
                        )
                        .withAddressLine("Россия, Москва, улица Льва Толстого")
                        .withAreaInfo(AreaInfo.newBuilder().withAdministrativeAreaName("Москва").build())
                        .withLocalityInfo(LocalityInfo.newBuilder()
                                .withLocalityName("Москва")
                                .withThoroughfareName("улица Льва Толстого")
                                .withPremiseNumber("213")
                                .build())
                        .build()
                )
                .withBoundary(Boundary.newBuilder().withEnvelopeUpper("37.590991 55.736868").withEnvelopeLower("37" +
                        ".582475 55.731587").build())
                .withAccuracy("0.80000000000000004")
                .build();
    }

    /**
     * Адрес аутлета в запросе не заполнен.
     */
    @Test
    void testMissedAddress() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setOutletAddress(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Region id (идентификатор города)  в запросе не заполнен.
     */
    @Test
    void testMissedRegionId() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getOutletAddress().setRegionId(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }


    /**
     * Телефон аутлета в запросе не заполнен.
     */
    @Test
    void testMissedPhone() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setPhones(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }


    /**
     * Расписание аутлета в запросе не заполнено.
     */
    @Test
    void testMissedWorkingSchedule() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setWorkingSchedule(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Время в расписании аутлета в неправильном формате.
     */
    @Test
    void testWrongScheduleFormat() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setStartTime(":00");

        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174)
        );
    }

    /**
     * Неправильное время в расписании аутлета.
     */
    @Test
    void testWrongScheduleValue() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setStartTime("24:00");
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Начало и конец работы совпадают в расписании аутлета.
     */
    @Test
    void testEqStartEndDateScheduleValue() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setStartTime("09:00");
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setEndTime("09:00");
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }

    /**
     * Начало и конец работы не совпадают в расписании аутлета.
     */
    @Test
    void testNEqStartEndDateScheduleValue() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setStartTime("09:00");
        outletRequest.getWorkingSchedule().getScheduleItems().get(0).setEndTime("10:00");
        OutletInfo oi = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);
        Assertions.assertEquals(outletRequest.getWorkingSchedule().getScheduleItems().get(0).getStartTime(),
                oi.getSchedule().getLines().get(0).localStartTime().toString());
        Assertions.assertEquals(outletRequest.getWorkingSchedule().getScheduleItems().get(0).getEndTime(),
                oi.getSchedule().getLines().get(0).localEndTime().toString());
    }

    /**
     * Нет расписания аутлета в запросе.
     */
    @Test
    void testMissedScheduleItems() {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.getWorkingSchedule().setScheduleItems(null);
        Assertions.assertThrows(
                ApiInvalidRequestException.class,
                () -> outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174));
    }


    /**
     * Проверяет случай когда тип аутлета RETAIL (пустые настройки правил доставки).
     */
    @Test
    void testRetailOutletInfo() throws ScheduleFactory.SchedulingException {
        OutletDTO outletRequest = getOutletRequest();
        outletRequest.setType(OutletTypeDTO.RETAIL);
        OutletInfo result = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);
        Assertions.assertEquals(getExpectedRetailOutletInfo(), result);
    }

    /**
     * Модель с пустыми строками в необязательных параметрах.
     */
    @Test
    void testEmptyStrings() throws ScheduleFactory.SchedulingException {
        final OutletDTO outletRequest = getOutletRequest();
        outletRequest.setEmails(List.of(""));
        outletRequest.getOutletAddress().setStreet("");
        outletRequest.getOutletAddress().setNumber("");
        outletRequest.getOutletAddress().setBuilding("");
        outletRequest.getOutletAddress().setEstate("");
        outletRequest.getOutletAddress().setBlock("");
        outletRequest.getOutletAddress().setAddrAdditional("");
        final OutletInfo result = outletHelper.convertOutletDTOToInfo(outletRequest, getRegion(), 774, 174);

        final OutletInfo expected = getExpectedOutletInfo();
        expected.setEmails(List.of());
        final Address expectedAddress = new Address.Builder()
                .setCity("СПБ")
                .setStreet(null)
                .setNumber(null)
                .setBuilding(null)
                .setEstate(null)
                .setBlock(null)
                .setOther(null)
                .build();
        expected.setAddress(expectedAddress);

        Assertions.assertEquals(expected, result);
    }

    private Region getRegion() {
        return new Region(2L, "СПБ", RegionType.CITY, null);
    }

    private OutletDTO getOutletRequestWithoutDelivery() {
        OutletDTO outletRequestExpected = new OutletDTO();
        outletRequestExpected.setType(OutletTypeDTO.DEPOT);
        outletRequestExpected.setName("Место тестировщика");
        outletRequestExpected.setMain(true);
        outletRequestExpected.setShopOutletCode("strOutlet");

        OutletAddressDTO outletAddressDTO = new OutletAddressDTO();
        outletAddressDTO.setStreet("Пискаревский проспект");
        outletAddressDTO.setNumber("2");
        outletAddressDTO.setEstate("3407");
        outletAddressDTO.setBlock("2");
        outletAddressDTO.setRegionId(2L);
        outletRequestExpected.setOutletAddress(outletAddressDTO);
        outletRequestExpected.setVisibility(OutletVisibility.VISIBLE);


        OutletWorkingScheduleDTO outletWorkingScheduleDTO = new OutletWorkingScheduleDTO();
        outletWorkingScheduleDTO.setWorkInHoliday(true);
        List<OutletScheduleItemDTO> outletScheduleItemDTOList = new ArrayList<>();
        OutletScheduleItemDTO outletScheduleItemDTO = new OutletScheduleItemDTO();
        outletScheduleItemDTO.setEndDay(OutletScheduleItemDTO.DayOfWeek.THURSDAY);
        outletScheduleItemDTO.setStartDay(OutletScheduleItemDTO.DayOfWeek.MONDAY);
        outletScheduleItemDTO.setStartTime("09:00");
        outletScheduleItemDTO.setEndTime("20:30");
        outletScheduleItemDTOList.add(outletScheduleItemDTO);
        OutletScheduleItemDTO outletScheduleItemDTO1 = new OutletScheduleItemDTO();
        outletScheduleItemDTO1.setEndDay(OutletScheduleItemDTO.DayOfWeek.FRIDAY);
        outletScheduleItemDTO1.setStartDay(OutletScheduleItemDTO.DayOfWeek.FRIDAY);
        outletScheduleItemDTO1.setStartTime("00:00");
        outletScheduleItemDTO1.setEndTime("23:00");
        outletScheduleItemDTOList.add(outletScheduleItemDTO1);
        outletWorkingScheduleDTO.setScheduleItems(outletScheduleItemDTOList);

        outletRequestExpected.setWorkingSchedule(outletWorkingScheduleDTO);

        outletRequestExpected.setEmails(ru.yandex.common.util.collections.CollectionFactory.list("ofmtest@yandex.ru"));

        outletRequestExpected.setPhones(CollectionFactory.list("+ 7 (345) 919191991",
                "+ 7 (543) 12333123111"));
        outletRequestExpected.setCoords("56.156131,35.802831");

        return outletRequestExpected;

    }

    private OutletInfo getExpectedRetailOutletInfo() throws ScheduleFactory.SchedulingException {
        OutletInfo outletInfo = getExpectedOutletInfoWithoutDelivery();
        outletInfo.setType(OutletType.RETAIL);
        DeliveryRule deliveryRule = new DeliveryRule();
        deliveryRule.setWorkInHoliday(true);
        deliveryRule.setUnspecifiedDeliveryInterval(false);
        outletInfo.addDeliveryRule(deliveryRule);
        return outletInfo;
    }


    private OutletInfo getExpectedOutletInfoWithoutDelivery() throws ScheduleFactory.SchedulingException {
        OutletInfo outletInfoExpected = new OutletInfo(174, 774/*сетим магазинный идентификатор отдельно*/,
                OutletType.DEPOT, "Место тестировщика", true, "strOutlet");
        outletInfoExpected.setAddress(new Address.Builder()
                .setCity("СПБ")
                .setStreet("Пискаревский проспект")
                .setNumber("2")
                .setEstate("3407")
                .setBlock("2")
                .build());
        outletInfoExpected.setHidden(OutletVisibility.VISIBLE);
        ScheduleFactory scheduleFactory = new ScheduleFactory();
        scheduleFactory.addLine(ScheduleLine.DayOfWeek.MONDAY, ScheduleLine.DayOfWeek.THURSDAY, 9, 0, 20, 30, 2);
        scheduleFactory.addLine(ScheduleLine.DayOfWeek.FRIDAY, ScheduleLine.DayOfWeek.FRIDAY, 0, 0, 23, 0, 2);
        Schedule schedule = scheduleFactory.getSchedule();
        schedule.setId(174);
        outletInfoExpected.setSchedule(schedule);
        outletInfoExpected.setEmails(ru.yandex.common.util.collections.CollectionFactory.list("ofmtest@yandex.ru"));

        PhoneNumber.Builder pnb1 = PhoneNumber.builder();
        pnb1.setCountry("7").setCity("345").setNumber("919191991").setPhoneType(PhoneType.PHONE);

        PhoneNumber.Builder pnb2 = PhoneNumber.builder();
        pnb2.setCountry("7").setCity("543").setNumber("12333123111").setPhoneType(PhoneType.PHONE);
        outletInfoExpected.setPhones(CollectionFactory.list(
                pnb1.build(), pnb2.build()));

        outletInfoExpected.setGeoInfo(new GeoInfo(Coordinates.valueOf("56.156131, 35.802831"), 2L));


        return outletInfoExpected;

    }

    private OutletDTO getOutletRequest() {
        OutletDTO outletInfoExpected = getOutletRequestWithoutDelivery();
        OutletDeliveryRuleDTO deliveryRule = new OutletDeliveryRuleDTO();
        deliveryRule.setCost(new BigDecimal(100));
        deliveryRule.setPriceFreePickup(new BigDecimal(122L));
        deliveryRule.setMinDeliveryDays(2);
        deliveryRule.setMaxDeliveryDays(2);
        deliveryRule.setOrderBefore(24);
        deliveryRule.setUnspecifiedDeliveryInterval(false);
        deliveryRule.setDeliveryServiceId(113L);

        outletInfoExpected.setDeliveryRules(CollectionFactory.list(deliveryRule));
        return outletInfoExpected;
    }

    private OutletInfo getExpectedOutletInfo() throws ScheduleFactory.SchedulingException {
        OutletInfo outletInfoExpected = getExpectedOutletInfoWithoutDelivery();
        DeliveryRule deliveryRule = new DeliveryRule();
        deliveryRule.setCost(new BigDecimal(100));
        deliveryRule.setPriceTo(new BigDecimal(122L));
        deliveryRule.setMinDeliveryDays(2);
        deliveryRule.setMaxDeliveryDays(2);
        deliveryRule.setWorkInHoliday(true);
        deliveryRule.setDateSwitchHour(24);
        deliveryRule.setUnspecifiedDeliveryInterval(false);
        DeliveryServiceInfo dsi = new DeliveryServiceInfo();
        dsi.setId(113L);
        deliveryRule.setDeliveryServiceInfo(dsi);
        outletInfoExpected.addDeliveryRule(deliveryRule);
        return outletInfoExpected;
    }

}
