package ru.yandex.direct.intapi.entity.campaigns.add;

import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductCalcType;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.model.ProductUnit;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.entity.product.repository.ProductsCache;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.campaigns.service.CampaignsAddService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsAddServiceTest {
    @Autowired
    private CampaignsAddService service;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ProductsCache productsCache;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Test
    public void zen_creates() {
        var products = Set.of(new Product()
                        .withId(513389L)
                        .withCurrencyCode(CurrencyCode.RUB)
                        .withEngineId(7L)
                        .withUnit(ProductUnit.SHOWS)
                        .withCalcType(ProductCalcType.CPM)
                        .withUnitScale(1L)
                        .withPublicDescriptionKey("")
                        .withPublicNameKey("")
                        .withThemeId(0L)
                        .withType(ProductType.AUTO_IMPORT));
        productRepository.insertNewProducts(dslContextProvider.ppcdict(), products);

        productsCache.invalidate();

        var clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());
        var representativeInfo = steps.userSteps().createRepresentative(clientInfo);
        var result = service.createZenTextCampaign(representativeInfo.getUid(),
                Objects.requireNonNull(clientInfo.getClient())).get(0);
        assertTrue("запрос выполнился успешно", result.isSuccessful());
        var cid = result.getResult();
        var camps = campaignService
                .getCampaigns(Objects.requireNonNull(clientInfo.getClientId()), Set.of(cid));
        assertEquals("кампания создалась", 1, camps.size());
    }
}
