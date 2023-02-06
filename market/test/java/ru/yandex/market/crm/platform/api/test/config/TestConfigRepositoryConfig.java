package ru.yandex.market.crm.platform.api.test.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.platform.services.config.ConfigRepository;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.TargetConfig;
import ru.yandex.market.crm.platform.config.TestConfigs;
import ru.yandex.market.crm.platform.services.config.ConfigRepositoryImpl;
import ru.yandex.market.crm.platform.services.config.ConfigurationInitializer;
import ru.yandex.market.crm.platform.models.MinimalExample;
import ru.yandex.market.crm.platform.models.NoTimeExample;
import ru.yandex.market.crm.platform.models.TestCartEvent;
import ru.yandex.market.crm.platform.models.UidRelation;
import ru.yandex.market.crm.platform.models.OrderDvk;
import ru.yandex.market.crm.platform.services.mis.MarketIdentificationDAOImpl;

/**
 * @author apershukov
 */
@Configuration
public class TestConfigRepositoryConfig {

    public static final String CART_EVENT_FACT = "TestCartEventFact";
    public static final String MINIMAL_EXAMPLE = "MinimalExample";
    public static final String MAXIMAL_EXAMPLE = "MaximalExample";
    public static final String NO_TIME_EXAMPLE = "NoTimeExample";
    public static final String UID_RELATION = MarketIdentificationDAOImpl.FACT_ID;
    public static final String ORDER_DVK = "OrderDvk";

    @Bean
    public ConfigRepository configRepository() {
        Map<String, FactConfig> configs = new HashMap<>();

        configs.put(CART_EVENT_FACT.toLowerCase(), TestConfigs.factConfig(CART_EVENT_FACT, TestCartEvent.class));
        configs.put(MINIMAL_EXAMPLE.toLowerCase(), TestConfigs.factConfig(MINIMAL_EXAMPLE, MinimalExample.class));
        configs.put(MAXIMAL_EXAMPLE.toLowerCase(), TestConfigs.factConfig(MAXIMAL_EXAMPLE, MinimalExample.class));
        configs.put(NO_TIME_EXAMPLE.toLowerCase(), TestConfigs.factConfig(NO_TIME_EXAMPLE, NoTimeExample.class));
        configs.put(UID_RELATION.toLowerCase(), TestConfigs.factConfig(UID_RELATION, UidRelation.class));
        configs.put(ORDER_DVK.toLowerCase(), TestConfigs.factConfig(ORDER_DVK, OrderDvk.class));

        return new ConfigRepositoryImpl(new ConfigurationInitializer() {
            @Override
            public Map<String, FactConfig> get() {
                return configs;
            }

            @Override
            public Multimap<String, TargetConfig> getFactsIndex() {
                return Multimaps.newMultimap(new HashMap<>(), ArrayList::new);
            }

            @Override
            public Multimap<String, FactConfig> getTargetsIndex() {
                return Multimaps.newMultimap(new HashMap<>(), ArrayList::new);
            }
        });
    }
}
