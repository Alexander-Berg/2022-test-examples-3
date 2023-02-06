package ru.yandex.market.checkout.checkouter.yauslugi.rest;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotRequest;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotsResponse;
import ru.yandex.market.checkout.util.yauslugi.YaUslugiServiceTestConfigurer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author zagidullinri
 * @date 31.05.2022
 */
public class YaUslugiClientLoggingTest extends AbstractServicesTestBase {

    private static final Logger KV_LOG = (Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG);

    @Autowired
    private WireMockServer yaUslugiMock;
    @Autowired
    private YaUslugiApi yaUslugiClient;
    @Autowired
    private YaUslugiServiceTestConfigurer yaUslugiServiceTestConfigurer;
    private final InMemoryAppender appender = new InMemoryAppender();
    private Level oldLevel;

    @BeforeEach
    void init() {
        KV_LOG.addAppender(appender);
        oldLevel = KV_LOG.getLevel();
        KV_LOG.setLevel(Level.INFO);
        appender.clear();
        appender.start();

}

    @AfterEach
    public void tearDown() {
        KV_LOG.detachAppender(appender);
        KV_LOG.setLevel(oldLevel);
    }


    @AfterEach
    public void resetMocks() {
        yaUslugiMock.resetAll();
    }

    @Test
    public void shouldWriteTimeslotsSuccessfullyLoaded() {
        var response = new YaServiceTimeSlotsResponse();
        response.setTimeslots(List.of(new YaServiceTimeSlotDto()));
        yaUslugiServiceTestConfigurer.mockGetTimeslots(response);

        yaUslugiClient.timeslots(new YaServiceTimeSlotRequest());

        assertKvLogExists("timeslots_successfully_loaded");
    }

    @Test
    public void shouldWriteTimeslotsEmptyResult() {
        var response = new YaServiceTimeSlotsResponse();
        response.setTimeslots(List.of());
        yaUslugiServiceTestConfigurer.mockGetTimeslots(response);

        yaUslugiClient.timeslots(new YaServiceTimeSlotRequest());

        assertKvLogExists("timeslots_empty_result");
    }

    @Test
    public void shouldWriteTimeslotsLoadingFailed() {
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/get_cached_slots"))
                        .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
        );

        assertThrows(Exception.class, () -> yaUslugiClient.timeslots(new YaServiceTimeSlotRequest()));

        assertKvLogExists("timeslots_loading_failed");
    }

    private void assertKvLogExists(String subkey) {
        assertNotNull(appender.getRaw().stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains(
                        "ya_uslugi_client\t" + subkey + "\t1.0"
                ))
                .findFirst()
                .orElse(null));
    }
}
