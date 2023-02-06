package ru.yandex.market.core.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.datacamp.shopdata.ShopsDataReaderProvider;
import ru.yandex.market.sqb.service.config.reader.ClasspathConfigurationReader;

/**
 * Тестовый конфиг с ресурсами шопсдаты.
 * Берем настоящую шопсдату из автоконфигураций.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Configuration
public class ShopsDataTestConfig {

    @Bean
    public ShopsDataReaderProvider shopsDataReaderProvider() {
        Map<CampaignType, ClasspathConfigurationReader> readers = Map.of(
                CampaignType.SHOP, new ClasspathConfigurationReader(getClass(), "/mbi-billing/shops_data.xml"),
                CampaignType.SUPPLIER, new ClasspathConfigurationReader(getClass(), "/mbi-billing/suppliers_data.xml")
        );

        return readers::get;
    }
}
