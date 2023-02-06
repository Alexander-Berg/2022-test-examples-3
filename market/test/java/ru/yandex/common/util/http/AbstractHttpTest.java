package ru.yandex.common.util.http;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Ignore;
import org.junit.Rule;
import ru.yandex.common.util.URLUtils;

import javax.annotation.Nonnull;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Базовый класс для unit-тестов HTTP-компонент.
 *
 * @author Vladislav Bauer
 */
@Ignore
public abstract class AbstractHttpTest {

    static final String CONTENT = "The roof, the roof, the roof is on fire";
    static final String HANDLER = "/handler-ok";
    static final String HANDLER_FAIL_INTERNAL = "/handler-fail-internal";
    static final String HANDLER_FAIL_IO = "/handler-fail-io";


    @Rule
    public final WireMockRule wm = new WireMockRule(wireMockConfig().dynamicPort());


    @Nonnull
    String getHandlerUri(final String path) throws URISyntaxException {
        final String bindAddress = wm.getOptions().bindAddress();
        final int port = wm.port();

        return new URIBuilder()
            .setScheme(URLUtils.SCHEME_HTTP)
            .setHost(bindAddress)
            .setPort(port)
            .setPath(path)
            .build()
            .toString();
    }

}
