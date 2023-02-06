package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.banner.type.additionalhrefs.BannerWithAdditionalHrefsConstants.MAX_ADDITIONAL_HREFS_COUNT_ON_BANNER;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.isEmptyCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BannerAdditionalHrefsValidatorTest {

    @Test
    public void additionalHrefsNull() {
        List<BannerAdditionalHref> additionalHrefs = null;

        ValidationResult<List<BannerAdditionalHref>, Defect> result = validate(additionalHrefs);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void additionalHrefsEmpty() {
        List<BannerAdditionalHref> additionalHrefs = emptyList();

        ValidationResult<List<BannerAdditionalHref>, Defect> result = validate(additionalHrefs);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void additionalHrefsMaxItems() {
        List<BannerAdditionalHref> additionalHrefs = IntStream.range(0, MAX_ADDITIONAL_HREFS_COUNT_ON_BANNER + 1)
                .mapToObj(i -> validAdditionalHref())
                .collect(Collectors.toList());

        ValidationResult<List<BannerAdditionalHref>, Defect> result = validate(additionalHrefs);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(),
                maxCollectionSize(MAX_ADDITIONAL_HREFS_COUNT_ON_BANNER)))));
    }

    @Test
    public void additionalHrefsNullItem() {
        List<BannerAdditionalHref> additionalHrefs = singletonList(null);

        ValidationResult<List<BannerAdditionalHref>, Defect> result = validate(additionalHrefs);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0)),
                notNull()))));
    }

    @Test
    public void additionalHrefsNotAllowed() {
        List<BannerAdditionalHref> additionalHrefs = List.of(validAdditionalHref());

        ValidationResult<List<BannerAdditionalHref>, Defect> result = validate(additionalHrefs, false);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(),
                isEmptyCollection()))));
    }

    private BannerAdditionalHref validAdditionalHref() {
        return new BannerAdditionalHref().withHref("http://ya.ru");
    }

    private ValidationResult<List<BannerAdditionalHref>, Defect> validate(
            List<BannerAdditionalHref> additionalHrefs) {
        return validate(additionalHrefs, true);
    }

    private ValidationResult<List<BannerAdditionalHref>, Defect> validate(
            List<BannerAdditionalHref> additionalHrefs, boolean additionalHrefsAllowed) {
        return new BannerAdditionalHrefsValidator(additionalHrefsAllowed).apply(additionalHrefs);
    }

}
