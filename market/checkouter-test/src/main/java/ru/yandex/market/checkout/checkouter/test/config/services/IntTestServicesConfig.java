package ru.yandex.market.checkout.checkouter.test.config.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.common.util.DirectExecutor;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.storage.Storage;

@ComponentScan(basePackages = {
        "ru.yandex.market.checkout.util"
}, excludeFilters = {
        // Не подхватывать службу из push-api
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "ru.yandex.market.checkout.util" +
                ".PushApiTestSerializationService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "ru.yandex.market.checkout.util.shopapi" +
                ".ShopApiConfigurer")
})
@Configuration
public class IntTestServicesConfig {

    @Bean
    public DirectExecutor returnServiceExecutor() {
        return new DirectExecutor();
    }

    @Bean
    public DirectExecutor warrantyServiceExecutor() {
        return new DirectExecutor();
    }

    @Bean
    public ReceiptRepairHelper receiptRepairHelper() {
        return new ReceiptRepairHelper();
    }

    @Bean
    public OrderInsertHelper orderInsertHelper(Storage storage, OrderWritingDao orderWritingDao) {
        return new OrderInsertHelper(storage, orderWritingDao);
    }
}
