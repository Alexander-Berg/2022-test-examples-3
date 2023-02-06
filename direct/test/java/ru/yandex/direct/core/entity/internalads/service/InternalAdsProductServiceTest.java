package ru.yandex.direct.core.entity.internalads.service;

import java.util.Collections;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.util.StringUtils;

import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProductOption;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdsProductServiceTest {
    @Autowired
    private InternalAdsProductService service;

    @Autowired
    private Steps steps;

    private ClientId clientId;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
    }

    @Test
    public void testCreateAndGet() {
        InternalAdsProduct createdProduct = new InternalAdsProduct()
                .withClientId(clientId)
                .withName("product name")
                .withDescription("product description")
                .withOptions(Collections.emptySet());
        service.createProduct(createdProduct);

        InternalAdsProduct fetchedProduct = service.getProduct(clientId);
        assertThat(fetchedProduct).isEqualTo(createdProduct);
    }

    @Test
    public void testCreateWithOptionsAndGet() {
        InternalAdsProduct createdProduct = new InternalAdsProduct()
                .withClientId(clientId)
                .withName("product name 2")
                .withDescription("product description 2")
                .withOptions(Set.of(InternalAdsProductOption.SOFTWARE));
        service.createProduct(createdProduct);

        InternalAdsProduct fetchedProduct = service.getProduct(clientId);
        assertThat(fetchedProduct).isEqualTo(createdProduct);
    }

    @Test
    public void testUpdate() {
        ClientInfo adProduct = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        String productName = service.getProduct(adProduct.getClientId()).getName();
        InternalAdsProduct updateProduct = new InternalAdsProduct()
                .withClientId(adProduct.getClientId())
                .withName("test update name") //проверяем, что при обновлении не перезатираем имя продукта
                .withDescription("test update description" + StringUtils.randomAlphanumeric(20))
                .withOptions(Set.of(InternalAdsProductOption.SOFTWARE));
        service.updateProduct(updateProduct);

        InternalAdsProduct fetchedProduct = service.getProduct(adProduct.getClientId());
        var expectedProduct = updateProduct
                .withName(productName);
        assertThat(fetchedProduct).isEqualTo(expectedProduct);
    }

}
