package ru.yandex.market.deliverycalculator.storage.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deliverycalculator.storage.config.DeliveryCalculatorStorageConfig;
import ru.yandex.market.deliverycalculator.storage.config.JpaConfig;
import ru.yandex.market.deliverycalculator.storage.repository.ProtoBucketEntityRepository;
import ru.yandex.market.deliverycalculator.storage.repository.ProtoOptionGroupEntityRepository;
import ru.yandex.market.deliverycalculator.storage.service.impl.ProtoStorageServiceImpl;
import ru.yandex.market.deliverycalculator.storage.service.impl.YaDeliveryStorageServiceImpl;

@Configuration
@Import({
        EmbeddedPostgresConfig.class,
        JpaConfig.class,
        DeliveryCalculatorStorageConfig.class
})
public class DeliveryCalculatorStorageTestConfig {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ProtoBucketEntityRepository protoBucketEntityRepository;

    @Autowired
    private ProtoOptionGroupEntityRepository protoOptionGroupEntityRepository;

    @Bean
    public ProtoStorageServiceImpl protoStorageService() {
        return new ProtoStorageServiceImpl(protoBucketEntityRepository, protoOptionGroupEntityRepository);
    }

    @Bean
    public YaDeliveryStorageServiceImpl yaDeliveryStorageService() {
        return new YaDeliveryStorageServiceImpl(transactionTemplate);
    }

}
