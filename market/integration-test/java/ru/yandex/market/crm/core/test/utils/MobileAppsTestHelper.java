package ru.yandex.market.crm.core.test.utils;

import java.util.List;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.mobile.features.Feature;
import ru.yandex.market.crm.core.domain.mobile.features.FeatureDescription;
import ru.yandex.market.crm.core.domain.mobile.features.FeatureFlag;
import ru.yandex.market.crm.core.domain.mobile.features.FrequencyThrottling;
import ru.yandex.market.crm.core.domain.mobile.features.SubscriptionsFeature;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@Component
public class MobileAppsTestHelper implements StatefulHelper {

    public static SubscriptionsFeature subscriptions(YPath subscriptionsPath) {
        var feature = new SubscriptionsFeature();
        feature.setSubscriptionsPath(subscriptionsPath);
        return feature;
    }

    private static FeatureFlag flag(Feature feature) {
        return new FeatureFlag(feature);
    }

    private final JdbcTemplate jdbcTemplate;
    private final JsonSerializer jsonSerializer;

    private final MobileApplication marketApp;

    public MobileAppsTestHelper(@Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                                JsonSerializer jsonSerializer,
                                YtTestTables ytTestTables,
                                CommunicationTables communicationTables) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSerializer = jsonSerializer;

        var subscriptionsFeature = new SubscriptionsFeature();
        subscriptionsFeature.setSubscriptionsPath(ytTestTables.getChytUuidsWithSubscriptions());

        var throttlingFeature = new FrequencyThrottling();
        throttlingFeature.setDefaultDailyLimit(3);
        throttlingFeature.setCommunicationsPath(communicationTables.pushTable());

        this.marketApp = new MobileApplication();
        marketApp.setId(MobileApplication.MARKET_APP);
        marketApp.setMetricaAppId(MetricaMobileApp.BERU.getId());
        marketApp.setDeviceIdsTable(ytTestTables.getChytUuidsWithTokens());
        marketApp.setFeatures(List.of(
                subscriptionsFeature,
                throttlingFeature,
                flag(Feature.BANNED_PROMOCODES),
                flag(Feature.GLOBAL_CONTROL)
        ));
    }

    public void insertApplication(String id,
                                  int metricaAppId,
                                  YPath deviceIdsTable,
                                  List<FeatureDescription> features) {
        var application = new MobileApplication();
        application.setId(id);
        application.setName(id);
        application.setMetricaAppId(metricaAppId);
        application.setDeviceIdsTable(deviceIdsTable);
        application.setFeatures(features);

        jdbcTemplate.update(
                """
                INSERT INTO mobile_applications (id, config)
                VALUES (?, ?::jsonb)
                ON CONFLICT (id) DO UPDATE
                SET
                    config = EXCLUDED.config
                """,
                application.getId(),
                jsonSerializer.writeObjectAsString(application)
        );
    }

    public void prepareMarketApp() {
        insertApplication(
                marketApp.getId(),
                marketApp.getMetricaAppId(),
                marketApp.getDeviceIdsTable(),
                marketApp.getFeatures()
        );
    }

    @Override
    public void setUp() {
        prepareMarketApp();
    }

    @Override
    public void tearDown() {
    }

    public MobileApplication getMarketApp() {
        return marketApp;
    }
}
