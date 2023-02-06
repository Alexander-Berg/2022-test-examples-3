package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateTransportationWaybillL4STest;

@DisplayName("Генерация транспортной накладной с фронта, через L4S")
@DatabaseSetup("/repository/partner-shipment/common.xml")
public class PartnerShipmentGenerateTransportationWaybillL4STest
    extends AbstractPartnerShipmentGenerateTransportationWaybillL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/back-office/partner/shipments/" + shipmentId + "/transportation-waybill";
    }
}
