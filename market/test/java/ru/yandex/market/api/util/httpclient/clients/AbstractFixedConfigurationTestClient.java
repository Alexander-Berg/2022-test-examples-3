package ru.yandex.market.api.util.httpclient.clients;

/**
 * Created by tesseract on 03.03.17.
 */
public class AbstractFixedConfigurationTestClient extends AbstractTestClient {

    private final String configurationName;

    protected AbstractFixedConfigurationTestClient(String configurationName) {
        this.configurationName = configurationName;
    }

    @Override
    protected String resolveConfigurationName() {
        return configurationName;
    }
}
