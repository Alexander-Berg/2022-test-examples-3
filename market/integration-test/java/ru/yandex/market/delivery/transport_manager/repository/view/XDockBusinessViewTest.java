package ru.yandex.market.delivery.transport_manager.repository.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class XDockBusinessViewTest extends AbstractContextualTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("/repository/view/xdock_transportation_view.xml")
    void test() {
        List<XDockTransportation> transportations = jdbcTemplate.query(
            "SELECT * FROM xdock_transportations",
            new BeanPropertyRowMapper<>(XDockTransportation.class)
        );

        XDockTransportation firstParty =
            new XDockTransportation()
                .setPartnerFfId(1L)
                .setPartnerDcId(2L)
                .setDcFfId(50L)
                .setWmsId("0000000623")
                .setAxaptaId("Зп-370098316")
                .setFfwfId(1L)
                .setSupplierId("111")
                .setRealSupplierId("777")
                .setRealSupplierName("Oduvanchik")
                .setOutboundName("Yandex")
                .setOutboundInn("INN1")
                .setOutboundMarketId(147L)
                .setDcName("РЦ1")
                .setFfName("Томилино")
                .setMover("Перевозчик")
                .setCreated(LocalDateTime.parse("2021-07-23T12:00"))
                .setDcInboundPlanDate(LocalDate.of(2021, 7, 31))
                .setDcInboundFact(LocalDateTime.parse("2021-07-26T23:10:00.00"))
                .setDcOutboundFact(LocalDateTime.parse("2021-07-30T23:00:00.00"))
                .setDcOutboundPlan(LocalDateTime.parse("2021-07-30T21:00:00"))
                .setFfInboundPlanDate(LocalDate.of(2021, 7, 31))
                .setFfInboundPlan(LocalDateTime.parse("2021-07-31T21:00:00"))
                .setAxaptaMovementOrderId("ЗПер0011951")
                .setItems(30L)
                .setPallets(0L)
                .setDcId(3L)
                .setBoxes(4L)
                .setCargoTypes("[\"CHILLED_FOOD\", \"JEWELRY\", \"VALUABLE\"]")
                .setMixPalletsCountForDirection(1)
                .setDcOutboundTmId("TMU7")
                .setDcFfMoverCompleted(LocalDateTime.parse("2022-07-22T12:00:00"))
                .setFfInboundArrivedFact(LocalDateTime.parse("2022-07-22T12:05:00"))
                .setMoverCar("A123BC777");

        XDockTransportation thirdParty =
            new XDockTransportation()
                .setPartnerFfId(3L)
                .setPartnerDcId(4L)
                .setDcFfId(50L)
                .setWmsId("0000000624")
                .setFfwfId(2L)
                .setSupplierId("222")
                .setOutboundName("Romashka")
                .setOutboundInn("INN2")
                .setOutboundMarketId(134L)
                .setDcName("РЦ1")
                .setFfName("Томилино")
                .setMover("Перевозчик")
                .setCreated(LocalDateTime.parse("2021-07-23T13:00"))
                .setDcInboundPlanDate(LocalDate.of(2021, 7, 30))
                .setDcInboundFact(LocalDateTime.parse("2021-07-26T23:20:00.00"))
                .setDcOutboundFact(LocalDateTime.parse("2021-07-30T23:00:00.00"))
                .setDcOutboundPlan(LocalDateTime.parse("2021-07-30T21:00:00"))
                .setFfInboundPlanDate(LocalDate.of(2021, 7, 31))
                .setFfInboundPlan(LocalDateTime.parse("2021-07-31T21:00:00"))
                .setItems(36L)
                .setBoxes(0L)
                .setPallets(3L)
                .setDcId(3L)
                .setMixPalletsCountForDirection(1)
                .setDcOutboundTmId("TMU7")
                .setDcFfMoverCompleted(LocalDateTime.parse("2022-07-22T12:00:00"))
                .setMoverCar("A123BC777");


        XDockTransportation bbxd =
            new XDockTransportation()
                .setPartnerFfId(5L)
                .setPartnerDcId(null)
                .setDcFfId(60L)
                .setWmsId("0000000625")
                .setFfwfId(6L)
                .setSupplierId("333")
                .setOutboundName("Yandex")
                .setOutboundInn("INN1")
                .setOutboundMarketId(147L)
                .setDcName("РЦ1")
                .setFfName("Томилино")
                .setMover("Перевозчик")
                .setCreated(LocalDateTime.parse("2021-07-23T13:00"))
                .setDcInboundPlanDate(null)
                .setDcInboundFact(LocalDateTime.parse("2021-07-23T18:00:00.00"))
                .setDcOutboundFact(LocalDateTime.parse("2021-08-01T23:00:00.00"))
                .setDcOutboundPlan(LocalDateTime.parse("2021-08-01T22:00:00"))
                .setFfInboundPlanDate(LocalDate.of(2021, 8, 2))
                .setFfInboundPlan(LocalDateTime.parse("2021-08-02T10:00:00"))
                .setItems(36L)
                .setBoxes(0L)
                .setPallets(0L)
                .setDcId(3L)
                .setTransportationSubtype("BREAK_BULK_XDOCK")
                .setMixPalletsCountForDirection(2)
                .setFfInboundSlot(1000L)
                .setDcOutboundSlot(2000L)
                .setMainSupplyWmsId("000000001")
                .setDcWmsId("000000002")
                .setTargetWarehouseWmsId("000000003")
                .setDcOutboundTmId("TMU70");

        assertContainsExactlyInAnyOrder(transportations, firstParty, thirdParty, bbxd);

    }

    @Data
    @Accessors(chain = true)
    private static class XDockTransportation {
        private Long partnerFfId;
        private Long partnerDcId;
        private Long dcFfId;
        private String wmsId;
        private String axaptaId;
        private Long ffwfId;
        private String outboundName;
        private String outboundInn;
        private Long outboundMarketId;
        private String dcName;
        private String ffName;
        private String mover;
        private LocalDateTime created;
        private LocalDate dcInboundPlanDate;
        private LocalDateTime dcInboundFact;
        private LocalDateTime dcOutboundFact;
        private LocalDateTime dcOutboundPlan;
        private LocalDate ffInboundPlanDate;
        private LocalDateTime ffInboundPlan;
        private Long items;
        private Long pallets;
        private Long boxes;
        private String supplierId;
        private String realSupplierId;
        private String realSupplierName;
        private String axaptaMovementOrderId;
        private Long dcId;
        private String cargoTypes;
        private String transportationSubtype;
        private int mixPalletsCountForDirection;
        private Long dcOutboundSlot;
        private Long ffInboundSlot;
        private String mainSupplyWmsId;
        private String dcWmsId;
        private String dcOutboundTmId;
        private String targetWarehouseWmsId;
        private LocalDateTime dcFfMoverCompleted;
        private LocalDateTime ffInboundArrivedFact;
        private String moverCar;
    }
}



