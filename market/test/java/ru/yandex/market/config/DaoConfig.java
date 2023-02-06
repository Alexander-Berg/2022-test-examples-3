package ru.yandex.market.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.order.CpaOrderStatusHistoryDao;
import ru.yandex.market.core.order.DbOrderDao;
import ru.yandex.market.core.order.OrderTransactionServiceImpl;
import ru.yandex.market.core.order.ReceiptItemDao;
import ru.yandex.market.core.order.ServiceFeePartitionDao;
import ru.yandex.market.core.order.ServiceFeePartitionService;

@Configuration
@Import(EmbeddedPostgresConfig.class)
public class DaoConfig {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Bean
    public CpaOrderStatusHistoryDao cpaOrderStatusHistoryDao() {
        return new CpaOrderStatusHistoryDao(pgNamedParameterJdbcTemplate);
    }

    @Bean
    public DbOrderDao dbOrderDao() {
        return new DbOrderDao(pgNamedParameterJdbcTemplate, pgJdbcTemplate, serviceFeePartitionDao());
    }

    @Bean
    public ServiceFeePartitionDao serviceFeePartitionDao() {
        return new ServiceFeePartitionDao(pgNamedParameterJdbcTemplate);
    }

    @Bean
    public ServiceFeePartitionService serviceFeePartitionService() {
        return new ServiceFeePartitionService(serviceFeePartitionDao());
    }

    @Bean
    OrderTransactionServiceImpl orderTransactionService() {
        return new OrderTransactionServiceImpl(
                dbOrderDao(),
                serviceFeePartitionService()
        );
    }

    @Bean
    public ReceiptItemDao receiptItemDao() {
        return new ReceiptItemDao(pgNamedParameterJdbcTemplate);
    }

}
