package ru.yandex.market.core.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.util.spring.HideFromComponentScan;
import ru.yandex.market.mbi.environment.EnvironmentService;

@Configuration
@HideFromComponentScan
class DbOrderServiceTestConfig {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ServiceFeePartitionDao serviceFeePartitionDao;

    @Bean
    public DbOrderService orderService(final JdbcTemplate jdbcTemplate,
                                       final NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        DbOrderService orderService = new DbOrderService(environmentService, serviceFeePartitionDao);
        orderService.setJdbcTemplate(jdbcTemplate);
        orderService.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
        return orderService;
    }

}
