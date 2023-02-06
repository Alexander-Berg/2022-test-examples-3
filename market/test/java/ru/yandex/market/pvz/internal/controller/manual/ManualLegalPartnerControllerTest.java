package ru.yandex.market.pvz.internal.controller.manual;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualLegalPartnerControllerTest extends BaseShallowTest {

    private final TestLegalPartnerFactory legalPartnerFactory;

    @Test
    void editSensitive() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartnerFactory.forceApproveWithOffer(legalPartner.getId(), LocalDate.of(2021, 12, 7));

        mockMvc.perform(
                patch("/manual/partners/" + legalPartner.getId() + "/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("partner/request_edit_sensitive.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("partner/response_edit_sensitive.json"),
                        legalPartner.getId(), legalPartner.getPartnerId(),
                        legalPartner.getOrganization().getTaxpayerNumber()), true));
    }

    @Test
    void tryToEditSensitiveWithInvalidEmail() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartnerFactory.forceApproveWithOffer(legalPartner.getId(), LocalDate.of(2021, 12, 7));

        mockMvc.perform(
                patch("/manual/partners/" + legalPartner.getId() + "/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("partner/request_edit_sensitive_with_invalid_email.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void editBalanceClientId() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        mockMvc.perform(
                        patch("/manual/partners/" + legalPartner.getId() + "/balance-client")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/request_edit_balance_client_id.json")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void editBalanceClientIdWithInvalidJson() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        mockMvc.perform(
                        patch("/manual/partners/" + legalPartner.getId() + "/balance-client")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/request_edit_invalid_balance_client_id.json")))
                .andExpect(status().is4xxClientError());
    }

}
