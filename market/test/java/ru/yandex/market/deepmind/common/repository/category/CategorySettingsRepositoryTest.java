package ru.yandex.market.deepmind.common.repository.category;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseJooqRepositoryTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.mbo.jooq.repo.JooqRepository;

/**
 * @author eremeevvo
 * @since 08.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CategorySettingsRepositoryTest extends DeepmindBaseJooqRepositoryTestClass<CategorySettings, Long> {
    @Autowired
    private CategorySettingsRepository categorySettingsRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    private Season season;

    public CategorySettingsRepositoryTest() {
        super(CategorySettings.class, CategorySettings::getCategoryId);
        generatedFields = new String[]{"modifiedAt"};
    }

    @Override
    protected JooqRepository<CategorySettings, ?, Long, ?, ?> repository() {
        return categorySettingsRepository;
    }

    @Before
    public void setUp() {
        season = seasonRepository.save(random.nextObject(Season.class).setId(null));
    }

    @Override
    protected CategorySettings random() {
        return super.random().setSeasonId(season.getId());
    }
}
