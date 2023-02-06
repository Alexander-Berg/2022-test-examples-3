package ru.yandex.market.vendors.analytics.core.service.widget;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.vendors.analytics.core.model.access.UserAccessLevels;
import ru.yandex.market.vendors.analytics.core.model.dashboard.DashboardType;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.partner.PartnerType;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.AccessLevel;
import ru.yandex.market.vendors.analytics.core.model.widget.DefaultWidgetParams;
import ru.yandex.market.vendors.analytics.core.service.widget.creator.DefaultWidgetCreator;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;
import ru.yandex.market.vendors.analytics.core.utils.json.ObjectMapperFactory;

/**
 * Тесты для сервиса {@link DefaultWidgetFactory}.
 *
 * @author ogonek
 */
public class DefaultWidgetFactoryTest {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private static final DefaultWidgetParams DEFAULT_WIDGET_PARAMS = new DefaultWidgetParams(1L, 1L, null, null);

    @ParameterizedTest
    @MethodSource("getDefaultCategorySalesArguments")
    @DisplayName("Проверяет, что дефолтные запросы имеют ожидаемый вид")
    void getDefaultCategorySales(
            String fileName,
            WidgetType widgetType,
            AccessLevel shopLevel,
            AccessLevel vendorLevel
    ) throws JsonProcessingException {
        String expected = StringTestUtil.getString(getClass(), fileName);
        var partnerTypeLevels = new HashMap<PartnerType, AccessLevel>();
        partnerTypeLevels.put(PartnerType.SHOP, shopLevel);
        partnerTypeLevels.put(PartnerType.VENDOR, vendorLevel);
        JsonNode actualJsonNode = DefaultWidgetFactory.getDefaultWidgetParams(
                widgetType,
                DEFAULT_WIDGET_PARAMS,
                new UserAccessLevels(partnerTypeLevels)
        );
        String actual = OBJECT_MAPPER.writeValueAsString(actualJsonNode);
        JsonUnitUtils.assertJsonWithDatesEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("allTemplatesExistsArguments")
    @DisplayName("Проверяет, что для всех WidgetType в отчёте по категориям есть свой дефолтный запрос")
    void allTemplatesExists(WidgetType widgetType) {
        Map<WidgetType, DefaultWidgetCreator> templatesFactory =
                DefaultWidgetFactory.getParamsTemplates();
        Assertions.assertNotNull(templatesFactory.get(widgetType));
    }

    private static Stream<Arguments> getDefaultCategorySalesArguments() {
        return Stream.of(
                Arguments.of(
                        "data/CategoryAveragePrice.json",
                        WidgetType.CATEGORY_AVERAGE_PRICE,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryMarketShare.json",
                        WidgetType.CATEGORY_MARKET_SHARE,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryMarketShareWithBrand.json",
                        WidgetType.CATEGORY_MARKET_SHARE_WITH_BRAND,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryBrandsMarketShare.json",
                        WidgetType.CATEGORY_BRANDS_MARKET_SHARE,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryPriceSegments.json",
                        WidgetType.CATEGORY_PRICE_SEGMENTS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryPriceSegmentsWithBrand.json",
                        WidgetType.CATEGORY_PRICE_SEGMENTS_WITH_BRAND,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/BrandsPriceSegments.json",
                        WidgetType.BRANDS_PRICE_SEGMENTS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/ParentCategoryMarketShare.json",
                        WidgetType.PARENT_CATEGORY_MARKET_SHARE,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/BrandMarketShareByShops.json",
                        WidgetType.BRAND_MARKET_SHARE_BY_SHOPS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/BrandPercentByShops.json",
                        WidgetType.BRAND_PERCENT_BY_SHOPS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategoryGrowthWaterfall.json",
                        WidgetType.CATEGORY_GROWTH_WATERFALL,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/BrandsGrowth.json",
                        WidgetType.BRANDS_GROWTH,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/ModelsContribution.json",
                        WidgetType.MODELS_CONTRIBUTION,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/CategorySocDemDistribution.json",
                        WidgetType.CATEGORY_SOC_DEM_DISTRIBUTION,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/ModelSocDemDistribution.json",
                        WidgetType.MODEL_SOC_DEM_DISTRIBUTION,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/model/VendorCategoryTopModels.json",
                        WidgetType.CATEGORY_TOP_MODELS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/model/ShopCategoryTopModels.json",
                        WidgetType.CATEGORY_TOP_MODELS,
                        AccessLevel.FROM_0_5_TO_1, null
                ),
                Arguments.of(
                        "data/model/CategoryMarketShareByRegions.json",
                        WidgetType.CATEGORY_MARKET_SHARE_BY_REGIONS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/model/CategoryMarketShareByRegionsWithBrand.json",
                        WidgetType.CATEGORY_MARKET_SHARE_BY_REGIONS_WITH_BRAND,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/model/CategoryTopBrands.json",
                        WidgetType.CATEGORY_BRANDS_SHARE_BY_REGIONS,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/model/ModelAveragePrice.json",
                        WidgetType.MODEL_AVERAGE_PRICE,
                        AccessLevel.FROM_10_TO_100, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/SearchInterestDistribution.json",
                        WidgetType.SEARCH_INTEREST_DISTRIBUTION,
                        AccessLevel.FROM_1_TO_5, AccessLevel.VENDOR_LITE
                ),
                Arguments.of(
                        "data/PopularParams.json",
                        WidgetType.POPULAR_PARAMS,
                        AccessLevel.FROM_1_TO_5, AccessLevel.VENDOR
                ),
                Arguments.of(
                        "data/SalesChannels.json",
                        WidgetType.CATEGORY_SALES_CHANNELS,
                        null, AccessLevel.VENDOR_LITE
                )
        );
    }

    private static Stream<Arguments> allTemplatesExistsArguments() {
        return DashboardType.CATEGORY.getWidgetTypes().stream()
                .map(Arguments::of);
    }
}
