package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;
import ru.yandex.market.ir.http.Classifier;

import java.util.Arrays;

/**
 * @author dimkarp93
 */
@Service
public class ClassifierTestClient extends AbstractFixedConfigurationTestClient {
    public ClassifierTestClient() {
        super("Classifier");
    }

    public HttpResponseConfigurer classify(Classifier.ClassificationRequest request, String filename) {
        return classify(request).ok().body(filename);
    }

    public HttpResponseConfigurer classify(Classifier.ClassificationRequest request) {
        return configure(r -> r.serverMethod("/classify")
            .body(x -> Arrays.equals(request.toByteArray(), x), "classifier post request protobuf")
            .post());
    }
}
