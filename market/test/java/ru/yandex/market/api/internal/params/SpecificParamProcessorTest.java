package ru.yandex.market.api.internal.params;

import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.URIMatcher;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.crm.util.Urls;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.function.Consumer;

/**
 * @author dimkarp93
 */
@WithMocks
public class SpecificParamProcessorTest extends UnitTestBase {
    @Mock
    private ParamProcessor processor;

    private static final SpecificParamProcessor PARAM_PROCESSOR = new SpecificParamProcessor("report_");

    @Test
    public void processSingleParam() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("report_hid", "123")
            .build();

        PARAM_PROCESSOR.processParams(request, processor);

        Mockito.verify(processor)
            .add("hid", "123");
        Mockito.verify(processor).run();
    }

    @Test
    public void processIgnoreNonReportParam() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("hid", "123")
            .param("glfilter", "123:213")
            .build();

        PARAM_PROCESSOR.processParams(request, processor);

        Mockito.verify(processor, Mockito.never())
            .add(Mockito.anyString(), Mockito.any(String.class));
    }


    @Test
    public void processSingleParamWithList() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("report_glfilter", "123:23,34")
            .build();

        PARAM_PROCESSOR.processParams(request, processor);

        Mockito.verify(processor)
            .add("glfilter", "123:23,34");
        Mockito.verify(processor).run();
    }

    @Test
    public void processMultiParam() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("report_glfilter", "123:23")
            .param("report_glfilter", "345:67")
            .build();

        PARAM_PROCESSOR.processParams(request, processor);

        Mockito.verify(processor)
            .add("glfilter", "123:23", "345:67");
        Mockito.verify(processor).run();
    }

    @Test
    public void processSeveralParams() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("report_glfilter", "123:23")
            .param("report_hid", "90")
            .build();

        PARAM_PROCESSOR.processParams(request, processor);

        Mockito.verify(processor)
            .add("glfilter", "123:23");
        Mockito.verify(processor)
            .add("hid", "90");
        Mockito.verify(processor).run();
    }

    @Test
    public void setListParams() {
        doUriTest(
            p -> p.add("hid", new String[]{"123,456"}),
            URIMatcher.hasQueryParams("hid", "123,456")
        );
    }

    @Test
    public void setSeveralParams() {
        doUriTest(
            p -> {
                p.add("hid", "123");
                p.add("glfilter", "45:67");
            },
            URIMatcher.uri(
                URIMatcher.hasQueryParams("hid", "123"),
                URIMatcher.hasQueryParams("glfilter", "45:67")
            )
        );
    }

    @Test
    public void setMultiParams() {
        doUriTest(
            p -> p.add("hid", "123", "456"),
            URIMatcher.hasQueryParams("hid", "123", "456")
        );
    }


    @Test
    public void setOverwriteParams() {
        URIBuilder builder = new URIBuilder();

        builder.addParameter("hid", "56");

        doUriTest(
            builder,
            p -> p.add("hid", "123"),
            URIMatcher.uri(
                URIMatcher.hasQueryParams("hid", "123"),
                URIMatcher.hasNoQueryParams("hid", "56")
            )
        );
    }

    @Test
    public void notClearQueryParams() {
        URIBuilder builder = new URIBuilder();

        builder.addParameter("other", "o");

        doUriTest(
            builder,
            p -> p.add("hid", "123"),
            URIMatcher.uri(
                URIMatcher.hasQueryParams("hid", "123"),
                URIMatcher.hasQueryParams("other", "o")
            )
        );
    }

    private static void doUriTest(Consumer<URIBuilderProcessor> configurer,
                                  Matcher<URI> matcher) {
        doUriTest(
            new URIBuilder(),
            configurer,
            matcher
        );
    }

    private static void doUriTest(URIBuilder builder,
                                  Consumer<URIBuilderProcessor> configurer,
                                  Matcher<URI> matcher) {
        URIBuilderProcessor processor = URIBuilderProcessor.of(builder);
        configurer.accept(processor);
        processor.run();

        Assert.assertThat(Urls.toUri(builder), matcher);
    }

}
