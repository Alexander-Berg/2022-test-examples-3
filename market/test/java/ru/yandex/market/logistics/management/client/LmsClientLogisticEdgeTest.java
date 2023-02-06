package ru.yandex.market.logistics.management.client;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.logistic.edge.LogisticEdgeDto;
import ru.yandex.market.logistics.management.entity.request.logistic.edge.UpdateLogisticEdgesRequest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonContent;

@DisplayName("Ребра логистического графа")
class LmsClientLogisticEdgeTest extends AbstractClientTest {

    @Test
    void updateLogisticEdges() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/logistic-edges").toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonContent("data/controller/logisticEdge/request.json", false, false))
            .andRespond(withStatus(OK));

        softly.assertThatCode(
            () -> client.updateLogisticEdges(
                UpdateLogisticEdgesRequest.newBuilder()
                    .createEdges(Set.of(LogisticEdgeDto.of(1L, 2L), LogisticEdgeDto.of(2L, 3L)))
                    .deleteEdges(Set.of(LogisticEdgeDto.of(2L, 1L), LogisticEdgeDto.of(3L, 2L)))
                    .build()
            )
        )
            .doesNotThrowAnyException();
    }
}
