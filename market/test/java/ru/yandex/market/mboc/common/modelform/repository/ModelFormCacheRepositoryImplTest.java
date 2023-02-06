package ru.yandex.market.mboc.common.modelform.repository;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class ModelFormCacheRepositoryImplTest extends BaseDbTestClass {
    @Autowired
    private ModelFormCacheRepository modelFormRepository;

    // Remove if more tests are added
    @Test
    public void exists() {
        modelFormRepository.insert(new CachedModelForm(123, true));
        Assertions.assertThat(modelFormRepository.findById(123L).isPublished()).isTrue();
    }
}
