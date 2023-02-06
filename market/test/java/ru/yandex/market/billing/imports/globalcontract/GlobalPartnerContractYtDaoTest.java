package ru.yandex.market.billing.imports.globalcontract;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.core.partner.model.PartnerContractType;
import ru.yandex.market.yt.YtClusterStub;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultShopContract;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.toYTreeMapNode;

@ParametersAreNonnullByDefault
class GlobalPartnerContractYtDaoTest extends FunctionalTest {

    private static final String TEST_DATE = "2021-11-11";
    private static final String SHOP_CONTRACT_TABLES_DIR = "//global/shop_contract/";

    private YtClusterStub hahn = new YtClusterStub("hahn");
    private YtClusterStub arnold = new YtClusterStub("arnold");

    private GlobalPartnerContractYtDao ytDao;

    @BeforeEach
    void setUp() {
        ytDao = new GlobalPartnerContractYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}),
                SHOP_CONTRACT_TABLES_DIR
        );
    }

    @Test
    void fetchShopContracts() {
        var date = LocalDate.parse(TEST_DATE);
        var incomeContract = defaultShopContract(PartnerContractType.INCOME)
                .setBalanceContract("140748/21")
                .build();
        var outcomeContract = GlobalCheckouterShopContract.builder().of(incomeContract)
                .setContractType(PartnerContractType.OUTCOME)
                .build();
        var shopContracts = List.of(incomeContract, outcomeContract);

        hahn.addTable(
                SHOP_CONTRACT_TABLES_DIR + TEST_DATE,
                StreamEx.of(shopContracts).map(contract -> toYTreeMapNode(contract, date)).toList()
        );

        var fetchedContracts = ytDao.getShopContracts(date);

        assertThat(fetchedContracts).usingRecursiveFieldByFieldElementComparator()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(shopContracts);
    }
}
