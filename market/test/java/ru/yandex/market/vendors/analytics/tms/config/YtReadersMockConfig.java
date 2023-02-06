package ru.yandex.market.vendors.analytics.tms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.vendors.analytics.core.model.brand.BrandInfo;
import ru.yandex.market.vendors.analytics.core.model.categories.Category;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryWithDepatment;
import ru.yandex.market.vendors.analytics.core.jpa.entity.FarmaHid;
import ru.yandex.market.vendors.analytics.core.model.external.UnfulfilledDemandData;
import ru.yandex.market.vendors.analytics.core.model.hiding.CategoryHiding;
import ru.yandex.market.vendors.analytics.core.model.hiding.CityTypeHiding;
import ru.yandex.market.vendors.analytics.core.model.hiding.ModelHiding;
import ru.yandex.market.vendors.analytics.core.model.hiding.RegionHiding;
import ru.yandex.market.vendors.analytics.core.model.hiding.VendorHiding;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.EcomCounter;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.MetricsCounter;
import ru.yandex.market.vendors.analytics.core.model.price.CategoryPriceSegments;
import ru.yandex.market.vendors.analytics.core.model.region.RegionInfo;
import ru.yandex.market.vendors.analytics.tms.yt.YtCategoryReader;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableIteratorCreator;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;
import ru.yandex.market.vendors.analytics.tms.yt.model.PartnerBusinessLinkDTO;
import ru.yandex.market.vendors.analytics.tms.yt.model.YtModelInfoReader;

import static org.mockito.Mockito.mock;

/**
 * @author antipov93.
 */
@Configuration
@SuppressWarnings("unchecked")
public class YtReadersMockConfig {

    @Bean
    public YtCategoryReader ytCategoryReader() {
        return mock(YtCategoryReader.class);
    }

    @Bean
    public YtModelInfoReader ytModelInfoReader() {
        return mock(YtModelInfoReader.class);
    }

    @Bean
    public YtTableReader<EcomCounter> ytEcomCounterReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<MetricsCounter> ytMetricsCounterReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<PartnerBusinessLinkDTO> partnerBusinessDictYtReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<RegionInfo> ytRegionReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<BrandInfo> ytBrandReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<BrandInfo> ytFarmaVendorsReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<FarmaHid> ytFarmaHidReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<CategoryHiding> ytCategoryWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<VendorHiding> ytVendorWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<ModelHiding> ytModelsWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<RegionHiding> ytRegionsWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<CityTypeHiding> ytCityTypeWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<Category> ytCategoryNamesReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<CategoryWithDepatment> ytManagerWhitelistReader() {
        return mockYtTableReader();
    }

    @Bean
    public YtTableReader<CategoryPriceSegments> ytCategoryPriceSegmentsReader() {
        return mockYtTableReader();
    }

    // Для табличек про скрытия

    @Bean
    public YtTableIteratorCreator ytCategorySimpleIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytCategoryRegionIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytCategoryRegionCityTypeIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytBrandSimpleIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytBrandRegionIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytBrandRegionCityTypeIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytModelSimpleIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytModelRegionIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    @Bean
    public YtTableIteratorCreator ytModelRegionCityTypeIteratorCreator() {
        return mockYtTableIteratorCreator();
    }

    private <T> YtTableReader<T> mockYtTableReader() {
        return mock(YtTableReader.class);
    }

    private YtTableIteratorCreator mockYtTableIteratorCreator() {
        return mock(YtTableIteratorCreator.class);
    }

}
