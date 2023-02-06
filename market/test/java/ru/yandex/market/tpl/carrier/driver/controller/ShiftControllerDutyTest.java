package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.transport.TestTransportTypeHelper.TransportTypeGenerateParam;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.EcologicalClass;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ShiftControllerDutyTest extends BaseDriverApiIntTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final TestUserHelper testUserHelper;

    private final DutyGenerator dutyGenerator;
    private final RunHelper runHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private Duty duty;

    private Transport transport;
    private Company company;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        deliveryService = testUserHelper.deliveryService(123L, Set.of(company));

        TransportTypeGenerateParam transportTypeGenerateParam = TransportTypeGenerateParam.builder()
                .name(TestUserHelper.DEFAULT_TRANSPORT_NAME)
                .capacity(BigDecimal.ZERO)
                .palletsCapacity(33)
                .company(company)
                .grossWeightTons(new BigDecimal("2.0"))
                .maxLoadOnAxleTons(new BigDecimal("4.0"))
                .maxWeightTons(new BigDecimal("8.0"))
                .lengthMeters(new BigDecimal("3"))
                .heightMeters(new BigDecimal("4"))
                .widthMeters(new BigDecimal("5"))
                .ecologicalClass(EcologicalClass.EURO5)
                .build();
        transport = testUserHelper.findOrCreateTransport(TestUserHelper.DEFAULT_TRANSPORT_NAME,
                transportTypeGenerateParam, company);
        user = testUserHelper.findOrCreateUser(UID);

        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_AHEAD_MINUTES, 12 * 60);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_BEHIND_MINUTES, -12 * 60);
    }

    @Test
    void startDutyShift() throws Exception {
        duty = dutyGenerator.generate(db -> db.deliveryServiceId(deliveryService.getId()));
        UserShift dutyUserShift = runHelper.assignUserAndTransport(duty.getRun(), user, transport);
        mockMvc.perform(post("/api/shifts/{id}/checkin", dutyUserShift.getId())
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").value(UserShiftStatus.ON_TASK.name()));

    }
}
