package ru.yandex.travel.api.services.orders.happy_page;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.api.services.hotels.geobase.GeoBaseHelpers;
import ru.yandex.travel.api.services.hotels.regions.RegionsService;
import ru.yandex.travel.api.services.orders.happy_page.model.CrossSaleBlockType;
import ru.yandex.travel.api.services.orders.happy_page.model.MarketWidgetCrossSalePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class MarketWidgetPayloadProviderTest {
    private static final Integer RUSSIA_COUNTRY_GEO_ID = 225;
    private static final Integer BELARUS_COUNTRY_GEO_ID = 29630;

    private static final Integer MO_REGION_GEO_ID = 1;
    private static final Integer SVERDLOVSK_REGION_GEO_ID = 11162;
    private static final Integer MINSK_REGION_GEO_ID = 29630;

    private static final Integer MOSCOW_GEO_ID = 213;
    private static final Integer YEKATERINBURG_GEO_ID = 54;
    private static final Integer MINSK_GEO_ID = 157;

    private RegionsService regionsService;

    private MarketWidgetPayloadProvider marketWidgetPayloadProvider;

    @Before
    public void setUp() {
        regionsService = Mockito.mock(RegionsService.class);
        var domain = "ru";
        when(regionsService.getRegionRoundTo(MOSCOW_GEO_ID, GeoBaseHelpers.REGION_REGION_TYPE, domain))
                .thenReturn(MO_REGION_GEO_ID);
        when(regionsService.getRegionRoundTo(MOSCOW_GEO_ID, GeoBaseHelpers.COUNTRY_REGION_TYPE, domain))
                .thenReturn(RUSSIA_COUNTRY_GEO_ID);

        when(regionsService.getRegionRoundTo(YEKATERINBURG_GEO_ID, GeoBaseHelpers.REGION_REGION_TYPE, domain))
                .thenReturn(SVERDLOVSK_REGION_GEO_ID);
        when(regionsService.getRegionRoundTo(YEKATERINBURG_GEO_ID, GeoBaseHelpers.COUNTRY_REGION_TYPE, domain))
                .thenReturn(RUSSIA_COUNTRY_GEO_ID);

        when(regionsService.getRegionRoundTo(MINSK_GEO_ID, GeoBaseHelpers.REGION_REGION_TYPE, domain))
                .thenReturn(MINSK_REGION_GEO_ID);
        when(regionsService.getRegionRoundTo(MINSK_GEO_ID, GeoBaseHelpers.COUNTRY_REGION_TYPE, domain))
                .thenReturn(BELARUS_COUNTRY_GEO_ID);

        marketWidgetPayloadProvider = marketWidgetPayloadProvider();
    }

    private MarketWidgetPayloadProvider marketWidgetPayloadProvider() {
        MarketWidgetProperties.MarketWidgetParameters defaultParams = createDefaultMarketWidgetParameters();

        var moscowFilters = new MarketWidgetProperties.MarketWidgetFilters();
        moscowFilters.setRegionGeoId(MO_REGION_GEO_ID);
        moscowFilters.setCountryGeoId(RUSSIA_COUNTRY_GEO_ID);
        var russiaFilters = new MarketWidgetProperties.MarketWidgetFilters();
        russiaFilters.setCountryGeoId(RUSSIA_COUNTRY_GEO_ID);

        MarketWidgetProperties.MarketWidgetSettings moscowSettings = createMarketWidgetSettings("Московский виджет", moscowFilters);
        MarketWidgetProperties.MarketWidgetSettings russiaSettings = createMarketWidgetSettings("Российский виджет", russiaFilters);
        MarketWidgetProperties.MarketWidgetSettings noFiltersSettings = createMarketWidgetSettings("Виджет без ограничений", null);

        Map<String, MarketWidgetProperties.MarketWidgetSettings> configSettings = Map.of(
                "market-widget-moscow", moscowSettings,
                "market-widget-russia", russiaSettings,
                "market-widget-no-filters", noFiltersSettings
        );
        MarketWidgetProperties config = new MarketWidgetProperties();
        config.setMarketWidgetSettings(configSettings);
        return new MarketWidgetPayloadProvider(regionsService, config);
    }

    private MarketWidgetProperties.MarketWidgetParameters createDefaultMarketWidgetParameters() {
        var defaultParams = new MarketWidgetProperties.MarketWidgetParameters();
        defaultParams.setClid(123);
        defaultParams.setThemeId(123);
        defaultParams.setSearchModelIds(List.of(123));
        return defaultParams;
    }

    private MarketWidgetProperties.MarketWidgetSettings createMarketWidgetSettings(
            String title, MarketWidgetProperties.MarketWidgetFilters filters
    ) {
        var marketWidgetSettings = new MarketWidgetProperties.MarketWidgetSettings();
        marketWidgetSettings.setTitle(title);
        marketWidgetSettings.setType("offers");
        marketWidgetSettings.setFallback(true);
        marketWidgetSettings.setParams(createDefaultMarketWidgetParameters());
        marketWidgetSettings.setFilters(filters);
        return marketWidgetSettings;
    }

    @Test
    public void testMoscowMarketWidget() {
        var happyPageBlock = createHappyPageBlock("market-widget-moscow");
        var moscowMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MOSCOW_GEO_ID, happyPageBlock
        );
        var yekaterinburgMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                YEKATERINBURG_GEO_ID, happyPageBlock
        );
        var minskMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MINSK_GEO_ID, happyPageBlock
        );

        MarketWidgetCrossSalePayload moscowMarketWidgetPayload = moscowMarketWidgetPayloadFuture.join();
        assertThat(moscowMarketWidgetPayload.getTitle()).isEqualTo("Московский виджет");

        assertThat(yekaterinburgMarketWidgetPayloadFuture.isCompletedExceptionally()).isTrue();
        assertThat(minskMarketWidgetPayloadFuture.isCompletedExceptionally()).isTrue();
    }

    @Test
    public void testRussiaMarketWidget() {
        var happyPageBlock = createHappyPageBlock("market-widget-russia");
        var moscowMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MOSCOW_GEO_ID, happyPageBlock
        );
        var yekaterinburgMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                YEKATERINBURG_GEO_ID, happyPageBlock
        );
        var minskMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MINSK_GEO_ID, happyPageBlock
        );

        MarketWidgetCrossSalePayload moscowMarketWidgetPayload = moscowMarketWidgetPayloadFuture.join();
        assertThat(moscowMarketWidgetPayload.getTitle()).isEqualTo("Российский виджет");

        MarketWidgetCrossSalePayload yekaterinburgMarketWidgetPayload = yekaterinburgMarketWidgetPayloadFuture.join();
        assertThat(yekaterinburgMarketWidgetPayload.getTitle()).isEqualTo("Российский виджет");

        assertThat(minskMarketWidgetPayloadFuture.isCompletedExceptionally()).isTrue();
    }

    @Test
    public void testNoFiltersMarketWidget() {
        var happyPageBlock = createHappyPageBlock("market-widget-no-filters");
        var moscowMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MOSCOW_GEO_ID, happyPageBlock
        );
        var yekaterinburgMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                YEKATERINBURG_GEO_ID, happyPageBlock
        );
        var minskMarketWidgetPayloadFuture = marketWidgetPayloadProvider.get(
                MINSK_GEO_ID, happyPageBlock
        );

        MarketWidgetCrossSalePayload moscowMarketWidgetPayload = moscowMarketWidgetPayloadFuture.join();
        assertThat(moscowMarketWidgetPayload.getTitle()).isEqualTo("Виджет без ограничений");

        MarketWidgetCrossSalePayload yekaterinburgMarketWidgetPayload = yekaterinburgMarketWidgetPayloadFuture.join();
        assertThat(yekaterinburgMarketWidgetPayload.getTitle()).isEqualTo("Виджет без ограничений");

        MarketWidgetCrossSalePayload minskMarketWidgetPayload = minskMarketWidgetPayloadFuture.join();
        assertThat(minskMarketWidgetPayload.getTitle()).isEqualTo("Виджет без ограничений");

    }

    private HappyPageProperties.BlockSettings createHappyPageBlock(String marketWidgetName) {
        HappyPageProperties.BlockSettings block = new HappyPageProperties.BlockSettings();
        block.setType(CrossSaleBlockType.YANDEX_MARKET);
        block.setOrder(1);
        block.setMarketWidgetName(marketWidgetName);
        return block;
    }
}
