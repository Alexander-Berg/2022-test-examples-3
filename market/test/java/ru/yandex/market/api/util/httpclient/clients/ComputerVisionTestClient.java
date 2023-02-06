package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

import static ru.yandex.market.api.internal.computervision.ComputerVisionClientImpl.*;

/**
 * Created by tesseract on 11.03.17.
 */
@Service
public class ComputerVisionTestClient extends AbstractFixedConfigurationTestClient {

    public ComputerVisionTestClient() {
        super("ComputerVision");
    }

    public void looksas(String url, String cbird, String resource) {
        configure(x -> x.get().param("url", url).param("cbird", cbird)
                    .header(COMPUTER_VISION_SECRET_HEADER_NAME, COMPUTER_VISION_SECRET))
            .ok().body(resource);
    }
}
