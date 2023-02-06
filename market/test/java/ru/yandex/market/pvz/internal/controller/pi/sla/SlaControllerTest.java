package ru.yandex.market.pvz.internal.controller.pi.sla;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.sla.SlaLegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.sla.SlaPickupPointRepository;
import ru.yandex.market.pvz.core.domain.sla.entity.SlaLegalPartner;
import ru.yandex.market.pvz.core.domain.sla.entity.SlaPickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaControllerTest extends BaseShallowTest {

    private final TestableClock clock;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final SlaPickupPointRepository slaPickupPointRepository;
    private final SlaLegalPartnerRepository slaLegalPartnerRepository;
    private final PickupPointRepository pickupPointRepository;

    private LegalPartner partner;
    private PickupPoint pickupPoint;
    private PickupPoint pickupPoint2;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.EPOCH, clock.getZone());
        pickupPointRepository.deleteAll();

        partner = legalPartnerFactory.createLegalPartner();
        pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());
        pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.forceReceive(order.getId(), LocalDate.of(2021, 10, 2));
    }

    @Test
    void getSlaPickupPoint() throws Exception {

        slaPickupPointRepository.save(buildSlaLegalPartner(pickupPoint));
        slaPickupPointRepository.save(buildSlaLegalPartner(pickupPoint2));

        var expectedJson = String.format(getFileContent("sla/response_sla_pickup_points.json"),
                pickupPoint.getId(), pickupPoint2.getId());
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla/pickup-points")
                .contentType(MediaType.APPLICATION_JSON)
                .param("month", "10")
                .param("year", "2021")
                .param("pickupPointIds", String.format("%s,%s", pickupPoint.getId(), pickupPoint2.getId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void getSlaPickupPointNoPickupPointIds() throws Exception {

        slaPickupPointRepository.save(buildSlaLegalPartner(pickupPoint));
        slaPickupPointRepository.save(buildSlaLegalPartner(pickupPoint2));

        var expectedJson = String.format(getFileContent("sla/response_sla_pickup_points.json"),
                pickupPoint.getId(), pickupPoint2.getId());
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla/pickup-points")
                .contentType(MediaType.APPLICATION_JSON)
                .param("month", "10")
                .param("year", "2021"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void getSlaPickupPointEmpty() throws Exception {
        var expectedJson = String.format(getFileContent("sla/response_sla_pickup_points_empty.json"),
                pickupPoint2.getId(), pickupPoint.getId());
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla/pickup-points")
                .contentType(MediaType.APPLICATION_JSON)
                .param("month", "10")
                .param("year", "2021"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, true));
    }

    @Test
    void getLegalPartnerSlaInfo() throws Exception {
        var expectedJson = String.format(getFileContent("sla/response_sla_partner_info.json"),
                partner.getCreatedAt(),
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), pickupPoint.getActive(),
                pickupPoint2.getId(), pickupPoint2.getPvzMarketId(), pickupPoint2.getName(), pickupPoint2.getActive()
        );
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla/partner-info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getSlaLegalPartner() throws Exception {

        slaLegalPartnerRepository.save(buildSlaLegalPartner());

        var expectedJson = String.format(getFileContent("sla/response_sla_partner.json"),
                partner.getCreatedAt(),
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), pickupPoint.getActive(),
                pickupPoint2.getId(), pickupPoint2.getPvzMarketId(), pickupPoint2.getName(), pickupPoint2.getActive()
        );
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla")
                .contentType(MediaType.APPLICATION_JSON)
                .param("month", "10")
                .param("year", "2021"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getSlaLegalPartnerEmpty() throws Exception {
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/sla")
                .contentType(MediaType.APPLICATION_JSON)
                .param("month", "10")
                .param("year", "2021"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("sla/response_sla_partner_empty.json"), true));
    }

    private SlaPickupPoint buildSlaLegalPartner(PickupPoint pickupPoint) {
        return SlaPickupPoint.builder()
                .pickupPoint(pickupPoint)
                .reportMonth("2021-10")
                .arrivedOrdersCount(1000L)
                .acceptTimeliness(0.83)
                .storageTermTimeliness(0.0)
                .redemptionRate(0.94)
                .hasActualDebt(false)
                .hadDebtInActualMonth(true)
                .clientComplaint(0.0)
                .courierComplaint(0.64)
                .supplierComplaint(1.0)
                .totalComplaint(1.64)
                .rating(89L)
                .modificationDate(LocalDate.now())
                .build();
    }

    private SlaLegalPartner buildSlaLegalPartner() {
        return SlaLegalPartner.builder()
                .legalPartner(partner)
                .reportMonth("2021-10")
                .arrivedOrdersCount(1000L)
                .acceptTimeliness(0.83)
                .storageTermTimeliness(0.0)
                .redemptionRate(0.94)
                .hasActualDebt(false)
                .hadDebtInActualMonth(true)
                .clientComplaint(0.0)
                .courierComplaint(0.64)
                .supplierComplaint(1.0)
                .totalComplaint(1.64)
                .rating(BigDecimal.valueOf(89.0))
                .modificationDate(LocalDate.now())
                .build();
    }

}
