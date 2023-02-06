package ru.yandex.market.api.partner.controllers.outlet.serialization;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletAddressDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDeliveryRuleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletScheduleItemDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletStatusDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletTypeDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletWorkingScheduleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.RegionTypeDTO;
import ru.yandex.market.api.partner.controllers.region.model.RegionDTO;
import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.schedule.ScheduleFactory;

/**
 * Проверяет авто сериализацию выдачи аутлетных ручек ПАПИ  {@link OutletDTO}.
 */
public class OutletDTOSerializationTest extends BaseJaxbSerializationTest {

    @Test
    public void testJsonOutletResponseSerialization() throws IOException, ScheduleFactory.SchedulingException, ParseException {
        OutletDTO outletResponse = getOutletResponse();

        String expectedJson = IOUtils.readInputStream(this.getClass().getResourceAsStream("OutletDTOSerializationTest_outletResponse.json"));
        String expectedXml = IOUtils.readInputStream(this.getClass().getResourceAsStream("OutletDTOSerializationTest_outletResponse.xml"));
        testSerialization(outletResponse, expectedJson, expectedXml);
    }


    public OutletDTO getOutletResponse() throws ScheduleFactory.SchedulingException, ParseException {
        OutletAddressDTO address = new OutletAddressDTO();
        address.setCity("Санкт-Петербург");
        address.setStreet("Пискаревский проспект");
        address.setNumber("2");
        address.setEstate("3407");
        address.setBlock("2");
        address.setRegionId(2L);

        OutletDTO outletResponse = new OutletDTO();
        outletResponse.setId(123);
        outletResponse.setName("Место тестировщика");
        outletResponse.setType(
                OutletTypeDTO.DEPOT);
        outletResponse.setMain(
                true);
        outletResponse.setStatus(OutletStatusDTO.AT_MODERATION);
        outletResponse.setModerationReason("just return back");
        outletResponse.setVisibility(OutletVisibility.VISIBLE);
        outletResponse.setWorkingTime("пн-чт 9:00-20:30");
        outletResponse.setShopOutletId("strOutlet");
        outletResponse.setShopOutletCode("strOutlet");
        outletResponse.setOutletAddress(address);
        outletResponse.setRegion(new RegionDTO(2, "Санкт-Петербург", RegionTypeDTO.CITY, new RegionDTO(10174, "Санкт-Петербург и Ленинградская область", RegionTypeDTO.REPUBLIC,
                new RegionDTO(17, "Северо-Западный федеральный округ", RegionTypeDTO.AREA, new RegionDTO(225, "Россия", RegionTypeDTO.COUNTRY, null)))));
        outletResponse.setCoords(
                "56.156131, 35.802831");
        outletResponse.setPhones(CollectionFactory.list("+ 7 (345) 919-191991",
                "+ 7 (543) 123-33123111"));
        outletResponse.setEmails(CollectionFactory.list("ofmtest@yandex.ru"));
        outletResponse.setDeliveryRules(CollectionFactory.list(getDeliveryRule()));
//           outletResponse.wsetW     false,
        outletResponse.setWorkingSchedule(getSchedule());

        return outletResponse;
    }


    private OutletWorkingScheduleDTO getSchedule() throws ScheduleFactory.SchedulingException {

        OutletWorkingScheduleDTO schedule = new OutletWorkingScheduleDTO();
        schedule.setWorkInHoliday(true);
        List<OutletScheduleItemDTO> scheduleItems = new ArrayList();
        OutletScheduleItemDTO scheduleLine = new OutletScheduleItemDTO();
        scheduleLine.setStartDay(OutletScheduleItemDTO.DayOfWeek.MONDAY);
        scheduleLine.setEndDay(OutletScheduleItemDTO.DayOfWeek.THURSDAY);
        scheduleLine.setStartTime("09:00");
        scheduleLine.setEndTime("20:30");
        scheduleItems.add(scheduleLine);
        schedule.setScheduleItems(scheduleItems);
        return schedule;
    }


    public OutletDeliveryRuleDTO getDeliveryRule() throws ScheduleFactory.SchedulingException {
        OutletDeliveryRuleDTO deliveryRule = new OutletDeliveryRuleDTO();
        deliveryRule.setCost(new BigDecimal(100));
        //Deprecated
        deliveryRule.setPriceTo(new BigDecimal(122L));
        deliveryRule.setPriceFreePickup(new BigDecimal(122L));
        deliveryRule.setMinDeliveryDays(2);
        deliveryRule.setMaxDeliveryDays(2);
        deliveryRule.setWorkInHoliday(true);
        //Deprecated
        deliveryRule.setDateSwitchHour(24);
        deliveryRule.setOrderBefore(24);
        deliveryRule.setUnspecifiedDeliveryInterval(false);

        deliveryRule.setShipperName("Собственная служба");
        deliveryRule.setShipperHumanReadbleId("Self");
        deliveryRule.setDeliveryServiceId(99L);
        deliveryRule.setShipperId(99L);
        return deliveryRule;
    }
}
