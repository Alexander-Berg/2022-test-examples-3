package ru.yandex.market.wms.servicebus.api.external.logistics.server.converter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.Car;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.Inbound;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.LegalEntity;
import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.shared.libs.utils.JsonUtil;

class InboundConverterTest {

    private InboundConverter converter = new InboundConverter();

    @Test
    public void conversionTest() {
        List<Person> persons = new ArrayList<>();
        persons.add(Person.builder("courier_1").build());
        persons.add(Person.builder("courier_2").build());


        Courier courier = Courier.builder().setCar(
                Car.builder("AAA").build())
                .setLegalEntity(LegalEntity.builder().setName("Courier_company").build())
                .setPhone(Phone.builder("123-456").build()).setPersons(persons).build();

        LogisticPoint logisticPoint = LogisticPoint.builder(new ResourceId("lp_yandex_id", "lp_partner_id"))
                .setLocation(Location.builder("Country", "region", "locality").build()).build();

        LogisticPoint party = LogisticPoint.builder(new ResourceId("party_yandex_id", "party_partner_id"))
                .setLocation(Location.builder("party_Country", "party_region", "party_locality").build()).build();

        ResourceId resourceId = new ResourceId("yandex_id", "partner_id");
        //YYYY-MM-DDThh:mm:ss+hh:mm/YYYY-MM-DDThh:mm:ss+hh:mm.
        Inbound inbound = Inbound.builder(resourceId, InboundType.WH2WH,
                DateTimeInterval.fromFormattedValue("2021-04-10T12:15:00+00:00/2021-04-14T16:24:00+00:00"))
                .setComment("comment")
                .setCourier(courier)
                .setLogisticPoint(logisticPoint)
                .setShipper(Party.builder(party).build()).build();

        //will convert only fields used in ru.yandex.market.wms.api.service.inbound.InboundService
        ru.yandex.market.logistic.api.model.fulfillment.Inbound convert = converter.convert(inbound);
        String result = JsonUtil.writeValueAsString(convert);

        JsonAssertUtils.assertFileNonExtensibleEquals("converter/inbound_conversion_model.json", result);
    }

}
