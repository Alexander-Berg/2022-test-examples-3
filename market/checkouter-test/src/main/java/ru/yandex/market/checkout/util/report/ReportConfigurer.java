package ru.yandex.market.checkout.util.report;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.util.StreamUtils;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.ShowUrlsParam;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.REPORT_EXPERIMENTS_PARAM;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 07/07/2017
 */
public class ReportConfigurer {

    private final WireMockServer reportMock;
    private final WireMockServer fallbackReportMock;

    private final ReportResponseGenerator generator = new ReportResponseGenerator();

    public ReportConfigurer(WireMockServer reportMock, WireMockServer fallbackReportMock) {
        this.reportMock = reportMock;
        this.fallbackReportMock = fallbackReportMock;
    }

    private List<WireMockServer> getReportMocks() {
        return Arrays.asList(reportMock, fallbackReportMock);
    }

    @Nonnull
    public List<LoggedRequest> findPlaceCall(@Nonnull MarketReportPlace place) {
        return getReportMocks().stream()
                .flatMap(mock -> StreamUtils.stream(mock.findAll(
                        getRequestedFor(anyUrl())
                                .withQueryParam("place", equalTo(place.getId()))
                ))).collect(Collectors.toUnmodifiableList());
    }

    public void mockReportPlace(MarketReportPlace place, ReportGeneratorParameters parameters) {
        mockReportPlace(
                place,
                parameters,
                b -> {
                }
        );
    }

    public void mockReportPlace(
            MarketReportPlace place,
            ReportGeneratorParameters parameters,
            Consumer<MappingBuilder> mappingBuilderModifier
    ) {
        Order order = parameters.getOrder();
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(place.getId()))
                .withQueryParam("regional-delivery", absent());

        configureAdditionalUrlMapping(place, builder, order, parameters);

        mappingBuilderModifier.accept(builder);
        parameters.getMappingBuilderModifier(place).orElse(
                b -> b.willReturn(
                        new ResponseDefinitionBuilder().withBody(generateResponse(parameters, place))
                )
        ).accept(builder);
        getReportMocks()
                .forEach(mock -> mock.stubFor(
                        builder
                        )
                );

        mockRegionalDeliveryIfNecessary(place, parameters, order);
    }

    public void mockReportPlaceError(
            @Nonnull MarketReportPlace place,
            @Nonnull ReportGeneratorParameters parameters,
            @Nonnull Fault fault
    ) {
        Order order = parameters.getOrder();
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(place.getId()))
                .withQueryParam("regional-delivery", absent());

        configureAdditionalUrlMapping(place, builder, order, parameters);

        builder.willReturn(new ResponseDefinitionBuilder().withFault(fault));

        getReportMocks()
                .forEach(mock -> mock.stubFor(builder));

        mockRegionalDeliveryIfNecessary(place, parameters, order);
    }

    private void mockRegionalDeliveryIfNecessary(MarketReportPlace place, ReportGeneratorParameters parameters,
                                                 Order order) {
        MappingBuilder regionalDeliveryBuilder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(place.getId()))
                .withQueryParam("regional-delivery", equalTo("1"));

        configureAdditionalUrlMapping(place, regionalDeliveryBuilder, order, parameters);

        getReportMocks().forEach(mock ->
                mock.stubFor(
                        regionalDeliveryBuilder.willReturn(
                                new ResponseDefinitionBuilder().withBody(generateResponse(parameters, place))
                        )
                )
        );
    }

    public void mockOutlets() throws IOException {
        mockOutlets(null);
    }

    public void mockOutlets(@Nullable ReportGeneratorParameters parameters) throws IOException {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.OUTLETS.getId()));
        if (parameters != null) {
            addRearrFactors(builder, parameters);
        }
        for (WireMockServer mock : getReportMocks()) {
            mock.stubFor(builder.willReturn(aResponse()
                    .withBody(IOUtils.toString(
                            ReportConfigurer.class.getResource("/files/report/outlets.xml"),
                            StandardCharsets.UTF_8))
                    .withTransformers("response-template")
            ));
        }
    }

    public void mockActualDeliveryFromString(String reportResponse) {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.ACTUAL_DELIVERY.getId()));
        for (WireMockServer mock : getReportMocks()) {
            mock.stubFor(builder.willReturn(aResponse()
                    .withBody(reportResponse)
                    .withTransformers("response-template")
            ));
        }
    }

    public void mockCurrencyConvert(ReportGeneratorParameters parameters,
                                    List<DeliveryResponse> pushApiDeliveryResponses,
                                    Currency buyerCurrency) {
        if (parameters.getDeliveryCurrency() == parameters.getShopCurrency()) {
            return;
        }

        pushApiDeliveryResponses.stream()
                .map(DeliveryResponse::getPrice)
                .distinct()
                .forEach(price -> {
                    Currency deliveryCurrency = parameters.getDeliveryCurrency();
                    Currency shopCurrency = parameters.getShopCurrency();
                    BigDecimal shopRate = parameters.getCurrencyRates().get(Pair.of(deliveryCurrency, shopCurrency));
                    if (shopRate == null) {
                        throw new IllegalArgumentException("shopRate was not found: " + deliveryCurrency + "->" +
                                shopCurrency);
                    }
                    BigDecimal buyerRate = parameters.getCurrencyRates().get(Pair.of(deliveryCurrency, buyerCurrency));
                    if (buyerRate == null) {
                        throw new IllegalArgumentException("shopRate was not found: " + deliveryCurrency + "->" +
                                buyerCurrency);
                    }

                    mockCurrencyRate(price, deliveryCurrency, shopCurrency, shopRate);
                    mockCurrencyRate(price, deliveryCurrency, buyerCurrency, buyerRate);
                });

    }

    public void mockCurrencyConvert(ReportGeneratorParameters parameters,
                                    ActualDelivery actualDelivery,
                                    Currency buyerCurrency) {
        Currency shopCurrency = parameters.getShopCurrency();

        if (actualDelivery == null) {
            return;
        }
        StreamUtils.stream(actualDelivery.getResults())
                .flatMap(r -> Stream.of(r.getPickup(), r.getDelivery(), r.getPost())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream))
                .forEach(ldo -> {
                    if (ldo.getCurrency() == null) {
                        return;
                    }
                    if (buyerCurrency != null) {
                        mockConvert(parameters, buyerCurrency, ldo);
                    }
                    if (shopCurrency != null) {
                        mockConvert(parameters, shopCurrency, ldo);
                    }
                });
    }

    private void mockConvert(ReportGeneratorParameters parameters, Currency targetCurrency, LocalDeliveryOption ldo) {
        if (targetCurrency == ldo.getCurrency()) {
            return;
        }

        BigDecimal targetRate = parameters.getCurrencyRates().get(Pair.of(ldo.getCurrency(), targetCurrency));
        if (targetRate == null) {
            throw new IllegalArgumentException("targetRate was not found: " + ldo.getCurrency() + "->" +
                    targetCurrency);
        }
        mockCurrencyRate(ldo.getPrice(), ldo.getCurrency(), targetCurrency, targetRate);
    }


    private void mockCurrencyRate(BigDecimal price, Currency currencyFrom, Currency currencyTo, BigDecimal rate) {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.CURRENCY_CONVERT.getId()))
                .withQueryParam("currency-from", equalTo(currencyFrom.name()))
                .withQueryParam("currency-to", equalTo(currencyTo.name()))
                .withQueryParam("currency-value", equalTo(price.toString()));


        JSONObject result = new JSONObject();
        result.put("currencyFrom", currencyFrom);
        result.put("currencyTo", currencyTo);
        result.put("value", price);
        result.put("renderedValue", price.setScale(0, RoundingMode.HALF_UP));
        result.put("convertedValue", rate.multiply(price));
        result.put("renderedConvertedValue", rate.multiply(price).setScale(0, RoundingMode.HALF_UP));

        getReportMocks().forEach(mock ->
                mock.stubFor(builder.willReturn(aResponse().withBody(result.toString())))
        );
    }

    public String generateResponse(ReportGeneratorParameters parameters, MarketReportPlace place) {
        return generator.generate(place, parameters);
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private void configureAdditionalUrlMapping(MarketReportPlace place,
                                               MappingBuilder mappingBuilder,
                                               Order order,
                                               ReportGeneratorParameters parameters) {
        switch (place) {
            case OFFER_INFO:
                addRearrFactors(mappingBuilder, parameters);
                configureOfferInfoUrlMapping(mappingBuilder, order, parameters);
                break;
            case SHOP_INFO:
                configureShopInfoUrlMapping(mappingBuilder, order);
                break;
            case MODEL_INFO:
                addRearrFactors(mappingBuilder, parameters);
                configureModelInfoUrlMapping(mappingBuilder, order);
                break;
            case ACTUAL_DELIVERY:
                addRearrFactors(mappingBuilder, parameters);
                configureActualDeliveryUrlMapping(mappingBuilder, order, parameters);
                break;
        }
    }

    private void addRearrFactors(MappingBuilder mappingBuilder, ReportGeneratorParameters parameters) {
        if (StringUtils.isNotEmpty(parameters.getExperiments())) {
            mappingBuilder.withQueryParam(REPORT_EXPERIMENTS_PARAM, equalTo(parameters.getExperiments()));
        }
    }

    private void configureShopInfoUrlMapping(MappingBuilder mappingBuilder, Order order) {
        mappingBuilder
                .withQueryParam("fesh", equalTo(String.valueOf(order.getShopId())));
    }

    private void configureModelInfoUrlMapping(MappingBuilder mappingBuilder, Order order) {
        String modelIds = order.getItems().stream()
                .filter(item -> item.getModelId() != null)
                .map(OrderItem::getModelId)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        mappingBuilder
                .withQueryParam("hyperid", equalTo(modelIds))
                .withQueryParam("bsformat", equalTo("2"));
    }

    private void configureOfferInfoUrlMapping(MappingBuilder mappingBuilder, Order order,
                                              ReportGeneratorParameters parameters) {
        mappingBuilder
                .withQueryParam("fesh", equalTo(String.valueOf(order.getShopId())))
                .withQueryParam("cpa-category-filter", equalTo("0"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("show-urls", equalTo(ShowUrlsParam.DECRYPTED.getId()))
                .withQueryParam("rids", equalTo(String.valueOf(parameters.getRegionId())));

        for (OrderItem item : order.getItems()) {
            FeedOfferId feedOfferId = item.getFeedOfferId();
            mappingBuilder.withQueryParam("feed_shoffer_id",
                    equalTo(feedOfferId.getFeedId() + "-" + feedOfferId.getId()));
        }
    }

    private void configureActualDeliveryUrlMapping(MappingBuilder mappingBuilder,
                                                   Order order,
                                                   ReportGeneratorParameters parameters) {
        mappingBuilder
                .withQueryParam("regional-delivery", equalTo("1"))
                .withQueryParam("pickup-options", equalTo("grouped"))
                .withQueryParam("pickup-options-extended-grouping", equalTo("1"))
                .withQueryParam("feedid", matching("\\d*"))
                .withQueryParam("currency", equalTo("RUR"));
        if (!parameters.isConfigurePreciseActualDelivery()) {
            mappingBuilder
                    .withQueryParam("preferable-courier-delivery-day", absent())
                    .withQueryParam("preferable-courier-delivery-service", absent());
        }
        if (!parameters.getExtraActualDeliveryParams().containsKey("offers-list")) {
            Set<String> wareMd5Set = order.getItems().stream()
                    .map(OfferItem::getWareMd5)
                    .filter(StringUtils::isNotBlank)
                    .map(Pattern::quote)
                    .collect(Collectors.toSet());
            if (!wareMd5Set.isEmpty()) {
                mappingBuilder.withQueryParam("offers-list", matching(
                        wareMd5Set.stream()
                                .collect(Collectors.joining("|", ".*(", ").*"))
                ));
            }
        }

        parameters.getExtraActualDeliveryParams().forEach((k, v) -> mappingBuilder.withQueryParam(k, equalTo(v)));

        // проверку флажков parameters.isUserHasPrime()/yaplus убрал, т.к. не совсем корректная логика получалась
        // скорее данные флажки должны контролировать выдачу, но никак не приводить к 404 если
        // в запросе не передан перк
        Optional.ofNullable(order)
                .flatMap(o -> Optional.ofNullable(o.getDelivery()))
                .filter(d -> d.getType() == DeliveryType.POST)
                .flatMap(d -> Optional.ofNullable(d.getBuyerAddress()))
                .flatMap(a -> Optional.ofNullable(a.getPostcode()))
                .ifPresent(
                        postCode -> mappingBuilder.withQueryParam("post-index", equalTo(postCode))
                );
    }

    public void mockDefaultCreditInfo() {
        generator.defaultCreditInfo();
    }

    public void mockCreditInfoWithoutOffers() {
        generator.creditInfoWithoutOffers();
    }

    public void mockCreditInfo(String path) {
        generator.creditInfo(path);
    }
}
