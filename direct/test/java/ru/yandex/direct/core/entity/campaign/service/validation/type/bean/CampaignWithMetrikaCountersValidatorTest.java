package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.LongStreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCounterIsUnavailable;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithMetrikaCountersValidatorTest {

    @Test
    public void testSuccess_withCountersAccessValidation() {
        var counterIdSet = Set.of(1L);
        var campaign = new TextCampaign()
                .withMetrikaCounters(List.of(1L));

        var validator = CampaignWithMetrikaCountersValidator.build(counterIdSet, emptySet(), true,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters());
        var result = validator.apply(campaign);

        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testSuccess_withoutCountersAccessValidationAndValidCounter() {
        var counterIdSet = Set.of(1L);
        var campaign = new TextCampaign()
                .withMetrikaCounters(List.of(1L));

        var validator = CampaignWithMetrikaCountersValidator.build(counterIdSet, emptySet(), false,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters());
        var result = validator.apply(campaign);

        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testSuccess_withoutCountersAccessValidationAndInvalidCounter() {
        var counterIdSet = Set.of(1L);
        var campaign = new TextCampaign()
                .withMetrikaCounters(List.of(2L));

        var validator = CampaignWithMetrikaCountersValidator.build(counterIdSet, emptySet(), false,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters());
        var result = validator.apply(campaign);

        assertThat(result, hasNoDefectsDefinitions());
    }

    @Parameterized.Parameters(name = "system count of counters: {0}, client count of counters: {1}, " +
            "has defect: {2}, description: {3}")
    public static Collection<Object[]> parametersForCheckMaxCountOfCounters() {
        return List.of(new Object[][]{
                {0, 100, false, "Максимальное количество счетчиков"},
                {0, 101, true, "Превышение количества счетчиков"},
                {1, 101, false, "Максимальное количество счетчиков с учетом системных"},
                {1, 102, true, "Превышение количества счетчиков с учетом системных"}
        });
    }

    @Test
    @Parameters(method = "parametersForCheckMaxCountOfCounters")
    @TestCaseName("description = {3}")
    public void validate_DifferentCountOfCounters(Integer countOfSystemCounters,
                                                  Integer countOfClientCounters,
                                                  boolean hasMaxCountDefect,
                                                  @SuppressWarnings("unused") String description) {
        Set<Long> systemCounterIds = countOfSystemCounters > 0 ?
                LongStreamEx.range(1L, 1L + countOfSystemCounters)
                        .boxed()
                        .collect(Collectors.toSet()) :
                emptySet();

        var clientCounterIds = LongStreamEx.range(1L, 1L + countOfClientCounters)
                .boxed()
                .collect(Collectors.toList());
        var campaign = new TextCampaign()
                .withMetrikaCounters(clientCounterIds);

        var validator = CampaignWithMetrikaCountersValidator.build(emptySet(), systemCounterIds, false,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters());
        var vr = validator.apply(campaign);
        if (hasMaxCountDefect) {
            assertThat(vr, hasDefectDefinitionWith(validationError(
                    path(field(CampaignWithMetrikaCounters.METRIKA_COUNTERS)),
                    maxCollectionSize(MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS))));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    @Test
    @Parameters(method = "invalidCountersParametersWithAccessValidation")
    @TestCaseName("description = {4}")
    public void testFails_withCountersAccessValidation(Set<Long> counterIdSet, List<Long> counters, Path path,
                                                       Defect defect, @SuppressWarnings("unused") String description) {
        var campaign = new TextCampaign().withMetrikaCounters(counters);
        var result = CampaignWithMetrikaCountersValidator.build(counterIdSet, emptySet(), true,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters())
                .apply(campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path, defect)));
    }

    @Test
    @Parameters(method = "invalidCountersParametersWithoutAccessValidation")
    @TestCaseName("description = {4}")
    public void testFails_withoutCountersAccessValidation(Set<Long> counterIdSet, List<Long> counters,
                                                          Path path, Defect defect,
                                                          @SuppressWarnings("unused") String description) {
        var campaign = new TextCampaign().withMetrikaCounters(counters);
        var result = CampaignWithMetrikaCountersValidator.build(counterIdSet, emptySet(), false,
                MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS, campaign.getMinSizeOfMetrikaCounters())
                .apply(campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path, defect)));
    }

    @Parameterized.Parameters(name = "counter ids: {0}, counters: {1}, path: {2}, defect: {3}, description: {4}")
    public static Collection<Object[]> invalidCountersParametersWithAccessValidation() {
        return List.of(new Object[][]{
                {Set.of(1L, 2L), nonUniqueCounters(), countersPath(), duplicatedElement(), "Не уникальные счётчики"},
                {Set.of(1L), Collections.<Long>singletonList(null), countersPath(), notNull(), "Счётчик без " +
                        "идентификатора"},
                {Set.of(1L), List.of(2L), countersPath(), metrikaCounterIsUnavailable(),
                        "Неизвестный идентификатор счётчика"}
        });
    }

    @Parameterized.Parameters(name = "counter ids: {0}, counters: {1}, path: {2}, defect: {3}, description: {4}")
    public static Collection<Object[]> invalidCountersParametersWithoutAccessValidation() {
        return List.of(new Object[][]{
                {Set.of(1L, 2L), nonUniqueCounters(), countersPath(), duplicatedElement(), "Не уникальные счётчики"},
                {Set.of(1L), Collections.<Long>singletonList(null), countersPath(), notNull(), "Счётчик без " +
                        "идентификатора"},
        });
    }

    private static Path countersPath() {
        return path(field(CampaignWithMetrikaCounters.METRIKA_COUNTERS), index(0));
    }

    private static List<Long> nonUniqueCounters() {
        return List.of(1L, 1L);
    }
}
