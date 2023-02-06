package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;
import ru.yandex.market.ir.http.Matcher;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static ru.yandex.market.api.util.functional.Functionals.compose;

/**
 * @author dimkarp93
 */
@Service
public class MatcherTestClient extends AbstractFixedConfigurationTestClient {
    public MatcherTestClient() {
        super("Matcher");
    }

    public HttpResponseConfigurer matchBatch(Matcher.OfferBatch offerBatch,
                                             String filename) {
        return match(r -> r.serverMethod("/MatchBatch"), offerBatch::toByteArray, filename);
    }

    public HttpResponseConfigurer matchString(Matcher.LocalizedText localizedText,
                                              String filename) {
        return match(r -> r.serverMethod("/MatchString"), localizedText::toByteArray, filename);
    }

    public HttpResponseConfigurer multiMatch(Matcher.Offer offer,
                                             String filename) {
        return match(r -> r.serverMethod("/MultiMatch"), offer::toByteArray, filename);
    }

    public HttpResponseConfigurer multiMatchString(Matcher.LocalizedText localizedText, String filename) {
        return match(r -> r.serverMethod("/MultiMatchString"), localizedText::toByteArray, filename);
    }

    public HttpResponseConfigurer match(Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> func,
                                        Supplier<byte[]> toByte,
                                        String filename) {
        return configure(compose(r -> matchBody(r, toByte), func)).ok().body(filename);
    }

    private HttpRequestExpectationBuilder matchBody(HttpRequestExpectationBuilder request,
                                                    Supplier<byte[]> toByte) {
        return request.post().body(b -> Arrays.equals(b, toByte.get()), "matcher post request protobuf");
    }
}
