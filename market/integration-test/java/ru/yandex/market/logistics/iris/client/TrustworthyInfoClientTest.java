package ru.yandex.market.logistics.iris.client;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClientImpl;
import ru.yandex.market.logistics.iris.client.http.IrisHttpMethod;
import ru.yandex.market.logistics.iris.client.model.entity.Dimensions;
import ru.yandex.market.logistics.iris.client.model.entity.TrustworthyItem;
import ru.yandex.market.logistics.iris.client.model.request.TrustworthyInfoRequest;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyInfoResponse;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyItemInfo;
import ru.yandex.market.logistics.iris.client.utils.TestHttpTemplateImpl;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFieldProvider;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class TrustworthyInfoClientTest extends AbstractClientTest {

    private final TrustworthyInfoClient client =
        new TrustworthyInfoClientImpl(new TestHttpTemplateImpl(uri, restTemplate));
    private final PredefinedFieldProvider provider =
        PredefinedFieldProvider.of(PredefinedFields.getAllPredefinedFields());

    @Test
    public void getTrustworthyInfo() {
        String path = IrisHttpMethod.TRUSTWORTHY_INFO;
        ResponseCreator responseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(extractFileContent("fixtures/trustworthy_info/trustworthy_info_response.json"));
        UriComponents trustworthyRequestUri = UriComponentsBuilder.fromHttpUrl(uri).path(path).build();
        mockServer.expect(requestTo(trustworthyRequestUri.toUriString()))
            .andExpect(
                content().json(extractFileContent("fixtures/trustworthy_info/trustworthy_info_request.json"), false))
            .andExpect(method(HttpMethod.POST))
            .andRespond(responseCreator);
        TrustworthyInfoRequest request = new TrustworthyInfoRequest(
            Lists.newArrayList(ItemIdentifier.of("1", "sku1"), ItemIdentifier.of("1", "sku3")),
            Lists.newArrayList("dimensions", "weight_gross")
        );
        TrustworthyInfoResponse response = client.getTrustworthyInfo(request);

        mockServer.verify();
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertEquals(2, response.getResult().size());
        TrustworthyItemInfo first = getBytPartnerIdAndSkuOrThrow("1", "sku1", response);
        TrustworthyItemInfo second = getBytPartnerIdAndSkuOrThrow("1", "sku3", response);
        TrustworthyItem firstTrustworthyItem = first.getTrustworthyItem();
        Dimensions expected = new Dimensions(bDcm(10.2), bDcm(12.23), bDcm(13.37));
        assertEquals(expected, firstTrustworthyItem.getDimensions());
        TrustworthyItem secondTrustworthyItem = second.getTrustworthyItem();
        assertEquals(bDcm(1212.120), secondTrustworthyItem.getWeightGross());
    }


    private TrustworthyItemInfo getBytPartnerIdAndSkuOrThrow(String id, String sku, TrustworthyInfoResponse response) {
        return response.getResult().stream()
            .filter(it -> it.getPartnerSku().equals(sku) && it.getPartnerId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format(
                "Not found item with partner id %s and sku %s", id, sku)));
    }

    private static BigDecimal bDcm(double value) {
        return BigDecimal.valueOf(value);
    }
}
