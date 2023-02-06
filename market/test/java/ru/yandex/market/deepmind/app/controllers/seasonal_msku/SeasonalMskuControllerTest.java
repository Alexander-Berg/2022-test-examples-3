package ru.yandex.market.deepmind.app.controllers.seasonal_msku;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.controllers.seasonal_msku.UpdateSeasonalMskuRequest.UpdateSeasonalMskuState;
import ru.yandex.market.deepmind.app.controllers.seasonal_msku.UpdateSeasonalMskuRequest.UpdateSeasonalMskuType;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilterConverter;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalDictionary;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalMsku;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalDictionaryRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;

public class SeasonalMskuControllerTest extends DeepmindBaseAppDbTestClass {

    private SeasonalMskuController seasonalMskuController;

    @Resource
    private SeasonalDictionaryRepository seasonalDictionaryRepository;
    @Resource
    private SeasonalMskuRepository seasonalMskuRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;

    @Before
    public void setUp() throws Exception {
        var deepmindCategoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        var backgroundServiceMock = new BackgroundServiceMock();

        var extendedMskuFilterConverter = new ExtendedMskuFilterConverter(
            deepmindSupplierRepository,
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            deepmindCategoryCachingServiceMock
        );

        seasonalMskuController = new SeasonalMskuController(seasonalDictionaryRepository, seasonalMskuRepository,
            transactionTemplate, extendedMskuFilterConverter, deepmindMskuRepository, backgroundServiceMock);
        seasonalMskuController.setBatchSize(1000);
    }

    @Test
    public void list() {
        prepareData();

        var seasonalMskus = seasonalMskuController.list(List.of(100L, 200L, 300L));

        Assertions.assertThat(seasonalMskus)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of(1L)),
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(2L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(2L, 3L))
            );
    }

    @Test
    public void filterData() {
        prepareData();

        var seasonalMskus = seasonalMskuController.filter(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(100L, 300L)), OffsetFilter.all());

        Assertions.assertThat(seasonalMskus)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of(1L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(2L, 3L))
            );
    }

    @Test
    public void filterBySeasonalId() {
        prepareData();

        var seasonalMskus1 = seasonalMskuController.filter(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(2L)), OffsetFilter.all());

        Assertions.assertThat(seasonalMskus1)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(2L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(2L, 3L))
            );

        var seasonalMskus2 = seasonalMskuController.filter(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(1L, 3L)), OffsetFilter.all());

        Assertions.assertThat(seasonalMskus2)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of(1L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(2L, 3L))
            );
    }

    @Test
    public void filterDistinct() {
        prepareData();

        // by msku ids
        var distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(100L, 200L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(200L, 300L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(2L, 3L);

        // by seasonal ids
        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(1L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(2L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(2L, 3L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(2L, 3L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(2L, 3L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(1L, 3L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L, 3L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(1L, 2L)));
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L, 3L);

        // complex
        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(100L, 200L))
            .setCategoryIds(List.of(100L))
        );
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setSeasonalIds(List.of(1L, 2L))
            .setCategoryIds(List.of(100L))
        );
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L, 3L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(100L, 200L))
            .setSeasonalIds(List.of(1L, 2L))
        );
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L);

        distinctSeasonals = seasonalMskuController.filterDistinct(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(100L, 200L))
            .setSeasonalIds(List.of(1L, 2L))
            .setCategoryIds(List.of(100L))
        );
        Assertions.assertThat(distinctSeasonals).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void changeData() {
        prepareData();

        deepmindMskuRepository.save(msku(400));

        seasonalMskuController.changeSeasonalMsku(List.of(
            // delete seasonals for 100 msku
            new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of()),
            // change seasonal ids for 200 msku
            new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(1L, 3L)),
            // dont change 300 msku
            // add new 400 msku
            new DisplaySeasonalMsku().setMskuId(400L).setSeasonalList(List.of(2L, 1L))
        ));

        var seasonalMskus = seasonalMskuController.list(List.of(100L, 200L, 300L, 400L));

        Assertions.assertThat(seasonalMskus)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of()),
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(1L, 3L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(2L, 3L)),
                new DisplaySeasonalMsku().setMskuId(400L).setSeasonalList(List.of(1L, 2L))
            );
    }

    @Test
    public void testUpdateAddDataInBackground() {
        prepareData();
        deepmindMskuRepository.save(msku(400));

        seasonalMskuController.setBatchSize(3);
        seasonalMskuController.changeSeasonalMskuAsync(new UpdateSeasonalMskuRequest()
            .setFilter(new ExtendedMskuFilter().setMarketSkuIds(List.of(100L, 300L, 400L)))
            .setStates(List.of(
                new UpdateSeasonalMskuState(1L, UpdateSeasonalMskuType.ADD),
                new UpdateSeasonalMskuState(3L, UpdateSeasonalMskuType.ADD)
            ))
        );

        var seasonalMskus = seasonalMskuController.list(List.of(100L, 200L, 300L, 400L));
        Assertions.assertThat(seasonalMskus)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of(1L, 3L)),
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(2L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(1L, 2L, 3L)),
                new DisplaySeasonalMsku().setMskuId(400L).setSeasonalList(List.of(1L, 3L))
            );
    }

    @Test
    public void testUpdateDeleteDataInBackground() {
        prepareData();
        deepmindMskuRepository.save(msku(400));

        seasonalMskuController.changeSeasonalMskuAsync(new UpdateSeasonalMskuRequest()
            .setFilter(new ExtendedMskuFilter().setMarketSkuIds(List.of(100L, 300L, 400L)))
            .setStates(List.of(
                new UpdateSeasonalMskuState(1L, UpdateSeasonalMskuType.REMOVE),
                new UpdateSeasonalMskuState(2L, UpdateSeasonalMskuType.REMOVE),
                new UpdateSeasonalMskuState(3L, UpdateSeasonalMskuType.REMOVE)
            ))
        );

        var seasonalMskus2 = seasonalMskuController.list(List.of(100L, 200L, 300L, 400L));
        Assertions.assertThat(seasonalMskus2)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of()),
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(2L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of()),
                new DisplaySeasonalMsku().setMskuId(400L).setSeasonalList(List.of())
            );
    }

    @Test
    public void testUpdateAddDeleteDataInBackground() {
        prepareData();
        deepmindMskuRepository.save(msku(400));

        seasonalMskuController.changeSeasonalMskuAsync(new UpdateSeasonalMskuRequest()
            .setFilter(new ExtendedMskuFilter().setMarketSkuIds(List.of(100L, 300L, 400L)))
            .setStates(List.of(
                new UpdateSeasonalMskuState(1L, UpdateSeasonalMskuType.ADD),
                new UpdateSeasonalMskuState(2L, UpdateSeasonalMskuType.REMOVE)
            ))
        );

        var seasonalMskus3 = seasonalMskuController.list(List.of(100L, 200L, 300L, 400L));
        Assertions.assertThat(seasonalMskus3)
            .containsExactlyInAnyOrder(
                new DisplaySeasonalMsku().setMskuId(100L).setSeasonalList(List.of(1L)),
                new DisplaySeasonalMsku().setMskuId(200L).setSeasonalList(List.of(2L)),
                new DisplaySeasonalMsku().setMskuId(300L).setSeasonalList(List.of(1L, 3L)),
                new DisplaySeasonalMsku().setMskuId(400L).setSeasonalList(List.of(1L))
            );
    }

    private void prepareData() {
        deepmindMskuRepository.save(msku(100L), msku(200L), msku(300L));

        seasonalDictionaryRepository.save(
            new SeasonalDictionary().setId(1L).setName("Новогодний"),
            new SeasonalDictionary().setId(2L).setName("Масленица"),
            new SeasonalDictionary().setId(3L).setName("Весна")
        );

        // mskuId: 100L -> Новогодний
        // mskuId: 200L -> Масленица
        // mskuId: 300L -> Масленица, Весна
        seasonalMskuRepository.save(
            new SeasonalMsku().setMskuId(100L).setSeasonalId(1L),
            new SeasonalMsku().setMskuId(200L).setSeasonalId(2L),
            new SeasonalMsku().setMskuId(300L).setSeasonalId(2L),
            new SeasonalMsku().setMskuId(300L).setSeasonalId(3L)
        );
    }

    private Msku msku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setCategoryId(100L)
            .setVendorId(-1L)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.SKU);
    }
}
