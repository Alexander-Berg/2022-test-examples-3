package ru.yandex.direct.grid.processing.service.product;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.model.product.GdProduct;
import ru.yandex.direct.grid.processing.model.product.GdProductCalcType;
import ru.yandex.direct.grid.processing.model.product.GdProductRestriction;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class ProductDataServiceTest {
    private static final ClientId clientId = ClientId.fromLong(123L);
    private static final Long campaignId = 1L;

    private AdGroupService adGroupService;
    private ProductDataService productDataService;

    @Before
    public void setUp() throws Exception {
        adGroupService = mock(AdGroupService.class);
        ProductService productService = mock(ProductService.class);
        FeatureService featureService = mock(FeatureService.class);
        CampaignService campaignService = mock(CampaignService.class);
        ClientService clientService = mock(ClientService.class);
        productDataService = new ProductDataService(adGroupService, productService, featureService, campaignService,
                clientService);
    }

    @Test
    public void disallowMixingGeoproductAndOtherGroups_emptyCampaign_allowAny() {
        when(adGroupService.getSimpleAdGroupsByCampaignIds(any(ClientId.class), anyCollection()))
                .thenReturn(emptyMap());

        GdProduct product = defaultGdProduct();
        productDataService.disallowMixingGeoproductAndOtherGroups(clientId, campaignId, List.of(product));

        product.getRestrictions().forEach(r -> assertThat(r.getActive(), is(true)));
    }

    private static GdProduct defaultGdProduct() {
        return new GdProduct()
                .withCalcType(GdProductCalcType.CPM)
                .withRestrictions(List.of(
                        new GdProductRestriction().withGroupType(GdAdGroupType.CPM_BANNER).withActive(true),
                        new GdProductRestriction().withGroupType(GdAdGroupType.CPM_VIDEO).withActive(true),
                        new GdProductRestriction().withGroupType(GdAdGroupType.CPM_GEOPRODUCT).withActive(true)
                ));
    }

    @Test
    public void disallowMixingGeoproductAndOtherGroups_campaignWithGeo_allowGeoOnly() {
        when(adGroupService.getSimpleAdGroupsByCampaignIds(any(ClientId.class), anyCollection()))
                .thenReturn(Map.of(campaignId, List.of(new CpmGeoproductAdGroup()
                        .withType(AdGroupType.CPM_GEOPRODUCT))));

        GdProduct product = defaultGdProduct();
        productDataService.disallowMixingGeoproductAndOtherGroups(clientId, campaignId, List.of(product));

        product.getRestrictions()
                .forEach(r -> assertThat(r.getActive(), is(r.getGroupType() == GdAdGroupType.CPM_GEOPRODUCT)));
    }

    @Test
    public void disallowMixingGeoproductAndOtherGroups_campaignWoGeo_allowAnyButGeo() {
        when(adGroupService.getSimpleAdGroupsByCampaignIds(any(ClientId.class), anyCollection()))
                .thenReturn(Map.of(campaignId, List.of(new CpmBannerAdGroup()
                        .withType(AdGroupType.CPM_BANNER))));

        GdProduct product = defaultGdProduct();
        productDataService.disallowMixingGeoproductAndOtherGroups(clientId, campaignId, List.of(product));

        product.getRestrictions().forEach(r -> assertThat(r.getActive(),
                is(r.getGroupType() != GdAdGroupType.CPM_GEOPRODUCT)));
    }

    @Test
    public void disallowMixingGeoproductAndOtherGroups_campaignWoGeo_allowAnyButGeo_keepInactive() {
        when(adGroupService.getSimpleAdGroupsByCampaignIds(any(ClientId.class), anyCollection()))
                .thenReturn(Map.of(campaignId, List.of(new CpmBannerAdGroup()
                        .withType(AdGroupType.CPM_BANNER))));

        GdProduct product = defaultGdProduct();
        GdAdGroupType inactiveGroupType = GdAdGroupType.CPM_VIDEO;
        product.getRestrictions().stream()
                .filter(r -> r.getGroupType() == inactiveGroupType)
                .forEach(r -> r.setActive(false));
        productDataService.disallowMixingGeoproductAndOtherGroups(clientId, campaignId, List.of(product));

        product.getRestrictions().forEach(r -> assertThat(r.getActive(),
                is(r.getGroupType() != GdAdGroupType.CPM_GEOPRODUCT && r.getGroupType() != inactiveGroupType)));
    }
}
