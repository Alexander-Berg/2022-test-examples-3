package ru.yandex.chemodan.app.psbilling.core.synchronization;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.config.FeaturesSynchronize;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationService;
import ru.yandex.chemodan.app.psbilling.core.util.RequestTemplate;

public class AddingFeatureOnTheFlyTest extends AbstractPsBillingCoreTest {
    @FeaturesSynchronize
    @Autowired
    private RestTemplate externalSystems;
    @Autowired
    protected UserServiceActualizationService userServiceActualizationService;

    @Test
    public void testTablesSynchronization() {
        Mockito.when(externalSystems.exchange(
                Mockito.anyString(), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("", HttpStatus.ACCEPTED));

        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.createProductFeature(
                product.getId(),
                psBillingProductsFactory.createFeature(FeatureType.TOGGLEABLE, b ->
                        b.activationRequestTemplate(post("http://some.yandex.ru/activate/#{uid}"))
                )
        );

        UserServiceEntity userService = psBillingUsersFactory.createUserService(product);

    }

    @NotNull
    private Option<RequestTemplate> post(String template) {
        return Option.of(new RequestTemplate(HttpMethod.POST, template, Option.empty(),
                Option.of(MediaType.APPLICATION_FORM_URLENCODED)));
    }
}
