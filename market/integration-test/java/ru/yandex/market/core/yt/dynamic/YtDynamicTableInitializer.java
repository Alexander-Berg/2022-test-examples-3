package ru.yandex.market.core.yt.dynamic;

import java.util.function.LongSupplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.core.config.DevJdbcConfig;
import ru.yandex.market.core.delivery.region_blacklist.dao.DeliveryRegionBlacklistYtConfig;
import ru.yandex.market.core.direct.feed.YtRefreshDaoConfig;
import ru.yandex.market.core.indexer.db.session.YtFeedSessionServiceConfig;
import ru.yandex.market.core.periodic_survey.yt.LastUserSurveyIdYtConfig;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtConfig;
import ru.yandex.market.core.supplier.promo.config.PromoYtConfiguration;
import ru.yandex.market.yt.client.YtDynamicTableClientFactory;

/**
 * Временное пристанище для инициализаторов дин таблиц.
 */
@SpringJUnitConfig({
        // environment
        DevJdbcConfig.class,
//        TestingJdbcConfig.class,
//        ProductionJdbcConfig.class,
        // yt configs
        YtDynamicTableConfig.class,
        PromoYtConfiguration.class,
        YtFeedSessionServiceConfig.class,
        PeriodicSurveyYtConfig.class,
        YtRefreshDaoConfig.class,
        LastUserSurveyIdYtConfig.class,
        DeliveryRegionBlacklistYtConfig.class
})
@Disabled
class YtDynamicTableInitializer {
    private static final Logger log = LoggerFactory.getLogger(YtDynamicTableInitializer.class);

    @Value("${mbi.robot.yt.token}")
    String requiredToken;

    @Autowired
    LongSupplier ytTableVersionSupplier;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer promoDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer feedSessionDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer periodicSurveyDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer refreshFeedDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer lastUserSurveyIdDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer deliveryRegionBlacklistDynamicTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer advBlueStrategiesTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer advWhiteStrategiesTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer advSelectedPartnersTableInitializer;

    @Autowired
    YtDynamicTableClientFactory.OnDemandInitializer nettingNewbiesExportTableInitializer;

    @Test
    void initPromoTables() {
        initialize(promoDynamicTableInitializer);
    }

    @Test
    void initFeedSessionTables() {
        initialize(feedSessionDynamicTableInitializer);
    }

    @Test
    void initPeriodicSurveyTables() {
        initialize(periodicSurveyDynamicTableInitializer);
    }

    @Test
    void initRefreshFeedTables() {
        initialize(refreshFeedDynamicTableInitializer);
    }

    @Test
    void initPeriodicSurveyLastUserSurveyIdTables() {
        initialize(lastUserSurveyIdDynamicTableInitializer);
    }

    @Test
    void initDeliveryRegionBlacklistTables() {
        initialize(deliveryRegionBlacklistDynamicTableInitializer);
    }

    @Test
    void initAdvBlueStrategiesTables() {
        initialize(advBlueStrategiesTableInitializer);
    }

    @Test
    void initAdvWhiteStrategiesTables() {
        initialize(advWhiteStrategiesTableInitializer);
    }

    @Test
    void initAdvSelectedPartnersTables() {
        initialize(advSelectedPartnersTableInitializer);
    }

    @Test
    void initNettingNewbiesExportTables() {
        initialize(nettingNewbiesExportTableInitializer);
    }

    void initialize(YtDynamicTableClientFactory.OnDemandInitializer initializer) {
        if (StringUtils.isBlank(requiredToken)) {
            throw new IllegalStateException("не забудь указать" +
                    " [mbi.robot.yt.token]" +
                    " в ***-datasource.properties!");
        }
        var version = ytTableVersionSupplier.getAsLong();
        if (version <= 0) {
            throw new IllegalStateException("не забудь указать" +
                    " [ru.yandex.market.core.yt.dynamic.YtDynamicTableConfig.tableRevisionHardcoded]" +
                    " в ***-datasource.properties!");
        }
        log.info("Initializing revision [{}] with {}", version, initializer);
        initializer.configureDynamicTables();
    }
}
