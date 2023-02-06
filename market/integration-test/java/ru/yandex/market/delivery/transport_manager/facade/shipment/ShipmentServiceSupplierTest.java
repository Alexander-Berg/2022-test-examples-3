package ru.yandex.market.delivery.transport_manager.facade.shipment;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderOperationTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.model.enums.ApiType;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.delivery.transport_manager.service.lgw.shipment.DsIntakeService;
import ru.yandex.market.delivery.transport_manager.service.lgw.shipment.DsSelfExportService;
import ru.yandex.market.delivery.transport_manager.service.lgw.shipment.FfIntakeService;
import ru.yandex.market.delivery.transport_manager.service.lgw.shipment.FfSelfExportService;
import ru.yandex.market.delivery.transport_manager.service.lgw.shipment.ShipmentService;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

public class ShipmentServiceSupplierTest extends AbstractContextualTest {
    @Autowired
    private DsIntakeService intakeService;
    @Autowired
    private FfIntakeService ffIntakeService;
    @Autowired
    private DsSelfExportService selfExportService;
    @Autowired
    private FfSelfExportService ffSelfExportService;
    @Autowired
    private ShipmentServiceSupplier shipmentServiceSupplier;

    @Test
    void getShipmentServiceTest_deliveryIntake_dsApi() {
        List<TransportationPartnerMethod> methods =
                List.of(new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE));
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo().setPartnerType(PartnerType.DELIVERY);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(OrderOperationTransportationType.INTAKE, methods, partnerInfo);
        softly.assertThat(shipmentService).isEqualTo(intakeService);
    }

    @Test
    void getShipmentService_WithTwoMethodsSupported_dsApi() {
        List<TransportationPartnerMethod> methods =
                List.of(
                        new TransportationPartnerMethod()
                                .setMethod(PartnerMethod.CREATE_INTAKE)
                                .setApiType(ApiType.DELIVERY),
                        new TransportationPartnerMethod()
                                .setMethod(PartnerMethod.CREATE_INTAKE)
                                .setApiType(ApiType.FULFILLMENT)
                );
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo().setPartnerType(PartnerType.DELIVERY);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(OrderOperationTransportationType.INTAKE, methods, partnerInfo);
        softly.assertThat(shipmentService).isEqualTo(intakeService);
    }

    @Test
    void getShipmentService_ByPartnerType_dsApi() {
        List<TransportationPartnerMethod> methods =
                List.of(new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE));
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo().setPartnerType(PartnerType.FULFILLMENT);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(OrderOperationTransportationType.INTAKE, methods, partnerInfo);
        softly.assertThat(shipmentService).isEqualTo(ffIntakeService);
    }

    @Test
    void getShipmentServiceTest_deliverySelfExport_dsApi() {
        List<TransportationPartnerMethod> methods = List.of(
                new TransportationPartnerMethod()
                        .setMethod(PartnerMethod.CREATE_SELF_EXPORT)
                        .setApiType(ApiType.DELIVERY)
        );
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo().setPartnerType(PartnerType.DELIVERY);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods, partnerInfo
            );
        softly.assertThat(shipmentService).isEqualTo(selfExportService);
    }

    @Test
    void getShipmentServiceTest_deliverySelfExport_noMethod_exception() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE)
        );
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo().setPartnerType(PartnerType.DELIVERY);
        softly.assertThat(
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods,
                partnerInfo
            )
        ).isNull();
    }

    @Test
    void getShipmentServiceTest_scIntake_ffApi() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE)
        );
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.SORTING_CENTER);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.INTAKE,
                methods,
                partnerInfo
            );
        softly.assertThat(shipmentService).isEqualTo(ffIntakeService);
    }

    @Test
    void getShipmentServiceTest_scSelfExport_ffApi() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod()
                    .setMethod(PartnerMethod.CREATE_SELF_EXPORT)
        );
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.SORTING_CENTER);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods,
                partnerInfo
            );
        softly.assertThat(shipmentService).isEqualTo(ffSelfExportService);
    }

    @Test
    void getShipmentServiceTest_scSelfExport_noMethod_exception() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE)
        );
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.SORTING_CENTER);
        softly.assertThat(
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods,
                partnerInfo
            )
        ).isNull();
    }

    @Test
    void getShipmentServiceTest_ffIntake_ffApi() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE)
        );
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.FULFILLMENT);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.INTAKE,
                methods,
                partnerInfo
            );
        softly.assertThat(shipmentService).isEqualTo(ffIntakeService);
    }

    @Test
    void getShipmentServiceTest_ffSelfExport_ffApi() {
        List<TransportationPartnerMethod> methods = List.of(
            new TransportationPartnerMethod()
                    .setMethod(PartnerMethod.CREATE_SELF_EXPORT)
        );
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.FULFILLMENT);
        ShipmentService shipmentService =
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods,
                partnerInfo
            );
        softly.assertThat(shipmentService).isEqualTo(ffSelfExportService);
    }

    @Test
    void getShipmentServiceTest_ffSelfExport_noMethod_exception() {
        List<TransportationPartnerMethod> methods = List.of(
                new TransportationPartnerMethod().setMethod(PartnerMethod.CREATE_INTAKE));
        TransportationPartnerInfo partnerInfo =
            new TransportationPartnerInfo().setPartnerType(PartnerType.FULFILLMENT);
        softly.assertThat(
            shipmentServiceSupplier.getShipmentService(
                OrderOperationTransportationType.SELF_EXPORT,
                methods,
                partnerInfo
            )
        ).isNull();
    }

}
