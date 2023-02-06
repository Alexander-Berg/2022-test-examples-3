package ru.yandex.market.logistics.management.controller;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@DatabaseSetup("/data/controller/partnerCustomerInfo/prepare_data.xml")
class PartnerCustomerInfoControllerTest extends AbstractContextualTest {

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partnerCustomerInfo/partner_with_info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void setCustomerInfoSuccessfully() throws Exception {
        setCustomerInfoToPartner(150, 10)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/partnerCustomerInfo/partner_with_info_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partnerCustomerInfo/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void setCustomerInfoToNonExistingPartner() throws Exception {
        setCustomerInfoToPartner(123456789, 10)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=123456789"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partnerCustomerInfo/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void setNonExistingCustomerInfoToPartner() throws Exception {
        setCustomerInfoToPartner(150, 1345332)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Контактной информации с id 1345332 не найдено"));
    }

    @Test
    @DatabaseSetup("/data/controller/partnerCustomerInfo/another_partner_with_info.xml")
    void changePartnerCustomerInfo() throws Exception {
        // Изначально партнер id=151 с PartnerCustomerInfo id=11
        setCustomerInfoToPartner(151, 10)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/partnerCustomerInfo/partner_with_changed_info_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Nonnull
    private ResultActions setCustomerInfoToPartner(long partnerId, long customerInfoId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
            .put("/externalApi/partners/" + partnerId + "/customerInfo/" + customerInfoId)
            .contentType(MediaType.APPLICATION_JSON));
    }
}
