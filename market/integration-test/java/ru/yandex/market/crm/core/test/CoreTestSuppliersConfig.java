package ru.yandex.market.crm.core.test;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.suppliers.CategoriesDataSupplier;
import ru.yandex.market.crm.core.suppliers.CategoriesImageLinksSupplier;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.suppliers.TestSubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
public class CoreTestSuppliersConfig {

    @Bean
    public CategoriesDataSupplier categoriesDataSupplier() {
        return new TestingCategorySupplier();
    }

    @Bean
    public CategoriesImageLinksSupplier categoriesImageLinksSupplier() {
        return mock(CategoriesImageLinksSupplier.class);
    }

    @Bean
    public SubscriptionsTypesSupplier subscriptionsTypesSupplier() {
        return new TestSubscriptionsTypesSupplier(Map.of(
                SubscriptionType.Channel.PUSH, SubscriptionTypes.STORE_PUSH_ORDER_STATUS.getId()
        ));
    }

    @Bean
    public TestGeoDataSupplier geoDataSupplier() {
        return new TestGeoDataSupplier();
    }
}
