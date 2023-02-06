package ru.yandex.market.tsum.spok.validation.validator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.spok.pipelines.SpokPipelineConstants;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.spok.validation.SpokValidationMessages.MISSING_REQUIRED_PARAMETER_MESSAGE;
import static ru.yandex.market.tsum.spok.validation.validator.SpokComponentNameValidator.COMPONENT_ALREADY_EXISTS_MESSAGE;
import static ru.yandex.market.tsum.spok.validation.validator.SpokComponentNameValidator.FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokComponentNameValidator.NAME_LENGTH_EXCEEDED;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class SpokComponentNameValidatorTest {
    public static final String EXISTING_COMPONENT = "existingComponent";
    public static final String NON_EXISTING_COMPONENT = "nonExistingComponent";
    public static final String LONG_COMPONENT_NAME = "veryLongComponentName";
    public static final String SOLOMON_PROJECT_ID = "market-test-service";
    @Mock
    private ComponentsDao componentsDao;

    @InjectMocks
    private SpokComponentNameValidator validator;

    @Before
    public void setUp() {
        when(componentsDao.getByName(EXISTING_COMPONENT)).thenReturn(new Component());
        when(componentsDao.getByName(NON_EXISTING_COMPONENT)).thenReturn(null);
        when(componentsDao.getByName(LONG_COMPONENT_NAME)).thenReturn(null);
    }

    @Test
    public void nameIsNull() {
        assertThat(validator.validate(serviceParams(null)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(MISSING_REQUIRED_PARAMETER_MESSAGE, FIELD_NAME)));
        verifyZeroInteractions(componentsDao);
    }

    @Test
    public void nameOfNonExistingComponent() {
        assertThat(validator.validate(serviceParams(NON_EXISTING_COMPONENT)))
                .isEqualTo(SpokValidationResult.ok());
        verify(componentsDao).getByName(NON_EXISTING_COMPONENT);
        verifyNoMoreInteractions(componentsDao);
    }

    @Test
    public void nameOfExistingComponent() {
        assertThat(validator.validate(serviceParams(EXISTING_COMPONENT)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(COMPONENT_ALREADY_EXISTS_MESSAGE, EXISTING_COMPONENT)));
        verify(componentsDao).getByName(EXISTING_COMPONENT);
        verifyNoMoreInteractions(componentsDao);
    }

    @Test
    public void nameLengthExceededTest() {
        int maxNameLength = Math.min(
            SpokPipelineConstants.CLICKPHITE_RESTRICTION_LENGTH -
                SpokPipelineConstants.AUTO_GRAPH_SUFFIX_LENGTH -
                SOLOMON_PROJECT_ID.length(),
            SpokPipelineConstants.GRAFANA_RESTRICTION_LENGTH -
                SpokPipelineConstants.AUTO_GRAPH_SUFFIX_LENGTH -
                "market-".length()
        );
        assertThat(validator.validate(serviceParams(LONG_COMPONENT_NAME)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(NAME_LENGTH_EXCEEDED, maxNameLength)));
        verify(componentsDao).getByName(LONG_COMPONENT_NAME);
        verifyNoMoreInteractions(componentsDao);
    }

    @Nonnull
    private ServiceParams serviceParams(@Nullable String name) {
        ServiceParams result = new ServiceParams();
        result.setName(name);
        result.setSolomonProjectId(SOLOMON_PROJECT_ID);
        return result;
    }
}
