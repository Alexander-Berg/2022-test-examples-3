package ru.yandex.direct.core.entity.bs;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jooq.types.ULong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.jobs.fatconfiguration.JobsFatTest;
import ru.yandex.direct.ytcomponents.config.OverridableTableMappings;
import ru.yandex.direct.ytwrapper.YtUtils;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.MountTable;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.proxy.request.StartTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bs.PhraseIdByDataMd5Repository.SCHEMA;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASEIDBYDATAMD5_BS;
import static ru.yandex.direct.ytwrapper.YtPathUtil.generateTemporaryPath;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;
import static ru.yandex.direct.ytwrapper.model.YtDynamicOperator.getWithTimeout;

@JobsFatTest
@ExtendWith(SpringExtension.class)
class PhraseIdByDataMd5RepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(PhraseIdByDataMd5RepositoryTest.class);
    private static final BigInteger MD51 = new BigInteger("18446744062736283516");
    private static final BigInteger PHRASE_ID1 = BigInteger.valueOf(1339396456L);
    private static final BigInteger MD52 = BigInteger.valueOf(3276236132L);
    private static final BigInteger PHRASE_ID2 = BigInteger.valueOf(724285616L);

    @Autowired
    private YtProvider ytProvider;

    @Autowired
    private OverridableTableMappings tableMappings;

    @Autowired
    private PhraseIdByDataMd5Repository repository;

    private String tablePath;

    @BeforeEach
    void prepare() {
        tablePath = generateTemporaryPath();

        logger.info("Map {} as {}", tablePath, PHRASEIDBYDATAMD5_BS.getName());
        tableMappings.addOverride(PHRASEIDBYDATAMD5_BS, tablePath);


        YtDynamicOperator operator = ytProvider.getDynamicOperator(YT_LOCAL);
        var createNodeReq = new CreateNode(tablePath, ObjectType.Table)
                .setRecursive(true)
                .addAttribute("dynamic", true)
                .addAttribute(YtUtils.SCHEMA_ATTR, SCHEMA.toYTree());
        operator.runRpcCommandWithTimeout(operator.getYtClient()::createNode, createNodeReq);
        logger.info("Executed CreateNode");
        getWithTimeout(operator.getYtClient().mountTableAndWaitTablets(new MountTable(YPath.simple(tablePath))),
                Duration.ofSeconds(40),
                "Failed to mount table " + PHRASEIDBYDATAMD5_BS.getName());
        logger.info("Table {} mounted", tablePath);

        ModifyRowsRequest modifyRowsRequest = new ModifyRowsRequest(tablePath, SCHEMA)
                .addInsert(List.of(MD51.longValue(), PHRASE_ID1.longValue()))
                .addInsert(List.of(ULong.valueOf("18446744001031267295").longValue(), 401088289L))
                .addInsert(List.of(70115167334L, 5269480L))
                .addInsert(List.of(MD52.longValue(), PHRASE_ID2.longValue()));

        logger.info("let's write test data to table");
        ApiServiceTransaction tx = operator.runRpcCommandWithTimeout(() -> operator.getYtClient()
                .startTransaction(StartTransaction.stickyMaster()));
        logger.info("Started tx {}", tx.getId());
        operator.runRpcCommandWithTimeout(() -> tx.modifyRows(modifyRowsRequest));
        logger.info("rows inserted");
        operator.runRpcCommandWithTimeout(tx::commit);
        logger.info("tx commited");
        logger.info("test data prepared");
    }

    @Test
    void smokeCheck() {
        List<BigInteger> hashes = List.of(MD52, BigInteger.valueOf(42L), MD51, new BigInteger("18446744001031267294"));
        // проверяем разом:
        // 1. вернулось то, что просили
        // 2. не вернулось того, чего нет в таблице
        // 3. беззнаковый long нигде "по дороге" не испортился
        assertThat(repository.getPhraseIdByMd5(hashes))
                .containsExactlyInAnyOrderEntriesOf(Map.of(MD51, PHRASE_ID1, MD52, PHRASE_ID2));
    }
}
