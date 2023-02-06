package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateTransportationWaybillL4STest;

@DisplayName("Генерация транспортной накладной через клиент, через L4S")
@DatabaseSetup("/repository/partner-shipment/common.xml")
public class InternalPartnerShipmentGenerateTransportationWaybillL4STest
    extends AbstractPartnerShipmentGenerateTransportationWaybillL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/internal/partner/shipments/" + shipmentId + "/transportation-waybill";
    }
}
