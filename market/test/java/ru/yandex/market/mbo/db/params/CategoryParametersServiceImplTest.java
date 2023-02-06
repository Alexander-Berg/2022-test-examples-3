package ru.yandex.market.mbo.db.params;

import java.util.Collections;
import java.util.Date;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.exceptions.OptionPropertyDuplicationException;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author commince
 * @date 04.04.2018
 */
public class CategoryParametersServiceImplTest {

    private static final long ABSENT_CATEGORY_ID = 1L;
    private static final long ABSENT_PARAMETER_ID = 11L;
    private static final long OK_CATEGORY_ID = 2L;
    private static final long PARENT_CATEGORY_ID = 3L;
    private static final long INHERITED_PARAMETER_ID = 12L;
    private static final long PARENT_PARAMETER_ID = 31L;
    private static final long INHERITED_GLOBAL_PARAMETER_ID = 13L;
    private static final long PARENT_GLOBAL_PARAMETER_ID = 32L;
    private static final long OK_LOCAL_VENDOR_ID = 41L;
    private static final long OK_GLOBAL_VENDOR_ID = 21L;
    private static final int RUS_LANG_ID = 225;
    private static final long USER_ID = 333L;

    private CategoryParametersServiceImpl categoryParametersService;

    private IParameterLoaderService parameterLoaderService;

    private ParameterService parameterService;

    private GlobalVendorService globalVendorService;

    private CategoryParametersExtractorService extractorService;

    @Before
    public void setup() {
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        parameterService = Mockito.mock(ParameterService.class);
        globalVendorService = Mockito.mock(GlobalVendorService.class);
        extractorService = Mockito.mock(CategoryParametersExtractorService.class);

        when(parameterLoaderService.loadCategoryEntitiesByHid(ABSENT_CATEGORY_ID))
            .thenThrow(new CategoryNotFoundException("Category not found"));
        when(parameterLoaderService.loadCategoryEntitiesByHid(OK_CATEGORY_ID))
            .thenReturn(getOkCategoryEntities());

        when(parameterLoaderService.loadCategoryEntitiesByHid(PARENT_CATEGORY_ID))
            .thenReturn(getParentCategoryEntities());
        when(parameterLoaderService.loadCategoryEntitiesByHid(IParameterLoaderService.GLOBAL_ENTITIES_HID))
            .thenReturn(getGlobalCategoryEntities());
        when(globalVendorService.loadVendor(OK_LOCAL_VENDOR_ID))
            .thenReturn(getGlobalVendor());
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
        categoryParametersService = new CategoryParametersServiceImpl();

        categoryParametersService.setParameterLoaderService(parameterLoaderService);
        categoryParametersService.setParameterService(parameterService);
        categoryParametersService.setGlobalVendorService(globalVendorService);
        categoryParametersService.setCategoryParametersExtractorService(extractorService);
    }

    @Test
    public void testOverrideOptionsSuccess() {
        MboParameters.OverrideOptionsResponse resp =
            categoryParametersService.overrideOptions(getOverrideOptionRequest(OK_CATEGORY_ID, PARENT_PARAMETER_ID));
        Assert.assertEquals(MboParameters.OperationStatusType.OK,
            resp.getOperationStatusList().get(0).getStatus());
    }

    @Test
    public void testOverrideOptionsNoCategory() {
        MboParameters.OverrideOptionsResponse resp =
            categoryParametersService.overrideOptions(
                getOverrideOptionRequest(ABSENT_CATEGORY_ID, PARENT_PARAMETER_ID));

        Assert.assertEquals(MboParameters.OperationStatusType.INTERNAL_ERROR,
            resp.getOperationStatusList().get(0).getStatus());
    }

    @Test
    public void testOverrideOptionsNoParameter() throws Exception {
        MboParameters.OverrideOptionsResponse resp =
            categoryParametersService.overrideOptions(getOverrideOptionRequest(OK_CATEGORY_ID, ABSENT_PARAMETER_ID));

        Assert.assertEquals(MboParameters.OperationStatusType.INTERNAL_ERROR,
            resp.getOperationStatusList().get(0).getStatus());
    }

    @Test
    public void returnEmptyResponseIfCategoryDoesntExists() {
        MboParameters.GetCategoryParametersRequest request = MboParameters.GetCategoryParametersRequest.newBuilder()
            .setCategoryId(OK_CATEGORY_ID + 2)
            .build();


        MboParameters.GetCategoryParametersResponse response = categoryParametersService.getParameters(request);

        MboParameters.Category emptyCategory = MboParameters.Category.newBuilder().build();
        assertThat(response.getCategoryParameters()).isNotSameAs(emptyCategory).isEqualTo(emptyCategory);
    }

    @Test
    public void returnParameters() {
        MboParameters.Category category = MboParameters.Category.newBuilder()
            .setHid(OK_CATEGORY_ID)
            .build();
        when(extractorService.extractCategory(OK_CATEGORY_ID)).thenReturn(category);
        MboParameters.GetCategoryParametersRequest request = MboParameters.GetCategoryParametersRequest.newBuilder()
            .setCategoryId(OK_CATEGORY_ID)
            .build();


        MboParameters.GetCategoryParametersResponse response = categoryParametersService.getParameters(request);


        assertThat(response.getCategoryParameters()).isSameAs(category);
    }

    @Test
    public void updateOptionExceptionResult() {
        MboParameters.UpdateOptionRequest request = MboParameters.UpdateOptionRequest.newBuilder()
            .setCategoryId(OK_CATEGORY_ID)
            .setOptionId(OK_LOCAL_VENDOR_ID)
            .setParamId(PARENT_PARAMETER_ID)
            .build();
        doThrow(new OptionPropertyDuplicationException())
            .when(parameterService).saveParameter(any(), anyLong(), any(), any());
        MboParameters.UpdateOptionResponse response = categoryParametersService.updateOption(request);
        Assertions.assertThat(response.getOperationStatus().getStatus()).isEqualTo(
            MboParameters.OperationStatusType.VALIDATION_ERROR
        );
    }

    private MboParameters.OverrideOptionsRequest getOverrideOptionRequest(long hid, long paramId) {
        return MboParameters.OverrideOptionsRequest.newBuilder()
            .setParamId(paramId)
            .setCategoryId(hid)
            .setUserId(1L)
            .addOptions(
                MboParameters.OverrideOptionInfo.newBuilder()
                    .setActive(true)
                    .setOptionId(OK_LOCAL_VENDOR_ID)
                    .addName(
                        MboParameters.Word.newBuilder()
                            .setLangId(RUS_LANG_ID)
                            .setName("test")
                            .build()
                    )
            ).build();
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

    private GlobalVendor getGlobalVendor() {
        GlobalVendor result = new GlobalVendor();
        result.setId(OK_GLOBAL_VENDOR_ID);
        return result;
    }
}
