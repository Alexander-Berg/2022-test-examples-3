package ru.yandex.market.crm.campaign.yt;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.campaign.services.ExecutorsConfig;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.crm.yql.client.YqlClient;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.yt.tx.TxRunner;
import ru.yandex.market.crm.yt.tx.YtTransactionDefinition;
import ru.yandex.market.crm.yt.utils.YtUtil;
import ru.yandex.market.mcrm.http.HttpClientConfiguration;
import ru.yandex.yt.rpcproxy.ETransactionType;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransactionOptions;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.rpc.RpcError;
import ru.yandex.yt.ytclient.tables.TableSchema;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        HttpClientConfiguration.class,
        YtConfig.class,
        ExecutorsConfig.class,
        YqlClient.class,
        TemplateService.class,
        JacksonConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class YtDinaTableTest {

    private static final Logger LOG = LoggerFactory.getLogger(YtDinaTableTest.class);

    private static final TableSchema TABLE_SCHEMA = YtUtil.getTableSchema("dina.yson");

    private static final YtTransactionDefinition TXD = new YtTransactionDefinition(ETransactionType.TT_TABLET)
            .setTimeout(Duration.ofSeconds(5));

    static {
        TXD.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Inject
    YtClient ytClient;
    @Inject
    YtFolders ytFolders;
    @Inject
    TxRunner txRunner;
    @Inject
    ApiServiceClient rpcClient;

    @Test
    public void checkRowLockConflict() {
        YPath table = ytFolders.getProtectedTmp().child("test");
        ytClient.createAndMountTable(table, "dina.yson");
        TableSchema sch = YtUtil.tableSchemaSelectCols(TABLE_SCHEMA, ImmutableSet.of("id", "value"));
        try {
            txRunner.runTabletTx(Duration.ofSeconds(5), () -> {
                ytClient.modifyRowsAsync(table, TABLE_SCHEMA, Entity.class,
                        Arrays.asList(
                                new Entity("0", "0", null),
                                new Entity("1", "1", 777L),
                                new Entity("2", null, 777L),
                                new Entity(null, null, null)
                        ),
                        Arrays.asList(
                                new Entity("4", "", null),
                                new Entity("5", "5", null),
                                new Entity(null, "6", null),
                                new Entity("7", "7", 7L)
                        ),
                        null
                ).join();
            });
        } catch (CompletionException e) {
            if (Objects.isNull(e.getCause())) {
                throw e;
            }
            if (e.getCause() instanceof RpcError) {
                RpcError rpcError = (RpcError) e.getCause();
                if (rpcError.getError().getCode() == 1700) {
                    LOG.info("--------- LOCK ROW CONFLICT DETECTED ------------");
                    return;
                }
            }
            throw e;
        }
        List<Entity> list =
                txRunner.call(new YtTransactionDefinition(ETransactionType.TT_TABLET).setTimeout(Duration.ofSeconds(5)),
                        () -> ytClient.lookup(table, TABLE_SCHEMA, Entity.class, Arrays.asList("0", "1", "2", "3", "4"
                                , "5",
                                "6", "7")
                        )
                );
        LOG.info("{}", list);
    }

    private void insertRow(String table, String key) {
        ApiServiceTransaction t = rpcClient.startTransaction(
                new ApiServiceTransactionOptions(ETransactionType.TT_TABLET).setSticky(true)
                        .setTimeout(Duration.ofSeconds(10)).setAutoAbort(true)
        ).join();
        t.modifyRows(
                new ModifyRowsRequest(table, TABLE_SCHEMA).addInsert(Arrays.asList(key, "4444"))
        ).join();
        t.commit().join();
    }

}
