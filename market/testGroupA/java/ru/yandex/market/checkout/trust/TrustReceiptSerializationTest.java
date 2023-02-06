package ru.yandex.market.checkout.trust;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.trust.client.TrustReceipt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class TrustReceiptSerializationTest {

    private final String jsonString = "{\n" +
            "  \"id\": 11109,\n" +
            "  \"dt\": \"2018-04-11 01:05:00\",\n" +
            "  \"fp\": 2524659315,\n" +
            "  \"document_index\": 45,\n" +
            "  \"shift_number\": 73,\n" +
            "  \"ofd_ticket_received\": false,\n" +
            "  \"receipt_content\": {\n" +
            "    \"firm_inn\": \"7736207543\",\n" +
            "    \"taxation_type\": \"OSN\",\n" +
            "    \"agent_type\": \"agent\",\n" +
            "    \"client_email_or_phone\": \"sometestuser@yandex-team.ru\",\n" +
            "    \"firm_reply_email\": \"mailer@market.yandex.ru\",\n" +
            "    \"firm_url\": \"market.yandex.ru\",\n" +
            "    \"supplier_phone\": \"+70987654321\",\n" +
            "    \"receipt_type\": \"return_income\",\n" +
            "    \"payments\": [\n" +
            "      {\n" +
            "        \"amount\": \"2890.00\",\n" +
            "        \"payment_type\": \"card\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"rows\": [\n" +
            "      {\n" +
            "        \"payment_type_type\": \"prepayment\",\n" +
            "        \"price\": \"2790.00\",\n" +
            "        \"qty\": \"1.00\",\n" +
            "        \"tax_type\": \"nds_18_118\",\n" +
            "        \"text\": \"Пухлый Усатый Тюлень\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"payment_type_type\": \"prepayment\",\n" +
            "        \"price\": \"100.00\",\n" +
            "        \"qty\": \"1.00\",\n" +
            "        \"tax_type\": \"nds_18_118\",\n" +
            "        \"text\": \"Доставка\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"additional_user_requisite\": {\n" +
            "      \"name\": \"trust_purchase_token\",\n" +
            "      \"value\": \"dc527a4253a3c09c069b1899161eaac3\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"receipt_calculated_content\": {\n" +
            "    \"total\": \"2890.00\",\n" +
            "    \"money_received_total\": \"2890.00\",\n" +
            "    \"rows\": [\n" +
            "      {\n" +
            "        \"qty\": \"1.00\",\n" +
            "        \"price\": \"2790.00\",\n" +
            "        \"payment_type_type\": \"prepayment\",\n" +
            "        \"amount\": \"2790.0000\",\n" +
            "        \"tax_pct\": \"18.00\",\n" +
            "        \"tax_amount\": \"425.59\",\n" +
            "        \"tax_type\": \"nds_18_118\",\n" +
            "        \"text\": \"Пухлый Усатый Тюлень\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"qty\": \"1.00\",\n" +
            "        \"price\": \"100.00\",\n" +
            "        \"payment_type_type\": \"prepayment\",\n" +
            "        \"amount\": \"100.0000\",\n" +
            "        \"tax_pct\": \"18.00\",\n" +
            "        \"tax_amount\": \"15.25\",\n" +
            "        \"tax_type\": \"nds_18_118\",\n" +
            "        \"text\": \"Доставка\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"tax_totals\": [\n" +
            "      {\n" +
            "        \"tax_type\": \"nds_18_118\",\n" +
            "        \"tax_pct\": \"18.00\",\n" +
            "        \"tax_amount\": \"440.84\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"totals\": [\n" +
            "      {\n" +
            "        \"payment_type\": \"card\",\n" +
            "        \"amount\": \"2890.00\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"firm_reply_email\": \"mailer@market.yandex.ru\",\n" +
            "    \"firm_url\": \"market.yandex.ru\",\n" +
            "    \"qr\": \"t=20180411T0105&s=2890.00&fn=9999078900011955&i=11109&fp=2524659315&n=2\"\n" +
            "  },\n" +
            "  \"firm\": {\n" +
            "    \"inn\": \"7736207543\",\n" +
            "    \"name\": \"ООО \\\"ЯНДЕКС\\\"\",\n" +
            "    \"reply_email\": \"mailer@market.yandex.ru\"\n" +
            "  },\n" +
            "  \"fn\": {\n" +
            "    \"sn\": \"9999078900011955\",\n" +
            "    \"model\": \"ФН-1\"\n" +
            "  },\n" +
            "  \"ofd\": {\n" +
            "    \"inn\": \"7704358518\",\n" +
            "    \"name\": \"ОБЩЕСТВО СОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \\\"ЯНДЕКС.ОФД\\\"\",\n" +
            "    \"check_url\": \"nalog.ru\"\n" +
            "  },\n" +
            "  \"kkt\": {\n" +
            "    \"sn\": \"00000000381001942057\",\n" +
            "    \"rn\": \"7060088548043365\",\n" +
            "    \"automatic_machine_number\": \"6660666\"\n" +
            "  },\n" +
            "  \"location\": {\n" +
            "    \"address\": \"119021, Россия, г. Москва, ул. Льва Толстого, д. 16\",\n" +
            "    \"description\": \"market.yandex.ru\"\n" +
            "  },\n" +
            "  \"origin\": \"online\",\n" +
            "  \"check_url\": \"https://greed-ts.paysys.yandex" +
            ".net:8019/?n=11109&fn=9999078900011955&fpd=2524659315\",\n" +
            "  \"localzone\": \"Europe/Moscow\"\n" +
            "}";

    @Test
    public void deserializeTrustReceipt() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TrustReceipt receipt = mapper.readValue(jsonString, TrustReceipt.class);

        assertThat(receipt.getReceiptContent(), notNullValue());
        assertThat(receipt.getReceiptContent().getRows(), hasSize(2));
        assertThat(receipt.getReceiptContent().getRows().get(0).getPaymentType(), equalTo("prepayment"));
        assertThat(receipt.getReceiptContent().getRows().get(0).getPrice(), equalTo(BigDecimal.valueOf(279000, 2)));
        assertThat(receipt.getReceiptContent().getRows().get(0).getQuantity(), equalTo(BigDecimal.valueOf(100, 2)));
        assertThat(receipt.getReceiptContent().getRows().get(0).getVatType(), equalTo(VatType.VAT_18_118.getTrustId()));
        assertThat(receipt.getReceiptContent().getRows().get(0).getText(), equalTo("Пухлый Усатый Тюлень"));
    }
}
