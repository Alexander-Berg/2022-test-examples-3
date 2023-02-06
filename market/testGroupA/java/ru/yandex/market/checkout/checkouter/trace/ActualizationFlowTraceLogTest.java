package ru.yandex.market.checkout.checkouter.trace;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.FetcherToggleProperty;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ActualizationFlowTraceLogTest extends AbstractWebTestBase {

    private static final Logger TRACE = (Logger) LoggerFactory.getLogger(Loggers.REQUEST_TRACE);
    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;
    private Parameters parameters;

    @BeforeEach
    void configure() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.clear();
        inMemoryAppender.start();

        TRACE.addAppender(inMemoryAppender);
        oldLevel = TRACE.getLevel();
        TRACE.setLevel(Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        TRACE.detachAppender(inMemoryAppender);
        TRACE.setLevel(oldLevel);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHaveGeocodeRecordInLog(boolean useAsyncFlow) {
        enableAsyncFlow(useAsyncFlow);

        parameters.getGeocoderParameters().setAutoMock(true);

        var multicart = orderCreateHelper.cart(parameters);
        multicart.setPresets(List.of(
                makePreset()
        ));

        assertThat(multicart.isValid(), is(true));

        List<Map<String, String>> events = inMemoryAppender.getTskvMaps().stream()
                .filter(hasRecord(Module.GEOCODE))
                .collect(Collectors.toUnmodifiableList());

        assertThat(events, hasSize(1));

        for (Map<String, String> event : events) {
            validateGeocodeRecord(event);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHaveGeobaseRecordInLog(boolean useAsyncFlow) {
        enableAsyncFlow(useAsyncFlow);

        parameters.getGeocoderParameters().setAutoMock(true);

        var multicart = orderCreateHelper.cart(parameters);
        multicart.setPresets(List.of(
                makePreset()
        ));

        assertThat(multicart.isValid(), is(true));

        List<Map<String, String>> events = inMemoryAppender.getTskvMaps().stream()
                .filter(hasRecord(Module.GEOCODE))
                .collect(Collectors.toUnmodifiableList());

        assertThat(events, hasSize(1));

        for (Map<String, String> event : events) {
            validateGeobaseRecord(event);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHaveLoyaltyRecordInLog(boolean useAsyncFlow) {
        enableAsyncFlow(useAsyncFlow);

        parameters.getBuiltMultiCart().setPromoCode("some promocode");

        var multicart = orderCreateHelper.cart(parameters);

        assertThat(multicart.isValid(), is(true));

        List<Map<String, String>> events = inMemoryAppender.getTskvMaps().stream()
                .filter(hasRecord(Module.MARKET_LOYALTY))
                .collect(Collectors.toUnmodifiableList());

        assertThat(events, hasSize(3));

        for (Map<String, String> event : events) {
            validateLoyaltyRecord(event);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHaveReportRecordInLog(boolean useAsyncFlow) {
        enableAsyncFlow(useAsyncFlow);

        var multicart = orderCreateHelper.cart(parameters);

        assertThat(multicart.isValid(), is(true));

        List<Map<String, String>> events = inMemoryAppender.getTskvMaps().stream()
                .filter(hasRecord(Module.REPORT))
                .filter(hasPlace(MarketReportPlace.OFFER_INFO)
                        .or(hasPlace(MarketReportPlace.SHOP_INFO))
                        .or(hasPlace(MarketReportPlace.ACTUAL_DELIVERY)))
                .collect(Collectors.toUnmodifiableList());

        assertThat(events, hasSize(3));

        for (Map<String, String> event : events) {
            validateReportRecord(event);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHaveStockStorageRecordInLog(boolean useAsyncFlow) {
        enableAsyncFlow(useAsyncFlow);

        var multicart = orderCreateHelper.cart(parameters);

        assertThat(multicart.isValid(), is(true));

        List<Map<String, String>> events = inMemoryAppender.getTskvMaps().stream()
                .filter(hasRecord(Module.STOCK_STORAGE))
                .collect(Collectors.toUnmodifiableList());

        assertThat(events, hasSize(1));

        for (Map<String, String> event : events) {
            validateStockStorageRecord(event);
        }
    }

    private Predicate<Map<String, String>> hasRecord(Module module) {
        return record -> module.toString().equals(record.get("target_module"));
    }

    private Predicate<Map<String, String>> hasPlace(MarketReportPlace place) {
        return record -> record.get("query_params").contains("place=" + place.getId());
    }

    private void validateReportRecord(Map<String, String> record) {
        assertThat(record, Matchers.hasEntry("type", "OUT"));
        assertThat(record, Matchers.hasEntry(is("target_host"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("time_millis"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("retry_num"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_method"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_id"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("query_params"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("http_method"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("http_code"), notNullValue()));
    }

    private void validateStockStorageRecord(Map<String, String> record) {
        assertThat(record, Matchers.hasEntry("type", "OUT"));
        assertThat(record, Matchers.hasEntry(is("target_host"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("time_millis"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("retry_num"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_method"), is("/order/getAvailableAmounts")));
        assertThat(record, Matchers.hasEntry(is("request_id"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("query_params"), is("/order/getAvailableAmounts")));
        assertThat(record, Matchers.hasEntry(is("http_method"), is("POST")));
        assertThat(record, Matchers.hasEntry(is("http_code"), is("200")));
    }

    private void validateLoyaltyRecord(Map<String, String> record) {
        assertThat(record, Matchers.hasEntry("type", "OUT"));
        assertThat(record, Matchers.hasEntry(is("target_host"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("time_millis"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("retry_num"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_method"), Matchers.oneOf(
                "/discount/calc/v3",
                "/promocodes/v1/activate",
                "/cashback/options"
        )));
        assertThat(record, Matchers.hasEntry(is("request_id"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("query_params"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("http_method"), is("POST")));
        assertThat(record, Matchers.hasEntry(is("http_code"), is("200")));
    }

    private void validateGeocodeRecord(Map<String, String> record) {
        assertThat(record, Matchers.hasEntry("type", "OUT"));
        assertThat(record, Matchers.hasEntry(is("target_host"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("time_millis"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("retry_num"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_method"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_id"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("query_params"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("http_method"), is("GET")));
        assertThat(record, Matchers.hasEntry(is("http_code"), is("200")));
    }

    private void validateGeobaseRecord(Map<String, String> record) {
        assertThat(record, Matchers.hasEntry("type", "OUT"));
        assertThat(record, Matchers.hasEntry(is("target_host"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("time_millis"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("retry_num"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_method"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("request_id"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("query_params"), notNullValue()));
        assertThat(record, Matchers.hasEntry(is("http_method"), is("GET")));
        assertThat(record, Matchers.hasEntry(is("http_code"), is("200")));
    }

    private void enableAsyncFlow(boolean enable) {
        if (enable) {
            checkouterFeatureWriter.writeValue(CollectionFeatureType.ENABLED_FETCHERS,
                    Arrays.stream(FetcherToggleProperty.values())
                            .map(FetcherToggleProperty::getCode)
                            .collect(Collectors.toUnmodifiableSet()));
        } else {
            checkouterFeatureWriter.writeValue(CollectionFeatureType.ENABLED_FETCHERS, Set.of());
        }
    }

    private PresetInfo makePreset() {
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setBuyerAddress(AddressProvider.getAddress(a -> a.setGps("55.7558,37.6173")));
        return presetInfo;
    }
}
