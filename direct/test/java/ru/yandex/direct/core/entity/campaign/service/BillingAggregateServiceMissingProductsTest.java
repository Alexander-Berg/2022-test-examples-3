package ru.yandex.direct.core.entity.campaign.service;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class BillingAggregateServiceMissingProductsTest {
    private static final Long CLIENT_ID = nextLong();

    @Mock
    private ProductService productService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CampaignTypedRepository campaignTypedRepository;

    @InjectMocks
    private BillingAggregateService billingAggregateService;

    private Client client;

    private static final Map<String, Product> CPM_RUB_PRODUCT_MAP = Map.ofEntries(
            Map.entry("CPM_BANNER_RUB", new Product()
                    .withId(nextLong())
                    .withCurrencyCode(CurrencyCode.RUB)
                    .withType(ProductType.CPM_BANNER)),
            Map.entry("CPM_AUDIO_RUB", new Product()
                    .withId(nextLong())
                    .withCurrencyCode(CurrencyCode.RUB)
                    .withType(ProductType.CPM_AUDIO)),
            Map.entry("CPM_VIDEO_RUB", new Product()
                    .withId(nextLong())
                    .withCurrencyCode(CurrencyCode.RUB)
                    .withType(ProductType.CPM_VIDEO)),
            Map.entry("CPM_INDOOR_RUB", new Product()
                    .withId(nextLong())
                    .withCurrencyCode(CurrencyCode.RUB)
                    .withType(ProductType.CPM_INDOOR)),
            Map.entry("CPM_OUTDOOR_RUB", new Product()
                    .withId(nextLong())
                    .withCurrencyCode(CurrencyCode.RUB)
                    .withType(ProductType.CPM_OUTDOOR))
    );
    private static final Product TEXT_RUB_PRODUCT = new Product()
            .withId(nextLong())
            .withCurrencyCode(CurrencyCode.RUB)
            .withType(ProductType.TEXT);

    private static final Product CPM_BANNER_USD = new Product()
            .withId(nextLong())
            .withCurrencyCode(CurrencyCode.USD)
            .withType(ProductType.CPM_BANNER);

    private static Object[] parametersData() {
        return new Object[][]{
                {
                        "CPM, рублевые, нет ни одного агрегата",
                        CurrencyCode.RUB,
                        Set.of(CampaignType.CPM_BANNER),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values()),
                        emptySet(),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values())
                },
                {
                        "CPM, рублевые, нет ни одного агрегата добаляемого типа",
                        CurrencyCode.RUB,
                        Set.of(CampaignType.CPM_BANNER),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values()),
                        Set.of(TEXT_RUB_PRODUCT.getId(), CPM_BANNER_USD.getId()),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values())
                },
                {
                        "CPM, рублевые, часть уже есть",
                        CurrencyCode.RUB,
                        Set.of(CampaignType.CPM_BANNER),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values()),
                        Set.of(CPM_RUB_PRODUCT_MAP.get("CPM_OUTDOOR_RUB").getId(),
                                CPM_RUB_PRODUCT_MAP.get("CPM_INDOOR_RUB").getId()),
                        Set.of(
                                CPM_RUB_PRODUCT_MAP.get("CPM_BANNER_RUB"),
                                CPM_RUB_PRODUCT_MAP.get("CPM_AUDIO_RUB"),
                                CPM_RUB_PRODUCT_MAP.get("CPM_VIDEO_RUB")
                        )
                },
                {
                        "TEXT, рублевый",
                        CurrencyCode.RUB,
                        Set.of(CampaignType.TEXT),
                        Set.of(TEXT_RUB_PRODUCT),
                        emptySet(),
                        emptySet()
                },
                {
                        "CPM+TEXT, рублевые, нет ни одного агрегата",
                        CurrencyCode.RUB,
                        Set.of(CampaignType.CPM_BANNER, CampaignType.TEXT),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values()),
                        emptySet(),
                        Set.copyOf(CPM_RUB_PRODUCT_MAP.values())
                },
        };
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(shardHelper.getShardByClientId(any())).thenReturn(1);
    }

    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("{0}")
    public void checkMissingProducts(String caseName, CurrencyCode currencyCode,
                                     Set<CampaignType> campaignTypes,
                                     Set<Product> neededProducts,
                                     Set<Long> existingProductIds,
                                     Set<Product> expectedProducts) {
        client = new Client()
                .withId(CLIENT_ID)
                .withClientId(CLIENT_ID)
                .withUsesQuasiCurrency(false)
                .withWorkCurrency(currencyCode);
        when(productService.calculateProductsForCampaigns(eq(campaignTypes), eq(currencyCode),
                eq(false))).thenReturn(neededProducts);

        when(campaignTypedRepository.getClientsTypedCampaignsByType(anyInt(), eq(ClientId.fromLong(client.getId())),
                eq(Set.of(CampaignType.BILLING_AGGREGATE))))
                .thenReturn(generateAggregatesWithProductIds(existingProductIds));

        assertThat(billingAggregateService.getMissingProducts(client, campaignTypes)).isEqualTo(expectedProducts);
    }

    private static Map generateAggregatesWithProductIds(Set<Long> existingProductIds) {
        return StreamEx.of(existingProductIds)
                .mapToEntry(Function.identity(), productId -> new BillingAggregateCampaign()
                        .withId(nextLong())
                        .withProductId(productId))
                .toMap();
    }
}
