package ru.yandex.market.papi.requests.limits;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.papi.requests.limits.model.CampaignHitRateLimitsInfo;
import ru.yandex.market.papi.requests.limits.model.ResourceHitLimit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

/**
 * Тесты для {@link CampaignHitRateLimitsInfoConverter}
 */
class CampaignHitRateLimitsInfoConverterTest {

    private CampaignHitRateLimitsInfoConverter convert;

    @BeforeEach
    void setUp() {
        convert = new CampaignHitRateLimitsInfoConverter();
    }

    @Test
    @DisplayName("Проверка конвертации модели без данных (запросов)")
    void testModelWithoutData() {
        CampaignHitRateLimitsInfo model = new CampaignHitRateLimitsInfo(10L, null, null);

        Element result = convert.convert(model);
        checkRootNode(result, 0);
    }

    @Test
    @DisplayName("Проверка конвертации модели только с параллельными запросами")
    void testModelWithParallelRequestsOnly() {
        List<String> expected = List.of("req1", "req2");
        CampaignHitRateLimitsInfo model = new CampaignHitRateLimitsInfo(10L, expected, null);

        Element result = convert.convert(model);
        checkRootNode(result, 1);
        checkParallelLimitRequestsNode(result, expected);
    }

    @Test
    @DisplayName("Проверка конвертации модели только с глобальными запросами")
    void testModelWithResourceLimitRequestsOnly() {
        List<ResourceHitLimit> expected = List.of(new ResourceHitLimit("req1", 20), new ResourceHitLimit("req2", 45));
        CampaignHitRateLimitsInfo model = new CampaignHitRateLimitsInfo(10L, null, expected);

        Element result = convert.convert(model);
        checkRootNode(result, 1);
        checkResourceLimitRequestsNode(result, expected);
    }

    @Test
    @DisplayName("Проверка конвертации модели с глобальными и параллельными запросами")
    void testModelWithParallelAndResourceRequests() {
        List<ResourceHitLimit> expectedResourceHitLimitRequests = List.of(
                new ResourceHitLimit("req1", 20),
                new ResourceHitLimit("req2", 45),
                new ResourceHitLimit("test", 100)
        );
        List<String> expectedParallelRequests = List.of("req1", "req2", "p_req1");
        CampaignHitRateLimitsInfo model = new CampaignHitRateLimitsInfo(
                10L,
                expectedParallelRequests,
                expectedResourceHitLimitRequests
        );

        Element result = convert.convert(model);
        checkRootNode(result, 2);
        checkParallelLimitRequestsNode(result, expectedParallelRequests);
        checkResourceLimitRequestsNode(result, expectedResourceHitLimitRequests);
    }

    private void checkResourceLimitRequestsNode(Element root, List<ResourceHitLimit> expectedRequests) {
        Element parallelReqNode = root.getChild("resource-limit-requests-info");
        assertThat(parallelReqNode.getChildren().size(), is(expectedRequests.size()));
        List<ResourceHitLimit> actualRequests = new ArrayList<>();
        for (Object child : parallelReqNode.getChildren()) {
            actualRequests.add(new ResourceHitLimit(
                    ((Element) child).getChild("request").getText(),
                    Long.parseLong(((Element) child).getChild("total").getText())
            ));
        }
        assertThat(actualRequests, containsInAnyOrder(expectedRequests.toArray()));
    }

    private void checkParallelLimitRequestsNode(Element root, List<String> expectedRequests) {
        Element parallelReqNode = root.getChild("parallel-limit-requests");
        assertThat(parallelReqNode.getChildren().size(), is(expectedRequests.size()));
        List<String> actualRequests = new ArrayList<>();
        for (Object child : parallelReqNode.getChildren()) {
            assertThat(((Element) child).getName(), is("request"));
            actualRequests.add(((Element) child).getText());
        }
        assertThat(actualRequests, containsInAnyOrder(expectedRequests.toArray()));
    }

    private void checkRootNode(Element root, int childrenSize) {
        assertThat(root.getName(), is("campaign-hit-rate-limits-info"));
        assertThat(root.getChildren().size(), is(childrenSize));
    }
}
