package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adExtensionNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.duplicatedAdExtensionId;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidIdInArrayElement;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxAdExtensionsExceeded;
import static ru.yandex.direct.core.entity.banner.type.callouts.BannerWithCalloutsConstants.MAX_CALLOUTS_COUNT_ON_BANNER;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(Parameterized.class)
public class BannerWithCalloutsValidatorProviderTest {

    private static final Long[] MAX_CALLOUTS_PLUS_1 =
            LongStreamEx.range(0, MAX_CALLOUTS_COUNT_ON_BANNER + 1).boxed().toArray(new Long[0]);

    @Autowired
    public BannerWithCalloutsValidatorProvider provider;

    @Parameterized.Parameter
    public List<Long> calloutIds;

    @Parameterized.Parameter(1)
    public Set<Long> existingCalloutIds;

    @Parameterized.Parameter(2)
    public Collection<Defect> expectedErrors;

    @Parameterized.Parameter(3)
    public Map<Path, Defect> expectedItemErrors;

    private ValidationResult<List<Long>, Defect> actual;

    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return asList(
                new TestCase(1L, 2L, 2L, null, 3L, 2L)
                        .withExisting(1L, 2L)
                        .expectItemError(3, notNull())
                        .expect(
                                duplicatedAdExtensionId(2L),
                                adExtensionNotFound(3L)),
                new TestCase(-3L, -1L, -1L, 2L, 3L, 3L)
                        .withExisting(2L, 3L)
                        .expectItemError(0, invalidIdInArrayElement())
                        .expectItemError(1, invalidIdInArrayElement())
                        .expectItemError(2, invalidIdInArrayElement())
                        .expect(
                                duplicatedAdExtensionId(3L)),
                new TestCase(MAX_CALLOUTS_PLUS_1)
                        .withExisting(MAX_CALLOUTS_PLUS_1)
                        .expect(
                                maxAdExtensionsExceeded(MAX_CALLOUTS_COUNT_ON_BANNER))

                );
    }

    @Before
    public void init() {
        actual = BannerWithCalloutsValidatorProvider.calloutIdsValidator(
                BannerDefects::maxAdExtensionsExceeded,
                existingCalloutIds).apply(calloutIds);
    }

    @Test
    public void validateCalloutIds_ReturnsExpectedDefects() {
        expectedErrors.forEach(err ->
                assertThat(actual, hasDefectDefinitionWith(validationError(path(), err))));

        assertThat(actual.getErrors(), hasSize(expectedErrors.size()));
    }

    @Test
    public void validateCalloutIds_ReturnsExpectedItemDefects() {
        expectedItemErrors.forEach((path, defect) ->
                assertThat(actual, hasDefectDefinitionWith(validationError(path, defect))));
    }

    private static class TestCase {
        private List<Long> calloutIds;
        private Set<Long> existing = new HashSet<>();
        private Map<Path, Defect> expectedItemErrors = new HashMap<>();

        TestCase(Long... calloutIds) {
            this.calloutIds = asList(calloutIds);
        }

        TestCase withExisting(Long... existing) {
            this.existing.addAll(asList(existing));
            return this;
        }

        TestCase expectItemError(int idx, Defect expected) {
            expectedItemErrors.put(path(index(idx)), expected);
            return this;
        }

        Object[] expect(Defect... errors) {
            return new Object[]{calloutIds, existing, asList(errors), expectedItemErrors};
        }
    }

}
