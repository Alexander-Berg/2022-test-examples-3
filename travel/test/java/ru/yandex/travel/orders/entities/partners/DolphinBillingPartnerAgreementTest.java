package ru.yandex.travel.orders.entities.partners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DolphinBillingPartnerAgreementTest {
    @Test
    public void testPenaltiesFieldNameMigration() throws Exception {
        ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        String oldJson = "{\"confirm_rate\": 0.13, \"refund_rates\": {\"0\": 0.8, \"5\": 0}, \"billing_client_id\": " +
                "67754227, \"billing_contract_id\": 1063211}";
        DolphinBillingPartnerAgreement a1 = mapper.readValue(oldJson, DolphinBillingPartnerAgreement.class);

        String newJson = "{\"confirm_rate\": 0.13, \"refund_penalties\": {\"0\": 0.8, \"5\": 0}, " +
                "\"billing_client_id\": 67754227, \"billing_contract_id\": 1063211}";
        DolphinBillingPartnerAgreement a2 = mapper.readValue(newJson, DolphinBillingPartnerAgreement.class);

        assertThat(a1).isEqualTo(a2);
        assertThat(a1.getRefundPenalties()).containsKeys(0, 5);
    }
}
