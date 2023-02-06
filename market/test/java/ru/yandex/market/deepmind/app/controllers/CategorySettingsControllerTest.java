package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.CategorySettingsWebFilter;
import ru.yandex.market.deepmind.app.pojo.DisplayCategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.web.DataPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of {@link CategorySettingsController}.
 */
public class CategorySettingsControllerTest extends DeepmindBaseAppDbTestClass {

    private static final long CATEGORY_ID1 = 1;
    private static final long CATEGORY_ID2 = 2;
    private static final long NEW_CATEGORY_ID = 3;

    private CategorySettingsController categorySettingsController;

    @Autowired
    private CategorySettingsRepository categorySettingsRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    private CategorySettings categorySettings1;
    private CategorySettings categorySettings2;
    private Season season1;
    private Season season2;

    @Before
    public void setup() {
        categorySettingsController = new CategorySettingsController(categorySettingsRepository);
        season1 = seasonRepository.save(new Season().setName("testSeason1"));
        season2 = seasonRepository.save(new Season().setName("testSeason2"));
        categorySettings1 = categorySettingsRepository
            .save(new CategorySettings(CATEGORY_ID1, season1.getId(), Instant.now()));
        categorySettings2 = categorySettingsRepository
            .save(new CategorySettings(CATEGORY_ID2, season1.getId(), Instant.now()));
    }

    @Test
    public void testList() throws Exception {
        DataPage<DisplayCategorySettings> page = categorySettingsController.list(new CategorySettingsWebFilter(),
            OffsetFilter.all());
        assertThat(page.getItems())
            .hasSize(2)
            .extracting(DisplayCategorySettings::getCategoryId)
            .containsExactlyInAnyOrder(categorySettings1.getCategoryId(), categorySettings2.getCategoryId());
    }

    @Test
    public void testListWithFilter() throws Exception {
        DataPage<DisplayCategorySettings> page = categorySettingsController.list(new CategorySettingsWebFilter()
            .setCategoryId(categorySettings1.getCategoryId()), OffsetFilter.all());
        assertThat(page.getItems())
            .hasSize(1)
            .extracting(DisplayCategorySettings::getCategoryId)
            .containsExactlyInAnyOrder(categorySettings1.getCategoryId());
    }

    @Test
    public void testInsert() throws Exception {
        DisplayCategorySettings created = categorySettingsController.set(
            new DisplayCategorySettings()
                .setSeasonId(season1.getId())
                .setCategoryId(NEW_CATEGORY_ID));

        assertThat(created.getCategoryId()).isEqualTo(NEW_CATEGORY_ID);
        assertThat(created.getSeasonId()).isEqualTo(season1.getId());

        List<CategorySettings> dbSettings = categorySettingsRepository.find(
            new CategorySettingsRepository.Filter()
                .setCategoryIds(NEW_CATEGORY_ID));

        assertThat(dbSettings).hasSize(1);
        assertThat(dbSettings.get(0).getCategoryId()).isEqualTo(NEW_CATEGORY_ID);
        assertThat(dbSettings.get(0).getSeasonId()).isEqualTo(season1.getId());
    }

    @Test
    public void testUpdate() throws Exception {
        CategorySettings byId = categorySettingsRepository.getById(categorySettings1.getCategoryId());
        DisplayCategorySettings created = categorySettingsController.set(
            new DisplayCategorySettings()
                .setSeasonId(season2.getId())
                .setCategoryId(byId.getCategoryId())
                .setModifiedAt(byId.getModifiedAt()));

        assertThat(created.getCategoryId()).isEqualTo(categorySettings1.getCategoryId());
        assertThat(created.getSeasonId()).isEqualTo(season2.getId());

        List<CategorySettings> dbSettings = categorySettingsRepository.find(
            new CategorySettingsRepository.Filter()
                .setCategoryIds(categorySettings1.getCategoryId()));

        assertThat(dbSettings).hasSize(1);
        assertThat(dbSettings.get(0).getCategoryId()).isEqualTo(categorySettings1.getCategoryId());
        assertThat(dbSettings.get(0).getSeasonId()).isEqualTo(season2.getId());
    }

    @Test
    public void testDelete() throws Exception {
        categorySettingsController.delete(new DisplayCategorySettings()
            .setCategoryId(categorySettings1.getCategoryId()));

        List<CategorySettings> settings = categorySettingsRepository
            .find(new CategorySettingsRepository.Filter()
                .setCategoryIds(Collections.singletonList(categorySettings1.getCategoryId())));
        assertThat(settings).isEmpty();

        DataPage<DisplayCategorySettings> page = categorySettingsController.list(new CategorySettingsWebFilter(),
            OffsetFilter.all());
        assertThat(page.getItems())
            .hasSize(1)
            .extracting(DisplayCategorySettings::getCategoryId)
            .containsExactlyInAnyOrder(categorySettings2.getCategoryId());
    }

    @Test
    public void testListUpdate() throws Exception {
        DataPage<DisplayCategorySettings> page = categorySettingsController.list(new CategorySettingsWebFilter(),
            OffsetFilter.all());
        DisplayCategorySettings displaySettings = page.getItems().get(0);
        displaySettings.setSeasonId(season2.getId());

        DisplayCategorySettings created = categorySettingsController.set(
            new DisplayCategorySettings()
                .setSeasonId(season2.getId())
                .setCategoryId(displaySettings.getCategoryId())
                .setModifiedAt(displaySettings.getModifiedAt()));

        assertThat(created.getCategoryId()).isEqualTo(categorySettings1.getCategoryId());
        assertThat(created.getSeasonId()).isEqualTo(season2.getId());
    }
}
