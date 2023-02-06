package ru.yandex.direct.intapi.fatconfiguration;

import java.util.HashMap;
import java.util.List;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.direct.common.mobilecontent.MobileContentStoreType;
import ru.yandex.direct.common.mobilecontent.MobileContentYtTable;
import ru.yandex.direct.common.mobilecontent.MobileContentYtTablesConfig;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.intapi.configuration.IntapiConfiguration;
import ru.yandex.direct.intapi.entity.balanceclient.service.NotifyPromocodeService;
import ru.yandex.direct.intapi.mobilecontent.utils.ApiMobileTableUtils;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.ytcomponents.config.DirectDynTablesMapping;
import ru.yandex.direct.ytcomponents.config.OverridableTableMappings;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.TestYtClusterConfigProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.dynamic.TableMappings;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicConfig;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicTypesafeConfig;
import ru.yandex.direct.ytwrapper.dynamic.YtQueryComposer;

import static ru.yandex.direct.grid.schema.yt.Tables.EXTDATAMOBILE_DIRECT;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyPromocodeService.NOTIFY_PROMOCODE_LOCK_BUILDER;
import static ru.yandex.direct.intapi.utils.TablesUtils.generatePrefix;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

@Configuration
@Import({IntapiConfiguration.class, CoreTestingConfiguration.class})
public class IntapiTestingConfiguration {

    @MockBean
    private RecentStatisticsService recentStatisticsService;

    @MockBean(name = NOTIFY_PROMOCODE_LOCK_BUILDER)
    private NotifyPromocodeService.LockBuilder notifyPromocodeLockBuilder;

    @Bean
    public YtDynamicConfig ytDynamicConfig(DirectConfig directConfig) {
        DirectConfig ytConfig = directConfig.getBranch("yt");
        return new YtDynamicTypesafeConfig(ytConfig.getConfig());
    }

    @Bean
    public OverridableTableMappings overridableTableMappings(DirectDynTablesMapping directDynTablesMapping) {
        return new OverridableTableMappings(directDynTablesMapping);
    }

    @Bean
    public TableMappings tableMappings(OverridableTableMappings overridableTableMappings) {
        return overridableTableMappings;
    }

    @Bean
    public MobileContentYtTablesConfig mobileContentYtTables(DirectConfig directConfig) {
        var shops = new HashMap<String, MobileContentYtTable>();
        DirectConfig mobileContentData = directConfig.getBranch("mobile_content_data");
        var tableName = EXTDATAMOBILE_DIRECT.getName();
        var clusterList = List.of(YT_LOCAL.getName());
        var temporaryPath = "//" + YtPathUtil.TEMPORARY_NODE_NAME;

        for (var type : MobileContentStoreType.values()) {
            var tablePath = YtPathUtil.generatePath(temporaryPath, generatePrefix(), tableName);
            var typeName = type.getName();
            mobileContentData.findBranch(typeName)
                    .ifPresent(c -> shops.put(typeName, new MobileContentYtTable(clusterList, tablePath)));
        }
        return new MobileContentYtTablesConfig(shops);
    }

    @Bean
    @Lazy
    public YtProvider ytProvider(TableMappings tableMappings, YtDynamicConfig dynConfig) {
        return new YtProvider(new TestYtClusterConfigProvider(), dynConfig, new YtQueryComposer(tableMappings, true));
    }

    @Bean
    public StatTablesUtils statTablesUtils(YtProvider ytProvider, OverridableTableMappings tableMappings) {
        return new StatTablesUtils(ytProvider, tableMappings);
    }

    @Bean
    public ApiMobileTableUtils apiMobileTableUtils(YtProvider ytProvider,
                                                   MobileContentYtTablesConfig mobileContentYtTables) {
        return new ApiMobileTableUtils(ytProvider, mobileContentYtTables);
    }
}
