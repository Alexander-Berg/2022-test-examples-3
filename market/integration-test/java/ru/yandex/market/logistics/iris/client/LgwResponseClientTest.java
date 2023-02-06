package ru.yandex.market.logistics.iris.client;

import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.iris.client.api.LgwResponseClient;
import ru.yandex.market.logistics.iris.client.api.LgwResponseClientImpl;
import ru.yandex.market.logistics.iris.client.http.IrisHttpMethod;
import ru.yandex.market.logistics.iris.client.model.entity.ErrorCode;
import ru.yandex.market.logistics.iris.client.model.entity.ErrorItem;
import ru.yandex.market.logistics.iris.client.model.entity.ErrorPair;
import ru.yandex.market.logistics.iris.client.model.entity.Param;
import ru.yandex.market.logistics.iris.client.model.entity.UnitId;
import ru.yandex.market.logistics.iris.client.utils.TestHttpTemplateImpl;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class LgwResponseClientTest extends AbstractClientTest {

    private final LgwResponseClient client =
        new LgwResponseClientImpl(new TestHttpTemplateImpl(uri, restTemplate));

    @Test
    public void putReferenceItemsFromLgwSuccessfullyReceived() {
        mockServer.expect(requestTo(UriComponentsBuilder.fromHttpUrl(uri)
            .pathSegment(IrisHttpMethod.LGW_PREFIX, IrisHttpMethod.PUT_REFERENCE_ITEMS_RESULT)
            .build().toUriString()))
            .andExpect(
                content().json(
                    extractFileContent("fixtures/lgw/receive_put_reference_items_result_request.json"), false))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK));

        client.setPutReferenceItemsSuccess(createSuccessfulItems(), createErrorItems());

        mockServer.verify();
    }

    private List<UnitId> createSuccessfulItems() {
        UnitId unitId = new UnitId("created_id", 1L, "created_article");
        return singletonList(unitId);
    }

    private List<ErrorItem> createErrorItems() {
        UnitId unitId = new UnitId("error_id", 2L, "error_article");
        Param param = new Param("param_key", "param_value", "param_comment");
        ErrorPair errorPair = new ErrorPair(ErrorCode.UNKNOWN_ERROR, "error_message",
            "error_description", singletonList(param));
        ErrorItem errorItem = new ErrorItem(unitId, errorPair);
        return singletonList(errorItem);
    }
}
