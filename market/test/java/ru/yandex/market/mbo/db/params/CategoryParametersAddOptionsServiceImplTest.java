package ru.yandex.market.mbo.db.params;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class CategoryParametersAddOptionsServiceImplTest {

    private static final long ABSENT_CATEGORY_ID = 1L;
    private static final long OK_CATEGORY_ID = 2L;
    private static final long PARENT_CATEGORY_ID = 3L;
    private static final long INHERITED_PARAMETER_ID = 12L;
    private static final long PARENT_PARAMETER_ID = 31L;
    private static final long INHERITED_GLOBAL_PARAMETER_ID = 13L;
    private static final long PARENT_GLOBAL_PARAMETER_ID = 32L;
    private static final long OK_LOCAL_VENDOR_ID = 41L;
    private static final long USER_ID = 333L;

    private CategoryParametersAddOptionsServiceImpl categoryParametersAddOptionsService;

    private IParameterLoaderService parameterLoaderService;

    private ParameterService parameterService;

    @Before
    public void setup() {
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        parameterService = Mockito.mock(ParameterService.class);

        when(parameterLoaderService.loadCategoryEntitiesByHid(ABSENT_CATEGORY_ID))
            .thenThrow(new CategoryNotFoundException("Category not found"));
        when(parameterLoaderService.loadCategoryEntitiesByHid(OK_CATEGORY_ID))
            .thenReturn(getOkCategoryEntities());

        when(parameterLoaderService.loadCategoryEntitiesByHid(PARENT_CATEGORY_ID))
            .thenReturn(getParentCategoryEntities());
        when(parameterLoaderService.loadCategoryEntitiesByHid(IParameterLoaderService.GLOBAL_ENTITIES_HID))
            .thenReturn(getGlobalCategoryEntities());

        when(parameterService.createDefaultSaveContext(anyLong())).thenReturn(new ParameterSaveContext(USER_ID));
        when(parameterLoaderService.loadParameter(anyLong(), anyLong()))
            .then(x -> {
                if (x.getArgument(1).equals(IParameterLoaderService.GLOBAL_ENTITIES_HID)) {
                    return getGlobalCategoryEntities()
                        .getParameterById(x.getArgument(0));
                }
                if (x.getArgument(1).equals(OK_CATEGORY_ID)) {
                    return getOkCategoryEntities()
                        .getParameterById(x.getArgument(0));
                }
                if (x.getArgument(1).equals(PARENT_CATEGORY_ID)) {
                    return getParentCategoryEntities()
                        .getParameterById(x.getArgument(0));
                }
                throw new CategoryNotFoundException("Category not found");
            });

        categoryParametersAddOptionsService = new CategoryParametersAddOptionsServiceImpl();
        categoryParametersAddOptionsService.setParameterLoaderService(parameterLoaderService);
        categoryParametersAddOptionsService.setParameterService(parameterService);
        categoryParametersAddOptionsService.setActiveAddOptionsMethod(true);
    }

    @Test
    public void addOptionToRootParameter() {
        MboParameters.AddOptionsRequest request = MboParameters.AddOptionsRequest.newBuilder()
            .setCategoryId(PARENT_CATEGORY_ID)
            .setParamId(PARENT_PARAMETER_ID)
            .addOption(MboParameters.Option.newBuilder().addName(WordProtoUtils.defaultWord("test"))).build();

        MboParameters.AddOptionsResponse response = categoryParametersAddOptionsService.addOptions(request);

        assertThat(response.getOperationStatus()).extracting(MboParameters.OperationStatus::getStatus)
            .isEqualTo(MboParameters.OperationStatusType.OK);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(parameterService).saveParameter(
            any(), captor.capture(), Mockito.anyLong(), any(), any());
        Long categoryId = captor.getValue();
        Assertions.assertThat(categoryId).isEqualTo(PARENT_CATEGORY_ID);
    }

    @Test
    public void addOptionToInheritedParameter() {
        MboParameters.AddOptionsRequest request = MboParameters.AddOptionsRequest.newBuilder()
            .setCategoryId(OK_CATEGORY_ID)
            .setParamId(PARENT_PARAMETER_ID)
            .addOption(MboParameters.Option.newBuilder().addName(WordProtoUtils.defaultWord("test"))).build();

        MboParameters.AddOptionsResponse response = categoryParametersAddOptionsService.addOptions(request);

        assertThat(response.getOperationStatus()).extracting(MboParameters.OperationStatus::getStatus)
            .isEqualTo(MboParameters.OperationStatusType.OK);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(parameterService).saveParameter(
            any(), captor.capture(), Mockito.anyLong(), any(), any());
        Long categoryId = captor.getValue();
        Assertions.assertThat(categoryId).isEqualTo(OK_CATEGORY_ID);
    }

    @Test
    public void addOptionToGlobalParameter() {
        MboParameters.AddOptionsRequest request = MboParameters.AddOptionsRequest.newBuilder()
            .setCategoryId(IParameterLoaderService.GLOBAL_ENTITIES_HID)
            .setParamId(PARENT_GLOBAL_PARAMETER_ID)
            .addOption(MboParameters.Option.newBuilder().addName(WordProtoUtils.defaultWord("test"))).build();

        MboParameters.AddOptionsResponse response = categoryParametersAddOptionsService.addOptions(request);

        assertThat(response.getOperationStatus()).extracting(MboParameters.OperationStatus::getStatus)
            .isEqualTo(MboParameters.OperationStatusType.OK);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(parameterService).saveParameter(
            any(), captor.capture(), Mockito.anyLong(), any(), any());
        Long categoryId = captor.getValue();
        Assertions.assertThat(categoryId).isEqualTo(IParameterLoaderService.GLOBAL_ENTITIES_HID);
    }

    @Test
    public void addOptionToInheritedFromGlobalParameter() {
        MboParameters.AddOptionsRequest request = MboParameters.AddOptionsRequest.newBuilder()
            .setCategoryId(OK_CATEGORY_ID)
            .setParamId(PARENT_GLOBAL_PARAMETER_ID)
            .addOption(MboParameters.Option.newBuilder().addName(WordProtoUtils.defaultWord("test"))).build();

        MboParameters.AddOptionsResponse response = categoryParametersAddOptionsService.addOptions(request);

        assertThat(response.getOperationStatus()).extracting(MboParameters.OperationStatus::getStatus)
            .isEqualTo(MboParameters.OperationStatusType.OK);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(parameterService).saveParameter(
            any(), captor.capture(), Mockito.anyLong(), any(), any());
        Long categoryId = captor.getValue();
        Assertions.assertThat(categoryId).isEqualTo(OK_CATEGORY_ID);
    }

    private CategoryEntities getOkCategoryEntities() {
        CategoryEntities categoryEntities =
            new CategoryEntities(OK_CATEGORY_ID, Collections.singletonList(new Date().getTime()));
        Parameter parent = new Parameter();
        parent.setId(PARENT_PARAMETER_ID);
        parent.setCategoryHid(PARENT_CATEGORY_ID);
        InheritedParameter okParameter = new InheritedParameter(parent);
        okParameter.setId(INHERITED_PARAMETER_ID);
        okParameter.setCategoryHid(OK_CATEGORY_ID);
        okParameter.addOption(new OptionImpl(new OptionImpl(OK_LOCAL_VENDOR_ID),
            Option.OptionType.VENDOR));
        categoryEntities.addParameter(okParameter);

        Parameter global = new Parameter();
        global.setCategoryHid(IParameterLoaderService.GLOBAL_ENTITIES_HID);
        global.setId(PARENT_GLOBAL_PARAMETER_ID);
        InheritedParameter inheritedGlobalParameter = new InheritedParameter(global);
        inheritedGlobalParameter.setId(INHERITED_GLOBAL_PARAMETER_ID);
        inheritedGlobalParameter.setCategoryHid(OK_CATEGORY_ID);
        categoryEntities.addParameter(inheritedGlobalParameter);

        return categoryEntities;
    }

    private CategoryEntities getParentCategoryEntities() {
        CategoryEntities categoryEntities =
            new CategoryEntities(PARENT_CATEGORY_ID, Collections.singletonList(new Date().getTime()));
        Parameter parent = new Parameter();
        parent.setId(PARENT_PARAMETER_ID);
        parent.setCategoryHid(PARENT_CATEGORY_ID);
        categoryEntities.addParameter(parent);
        return categoryEntities;
    }

    private CategoryEntities getGlobalCategoryEntities() {
        CategoryEntities categoryEntities = new CategoryEntities(
            IParameterLoaderService.GLOBAL_ENTITIES_HID, Collections.singletonList(new Date().getTime()));
        Parameter global = new Parameter();
        global.setCategoryHid(IParameterLoaderService.GLOBAL_ENTITIES_HID);
        global.setId(PARENT_GLOBAL_PARAMETER_ID);
        categoryEntities.addParameter(global);
        return categoryEntities;
    }
}
