package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.adgroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameIsNotSet;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoEmptyRegions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Линковка результатов валидации групп
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateAdGroupValidationLinkingTest extends ComplexUpdateAdGroupTestBase {

    @Test
    public void update_EmptyAdGroupWithError_ErrorInResultIsValid() {
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1);
        adGroupForUpdate.getAdGroup().withName(null);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroupForUpdate));

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.NAME.name())), adGroupNameIsNotSet())));
    }

    @Test
    public void update_TwoEmptyAdGroupWithErrors_ErrorsInResultAreValid() {
        createSecondAdGroup();

        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        adGroupForUpdate1.getAdGroup().withName(null);

        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2);
        adGroupForUpdate2.getAdGroup().withGeo(emptyList());

        MassResult<Long> result = updateAndCheckBothItemsAreInvalid(asList(adGroupForUpdate1, adGroupForUpdate2));

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field(AdGroup.NAME.name())), adGroupNameIsNotSet())));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(1), field(AdGroup.GEO.name())), geoEmptyRegions())));
    }
}
