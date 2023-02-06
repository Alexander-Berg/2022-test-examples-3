package ru.yandex.market.deepmind.app.controllers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.DisplaySeason;
import ru.yandex.market.deepmind.app.pojo.SeasonWebFilter;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.web.DataPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of {@link SeasonController}.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SeasonControllerTest extends DeepmindBaseAppDbTestClass {

    private static final String SEASON_NAME = "seasonName";
    private static final String SEASON_NAME2 = "seasonName2";

    private SeasonController seasonController;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MskuRepository deepmindMskuRepository;

    @Autowired
    private MskuStatusRepository mskuStatusRepository;

    private Season season;
    private Season season2;
    private EnhancedRandom mskuRandom;

    @Before
    public void setUp() {
        seasonController = new SeasonController(seasonRepository, mskuStatusRepository, deepmindMskuRepository,
            TransactionHelper.MOCK);

        mskuRandom = TestUtils.createMskuRandom();
        season = seasonRepository.save(new Season().setName(SEASON_NAME));
        season2 = seasonRepository.save(new Season().setName(SEASON_NAME2));
        Stream.iterate(10, i -> i + 1).limit(100)
            .forEach(i -> seasonRepository.save(new Season().setName(String.valueOf(i))));
    }

    @Test
    public void testList() throws Exception {
        DataPage<DisplaySeason> page = seasonController.list(new SeasonWebFilter().setIds(List.of(season.getId(),
            season2.getId())), OffsetFilter.all());

        assertThat(page.getItems())
            .hasSize(2)
            .extracting(s -> s.getSeason().getId())
            .containsExactlyInAnyOrder(season.getId(), season2.getId());
    }

    @Test
    public void testOffset() throws Exception {
        DataPage<DisplaySeason> page = seasonController.list(new SeasonWebFilter(), OffsetFilter.offset(50));

        assertThat(page.getTotalCount()).isEqualTo(52);
        assertThat(page.getItems()).hasSize(52);
    }

    @Test
    public void testLimit() throws Exception {
        DataPage<DisplaySeason> page = seasonController.list(new SeasonWebFilter(), OffsetFilter.limit(10));

        assertThat(page.getTotalCount()).isEqualTo(10);
        assertThat(page.getItems()).hasSize(10);
    }

    @Test
    public void testCreate() throws Exception {
        DisplaySeason created = seasonController.create(
            new DisplaySeason()
                .setupSeason(s -> s.setName("New!"))
                .setPeriods(Collections.singletonList(
                    new SeasonPeriod()
                        .setWarehouseId(1L)
                        .setFromMmDd("12-01")
                        .setToMmDd("12-31")
                        .setDeliveryFromMmDd("12-02")
                        .setDeliveryToMmDd("12-30")
                )));

        assertThat(created.getSeason().getId()).isGreaterThan(0);
        assertThat(created.getSeason().getName()).isEqualTo("New!");
        assertThat(created.getPeriods()).hasSize(1);

        SeasonRepository.SeasonWithPeriods fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(created.getSeason().getId())).get(0);

        assertThat(fromDb.getName()).isEqualTo(created.getSeason().getName());
        assertThat(fromDb.getPeriods()).hasSize(1);
        assertThat(fromDb.getPeriods().get(0)).satisfies(period -> {
            assertThat(period.getWarehouseId()).isEqualTo(1L);
            assertThat(period.getFromMmDd()).isEqualTo("12-01");
            assertThat(period.getToMmDd()).isEqualTo("12-31");
            assertThat(period.getDeliveryFromMmDd()).isEqualTo("12-02");
            assertThat(period.getDeliveryToMmDd()).isEqualTo("12-30");
            assertThat(period.getSeasonId()).isEqualTo(created.getSeason().getId());
        });
    }

    @Test
    public void testUpdate() throws Exception {
        DisplaySeason updated = seasonController.update(
            new DisplaySeason()
                .setupSeason(s -> s
                    .setId(season.getId())
                    .setName("New!")
                    .setModifiedAt(season.getModifiedAt()))
                .setPeriods(Collections.singletonList(
                    new SeasonPeriod().setWarehouseId(1L).setFromMmDd("12-01").setToMmDd("12-31")
                )));

        assertThat(updated.getSeason().getId()).isEqualTo(season.getId());
        assertThat(updated.getSeason().getName()).isEqualTo("New!");
        assertThat(updated.getPeriods()).hasSize(1);

        SeasonRepository.SeasonWithPeriods fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(updated.getSeason().getId())).get(0);

        assertThat(fromDb.getName()).isEqualTo(updated.getSeason().getName());
        assertThat(fromDb.getPeriods()).hasSize(1);
        assertThat(fromDb.getPeriods().get(0)).satisfies(period -> {
            assertThat(period.getWarehouseId()).isEqualTo(1L);
            assertThat(period.getFromMmDd()).isEqualTo("12-01");
            assertThat(period.getToMmDd()).isEqualTo("12-31");
            assertThat(period.getSeasonId()).isEqualTo(updated.getSeason().getId());
        });
    }

    @Test
    public void testUpdateDeletePeriod() throws Exception {
        seasonController.update(
            new DisplaySeason()
                .setupSeason(s -> s
                    .setId(season.getId())
                    .setName("New!")
                    .setModifiedAt(season.getModifiedAt()))
                .setPeriods(Collections.singletonList(
                    new SeasonPeriod().setWarehouseId(1L).setFromMmDd("12-01").setToMmDd("12-31")
                )));

        SeasonRepository.SeasonWithPeriods fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(season.getId())).get(0);

        SeasonRepository.SeasonWithPeriods finalFromDb = fromDb;
        seasonController.update(
            new DisplaySeason()
                .setupSeason(s -> s
                    .setId(finalFromDb.getId())
                    .setName("New!")
                    .setModifiedAt(finalFromDb.getSeason().getModifiedAt()))
                .setPeriods(Collections.emptyList()));

        fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(season.getId())).get(0);

        assertThat(fromDb.getPeriods()).isEmpty();
    }

    @Test
    public void testUpdatePeriod() throws Exception {
        seasonController.update(
            new DisplaySeason()
                .setupSeason(s -> s
                    .setId(season.getId())
                    .setName("New!")
                    .setModifiedAt(season.getModifiedAt()))
                .setPeriods(Collections.singletonList(
                    new SeasonPeriod().setWarehouseId(1L).setFromMmDd("12-01").setToMmDd("12-31")
                )));

        SeasonRepository.SeasonWithPeriods fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(season.getId())).get(0);
        SeasonPeriod p = fromDb.getPeriods().get(0);

        SeasonRepository.SeasonWithPeriods finalFromDb = fromDb;
        seasonController.update(
            new DisplaySeason()
                .setupSeason(s -> s
                    .setId(finalFromDb.getId())
                    .setName("New!")
                    .setModifiedAt(finalFromDb.getSeason().getModifiedAt()))
                .setPeriods(Collections.singletonList(
                    new SeasonPeriod().setWarehouseId(2L).setFromMmDd("11-01").setToMmDd("11-30")
                        .setId(p.getId()).setModifiedAt(p.getModifiedAt())
                )));

        fromDb = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(season.getId())).get(0);

        assertThat(fromDb.getPeriods()).hasSize(1);
        assertThat(fromDb.getPeriods().get(0)).satisfies(period -> {
            assertThat(period.getWarehouseId()).isEqualTo(2L);
            assertThat(period.getFromMmDd()).isEqualTo("11-01");
            assertThat(period.getToMmDd()).isEqualTo("11-30");
        });
    }

    @Test
    public void testCreateWithAlreadyExistingName() throws Exception {
        Assertions.assertThatCode(() ->
            seasonController.create(new DisplaySeason().setupSeason(s -> s.setName(SEASON_NAME)))
        ).hasMessageContaining("Уже содержится сезон с именем 'seasonName'");
    }

    @Test
    public void testDelete() throws Exception {
        seasonController.delete(season.getId());

        List<Season> seasons = seasonRepository.find(new SeasonRepository.Filter()
            .setIds(Collections.singletonList(season.getId())));
        assertThat(seasons).isEmpty();
    }

    @Test
    public void testDeleteUsingSeason() throws Exception {
        deepmindMskuRepository.save(mskuRandom.nextObject(Msku.class).setId(1L).setTitle("msku 1"));
        deepmindMskuRepository.save(mskuRandom.nextObject(Msku.class).setId(2L).setTitle("msku 2"));

        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setMarketSkuId(1L)
            .setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(season.getId()));
        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setMarketSkuId(2L)
            .setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(season.getId()));


        Assertions.assertThatCode(() ->
            seasonController.delete(season.getId())
        ).hasMessageContaining("Невозможно удалить сезон 'seasonName', так как он используется в следующих msku: " +
            "msku 1 (id: 1), msku 2 (id: 2)");
    }

    @Test
    public void testDeleteSeasonOnDeletedMsku() throws Exception {
        deepmindMskuRepository.save(mskuRandom.nextObject(Msku.class).setId(1L).setTitle("msku 1").setDeleted(true));

        mskuStatusRepository.save(mskuRandom.nextObject(MskuStatus.class).setMarketSkuId(1L)
            .setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(season.getId()));

        seasonController.delete(season.getId());
        List<Season> seasons = seasonRepository.find(new SeasonRepository.Filter()
            .setIds(Collections.singletonList(season.getId())));
        assertThat(seasons).isEmpty();
    }
}
