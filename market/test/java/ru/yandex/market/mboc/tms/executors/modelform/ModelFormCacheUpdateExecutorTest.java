package ru.yandex.market.mboc.tms.executors.modelform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.model.forms.ModelFormService;
import ru.yandex.market.mbo.model.forms.ModelForms;
import ru.yandex.market.mbo.model.forms.ModelForms.GetModelFormsRequest;
import ru.yandex.market.mbo.model.forms.ModelForms.GetModelFormsResponse;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.modelform.repository.ModelFormCacheRepository;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ModelFormCacheUpdateExecutorTest extends BaseDbTestClass {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ModelFormCacheRepository modelFormRepository;

    private ModelFormService mboModelFormService;

    private ModelFormCacheUpdateExecutor executor;

    private List<Category> categories;
    private Map<Long, CachedModelForm> existingForms;
    private Map<Long, ModelForms.ModelForm> updatingForms;

    @Before
    public void setUp() {
        mboModelFormService = mock(ModelFormService.class);
        executor = new ModelFormCacheUpdateExecutor(categoryRepository, modelFormRepository, mboModelFormService);

        int sampleSize = ModelFormCacheUpdateExecutor.BATCH_SIZE * 2;

        categories = new ArrayList<>(sampleSize);
        existingForms = new HashMap<>();
        updatingForms = new HashMap<>();

        for (long i = 0; i < sampleSize; i++) {
            categories.add(new Category().setCategoryId(i));
            if (i % 2 == 0) {
                boolean published = i % 8 == 0;
                existingForms.put(i, new CachedModelForm(i, published));
                if (i % 4 == 0) {
                    updatingForms.put(i, ModelForms.ModelForm.newBuilder()
                        .setCategoryId(i).setPublished(!published).build());
                }
            }
        }

        categoryRepository.insertBatch(categories);
        modelFormRepository.insertBatch(existingForms.values());

        doAnswer(this::mboAnswer).when(mboModelFormService).getModelForms(any(GetModelFormsRequest.class));
    }

    @Test
    public void ignoresBatchErrorButThrowsAtTheEnd() {
        reset(mboModelFormService);

        var exception = new IllegalStateException();
        doThrow(exception)
            .doAnswer(this::mboAnswer)
            .when(mboModelFormService).getModelForms(any(GetModelFormsRequest.class));

        assertThatThrownBy(() -> executor.execute()).hasCauseReference(exception);

        verify(mboModelFormService, times(2)).getModelForms(any());
    }

    @Test
    public void removesNonUpdated() {
        assertThat(modelFormRepository.findAll()).hasSize(existingForms.size());
        executor.execute();

        var current = modelFormRepository.findAll();
        assertThat(current).hasSize(updatingForms.size());
        current.forEach(f -> {
            assertThat(f.isPublished()).isEqualTo(!existingForms.get(f.getCategoryId()).isPublished());
        });
    }

    private GetModelFormsResponse mboAnswer(InvocationOnMock invocation) {
        var rq = (GetModelFormsRequest) invocation.getArgument(0);
        var forms = rq.getCategoryIdsList().stream()
            .map(updatingForms::get).filter(Objects::nonNull).collect(Collectors.toList());
        return GetModelFormsResponse.newBuilder().addAllModelForms(forms).build();
    }
}
