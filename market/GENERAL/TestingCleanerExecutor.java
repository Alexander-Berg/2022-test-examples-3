/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 10.05.2006</p>
 * <p>Time: 18:19:25</p>
 */

package ru.yandex.market.billing.tasks;

import java.util.HashSet;
import java.util.Set;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.ProtocolTransactionCallback;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.mbi.tms.monitor.MonitorFriendly;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Сбрасывает флаг {@link ParamType#SHOP_TESTING} у магазинов, которые в данный момент не тестируются.
 */
public class TestingCleanerExecutor implements Executor, MonitorFriendly {

    public static final String DESCRIPTION = "description";
    private static final String GET_TESTING_SQL =
            "select entity_id datasource_id " +
                    "from shops_web.param_value " +
                    "where param_type_id = 10 " +
                    "  and num_value = 1  " +
                    "  and entity_id > 0 " +
                    "minus " +
                    "select datasource_id " +
                    "from shops_web.datasources_in_testing " +
                    "where in_progress = 1";
    private final JdbcTemplate shopJdbcTemplate;
    private final ParamService paramService;
    private final ProtocolService protocolService;

    public TestingCleanerExecutor(JdbcTemplate shopJdbcTemplate,
                                  ParamService paramService,
                                  ProtocolService protocolService) {
        this.shopJdbcTemplate = shopJdbcTemplate;
        this.paramService = paramService;
        this.protocolService = protocolService;
    }

    public void doJob(JobExecutionContext context) {
        Set<Long> datasourceIds = shopJdbcTemplate.query(
                GET_TESTING_SQL,
                rs -> {
                    Set<Long> result = new HashSet<>();
                    while (rs.next()) {
                        result.add(rs.getLong(1));
                    }
                    return result;
                }
        );

        if (CollectionUtils.isNonEmpty(datasourceIds)) {
            doCleaning(datasourceIds, context);
        }
    }

    private void doCleaning(final Set<Long> datasourceIds, JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        String description = jobDetail.getJobDataMap().getString(DESCRIPTION);

        protocolService.executeInTransaction(new ProtocolTransactionCallback(
                new SystemActionContext(ActionType.MANAGE_SHOP_TESTING, description)
        ) {
            public Object doInProtocolTransaction(TransactionStatus status, long actionId) {
                for (Long datasourceId : datasourceIds) {
                    paramService.setParam(new BooleanParamValue(ParamType.SHOP_TESTING.getId(), 0,
                            datasourceId, false), actionId);
                }
                return null;
            }
        });
    }

}
