package ru.yandex.market.api.controller.v2.sku;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.controller.v2.SkuControllerV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.GetSkuSpecificationResult;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.Organization;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.SkuField;
import ru.yandex.market.api.domain.v2.SkuResult;
import ru.yandex.market.api.domain.v2.SkusResult;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.matchers.OfferMatcher;
import ru.yandex.market.api.matchers.OrganizationMatcher;
import ru.yandex.market.api.matchers.ShopInfoMatcher;
import ru.yandex.market.api.matchers.SkuMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.shop.OrganizationType;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.clients.ShopInfoTestClient;
import ru.yandex.market.api.util.httpclient.clients.TarantinoTestClient;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.SkuMatcher.id;
import static ru.yandex.market.api.matchers.SkuMatcher.offers;
import static ru.yandex.market.api.matchers.SkuMatcher.sku;

@WithContext
@ActiveProfiles(SkuControllerV2Test.PROFILE)
public class SkuControllerV2Test extends BaseTest {
    static final String PROFILE = "SkuControllerV2Test";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }

    @Inject
    private SkuControllerV2 skuController;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private ShopInfoTestClient shopInfoTestClient;

    @Inject
    private ClientHelper clientHelper;

    @Inject
    private TarantinoTestClient tarantinoClient;

    private MockClientHelper mockClientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void getSkuWithOfferId() {
        String skuId = "SkuTest";
        OfferId offerId = new OfferId("wareMd5", null);

        reportTestClient.sku(skuId, offerId.getWareMd5(), "sku-with-offerid.json");

        doTest(skuId, offerId);
    }

    @Test
    public void getSkuWithoutOfferId() {
        String skuId = "SkuTest";

        reportTestClient.sku(skuId, "sku-without-offerid.json");
        doTest(skuId, null);
    }

    @Test
    public void notFoundIfSkuNotInReport() {
        String skuId = "SkuTest";

        reportTestClient.sku(skuId, "sku-empty.json");

        exception.expect(NotFoundException.class);
        expectMessage("Sku", "SkuTest", "not found");

        doRequest(skuId, (OfferId) null);
    }

    @Test
    public void skuWithSupplier() {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(Client.Type.MOBILE);

            ctx.setClient(client);
            ctx.setClientVersionInfo(
                new KnownMobileClientVersionInfo(
                    Platform.IOS,
                    DeviceType.TABLET,
                    SemanticVersion.MIN
                )
            );
        });

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        String skuId = "SkuTest";

        reportTestClient.sku(skuId, "sku-with-supplier.json");
        shopInfoTestClient.supplier(4, "supplier_4.json");

        Sku sku = doRequest(skuId, Arrays.asList(SkuField.OFFERS, OfferFieldV2.SUPPLIER)).getSku();

        Matcher<Organization> organizationMatcher = OrganizationMatcher.organization(
            OrganizationMatcher.name("orgName 1"),
            OrganizationMatcher.orgn("12345"),
            OrganizationMatcher.type(OrganizationType.OOO)
        );

        Matcher<ShopInfoV2> supplierMatcher = ShopInfoMatcher.shop(
            ShopInfoMatcher.id(4L),
            ShopInfoMatcher.organizations(
                Matchers.hasItem(
                    organizationMatcher
                )
            )
        );

        assertThat(
            sku,
            sku(
                id(skuId),
                offers(
                    Matchers.contains(
                        OfferMatcher.offer(
                            OfferMatcher.offerId(OfferMatcher.wareMd5("Sku2Price50-iLVm1Goleg")),
                            OfferMatcher.supplier(supplierMatcher)
                        )
                    )
                )
            )
        );
    }

    @Test
    public void skuWithCmsSuccessful() {
        String skuId = "123";
        OfferId offerId = new OfferId("abc", "");

        reportTestClient.sku(skuId, "sku-with-cms.json");
        tarantinoClient.model(1L, "ru", "sku-with-cms-tarantino.json");

        Sku sku = skuController.skuById(
            skuId,
            offerId,
            false,
            Arrays.asList(SkuField.MODEL, SkuField.CMS),
            genericParams,
            null).waitResult().getSku();

        Assert.assertThat(
            sku,
            SkuMatcher.sku(
                SkuMatcher.id(skuId),
                SkuMatcher.cms(Matchers.not(Matchers.isEmptyOrNullString()))
            )
        );
    }

    @Test
    public void skusWithoutOffersWithSupplierLink() {
        String skuId = "228119468";

        reportTestClient.skus(Arrays.asList(skuId), "sku-without-offers.json");

        String infoUrl = skuController.skusByIds(
            Arrays.asList(skuId),
            false,
            Arrays.asList(SkuField.OFFERS, OfferFieldV2.SUPPLIER),
            genericParams
        ).waitResult().getInfoUrl();

        assertNull(infoUrl);
    }

    @Test
    public void skuWithCmsBatchSuccessful() {
        String skuId123 = "123";
        String skuId456 = "456";

        reportTestClient.skus(Arrays.asList(skuId123, skuId456), "sku-with-cms-batch.json");
        tarantinoClient.model(1L, "ru", "sku-with-cms-batch-tarantino-1.json");
        tarantinoClient.model(2L, "ru", "sku-with-cms-batch-tarantino-2.json");

        List<Sku> skus = skuController.skusByIds(
            Arrays.asList(skuId123, skuId456),
            false,
            Arrays.asList(SkuField.MODEL, SkuField.CMS),
            genericParams
        ).waitResult().getSkus();

        Assert.assertThat(
            skus,
            Matchers.containsInAnyOrder(
                SkuMatcher.sku(
                    SkuMatcher.id(skuId123),
                    SkuMatcher.cms(Matchers.containsString("{\n" +
                        "          \"entity\": \"product\",\n" +
                        "          \"id\": \"1231\"\n" +
                        "        }"))
                ),
                SkuMatcher.sku(
                    SkuMatcher.id(skuId456),
                    SkuMatcher.cms(Matchers.containsString("        {\n" +
                        "          \"entity\": \"product\",\n" +
                        "          \"id\": \"4562\"\n" +
                        "        }"))
                )
            )
        );
    }

    @Test
    public void skuWithCmsFailed() {
        String skuId = "123";
        OfferId offerId = new OfferId("abc", "");

        reportTestClient.sku(skuId, "sku-with-cms-failed.json");
        tarantinoClient.model(1L, "ru")
                    .status(HttpResponseStatus.INTERNAL_SERVER_ERROR);

        Sku sku = skuController.skuById(
            skuId,
            offerId,
            false,
            Arrays.asList(SkuField.MODEL, SkuField.CMS),
            genericParams,
            null).waitResult().getSku();

        Assert.assertThat(
            sku,
            SkuMatcher.sku(
                SkuMatcher.id(skuId),
                SkuMatcher.cms(Matchers.nullValue(String.class))
            )
        );
    }

    @Test
    public void skuWithCmsBatchFailed() {
        String skuId123 = "123";
        String skuId456 = "456";

        reportTestClient.skus(Arrays.asList(skuId123, skuId456), "sku-with-cms-batch.json");
        tarantinoClient.model(1L, "ru").status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        tarantinoClient.model(2L, "ru", "sku-with-cms-batch-tarantino-2.json");

        List<Sku> skus = skuController.skusByIds(
            Arrays.asList(skuId123, skuId456),
            false,
            Arrays.asList(SkuField.MODEL, SkuField.CMS),
            genericParams
        ).waitResult().getSkus();

        Assert.assertThat(
            skus,
            Matchers.containsInAnyOrder(
                SkuMatcher.sku(
                    SkuMatcher.id(skuId123),
                    SkuMatcher.cms(Matchers.nullValue(String.class))
                ),
                SkuMatcher.sku(
                    SkuMatcher.id(skuId456),
                    SkuMatcher.cms(Matchers.containsString("        {\n" +
                        "          \"entity\": \"product\",\n" +
                        "          \"id\": \"4562\"\n" +
                        "        }"))
                )
            )
        );
    }

    @Test
    public void skuWithCmsBySuccessfull() {
        String skuId = "123";
        OfferId offerId = new OfferId("abc", "");

        ContextHolder.update(ctx -> ctx.getRegionInfo().setRawRegionId(157));

        reportTestClient.sku(skuId, "sku-with-cms-by.json");
        tarantinoClient.model(1L, "by", "sku-with-cms-tarantino.json");

        Sku sku = skuController.skuById(
            skuId,
            offerId,
            false,
            Arrays.asList(SkuField.MODEL, SkuField.CMS),
            genericParams,
            null).waitResult().getSku();

        Assert.assertThat(
            sku,
            SkuMatcher.sku(
                SkuMatcher.id(skuId),
                SkuMatcher.cms(Matchers.not(Matchers.isEmptyOrNullString()))
            )
        );
    }

    @Test
    public void skuWithCreditInfo() {
        String skuId = "SkuTest";

        reportTestClient.sku(skuId, "sku-with-credit-info.json");

        SkuResult skuResult = doRequest(skuId, Collections.singleton(SkuField.OFFERS));
        Assert.assertEquals("1", skuResult.getSku().getOffers().get(0).getCreditInfo().getBestOptionId());
        Assert.assertEquals(1, skuResult.getCreditOptions().size());
        Assert.assertEquals("1", skuResult.getCreditOptions().get(0).getId());
    }

    @Test
    public void skuWithCategoryField() {
        String skuId = "123";

        reportTestClient.sku(skuId, "sku-with-category-info.json");

        SkuResult skuResult = doRequest(skuId, Collections.singleton(SkuField.CATEGORY));
        Sku sku = skuResult.getSku();

        Assert.assertEquals(91491, sku.getCategoryId());
        Assert.assertNotNull(sku.getCategory());
        Assert.assertEquals(91491, sku.getCategory().getId());
    }

    @Test
    public void skuFullSpecifications() {
        Long skuId = 100405030766L;

        reportTestClient.sku(skuId.toString(), "sku_full_specifications.json");

        GetSkuSpecificationResult result =
                skuController.getSkuFullSpecification(skuId.toString(), Collections.emptyList(), genericParams).waitResult();

        Assert.assertEquals(skuId, result.getId());
        Assert.assertEquals(2, result.getSpecificationGroups().size());
        Assert.assertEquals("Общие характеристики", result.getSpecificationGroups().get(0).getName());
        Assert.assertEquals("Питательная и энергетическая ценность", result.getSpecificationGroups().get(1).getName());
        Assert.assertEquals(5, result.getSpecificationGroups().get(0).getFeatures().size());
        Assert.assertEquals(5, result.getSpecificationGroups().get(1).getFeatures().size());
    }

    @Test
    public void skuFriendlySpecifications() {
        String skuId = "100469342753";
        reportTestClient.sku(skuId, "sku-full-and-friendly-specifications.json");

        SkuResult result = doRequest(skuId, Arrays.asList(SkuField.SPECIFICATION));
        List<SpecificationGroup.Feature> features = result.getSku().getSpecificationGroups().get(0).getFeatures();

        Assert.assertEquals(skuId, result.getSku().getId());
        Assert.assertEquals(1, result.getSku().getSpecificationGroups().size());
        Assert.assertEquals(12, features.size());
        Assert.assertEquals("ноутбук c экраном 13.3\"", features.get(0).getValue());
        Assert.assertEquals("накопитель (SSD) 128 ГБ", features.get(6).getValue());
        Assert.assertEquals("время работы 13 ч", features.get(10).getValue());
    }

    @Test
    public void noSpecsFieldSkuFriendlySpecifications() {
        String skuId = "100469342753";
        reportTestClient.sku(skuId, "sku-full-and-friendly-specifications.json");

        SkuResult result = doRequest(skuId, Collections.emptyList());

        Assert.assertEquals(skuId, result.getSku().getId());
        Assert.assertNull(result.getSku().getSpecificationGroups());
    }

    @Test
    public void psku() {
        String skuId = "100402204448";
        reportTestClient.sku(skuId, "psku-full.json");

        SkuResult result = doRequest(skuId, Collections.emptyList());
        Sku sku = result.getSku();

        Assert.assertEquals(skuId, sku.getId());
        Assert.assertEquals("partner", sku.getSkuType());
    }

    @Test
    public void pskuWithFilters() {
        String skuId = "100402204448";
        reportTestClient.sku(skuId, "psku-full.json");

        SkuResult result = doRequest(skuId, Collections.singletonList(SkuField.FILTERS));
        Sku sku = result.getSku();

        Assert.assertEquals(skuId, sku.getId());
    }

    @Test
    public void pskuWithJumpTable() {
        String skuId1 = "100402204448";
        String skuId2 = "100390554239";

        reportTestClient.skus(Arrays.asList(skuId1, skuId2), "psku-with-filters.json");

        SkusResult skusResult = skuController.skusByIds(
            Arrays.asList(skuId1, skuId2),
            false,
            Collections.singletonList(SkuField.FILTERS),
            genericParams
        ).waitResult();

        Sku sku1 = skusResult.getSkuById(skuId1);
        Sku sku2 = skusResult.getSkuById(skuId2);

        assertThat(sku1, sku(
            id(skuId1),
            SkuMatcher.name("Aloe Vera PSK#1"),
            SkuMatcher.description("DESCRIPTION: Все просят подержать, словно я купил черного матового котёнка. " +
                "Новый телефон, имидж на год гарантирован. Хорошая замена после iPhone 5."),
                SkuMatcher.skuType("partner")
        ));

        assertThat(sku2, sku(
            id(skuId2),
            SkuMatcher.name("Эппл iPhone 7"),
            SkuMatcher.description("DESCRIPTION: Все просят подержать, словно я купил черного матового котёнка. " +
                "Новый телефон, имидж на год гарантирован. Хорошая замена после iPhone 5."),
            SkuMatcher.skuType("partner")
        ));
    }

    public void doTest(String skuId, OfferId offerId) {
        Sku sku = doRequest(skuId, offerId).getSku();
        assertThat(
            sku,
            sku(
                id(skuId)
            )
        );
    }

    public SkuResult doRequest(String skuId, OfferId offerId) {
        return skuController.skuById(
            skuId,
            offerId,
            false,
            Collections.emptyList(),
            genericParams,
            null).waitResult();
    }

    public SkuResult doRequest(String skuId, Collection<? extends Field> fields) {
        return skuController.skuById(
            skuId,
            null,
            false,
            fields,
            genericParams,
            null).waitResult();
    }

}
