package ru.yandex.market.deepmind.common.repository.season;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseJooqRepositoryTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository.SeasonWithPeriods;
import ru.yandex.market.mbo.jooq.repo.JooqRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.season.SeasonRepository.DEFAULT_ID;

/**
 * @author eremeevvo
 * @since 08.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SeasonRepositoryTest extends DeepmindBaseJooqRepositoryTestClass<Season, Long> {
    @Autowired
    private SeasonRepository seasonRepository;

    public SeasonRepositoryTest() {
        super(Season.class, Season::getId);
        generatedFields = new String[]{"id", "modifiedAt"};
    }

    @Override
    protected JooqRepository<Season, ?, Long, ?, ?> repository() {
        return seasonRepository;
    }

    @Test
    public void filter() {
        List<Season> values = IntStream.range(1, BATCH_SIZE)
            .mapToObj(id -> random().setName(String.valueOf(id)))
            .collect(Collectors.toList());

        seasonRepository.save(values);

        List<Season> found = seasonRepository.find(new SeasonRepository.Filter().setName("7"));
        assertThat(found).extracting(Season::getName).containsOnly("7");
    }

    @Test
    public void testWithPeriodsForEmptyPeriods() {
        Season saved = seasonRepository.save(new Season().setName("Test"));
        List<SeasonWithPeriods> seasons = seasonRepository.findWithPeriods(SeasonRepository.Filter.all());
        assertThat(seasons).hasSize(1)
            .allSatisfy(season -> {
                assertThat(season.getSeason().getId()).isEqualTo(saved.getId());
                assertThat(season.getPeriods()).isEmpty();
            });
    }

    @Test
    public void testWithPeriodsUpdateAllWarehouse() {
        SeasonRepository.RelatedGroupUpdateResult<Long, Integer> res =
            seasonRepository.saveWithPeriods(new SeasonWithPeriods(
                new Season().setName("Test"),
                Arrays.asList(
                    new SeasonPeriod().setWarehouseId(DEFAULT_ID).setFromMmDd("07-01").setToMmDd("07-31"),
                    new SeasonPeriod().setWarehouseId(1L).setFromMmDd("09-01").setToMmDd("09-30")
                )));

        SeasonWithPeriods season = seasonRepository
            .findWithPeriods(new SeasonRepository.Filter().setIds(res.getId())).get(0);
        assertThat(season.getPeriods()).hasSize(2);
        assertThat(season.getPeriods().get(0).getWarehouseId()).isEqualTo(DEFAULT_ID);
        assertThat(season.getPeriods().get(1).getWarehouseId()).isEqualTo(1L);
    }

    @Test
    public void testWithPeriodsUpdate() {
        for (int i = 0; i < 4; i++) {
            SeasonRepository.RelatedGroupUpdateResult<Long, Integer> res =
                seasonRepository.saveWithPeriods(new SeasonWithPeriods(
                    new Season().setName("Test " + i),
                    Arrays.asList(
                        new SeasonPeriod().setWarehouseId(1L).setFromMmDd("07-01").setToMmDd("07-31"),
                        new SeasonPeriod().setWarehouseId(1L).setFromMmDd("09-01").setToMmDd("09-30"),
                        new SeasonPeriod().setWarehouseId(2L).setFromMmDd("07-01").setToMmDd("07-31")
                    )));
            Long seasonId = res.getId();

            List<SeasonWithPeriods> seasons = seasonRepository.findWithPeriods(SeasonRepository.Filter.all());

            if (i <= 1) {
                assertThat(seasons).hasSize(i + 1)
                    .allSatisfy(season -> assertThat(season.getPeriods())
                        .hasSize(3)
                        .extracting(SeasonPeriod::getSeasonId)
                        .allMatch(id -> id.equals(season.getSeason().getId())));
            } else {
                assertThat(seasons).hasSize(i + 1)
                    .allSatisfy(season -> assertThat(season.getPeriods())
                        .extracting(SeasonPeriod::getSeasonId)
                        .allMatch(id -> id.equals(season.getSeason().getId())));

                SeasonWithPeriods season =
                    seasons.stream().filter(s -> Objects.equals(s.getId(), seasonId)).findFirst().get();
                assertThat(season.getPeriods()).hasSize(3);

                // Drop one
                List<SeasonPeriod> periods = new ArrayList<>(season.getPeriods());
                assertThat(periods.removeIf(p -> p.getFromMmDd().equals("09-01"))).isTrue();
                // Update one
                periods.forEach(p -> {
                    if (p.getWarehouseId() == 1L) {
                        p.setFromMmDd("06-01");
                    }
                });
                // Add One
                periods.add(new SeasonPeriod().setWarehouseId(3L).setFromMmDd("11-01").setToMmDd("11-01"));
                // Add ont more to mess things a bit (at least try so)
                periods.add(new SeasonPeriod()
                    .setId(100500)
                    .setSeasonId(100500L)
                    .setWarehouseId(4L)
                    .setFromMmDd("10-01")
                    .setToMmDd("11-01"));

                SeasonRepository.RelatedGroupUpdateResult<Long, Integer> update =
                    seasonRepository.saveWithPeriods(new SeasonWithPeriods(
                        season.getSeason(),
                        periods.stream().map(SeasonPeriod::new).collect(Collectors.toList())));
                assertThat(update.getId()).isEqualTo(seasonId);
                assertThat(update.getItemIds()).hasSize(4);
                assertThat(update.getItemIds().get(0)).isEqualTo(periods.get(0).getId());
                assertThat(update.getItemIds().get(1)).isEqualTo(periods.get(1).getId());
                assertThat(periods.get(2).getId()).isNull();
                assertThat(update.getItemIds().get(2)).isGreaterThan(0);
                assertThat(update.getItemIds().get(3)).isGreaterThan(0).isNotEqualTo(100500);

                SeasonWithPeriods updated = seasonRepository.findWithPeriods(
                    new SeasonRepository.Filter().setIds(seasonId)).get(0);

                assertThat(updated.getPeriods()).hasSize(4);
                assertThat(updated.getPeriods())
                    .extracting(SeasonPeriod::getSeasonId)
                    .allMatch(s -> s.equals(seasonId));
                assertThat(updated.getPeriods())
                    .extracting(SeasonPeriod::getId)
                    .noneMatch(s -> s == 100500);

                Map<Long, SeasonPeriod> periodsMap = updated.getPeriods().stream()
                    .collect(Collectors.toMap(SeasonPeriod::getWarehouseId, Function.identity()));

                assertThat(periodsMap).hasSize(4);
                assertThat(periodsMap.get(1L).getFromMmDd()).isEqualTo("06-01");
                assertThat(periodsMap.get(2L).getFromMmDd()).isEqualTo("07-01");
                assertThat(periodsMap.get(3L).getFromMmDd()).isEqualTo("11-01");
                assertThat(periodsMap.get(4L).getFromMmDd()).isEqualTo("10-01");
            }
        }
    }

    @Test
    public void testSaveWithPeriodsAndReturn() {
        SeasonWithPeriods season = seasonRepository.saveWithPeriodsAndReturn(
            new SeasonWithPeriods(random(), Collections.singletonList(
                new SeasonPeriod().setWarehouseId(1L).setFromMmDd("12-01").setToMmDd("12-31"))));
        Long seasonId = season.getId();
        assertThat(seasonId).isGreaterThan(0);
        assertThat(season.getPeriods()).hasSize(1);

        List<SeasonPeriod> periods = new ArrayList<>(season.getPeriods());
        periods.add(new SeasonPeriod().setWarehouseId(DEFAULT_ID).setFromMmDd("12-01").setToMmDd("12-31"));
        season = seasonRepository.saveWithPeriodsAndReturn(new SeasonWithPeriods(season.getSeason(), periods));
        assertThat(season.getId()).isEqualTo(seasonId);
        assertThat(season.getPeriods()).hasSize(2);
        assertThat(season.getPeriods()).extracting(SeasonPeriod::getWarehouseId)
            .containsExactlyInAnyOrder(DEFAULT_ID, 1L);
    }
}
