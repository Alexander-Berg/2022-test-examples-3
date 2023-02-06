package ru.yandex.market.pvz.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualCrmControllerTest extends BaseShallowTest {

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestCrmPrePickupPointFactory prePickupPointFactory;

    @ParameterizedTest
    @EnumSource(value = PreLegalPartnerApproveStatus.class, names = {"REJECTED", "CHECKING"})
    void sendPreLegalPartnerToCheck(PreLegalPartnerApproveStatus status) throws Exception {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        preLegalPartnerFactory.forceChangeStatus(preLegalPartner.getId(), status);

        mockMvc.perform(post("/manual/crm/send-to-check?entityType=PRE_LEGAL_PARTNER&entityId="
                        + preLegalPartner.getId()))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @EnumSource(value = PreLegalPartnerApproveStatus.class, names = {"REJECTED", "CHECKING"},
            mode = EnumSource.Mode.EXCLUDE)
    void sendPreLegalPartnerToCheckInWrongStatus(PreLegalPartnerApproveStatus status) throws Exception {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        preLegalPartnerFactory.forceChangeStatus(preLegalPartner.getId(), status);

        mockMvc.perform(post("/manual/crm/send-to-check?entityType=PRE_LEGAL_PARTNER&entityId="
                        + preLegalPartner.getId()))
                .andExpect(status().is4xxClientError());
    }

    @ParameterizedTest
    @EnumSource(value = PrePickupPointApproveStatus.class, names = {"REJECTED", "CHECKING"})
    void sendPrePickupPointToCheck(PrePickupPointApproveStatus status) throws Exception {
        var prePickupPoint = prePickupPointFactory.create();

        prePickupPointFactory.forceChangeStatus(prePickupPoint.getId(), status);

        mockMvc.perform(post("/manual/crm/send-to-check?entityType=PRE_PICKUP_POINT&entityId="
                        + prePickupPoint.getId()))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @EnumSource(value = PrePickupPointApproveStatus.class, names = {"REJECTED", "CHECKING"},
            mode = EnumSource.Mode.EXCLUDE)
    void sendPrePickupPointToCheckInWrongStatus(PrePickupPointApproveStatus status) throws Exception {
        var prePickupPoint = prePickupPointFactory.create();

        prePickupPointFactory.forceChangeStatus(prePickupPoint.getId(), status);

        mockMvc.perform(post("/manual/crm/send-to-check?entityType=PRE_PICKUP_POINT&entityId="
                        + prePickupPoint.getId()))
                .andExpect(status().is4xxClientError());
    }

}
