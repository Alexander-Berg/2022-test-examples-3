package ru.yandex.market.delivery.rupostintegrationapp.service;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.entities.request.abstraction.AbstractRequest;
import ru.yandex.market.delivery.entities.request.abstraction.AbstractRequestContent;
import ru.yandex.market.delivery.entities.response.abstraction.AbstractResponseContent;
import ru.yandex.market.delivery.entities.response.ds.DsResponse;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.abstraction.AbstractService;

class ServiceProcessorTest extends BaseTest {
    private static final String REQUEST_TYPE = "testService";
    private static final String STRING_CONTENT = "filled_string";

    private ServiceProcessor processor = new ServiceProcessor();

    @Test
    void testProcessorExecution() {
        TestDsRequest request = new TestDsRequest();
        request.setRequestContent(new TestDsRequestContent());
        request.getRequestContent().setType(REQUEST_TYPE);

        DsResponse response = new TestService().execute(processor, request);
        TestDsResponseContent content = (TestDsResponseContent) response.getResponseContent();

        softly.assertThat(content.getType())
            .as("Response type was not filled correctly")
            .isEqualTo(REQUEST_TYPE);

        softly.assertThat(content.someString)
            .as("Service job method was not processed correctly")
            .isEqualTo(STRING_CONTENT);
    }

    static class TestDsRequestContent extends AbstractRequestContent {
    }

    static class TestDsRequest extends AbstractRequest<TestDsRequestContent> {
    }

    static class TestDsResponseContent extends AbstractResponseContent {
        String someString;
    }

    static class TestService extends AbstractService<TestDsRequest, TestDsResponseContent> {
        @Override
        public TestDsResponseContent doJob(TestDsRequest request) {
            TestDsResponseContent content = new TestDsResponseContent();
            content.someString = STRING_CONTENT;

            return content;
        }
    }
}
