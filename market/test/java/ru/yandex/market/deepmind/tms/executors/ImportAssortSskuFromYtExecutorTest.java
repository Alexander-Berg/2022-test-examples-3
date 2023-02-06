package ru.yandex.market.deepmind.tms.executors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AssortSsku;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AssortSskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_test.annotation.YqlTest;

@DbUnitDataSet(dataSource = "deepmindDataSource", before = "ImportAssortSskuFromYtExecutorTest.db.before.csv")
public class ImportAssortSskuFromYtExecutorTest extends DeepmindBaseDbTestClass {
    private static final String TABLE = "//home/market/prestable/mstat/dictionaries/mbo/warehouse_service/latest";

    @Resource
    private JdbcTemplate yqlJdbcTemplate;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private AssortSskuRepository assortSskuRepository;
    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;
    @Resource
    private QueryService queryService;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private ImportAssortSskuFromYtExecutor importAssortSskuFromYtExecutor;
    private OffersConverter offersConverter;

    @Before
    public void setUp() throws Exception {
        offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        importAssortSskuFromYtExecutor = new ImportAssortSskuFromYtExecutor(
            yqlJdbcTemplate,
            YPath.simple(TABLE),
            offersConverter,
            assortSskuRepository,
            transactionHelper,
            queryService
        );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportAssortSskuFromYtExecutorTest.yql.before.csv",
        yqlMock = "ImportAssortSskuFromYtExecutorTest.yql.mock"
    )
    public void testImport() {
        importAssortSskuFromYtExecutor.execute();

        var all = assortSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("modificationTs")
            .containsExactlyInAnyOrder(
                assortSsku(1191207, "200514", "200513"),
                assortSsku(1191207, "200514", "200508"),
                assortSsku(1191207, "200514", "200509"),
                assortSsku(1191207, "200514", "200510"),
                assortSsku(1191207, "200514", "200512"),
                assortSsku(1191207, "200514", "200511"),
                assortSsku(473244, "3434303", "567563501"),
                assortSsku(473244, "3434303", "567563503"),
                assortSsku(473244, "3434303", "3434301"),
                assortSsku(1191207, "200507", "200506"),
                assortSsku(1191207, "200507", "200503"),
                assortSsku(1191207, "200507", "200502"),
                assortSsku(1191207, "200507", "200501"),
                assortSsku(1191207, "200507", "200505"),
                assortSsku(1191207, "200507", "200504"),
                assortSsku(481621, "alisa-test-2", "alisa-test-1"),
                assortSsku(481621, "alisa-test-2", "alisa-test-3")
            );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportAssortSskuFromYtExecutorTest.simple.yql.before.csv",
        yqlMock = "ImportAssortSskuFromYtExecutorTest.simple.yql.mock"
    )
    public void testImportInsertUpdate() {
        var assortSsku1 = assortSskuRepository.save(assortSsku(473244, "3434303", "567563501"));
        var assortSsku2 = assortSskuRepository.save(assortSsku(473244, "3434303", "ssku-to-delete"));
        var assortSsku3 = assortSskuRepository.save(assortSsku(100500, "assort-ssku-to-delete", "ssku1"));
        var assortSsku4 = assortSskuRepository.save(assortSsku(100500, "assort-ssku-to-delete", "ssku2"));
        var assortSsku5 = assortSskuRepository.save(assortSsku(481621, "alisa-test-2", "alisa-test-1"));

        importAssortSskuFromYtExecutor.execute();

        var all = assortSskuRepository.findAllMap();
        Assertions.assertThat(all.values())
            .usingElementComparatorIgnoringFields("modificationTs")
            .containsExactlyInAnyOrder(
                assortSsku(473244, "3434303", "567563501"),
                assortSsku(473244, "3434303", "567563503"),
                assortSsku(473244, "3434303", "3434301"),
                assortSsku(481621, "alisa-test-2", "alisa-test-1"),
                assortSsku(481621, "alisa-test-2", "alisa-test-3")
            );

        // отдельно проверим, что у некоторых строк не поменялся modified_ts
        Assertions.assertThat(all.get(new ServiceOfferKey(473244, "567563501")))
            .extracting(AssortSsku::getModificationTs)
            .isEqualTo(assortSsku1.getModificationTs());
        Assertions.assertThat(all.get(new ServiceOfferKey(481621, "alisa-test-1")))
            .extracting(AssortSsku::getModificationTs)
            .isEqualTo(assortSsku5.getModificationTs());
    }

    private AssortSsku assortSsku(int supplierId, String assortSsku, String subSsku) {
        return new AssortSsku()
            .setSupplierId(supplierId).setSubSsku(subSsku)
            .setAssortSsku(assortSsku);
    }
}
