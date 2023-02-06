package ru.yandex.market.api.integration.sku;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.DetailsField;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.SkuField;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.matchers.SkuMatcher;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.sku.SkuService;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.HistoryTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

public class SkuServiceTest extends BaseTest {
    @Inject
    private SkuService skuService;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private HistoryTestClient historyTestClient;

    @Test
    public void skuListNull_doNotQueryToSkuOffers() {
        List<Sku> skus = Futures.waitAndGet(skuService.getSkus(null, null, false, null, null, genericParams)).getSkus();
        Assert.assertThat(skus, Matchers.empty());
    }

    @Test
    public void skuList_filterNullFeatures() {
        reportTestClient.skus(Arrays.asList("101146015031"), "skus_with_nulls.json");
        List<Sku> skus = Futures.waitAndGet(skuService.getSkus(Arrays.asList("101146015031"), null, false, null, Arrays.asList(DetailsField.FILTERS, SkuField.SPECIFICATION, ModelInfoField.SPECIFICATION,ModelInfoField.CATEGORY,ModelInfoField.MEDIA,ModelInfoField.RATING, OfferFieldV2.CATEGORY,OfferFieldV2.DELIVERY,OfferFieldV2.DISCOUNT,SkuField.MEDIA,SkuField.MODEL,SkuField.OFFERS,SkuField.FILTERS), genericParams)).getSkus();
        Assert.assertThat(skus, hasSize(1));
        Sku sku = skus.get(0);
        Assert.assertThat(sku.getSpecificationGroups(), hasSize(1));
        SpecificationGroup sg = sku.getSpecificationGroups().get(0);
        for (SpecificationGroup.Feature f : sg.getFeatures()) {
            Assert.assertThat(f.getValue(), not(isEmptyOrNullString()));
            Assert.assertThat(f.getValue(), not(is("null")));
        }
    }

    @Test
    public void skuList_enrichSpecsWithFilters() {
        reportTestClient.skus(Arrays.asList("101146015033"), "skus_specs_from_filters.json");
        reportTestClient.getModelInfoById(856386463, "skus_model_info.json");
        List<Sku> skus = Futures.waitAndGet(skuService.getSkus(Arrays.asList("101146015033"), null, false, null, Arrays.asList(ModelInfoField.CATEGORY,ModelInfoField.MEDIA,ModelInfoField.RATING,ModelInfoField.SPECIFICATION,ModelInfoField.VENDOR,OfferFieldV2.CATEGORY,OfferFieldV2.DELIVERY,OfferFieldV2.DISCOUNT,OfferFieldV2.SHOP,OfferFieldV2.SUPPLIER,OfferFieldV2.VENDOR,SkuField.MEDIA,SkuField.MODEL,SkuField.OFFERS,SkuField.PHOTOS,SkuField.FILTERS), genericParams)).getSkus();
        Assert.assertThat(skus, hasSize(1));
        ModelV2 model = skus.get(0).getModel();
        Assert.assertThat(model.getSpecificationGroups(), hasSize(1));
        SpecificationGroup sg = model.getSpecificationGroups().get(0);
        for (SpecificationGroup.Feature f : sg.getFeatures()) {
            Assert.assertThat(f.getValue(), not(isEmptyOrNullString()));
            Assert.assertThat(f.getValue(), not(is("null")));
        }
    }

    @Test
    public void skuList_enrichShopInfo() {
        reportTestClient.skus(Arrays.asList("101146015033"), "skus_specs_from_filters.json");
        reportTestClient.getModelInfoById(856386463, "skus_model_info.json");
        List<Sku> skus = Futures.waitAndGet(skuService.getSkus(Arrays.asList("101146015033"), null, false, null,
                Arrays.asList(ModelInfoField.CATEGORY, ModelInfoField.MEDIA, ModelInfoField.RATING,
                        ModelInfoField.SPECIFICATION, ModelInfoField.VENDOR, OfferFieldV2.CATEGORY,
                        OfferFieldV2.DELIVERY, OfferFieldV2.DISCOUNT, OfferFieldV2.SHOP, OfferFieldV2.SUPPLIER,
                        OfferFieldV2.VENDOR, SkuField.MEDIA, SkuField.MODEL, SkuField.OFFERS, SkuField.PHOTOS,
                        SkuField.FILTERS), genericParams)).getSkus();
        Assert.assertThat(skus, hasSize(1));
        OfferV2 offerV2 = skus.get(0).getOffers().get(0);
        ShopInfoV2 shopInfoV2 = (ShopInfoV2) offerV2.getShop();
        Assert.assertThat(shopInfoV2.getName(), equalTo("Divan24"));
        Assert.assertThat(offerV2.getSupplier().getName(), equalTo("Divan24"));
    }

    @Test
    public void skuListEmpty_doNotQueryToSkuOffers() {
        List<Sku> skus = Futures.waitAndGet(skuService.getSkus(Collections.emptyList(), null, false, null, null, genericParams)).getSkus();
        Assert.assertThat(skus, Matchers.empty());
    }
}
