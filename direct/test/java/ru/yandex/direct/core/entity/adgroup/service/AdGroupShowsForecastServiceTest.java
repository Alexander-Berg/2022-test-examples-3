package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordForecast;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.regions.Region;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupShowsForecastServiceTest {

    @Autowired
    Steps steps;

    @Autowired
    AdGroupsShowsForecastService serviceUnderTest;

    @Autowired
    AdGroupService adGroupService;

    @Autowired
    KeywordService keywordService;

    private String getGeoString(AdGroupInfo adGroup) {
        return String.join(",",
                mapList(adGroup.getAdGroup().getGeo(), String::valueOf));
    }

    private AdGroupShowsForecast getForecast(AdGroupInfo adGroup, List<KeywordInfo> keywordInfos, long forecastChange) {
        List<KeywordForecast> keywordForecasts = mapList(keywordInfos,
                k -> new KeywordForecast()
                        .withId(k.getId())
                        .withKeyword(k.getKeyword().getPhrase())
                        .withShowsForecast(k.getKeyword().getShowsForecast() + forecastChange));
        return new AdGroupShowsForecast()
                .withId(adGroup.getAdGroupId())
                .withGeo(getGeoString(adGroup))
                .withKeywordForecasts(keywordForecasts);
    }

    @Test
    public void testUpdateAdGroupsShowsForecast_UpdatesAllData() {
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusShowsForecast(StatusShowsForecast.NEW));
        KeywordInfo keywordOne = steps.keywordSteps().createKeyword(adGroup);
        KeywordInfo keywordTwo = steps.keywordSteps().createKeyword(adGroup);

        long forecastChange = 10;
        List<AdGroupShowsForecast> forecasts = Collections.singletonList(
                getForecast(adGroup, Arrays.asList(keywordOne, keywordTwo), forecastChange)
        );

        LocalDateTime forecastDate = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.SECONDS);
        serviceUnderTest.updateAdGroupsShowsForecast(keywordOne.getShard(), forecasts, forecastDate);

        AdGroup adGroupDb = Iterables.getFirst(
                adGroupService.getAdGroups(adGroup.getClientId(), Collections.singletonList(adGroup.getAdGroupId())),
                null);

        assertThat(adGroupDb, notNullValue());

        assertThat("Дата сменилась на новую", adGroupDb.getForecastDate(), equalTo(forecastDate));
        assertThat("Статус сменился на PROCESSED", adGroupDb.getStatusShowsForecast(),
                equalTo(StatusShowsForecast.PROCESSED));

        List<Keyword> keywords = keywordService
                .getKeywords(adGroup.getClientId(), Arrays.asList(keywordOne.getId(), keywordTwo.getId()));

        Map<Long, Keyword> idToKeyword = listToMap(keywords, Keyword::getId);

        assertThat("Прогноз в первой фразе изменился", idToKeyword.get(keywordOne.getId()).getShowsForecast(),
                equalTo(keywordOne.getKeyword().getShowsForecast() + forecastChange));
        assertThat("Прогноз во второй фразе изменился", idToKeyword.get(keywordTwo.getId()).getShowsForecast(),
                equalTo(keywordTwo.getKeyword().getShowsForecast() + forecastChange));
    }

    @Test
    public void testUpdateAdGroupsShowsForecast_NoUpdate_GeoChanged() {
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusShowsForecast(StatusShowsForecast.SENDING));
        KeywordInfo keywordOne = steps.keywordSteps().createKeyword(adGroup);
        KeywordInfo keywordTwo = steps.keywordSteps().createKeyword(adGroup);

        long forecastChange = 10;
        adGroup.getAdGroup().setGeo(Collections.singletonList(Region.MOSCOW_REGION_ID));
        List<AdGroupShowsForecast> forecasts = Collections.singletonList(
                getForecast(adGroup, Arrays.asList(keywordOne, keywordTwo), forecastChange)
        );

        LocalDateTime forecastDate = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.SECONDS);
        serviceUnderTest.updateAdGroupsShowsForecast(keywordOne.getShard(), forecasts, forecastDate);

        AdGroup adGroupDb = Iterables.getFirst(
                adGroupService.getAdGroups(adGroup.getClientId(), Collections.singletonList(adGroup.getAdGroupId())),
                null);

        assertThat(adGroupDb, notNullValue());

        assertThat("Дата не сменилась на новую", adGroupDb.getForecastDate(),
                equalTo(adGroup.getAdGroup().getForecastDate()));
        assertThat("Статус остался SENDING", adGroupDb.getStatusShowsForecast(),
                equalTo(StatusShowsForecast.SENDING));

        List<Keyword> keywords = keywordService
                .getKeywords(adGroup.getClientId(), Arrays.asList(keywordOne.getId(), keywordTwo.getId()));

        Map<Long, Keyword> idToKeyword = listToMap(keywords, Keyword::getId);

        assertThat("Прогноз в первой фразе не изменился", idToKeyword.get(keywordOne.getId()).getShowsForecast(),
                equalTo(keywordOne.getKeyword().getShowsForecast()));
        assertThat("Прогноз во второй фразе не изменился", idToKeyword.get(keywordTwo.getId()).getShowsForecast(),
                equalTo(keywordTwo.getKeyword().getShowsForecast()));
    }

    @Test
    public void testUpdateAdGroupsShowsForecast_UpdatesPartOfData() {
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusShowsForecast(StatusShowsForecast.SENDING));
        KeywordInfo keywordOne = steps.keywordSteps().createKeyword(adGroup);
        KeywordInfo keywordTwo = steps.keywordSteps().createKeyword(adGroup);

        long forecastChange = 10;
        List<AdGroupShowsForecast> forecasts = Collections.singletonList(
                getForecast(adGroup, Collections.singletonList(keywordTwo), forecastChange)
        );

        LocalDateTime forecastDate = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.SECONDS);
        serviceUnderTest.updateAdGroupsShowsForecast(keywordOne.getShard(), forecasts, forecastDate);

        AdGroup adGroupDb = Iterables.getFirst(
                adGroupService.getAdGroups(adGroup.getClientId(), Collections.singletonList(adGroup.getAdGroupId())),
                null);

        assertThat(adGroupDb, notNullValue());

        assertThat("Дата сменилась на новую", adGroupDb.getForecastDate(), equalTo(forecastDate));
        assertThat("Статус сменился на PROCESSED", adGroupDb.getStatusShowsForecast(),
                equalTo(StatusShowsForecast.PROCESSED));

        List<Keyword> keywords = keywordService
                .getKeywords(adGroup.getClientId(), Arrays.asList(keywordOne.getId(), keywordTwo.getId()));

        Map<Long, Keyword> idToKeyword = listToMap(keywords, Keyword::getId);

        assertThat("Прогноз в первой фразе не изменился", idToKeyword.get(keywordOne.getId()).getShowsForecast(),
                equalTo(keywordOne.getKeyword().getShowsForecast()));
        assertThat("Прогноз во второй фразе изменился", idToKeyword.get(keywordTwo.getId()).getShowsForecast(),
                equalTo(keywordTwo.getKeyword().getShowsForecast() + forecastChange));
    }

    @Test
    public void testUpdateAdGroupsShowsForecast_UpdatesOnlyDateAndStatus() {
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusShowsForecast(StatusShowsForecast.SENDING));
        KeywordInfo keywordOne = steps.keywordSteps().createKeyword(adGroup);
        KeywordInfo keywordTwo = steps.keywordSteps().createKeyword(adGroup);

        long forecastChange = 10;
        List<AdGroupShowsForecast> forecasts = Collections.singletonList(
                getForecast(adGroup, Collections.emptyList(), forecastChange)
        );

        LocalDateTime forecastDate = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.SECONDS);
        serviceUnderTest.updateAdGroupsShowsForecast(keywordOne.getShard(), forecasts, forecastDate);

        AdGroup adGroupDb = Iterables.getFirst(
                adGroupService.getAdGroups(adGroup.getClientId(), Collections.singletonList(adGroup.getAdGroupId())),
                null);

        assertThat(adGroupDb, notNullValue());

        assertThat("Дата сменилась на новую", adGroupDb.getForecastDate(), equalTo(forecastDate));
        assertThat("Статус сменился на PROCESSED", adGroupDb.getStatusShowsForecast(),
                equalTo(StatusShowsForecast.PROCESSED));

        List<Keyword> keywords = keywordService
                .getKeywords(adGroup.getClientId(), Arrays.asList(keywordOne.getId(), keywordTwo.getId()));

        Map<Long, Keyword> idToKeyword = listToMap(keywords, Keyword::getId);

        assertThat("Прогноз в первой фразе не изменился", idToKeyword.get(keywordOne.getId()).getShowsForecast(),
                equalTo(keywordOne.getKeyword().getShowsForecast()));
        assertThat("Прогноз во второй фразе не изменился", idToKeyword.get(keywordTwo.getId()).getShowsForecast(),
                equalTo(keywordTwo.getKeyword().getShowsForecast()));
    }

    @Test
    public void testUpdateAdGroupsShowsForecast_NoUpdate_ForecastIsNewer() {
        LocalDateTime oldForecastDate = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.SECONDS);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusShowsForecast(StatusShowsForecast.PROCESSED)
                .withForecastDate(oldForecastDate));
        KeywordInfo keywordOne = steps.keywordSteps().createKeyword(adGroup);
        KeywordInfo keywordTwo = steps.keywordSteps().createKeyword(adGroup);

        long forecastChange = 10;
        List<AdGroupShowsForecast> forecasts = Collections.singletonList(
                getForecast(adGroup, Collections.emptyList(), forecastChange)
        );

        LocalDateTime forecastDate = oldForecastDate.minusMinutes(5).truncatedTo(ChronoUnit.SECONDS);
        serviceUnderTest.updateAdGroupsShowsForecast(keywordOne.getShard(), forecasts, forecastDate);

        AdGroup adGroupDb = Iterables.getFirst(
                adGroupService.getAdGroups(adGroup.getClientId(), Collections.singletonList(adGroup.getAdGroupId())),
                null);

        assertThat(adGroupDb, notNullValue());

        assertThat("Дата не сменилась на новую", adGroupDb.getForecastDate(),
                equalTo(adGroup.getAdGroup().getForecastDate()));
        assertThat("Статус PROCESSED", adGroupDb.getStatusShowsForecast(),
                equalTo(StatusShowsForecast.PROCESSED));

        List<Keyword> keywords = keywordService
                .getKeywords(adGroup.getClientId(), Arrays.asList(keywordOne.getId(), keywordTwo.getId()));

        Map<Long, Keyword> idToKeyword = listToMap(keywords, Keyword::getId);

        assertThat("Прогноз в первой фразе не изменился", idToKeyword.get(keywordOne.getId()).getShowsForecast(),
                equalTo(keywordOne.getKeyword().getShowsForecast()));
        assertThat("Прогноз во второй фразе не изменился", idToKeyword.get(keywordTwo.getId()).getShowsForecast(),
                equalTo(keywordTwo.getKeyword().getShowsForecast()));
    }
}
