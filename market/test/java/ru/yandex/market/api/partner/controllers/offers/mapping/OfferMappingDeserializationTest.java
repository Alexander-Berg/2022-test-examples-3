package ru.yandex.market.api.partner.controllers.offers.mapping;

import org.junit.Test;

import ru.yandex.market.api.partner.controllers.deserialization.BaseJaxbDeserializationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class OfferMappingDeserializationTest extends BaseJaxbDeserializationTest {

    @Test
    public void deserializationJson() throws Exception {
        //language=json
        String json = "{ " +
                "  \"offerMappingEntries\": [ " +
                "    { " +
                "      \"offer\": { " +
                "        \"shopSku\": \"SKU.1\" " +
                "      }, " +
                "      \"mapping\": { " +
                "        \"marketSku\": 123456789 " +
                "      } " +
                "    }" +
                "  ]" +
                "}";

        OfferMappingEntryRequest actualJson = jsonMapper.readValue(json, OfferMappingEntryRequest.class);
        assertThat(actualJson.getEntries().get(0).getOffer().getShopSku()).isEqualTo("SKU.1");
        assertThat(actualJson.getEntries().get(0).getMapping().getMarketSku().longValue()).isEqualTo(123456789L);
    }

    @Test
    public void deserializationJsonMalformed() throws Exception {
        //language=json
        String json = "{\"offerMappingEntries\": [null]}";
        OfferMappingEntryRequest actualJson = jsonMapper.readValue(json, OfferMappingEntryRequest.class);
        assertThat(actualJson.getEntries()).isEmpty();
    }

    @Test
    public void deserializationXml() throws Exception {
        //language=xml
        String xml = "<offer-mapping-entries-update>" +
                "<offer-mapping-entries> " +
                "  <offer-mapping-entry>  " +
                "    <offer>  " +
                "      <shop-sku>SKU.1\"</shop-sku>  " +
                "    </offer>  " +
                "    <mapping>  " +
                "      <market-sku>123456789</market-sku>  " +
                "    </mapping>  " +
                "  </offer-mapping-entry>  " +
                "</offer-mapping-entries>" +
                "</offer-mapping-entries-update>";

        OfferMappingEntryRequest actualXml = xmlMapper.readValue(xml, OfferMappingEntryRequest.class);
        assertThat(actualXml.getEntries().get(0).getOffer().getShopSku()).isEqualTo("SKU.1\"");
        assertThat(actualXml.getEntries().get(0).getMapping().getMarketSku().longValue()).isEqualTo(123456789L);
    }

}
