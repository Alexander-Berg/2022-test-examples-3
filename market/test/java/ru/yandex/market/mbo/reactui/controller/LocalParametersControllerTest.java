package ru.yandex.market.mbo.reactui.controller;

import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamWithRelatedData;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.reactui.dto.ResponseDto;
import ru.yandex.market.mbo.reactui.dto.parameters.CategoryParameterDetailsDto;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.CategoryParameterDetailsMapper;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.LocalCategoryParameterMapper;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.ParamOptionDtoMapper;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.ParameterValuesChangesMapper;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.WordDtoMapper;
import ru.yandex.market.mbo.reactui.service.ParameterServiceRemote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class LocalParametersControllerTest {

    private LocalParametersController localParametersController;

    private ParameterServiceRemote parameterServiceRemote;
    private ParameterService parameterService;
    private AccessControlManager accessControlManager;
    private final ParameterLoaderServiceStub parameterLoader = new ParameterLoaderServiceStub();
    private final LocalCategoryParameterMapper localCategoryParameterMapper = Mappers
            .getMapper(LocalCategoryParameterMapper.class);
    private final CategoryParameterDetailsMapper categoryParameterDetailsMapper = Mappers
            .getMapper(CategoryParameterDetailsMapper.class);
    private final ParamOptionDtoMapper paramOptionDtoMapper = Mappers.getMapper(ParamOptionDtoMapper.class);

    @Autowired
    private ParameterValuesChangesMapper parameterValuesChangesMapper;

    @Before
    public void setUp() {
        WordDtoMapper wordDtoMapper = Mappers.getMapper(WordDtoMapper.class);
        ReflectionTestUtils.setField(categoryParameterDetailsMapper, "wordDtoMapper", wordDtoMapper);
        parameterServiceRemote = Mockito.mock(ParameterServiceRemote.class);
        parameterService = Mockito.mock(ParameterService.class);
        localParametersController = new LocalParametersController(
                parameterServiceRemote,
                localCategoryParameterMapper,
                categoryParameterDetailsMapper,
                parameterValuesChangesMapper,
                parameterService,
                accessControlManager
        );
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testLoadParameter() {
        final long categoryId = 33L;
        final long paramId = 2L;

        ParameterOverride override = new ParameterOverride(paramId);
        override.setCategoryHid(categoryId);
        override.setManualInheritance(true);

        CategoryParamWithRelatedData data = new CategoryParamWithRelatedData(override);
        when(parameterServiceRemote.loadParameterWithLinks(categoryId, paramId))
                .thenReturn(data);

        ResponseDto<CategoryParameterDetailsDto> response =
                localParametersController.loadParameter(categoryId, paramId);
        CategoryParameterDetailsDto categoryParameterDetailsDto = response.getData();
        assertEquals(categoryId, categoryParameterDetailsDto.getCategoryHid().longValue());
        assertEquals(paramId, categoryParameterDetailsDto.getId().longValue());
        assertTrue(categoryParameterDetailsDto.getManualInheritance());
    }

}
