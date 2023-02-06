package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationDataTest extends AdGroupsUpdateOperationTestBase {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("minusKeywordsId"));

    @Test
    public void prepareAndApply_NameIsChanged_UpdatedDataIsValid() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroup1);
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        AdGroup expectedAdGroup = adGroup1.withName(NEW_NAME)
                .withBsRarelyLoaded(false)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusShowsForecast(StatusShowsForecast.SENDING)
                .withMinusKeywords(emptyList());

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup1.getId())).get(0);
        assertThat("обновленная группа не соответствует ожидаемой",
                actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(STRATEGY));

        LocalDateTime actualLastChange = actualAdGroup.getLastChange();
        assertThat("LastChange выходит за ожидаемые границы",
                actualLastChange.isAfter(LocalDateTime.now().minusMinutes(1)) &&
                        actualLastChange.isBefore(LocalDateTime.now().plusMinutes(1)),
                is(true));
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_UpdatedDataIsValid() {
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(adGroup1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        AdGroup expectedAdGroup = adGroup1.withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withBsRarelyLoaded(false)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup1.getId())).get(0);
        assertThat("обновленная группа не соответствует ожидаемой",
                actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(STRATEGY));

        LocalDateTime actualLastChange = actualAdGroup.getLastChange();
        assertThat("LastChange выходит за ожидаемые границы",
                actualLastChange.isAfter(LocalDateTime.now().minusMinutes(1)) &&
                        actualLastChange.isBefore(LocalDateTime.now().plusMinutes(1)),
                is(true));
    }

    // должен проверять, что была выполнена предобработка отдельных минус-фраз,
    // удаление дублей и сортировка
    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_MinusKeywordsArePreparedBeforeSaving() {
        List<String> rawMinusKeywords = asList("купит слона", "как пройти в библиотеку ", "!купил слона", "бизнес");
        List<String> expectedPreparedMinusKeywords = asList("!как пройти !в библиотеку", "бизнес", "купит слона");
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup1.getId(), AdGroup.class);
        modelChanges.process(rawMinusKeywords, AdGroup.MINUS_KEYWORDS);

        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup1.getId())).get(0);
        assertThat("минус-фразы должны быть нормализованы перед сохранением",
                actualAdGroup.getMinusKeywords(), beanDiffer(expectedPreparedMinusKeywords));
    }

    // обновление при наличии изменений в минус-фразах

    @Test
    public void prepareAndApply_ContainsInvalidMinusKeywordsItem_ResultIsExpected() {
        List<ModelChanges<AdGroup>> modelChangesList = asList(
                modelChangesWithValidName(adGroup1),
                modelChangesWithInvalidMinusKeywords(adGroup2));
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true, false));
    }

    @Test
    public void prepareAndApply_ContainsValidMinusKeywordsItem_ResultIsExpected() {
        List<ModelChanges<AdGroup>> modelChangesList = asList(
                modelChangesWithValidName(adGroup1),
                modelChangesWithValidMinusKeywords(adGroup2.getId()));
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true, true));
    }

    // обновления в смежных таблицах

    @Test
    public void prepareAndApply_NameIsChanged_CampaignForecastDateIsNotChanged() {
        LocalDateTime expectedDate = campaign.getAutobudgetForecastDate();
        checkState(expectedDate != null);

        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroup1);
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        LocalDateTime actualTimestamp = dslContextProvider.ppc(shard)
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(campaign.getId()))
                .fetchOne()
                .value1();

        assertThat("campaigns.autobudget_forecast_date не должен был измениться",
                actualTimestamp, is(expectedDate));
    }

    @Test
    public void prepareAndApply_GeoIsChanged_CampaignForecastDateIsDropped() {
        LocalDateTime expectedDate = campaign.getAutobudgetForecastDate();
        checkState(expectedDate != null);

        ModelChanges<AdGroup> modelChanges = modelChangesWithGeo(adGroup1);
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        LocalDateTime sqlTimestamp = dslContextProvider.ppc(shard)
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(campaign.getId()))
                .fetchOne()
                .value1();

        assertThat("campaigns.autobudget_forecast_date должен был сброситься", sqlTimestamp, nullValue());
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_CampaignForecastDateIsDropped() {
        LocalDateTime expectedDate = campaign.getAutobudgetForecastDate();
        checkState(expectedDate != null);

        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(adGroup1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        LocalDateTime sqlTimestamp = dslContextProvider.ppc(shard)
                .select(CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(campaign.getId()))
                .fetchOne()
                .value1();

        assertThat("campaigns.autobudget_forecast_date должен был сброситься", sqlTimestamp, nullValue());
    }
}
