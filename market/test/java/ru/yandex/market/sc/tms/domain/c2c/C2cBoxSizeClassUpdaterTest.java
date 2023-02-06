package ru.yandex.market.sc.tms.domain.c2c;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClass;
import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClassCommandService;
import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClassMapper;
import ru.yandex.market.sc.core.domain.c2c.C2cBoxSizeClassRepository;
import ru.yandex.market.sc.core.external.taxi.TaxiLogPlatformClient;
import ru.yandex.market.sc.core.external.taxi.TaxiLogPlatformProperties;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.util.test.MockServerUtil;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class C2cBoxSizeClassUpdaterTest {

    private static final ClientAndServer MOCK_SERVER = MockServerUtil.INSTANCE.mockServer();

    private static final String DEFAULT_ICON_URL = "https://avatars.mds.yandex.net/get-logistics_photo/" +
            "6255253/2a000001821c794fdf27b66705a1a1b8a9d6/orig";

    private final C2cBoxSizeClassCommandService commandService;
    private final C2cBoxSizeClassRepository repository;
    private final C2cBoxSizeClassMapper mapper;

    private C2cBoxSizeClassUpdater updater;

    @BeforeEach
    void setup() {
        MOCK_SERVER.reset();
        updater = new C2cBoxSizeClassUpdater(commandService, mapper, new TaxiLogPlatformClient(
                new RestTemplate(),
                new TaxiLogPlatformProperties(null, MockServerUtil.INSTANCE.getUrl()),
                null
        ));
    }

    @Test
    void testBoxesAreSaved() {
        prepareMockServer(getRealResponse());

        assertThat(repository.findAll()).isEmpty();
        updater.update();

        assertThat(repository.findAll())
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(getDefaultSizes());
    }

    @Test
    void testBoxesAreUpdated() {
        prepareMockServer(getRealResponse());

        updater.update();
        repository.save(C2cBoxSizeClass.builder()
                .displayType("XL")
                .marketType("K-1 YMA")
                .width(9999)
                .length(999)
                .height(99)
                .weight(BigDecimal.valueOf(9000))
                .iconUrl(DEFAULT_ICON_URL)
                .build());

        updater.update();

        assertThat(repository.findAll())
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(getDefaultSizes());
    }

    private List<C2cBoxSizeClass> getDefaultSizes() {
        return List.of(
                C2cBoxSizeClass.builder()
                        .displayType("S")
                        .marketType("K-1 YMA")
                        .width(247)
                        .length(147)
                        .height(97)
                        .weight(BigDecimal.valueOf(12))
                        .iconUrl(DEFAULT_ICON_URL)
                        .build(),

                C2cBoxSizeClass.builder()
                        .displayType("M")
                        .marketType("K-6 YMF")
                        .width(347)
                        .length(247)
                        .height(147)
                        .weight(BigDecimal.valueOf(12))
                        .iconUrl(DEFAULT_ICON_URL)
                        .build(),

                C2cBoxSizeClass.builder()
                        .displayType("L")
                        .marketType("K-7 YMG")
                        .width(447)
                        .length(297)
                        .height(197)
                        .weight(BigDecimal.valueOf(12))
                        .iconUrl(DEFAULT_ICON_URL)
                        .build()
        );
    }

    private void prepareMockServer(String body) {
        MOCK_SERVER.when(
                HttpRequest.request()
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                        .withBody(body)
        );
    }

    @SneakyThrows
    private String getRealResponse() {
        return IOUtils.toString(
                this.getClass().getResourceAsStream("/c2c_box_size_logplatform_response.json"),
                StandardCharsets.UTF_8
        );
    }

}
