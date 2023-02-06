package ru.yandex.direct.web.entity.creative;

import java.util.Collections;
import java.util.List;

import one.util.streamex.LongStreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.creative.service.CreativeWebValidationService;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@DirectWebTest
public class CreativeWebValidationServiceTest {
    public CreativeWebValidationService creativeWebValidationService;

    public CreativeWebValidationServiceTest() {
        this.creativeWebValidationService = new CreativeWebValidationService();
    }

    @Test
    public void searchVideoCreatives_MoreThanLimit() {
        List<Long> creativeIds = LongStreamEx.rangeClosed(1, CreativeWebValidationService.MAX_LIST_SIZE + 1)
                .boxed()
                .toList();
        ValidationResult<List<Long>, Defect> validationResult =
                creativeWebValidationService.validateCreativeIds(creativeIds);
        assertThat(validationResult.flattenErrors(),
                contains(validationError(path(), RetargetingDefects.maxIdsInSelection())));
    }

    @Test
    public void searchVideoCreatives_InvalidId() {
        List<Long> creativeIds = Collections.singletonList(-1L);
        ValidationResult<List<Long>, Defect> validationResult =
                creativeWebValidationService.validateCreativeIds(creativeIds);
        assertThat(validationResult.flattenErrors(), contains(validationError(path(index(0)), validId())));
    }

    @Test
    public void searchVideoCreatives_EmptyIds() {
        List<Long> creativeIds = Collections.emptyList();
        ValidationResult<List<Long>, Defect> validationResult =
                creativeWebValidationService.validateCreativeIds(creativeIds);
        assertThat(validationResult.flattenErrors(), contains(validationError(path(), notEmptyCollection())));
    }
}
