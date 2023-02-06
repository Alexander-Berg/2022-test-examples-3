package ru.yandex.market.logistic.api.model.fulfillment;

import java.util.List;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class InboundParsingTest extends ParsingTest<Inbound> {

    public InboundParsingTest() {
        super(Inbound.class, "fixture/entities/inbound.xml");
    }

    @Override
    protected void performAdditionalAssertions(Inbound inbound) {
        assertions().assertThat(inbound.getInboundId())
            .as("Asserting inboundId instance")
            .isInstanceOf(ResourceId.class);
        final Consignment consignment = inbound.getConsignments().get(0);
        assertions().assertThat(consignment)
            .as("Asserting first instance of consignments")
            .isInstanceOf(Consignment.class);

        final Item item = consignment.getItem();
        assertions().assertThat(item)
            .as("Asserting first item is not null")
            .isNotNull();
        assertions().assertThat(item.getArticle())
            .as("Asserting first item article")
            .isEqualTo("Article");

        List<String> vendorCodes = item.getVendorCodes();
        assertions().assertThat(vendorCodes)
            .as("Asserting vendor codes are not null")
            .isNotNull();
        assertions().assertThat(vendorCodes)
            .as("Asserting vendor codes count is 3")
            .hasSize(3);
        assertions().assertThat(vendorCodes.get(0))
            .as("Asserting the first vendor code is '123'")
            .isEqualTo("123");
        assertions().assertThat(vendorCodes.get(1))
            .as("Asserting the second vendor code is '456'")
            .isEqualTo("456");
        assertions().assertThat(vendorCodes.get(2))
            .as("Asserting the third vendor code is 'ABC'")
            .isEqualTo("ABC");

        final List<Barcode> barcodes = item.getBarcodes();
        assertions().assertThat(barcodes)
            .as("Asserting barcodes are not null")
            .isNotNull();
        assertions().assertThat(barcodes.size())
            .as("Asserting barcodes count is 1")
            .isEqualTo(1);
        assertions().assertThat(barcodes.get(0).getCode())
            .as("Asserting barcode code is 'code'")
            .isEqualTo("code");
        assertions().assertThat(barcodes.get(0).getType())
            .as("Asserting barcode type is 'type'")
            .isEqualTo("type");

        List<Service> inboundServices = item.getInboundServices();
        assertions().assertThat(inboundServices)
            .as("Asserting inbound services are not null")
            .isNotNull();
        assertions().assertThat(inboundServices)
            .as("Asserting inbound services count is 1")
            .hasSize(3);
        assertions().assertThat(inboundServices.get(0).getCode())
            .as("Asserting the inbound service type is SORT")
            .isEqualTo(ServiceType.SORT);
        assertions().assertThat(inboundServices.get(1).getCode())
            .as("Asserting unknown service has been parsed correctly")
            .isEqualTo(ServiceType.UNKNOWN);
        assertions().assertThat(inboundServices.get(2).getCode())
            .as("Asserting code from enum is parsed ignoring case")
            .isEqualTo(ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY);
        assertions().assertThat(inboundServices.get(0).getName())
            .as("Asserting the inbound service name is 'Lunapark with BlackJack and whores'")
            .isEqualTo("Lunapark with BlackJack and whores");
        assertions().assertThat(inboundServices.get(0).getDescription())
            .as("Asserting the inbound service description is 'Short description for current service'")
            .isEqualTo("Short description for current service");
        assertions().assertThat(inboundServices.get(0).getIsOptional())
            .as("Asserting the inbound service is not optional")
            .isFalse();

        assertions().assertThat(inbound.getWarehouse())
            .as("Asserting warehouse instance")
            .isInstanceOf(Warehouse.class);
        assertions().assertThat(inbound.getCourier())
            .as("Asserting courier instance")
            .isInstanceOf(Courier.class);
        assertions().assertThat(inbound.getInterval().getFormatted())
            .as("Asserting interval value")
            .isEqualTo("2014-01-02T11:00:00+03:00/2015-02-12T12:00:05+03:00");
        assertions().assertThat(inbound.getComment())
            .as("Asserting comment value")
            .isEqualTo("wat");
    }
}
