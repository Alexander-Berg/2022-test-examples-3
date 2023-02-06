package ru.yandex.market.api.partner.controllers.outlet.deserialization;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.api.partner.controllers.deserialization.BaseJaxbDeserializationTest;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletAddressDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDeliveryRuleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletScheduleItemDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletTypeDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletWorkingScheduleDTO;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.schedule.ScheduleFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Проверяет десериализацию входного запроса {@link OutletDTO}в ручки PUT и POST /outlets.
 */
public class OutletDTODeserializationTest extends BaseJaxbDeserializationTest {
    private static final String EXTENSION_DELIMITER = "_";
    private static final String UTF8 = "UTF-8";

    @Test
    public void checkOutletRequestDeserialization() throws JSONException, SAXException, IOException, ScheduleFactory.SchedulingException {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        testJsonDeserialization(outletRequestExpected, getContentAsString("outletRequest.json"));
        testXmlDeserialization(outletRequestExpected, getContentAsString("outletRequest.xml"));
    }

    /**
     * Проверяем десериализацию json с неизвестным типом {@link OutletVisibility}
     */
    @Test
    public void checkJsonOutletCorruptedVisibilityInfoDeserialization() throws SAXException, ScheduleFactory.SchedulingException, JSONException, IOException {
        checkWrongJsonDeserialization("outletRequestCorruptedVisibility.json");
    }

    /**
     * Проверяем десериализацию json с неизвестным типом {@link OutletType}
     */
    @Test
    public void checkJsonWrongOutletTypeDeserialization() throws JSONException, SAXException, IOException, ScheduleFactory.SchedulingException {
        checkWrongJsonDeserialization("outletRequestCorruptedOutletType.json");
    }


    private void checkWrongJsonDeserialization(String fileName) throws IOException, JSONException, ScheduleFactory.SchedulingException, SAXException {
        try {
            testJsonDeserialization(getExpectedOutletDeliveryRuleRequest(), getContentAsString(fileName));
            Assert.fail();
        } catch (RuntimeException e) {
            Throwable ez = e.getCause();
            assertTrue(ez.getClass().equals(InvalidFormatException.class));
        }
    }

    /**
     * Проверяем десериализацию xml с неизвестным типом {@link OutletVisibility}
     */
    @Test
    public void checkXmlOutletCorruptedVisibilityInfoDeserialization() throws JSONException, SAXException, IOException, ScheduleFactory.SchedulingException {
        checkWrongXmDeserialization("outletRequestCorruptedVisibility.xml");
    }

    /**
     * Проверяем десериализацию xml с неизвестным типом {@link OutletType}
     */
    @Test
    public void checkXmlWrongOutletTypeInfoDeserialization() throws JSONException, SAXException, IOException, ScheduleFactory.SchedulingException {
        checkWrongXmDeserialization("outletRequestCorruptedOutletType.xml");
    }

    private void checkWrongXmDeserialization(String fileName) throws IOException, JSONException, ScheduleFactory.SchedulingException, SAXException {
        try {
            testXmlDeserialization(getExpectedOutletDeliveryRuleRequest(), getContentAsString(fileName));
            fail();
        } catch (RuntimeException e) {
            Throwable ez = e.getCause();
            assertTrue(ez.getClass().equals(InvalidFormatException.class));
        }
    }


    public OutletDTO getExpectedOutletRequestWithoutDelivery() throws ScheduleFactory.SchedulingException {
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
        outletWorkingScheduleDTO.setWorkInHoliday(false);
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

        outletRequestExpected.setPhones(CollectionFactory.list("+ 7 (345) 919-191991",
                "+ 7 (543) 123-33123111"));
        outletRequestExpected.setCoords("56.156131, 35.802831");

        return outletRequestExpected;

    }

    public OutletDTO getExpectedOutletDeliveryRuleRequest() throws ScheduleFactory.SchedulingException {
        OutletDTO outletInfoExpected = getExpectedOutletRequestWithoutDelivery();
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

    public String getContentAsString(String name) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(this.getClass().getSimpleName() + EXTENSION_DELIMITER + name), UTF8);
    }
}
