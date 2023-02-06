package ru.yandex.direct.grid.processing.service.shortener;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.frontdb.repository.FilterShortcutRepository;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.shortener.GdShortFilter;
import ru.yandex.direct.grid.processing.model.shortener.GdShortFilterUnion;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridShortenerServiceTest {

    @Autowired
    private GridShortenerService gridShortenerService;

    @Autowired
    private FilterShortcutRepository filterShortcutRepository;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ClientId clientId;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    }

    @Test
    public void getSavedFilter_keyFound_valueFromStorage() {
        GdCampaignFilter expectedFilter = new GdCampaignFilter().withArchived(false);
        String expectedFilterJson = "{\"archived\":false}";
        String key = filterShortcutsSteps.getHashForJsonFilter(expectedFilterJson);
        filterShortcutRepository.saveFilter(clientId, expectedFilterJson, null);

        Supplier<GdCampaignFilter> defaultFilterCreator = () -> new GdCampaignFilter().withArchived(true);

        GdCampaignFilter filter = gridShortenerService.getSavedFilter(key, clientId,
                GdCampaignFilter.class,
                defaultFilterCreator);

        assertThat(filter).isEqualTo(expectedFilter);
    }

    @Test
    public void getSavedFilter_keyNotFound_defaultValue() {
        String key = RandomStringUtils.randomAlphabetic(15);

        Supplier<GdCampaignFilter> defaultFilterCreator = () -> new GdCampaignFilter().withArchived(true);

        GdCampaignFilter filter = gridShortenerService.getSavedFilter(key, clientId,
                GdCampaignFilter.class,
                defaultFilterCreator);

        assertThat(filter).isEqualTo(defaultFilterCreator.get());
    }

    @Test
    public void saveFilter_Success() {
        GdShortFilterUnion gdShortFilterUnion =
                new GdShortFilterUnion().withAdFilter(new GdAdFilter().withArchived(true));
        String filterJson = "{\"archived\":true}";
        String expectedKey = filterShortcutsSteps.getHashForJsonFilter(filterJson);

        GdShortFilter gdShortFilter = gridShortenerService.saveFilter(clientId, gdShortFilterUnion, null);

        GdShortFilter expectedGdShortFilter = new GdShortFilter()
                .withFilterKey(expectedKey);
        assertThat(gdShortFilter).isEqualTo(expectedGdShortFilter);

        GdAdFilter filter = gridShortenerService.getSavedFilter(expectedKey, clientId,
                GdAdFilter.class,
                GdAdFilter::new);
        assertThat(filter).isEqualTo(gdShortFilterUnion.getAdFilter());
    }

    @Test
    public void saveFilterWithKey_Success() {
        GdShortFilterUnion gdShortFilterUnion =
                new GdShortFilterUnion().withAdFilter(new GdAdFilter().withArchived(true));
        String expectedKey = RandomStringUtils.randomAlphabetic(5);

        GdShortFilter gdShortFilter = gridShortenerService.saveFilter(clientId, gdShortFilterUnion, expectedKey);

        GdShortFilter expectedGdShortFilter = new GdShortFilter()
                .withFilterKey(expectedKey);
        assertThat(gdShortFilter).isEqualTo(expectedGdShortFilter);

        GdAdFilter filter = gridShortenerService.getSavedFilter(expectedKey, clientId,
                GdAdFilter.class,
                GdAdFilter::new);
        assertThat(filter).isEqualTo(gdShortFilterUnion.getAdFilter());
    }

    @Test
    public void saveFilter_NoFilter_Error() {
        thrown.expect(NoSuchElementException.class);
        GdShortFilterUnion gdShortFilterUnion =
                new GdShortFilterUnion();

        gridShortenerService.saveFilter(clientId, gdShortFilterUnion, null);
    }
}
