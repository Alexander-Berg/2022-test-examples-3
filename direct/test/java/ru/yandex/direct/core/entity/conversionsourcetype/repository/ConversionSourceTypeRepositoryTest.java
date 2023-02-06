package ru.yandex.direct.core.entity.conversionsourcetype.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConversionSourceTypeRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private ConversionSourceTypeRepository repository;

    @Test
    public void getNotDraftDoesNotContainDrafts() {
        ConversionSourceType draftConversionSourceType =
                steps.conversionSourceTypeSteps().addDraftConversionSourceType();

        List<ConversionSourceType> notDrafts = repository.getNotDrafts();

        assertThat(notDrafts).doesNotContain(draftConversionSourceType);
    }

    @Test
    public void addConversionSourceTypeSuccess() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().getDefaultConversionSourceType();

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().addConversionSourceTypeAndReturnAdded(expectedConversionSourceType);
        Long id = actualConversionSourceType.getId();

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType.withId(id));
    }

    @Test
    public void addConversionSourceTypeSuccessWithEnDescriptionAndName() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().getDefaultConversionSourceTypeWithEn();

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().addConversionSourceTypeAndReturnAdded(expectedConversionSourceType);
        Long id = actualConversionSourceType.getId();

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType.withId(id));
    }

    @Test
    public void updateConversionSourceTypeSuccessWithName() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().addDraftConversionSourceType().withName("new_name").withNameEn("new_name_en");

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().updateConversionSourceTypeAndReturnUpdated(expectedConversionSourceType);
        Long id = actualConversionSourceType.getId();

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType.withId(id));
    }

    @Test
    public void updateConversionSourceTypeSuccessWithDescription() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().addDraftConversionSourceType().withDescriptionEn("new_description_en").withDescription("new_description_en");

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().updateConversionSourceTypeAndReturnUpdated(expectedConversionSourceType);
        Long id = actualConversionSourceType.getId();

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType.withId(id));
    }

    @Test
    public void removeConversionSourceTypeSuccess() {
        ConversionSourceType removedConversionSourceType =
                steps.conversionSourceTypeSteps().addDefaultConversionSourceType();

        repository.remove(singletonList(removedConversionSourceType.getId()));

        List<ConversionSourceType> allConversionSourceType = repository.getAll();

        assertThat(removedConversionSourceType).isNotIn(allConversionSourceType);
    }

    @Test
    public void removeConversionSourceTypeSuccessWithEn() {
        ConversionSourceType removedConversionSourceType =
                steps.conversionSourceTypeSteps().addDefaultConversionSourceTypeWithEn();

        repository.remove(singletonList(removedConversionSourceType.getId()));

        List<ConversionSourceType> allConversionSourceType = repository.getAll();

        assertThat(removedConversionSourceType).isNotIn(allConversionSourceType);
    }
}
