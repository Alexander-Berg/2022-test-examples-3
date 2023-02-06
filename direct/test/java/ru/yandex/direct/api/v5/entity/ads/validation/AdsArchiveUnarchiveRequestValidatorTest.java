package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.ArchiveRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import one.util.streamex.StreamEx;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.maxBannersPerArchiveRequest;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.listOfUpdateItems;
import static ru.yandex.direct.api.v5.entity.ads.Constants.MAX_ARCHIVE_UNARCHIVE_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyElementsList;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class AdsArchiveUnarchiveRequestValidatorTest {
    private final AdsArchiveUnarchiveRequestValidator validator = new AdsArchiveUnarchiveRequestValidator(
            mock(AdsAdGroupTypeValidator.class));

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public ArchiveRequest request;

    @Parameterized.Parameter(2)
    public Matcher<ValidationResult<ArchiveRequest, DefectType>> resultMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(

                // Negative
                testCase("Max array length exceeded",
                        StreamEx.of(listOfUpdateItems(MAX_ARCHIVE_UNARCHIVE_IDS_COUNT + 1))
                                .map(AdUpdateItem::getId).toList(),
                        err(path(field("SelectionCriteria"), field("Ids")),
                                maxBannersPerArchiveRequest(MAX_ARCHIVE_UNARCHIVE_IDS_COUNT))),
                testCase("Empty request", Collections.emptyList(),
                        err(path(field("SelectionCriteria"), field("Ids")), emptyElementsList())),

                // Positive
                testCase("Normal request", Arrays.asList(1L, 2L), hasNoDefects())
        );
    }

    private static Object[] testCase(String description, List<Long> ids,
                                     Matcher<ValidationResult<ArchiveRequest, DefectType>> matcher) {
        return new Object[]{
                description,
                new ArchiveRequest().withSelectionCriteria(new IdsCriteria().withIds(ids)),
                matcher
        };
    }

    private static Matcher<ValidationResult<ArchiveRequest, DefectType>> err(Path path, DefectType defectType) {
        return hasDefectWith(validationError(path, defectType));
    }

    @Test
    public void validateTest() {
        assertThat(validator.validate(request), resultMatcher);
    }
}
