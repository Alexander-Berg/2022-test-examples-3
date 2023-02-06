package ru.yandex.market.billing.marketing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.marketing.dao.BilledMarketingCampaignDao;
import ru.yandex.market.billing.marketing.dao.BilledPromoOrderItemDao;
import ru.yandex.market.billing.marketing.dao.PromoOrderItemDao;
import ru.yandex.market.billing.marketing.dao.PromoOrderItemImportDao;

@Configuration
public class PartnerMarketingBillingTestConfig {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Bean
    public BilledMarketingCampaignDao billedMarketingCampaignDao() {
        return new BilledMarketingCampaignDao(pgNamedParameterJdbcTemplate);
    }

    @Bean
    public BilledPromoOrderItemDao billedPromoOrderItemDao() {
        return new BilledPromoOrderItemDao(pgNamedParameterJdbcTemplate);
    }

    @Bean
    public PromoOrderItemDao promoOrderItemDao() {
        return new PromoOrderItemDao(pgNamedParameterJdbcTemplate);
    }

    @Bean
    public PromoOrderItemImportDao promoOrderItemImportDao() {
        return new PromoOrderItemImportDao(pgNamedParameterJdbcTemplate);
    }

}
