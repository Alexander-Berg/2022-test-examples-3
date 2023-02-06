package ru.yandex.market.billing.imports.globalcontract;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.factoring.ContractPayoutFrequencyDao;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.model.PartnerContractType;
import ru.yandex.market.yt.YtClusterStub;

import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultShopContract;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.toYTreeMapNode;

@ParametersAreNonnullByDefault
class GlobalPartnerContractImportServiceTest extends FunctionalTest {
    private static final String TEST_DATE = "2021-11-22";
    private static final String SHOP_TABLES_DIR = "//global/shop-contracts/dir/";

    @Autowired
    private GlobalPartnerContractDao partnerContractDao;

    @Autowired
    private ContractPayoutFrequencyDao contractPayoutFrequencyDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private GlobalPartnerContractImportService service;

    private YtClusterStub hahn = new YtClusterStub("hahn");
    private YtClusterStub arnold = new YtClusterStub("arnold");

    @BeforeEach
    void setUp() {
        GlobalPartnerContractYtDao ytDao = new GlobalPartnerContractYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}),
                SHOP_TABLES_DIR
        );

        Clock clock = Clock.fixed(Instant.parse("2021-12-02T16:44:00Z"), ZoneId.systemDefault());

        service = new GlobalPartnerContractImportService(ytDao, partnerContractDao, contractPayoutFrequencyDao,
                transactionTemplate, clock);
    }

    @Test
    void process_emptyTable() {
        List.of(hahn, arnold).forEach(cluster ->
                cluster.addTable(SHOP_TABLES_DIR + TEST_DATE, List.of())
        );

        service.process(LocalDate.parse(TEST_DATE));
    }

    @DbUnitDataSet(before = "GlobalPartnerContractImportServiceTest.before.csv",
            after = "GlobalPartnerContractImportServiceTest.after.csv")
    @Test
    void process() {
        var date = LocalDate.parse(TEST_DATE);
        var contractDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        var oldContractsDate = LocalDate.parse("2021-11-10").atStartOfDay(ZoneId.systemDefault()).toInstant();

        var contracts = List.of(
                // Контракты нового магазина
                defaultShopContract(1, PartnerContractType.INCOME, contractDate)
                        .setBalanceContract("140701/21").build(),
                defaultShopContract(1, PartnerContractType.OUTCOME, contractDate)
                        .setBalanceContract("140702/21").build(),
                // Контракты ранее существовавшего магазина, никаких изменений не произошло
                defaultShopContract(2, PartnerContractType.INCOME, oldContractsDate).build(),
                defaultShopContract(2, PartnerContractType.OUTCOME, oldContractsDate).build(),
                // Новый контракт (даже один) ранее существовавшего магазина "убивает" предыдущие контракты магазина
                defaultShopContract(3, PartnerContractType.OUTCOME, contractDate).build()
        );
        List.of(hahn, arnold).forEach(cluster ->
                cluster.addTable(
                        SHOP_TABLES_DIR + TEST_DATE,
                        contracts.stream()
                                .map(contract -> toYTreeMapNode(contract, date))
                                .collect(Collectors.toList())
                )
        );

        service.process(date);
    }
}
