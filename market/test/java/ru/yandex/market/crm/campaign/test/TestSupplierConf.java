package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.campaign.suppliers.CryptaSegmentsSupplier;
import ru.yandex.market.crm.campaign.suppliers.MobilePhoneCharacteristicsSupplier;
import ru.yandex.market.crm.campaign.suppliers.NavTreeSupplier;
import ru.yandex.market.crm.campaign.suppliers.PromoTypesSupplier;
import ru.yandex.market.crm.core.test.CoreTestSuppliersConfig;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
@Import(CoreTestSuppliersConfig.class)
public class TestSupplierConf {

    @Bean
    public CryptaSegmentsSupplier cryptaSegmentsSupplier() {
        return mock(CryptaSegmentsSupplier.class);
    }

    @Bean
    public MobilePhoneCharacteristicsSupplier mobilePhoneCharacteristicsSupplier() {
        return mock(MobilePhoneCharacteristicsSupplier.class);
    }

    @Bean
    public NavTreeSupplier navTreeSupplier() {
        return mock(NavTreeSupplier.class);
    }

    @Bean
    public PromoTypesSupplier promoTypesSupplier() {
        return mock(PromoTypesSupplier.class);
    }
}
