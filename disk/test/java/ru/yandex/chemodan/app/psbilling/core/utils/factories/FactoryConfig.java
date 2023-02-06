package ru.yandex.chemodan.app.psbilling.core.utils.factories;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreDaoConfiguration;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;

@Configuration
@Import(PsBillingCoreDaoConfiguration.class)
public class FactoryConfig {
    @Bean
    public PaymentFactory paymentFactory(GroupTrustPaymentRequestDao groupTrustPaymentRequestDao, CardDao cardDao) {
        return new PaymentFactory(groupTrustPaymentRequestDao, cardDao);
    }
}
