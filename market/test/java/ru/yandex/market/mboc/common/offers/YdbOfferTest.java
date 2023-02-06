package ru.yandex.market.mboc.common.offers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.yandex.ydb.table.query.Params;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.ydb.dao.YdbQueryResult;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.ydb.YdbOffer;
import ru.yandex.market.mboc.common.offers.ydb.YdbOfferFilter;
import ru.yandex.market.mboc.common.offers.ydb.YdbOfferRepository;
import ru.yandex.market.mboc.common.offers.ydb.YdbOfferSyncService;
import ru.yandex.market.mboc.common.utils.BaseYdbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/*
Plan B to debug this test manually:
https://wiki.yandex-team.ru/konturassortmentprocess/howto/ydb-v-mboc/#dockerdljalokalnogozapuskaydbmacos

1. Start-up docker container manually:
docker run -d --rm --name ydb-local -h localhost \
  -p 2135:2135 -p 8765:8765 -p 2136:2136 \
  -v $(pwd)/ydb_certs:/ydb_certs -v $(pwd)/ydb_data:/ydb_data \
  -e YDB_DEFAULT_LOG_LEVEL=NOTICE \
  -e GRPC_TLS_PORT=2135 -e GRPC_PORT=2136 -e MON_PORT=8765 \
  registry.yandex.net/yandex-docker-local-ydb:latest

2. Add ENV variables to startup config in Idea
YA_TEST_RUNNER=0;YDB_DATABASE=/local;YDB_ENDPOINT=localhost:2136
 */
@Slf4j
public class YdbOfferTest extends BaseYdbTestClass {

    @Autowired
    private YdbOfferRepository ydbOfferRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @PostConstruct
    public void setup() {
        if (!tableExists("offer")) {
            createTableFor(YdbOffer.class, "offer");
            log.info("table created");
        }
    }

    @Before
    public void prepare() {
        ydbClient.executeRw("", "delete from `offer`;",
            Params.empty(), Duration.of(300, ChronoUnit.SECONDS));
    }

    @Test
    public void sampleCrudTestForYdb() {
        var offer = OfferTestUtils.simpleOffer();
        var ydbOffer = YdbOffer.from(offer);
        ydbOfferRepository.upsertAsync(List.of(ydbOffer)).join();

        var ydbOffers = ydbOfferRepository.selectAll(100);
        Assertions.assertThat(ydbOffers).hasSize(1);
        Assertions.assertThat(ydbOffers.get(0)).usingRecursiveComparison().isEqualTo(ydbOffer);
    }

    @Test
    public void observerInsertsTest() {
        supplierRepository.insertOrUpdate(OfferTestUtils.simpleSupplier());
        storageKeyValueService.putValue(YdbOfferSyncService.YDB_SYNC_ENABLED_KEY, true);

        var offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        var skuKey = offer.getBusinessSkuKey();
        var ydbOffers = ydbOfferRepository.selectAll(100);
        Assertions.assertThat(ydbOffers).hasSize(1);
        var ydbOffer = ydbOffers.stream()
            .filter(one -> one.getBusinessSkuKey().equals(skuKey))
            .findFirst().orElseThrow(RuntimeException::new);
        Assertions.assertThat(ydbOffer).usingRecursiveComparison().isEqualTo(YdbOffer.from(offer));
    }

    @Test
    public void offerColumnsTest() {
        var ydbColumns = getDefinedColumnsFor(YdbOffer.class);
        var ydbCompositeFields = getCompositeFields(YdbOffer.class);
        var skipped = Set.of(
            "content_comment", //content_comments used instead
            "service_offers", // not needed in context of YDB Offer
            "supplier_id",   // business_id used instead

            /* not mapped to up-to-date pg offer */
            "datacamp_content_ts",
            "content_model_mapping_category_id",
            "supplier_model_mapping_category_id",
            "markup_status"
        );

        var pgColumns = jdbcTemplate.query("SELECT distinct column_name\n" +
                "  FROM information_schema.columns\n" +
                " WHERE table_schema = 'mbo_category'\n" +
                "   AND table_name   = 'offer'\n",
            Map.of(),
            (rs, n) -> rs.getString("column_name"));

        var notMapped = pgColumns.stream()
            .filter(Predicate.not(ydbColumns::contains))
            .filter(Predicate.not(skipped::contains))
            .filter(one -> notPrefixedWithComposite(ydbCompositeFields, one))
            .collect(Collectors.toList());

        Assertions.assertThat(notMapped).isEmpty();
    }

    @Test
    public void ydbTransactionTest() {
        storageKeyValueService.putValue(YdbOfferSyncService.YDB_SYNC_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();
        Offer offer = OfferTestUtils.simpleOffer();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        offerRepository.insertOffer(offer);

        YdbQueryResult<YdbOffer> select =
            ydbOfferRepository.selectForUpdate(
                new YdbOfferFilter().setLimit(1).setOrderBy("")
                    .setKeys(List.of(new BusinessSkuKey(offer.getBusinessId(), offer.getShopSku()))));
        List<YdbOffer> result = select.getResult();
        Assertions.assertThat(result).hasSize(1);

        String newTitle = "very new title";
        Assert.assertNotEquals(offer.getTitle(), newTitle);
        result.forEach(o -> o.setTitle(newTitle));

        ydbOfferRepository.upsert(result, select.getTxId(), false);

        YdbQueryResult<YdbOffer> otherSelect =
            ydbOfferRepository.select(new YdbOfferFilter().setLimit(1).setOrderBy("")
                .setKeys(List.of(new BusinessSkuKey(offer.getBusinessId(), offer.getShopSku()))));

        Assertions.assertThat(otherSelect.getResult()).hasSize(1);
        otherSelect.getResult().forEach(o -> Assert.assertEquals(offer.getTitle(), o.getTitle()));

        ydbOfferRepository.commit(select.getTxId());

        otherSelect = ydbOfferRepository.select(new YdbOfferFilter().setLimit(1).setOrderBy("")
            .setKeys(List.of(new BusinessSkuKey(offer.getBusinessId(), offer.getShopSku()))));
        otherSelect.getResult().forEach(o -> Assert.assertEquals(newTitle, o.getTitle()));
    }

    private boolean notPrefixedWithComposite(Set<String> compositeFields, String pgField) {
        return !compositeFields.stream()
            .map(pgField::startsWith)
            .reduce(Boolean::logicalOr)
            .orElse(false);
    }
}
