package ru.yandex.market.billing.agency_commission;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.payout.control.dao.PayoutFrequencyDao;

@Configuration
@ParametersAreNonnullByDefault
public class AgencyCommissionTestConfig {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Bean
    @Primary
    public PayoutFrequencyDao pgPayoutFrequencyDao() {
        return new PayoutFrequencyDao(namedParameterJdbcTemplate);
    }

}
