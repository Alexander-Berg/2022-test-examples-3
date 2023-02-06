package ru.yandex.market.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @see ru.yandex.market.core.supplier.promo.config.PromoOfferCoreConfig
 */
@Configuration
public class PromoOfferCoreFunctionalTestConfig {
    @Bean("discountPromoTemplate")
    public Resource discountPromoTemplate() {
        return new ClassPathResource("reports/marketplace-sales.xlsm");
    }

    @Bean("discountPromoTemplateMultiPromo")
    public Resource discountPromoTemplateMultiPromo() {
        return new ClassPathResource("reports/marketplace-sales-multi-promo.xlsm");
    }

    @Bean("cheapestAsGiftPromoTemplate")
    public Resource cheapestAsGiftPromoTemplate() {
        return new ClassPathResource("reports/marketplace-sales-three-as-two.xlsm");
    }

    @Bean("cheapestAsGiftPromoTemplateMultiPromo")
    public Resource cheapestAsGiftPromoTemplateMultiPromo() {
        return new ClassPathResource("reports/marketplace-sales-three-as-two-multi-promo.xlsm");
    }

    @Bean("promocodePromoTemplate")
    public Resource promocodePromoTemplate() {
        return new ClassPathResource("reports/marketplace-promocode.xlsm");
    }

    @Bean("promocodePromoTemplateMultiPromo")
    public Resource promocodePromoTemplateMultiPromo() {
        return new ClassPathResource("reports/marketplace-promocode-multi-promo.xlsm");
    }

    @Bean("cashbackPromoTemplate")
    public Resource cashbackPromoTemplate() {
        return new ClassPathResource("reports/marketplace-sales-loyalty-program.xlsm");
    }
}
