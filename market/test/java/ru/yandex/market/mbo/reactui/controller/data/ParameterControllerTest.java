package ru.yandex.market.mbo.reactui.controller.data;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.reactui.dto.parameters.mappers.ParamOptionDtoMapper;
import ru.yandex.market.mbo.reactui.service.ParameterServiceRemote;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author yuramalinov
 * @created 27.11.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ParameterControllerTest {
    private ParameterController parameterController;
    private IParameterLoaderService parameterLoaderService;
    private ParameterServiceRemote parameterServiceRemote;

    private static final long DEFAULT_HID = -1L;
    private static final long ROOT_HID = 0L;

    private static final long ID1 = 0L;
    private static final long ID2 = 0L;

    private Parameter parameter1;
    private InheritedParameter parameter2;

    @Before
    public void setup() {
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        parameterServiceRemote = Mockito.mock(ParameterServiceRemote.class);
        ParamOptionDtoMapper paramOptionDtoMapper = Mappers.getMapper(ParamOptionDtoMapper.class);
        parameterController = new ParameterController(parameterLoaderService, paramOptionDtoMapper,
            parameterServiceRemote);

        Mockito.when(parameterLoaderService.loadParameter(ID1, DEFAULT_HID)).thenReturn(parameter1);
        Mockito.when(parameterLoaderService.loadParameter(ID2, ROOT_HID)).thenReturn(parameter2);

    }

    @Test
    public void getValueDetails() {
        // TODO сделать для 36845
    }


    @Test
    public void testItProxies() {
        Map<Long, String> names = Map.of(1L, "Test", 2L, "Test2");
        Mockito.when(parameterLoaderService.loadRusParamNames())
            .thenReturn(names);
        Assertions.assertThat(parameterController.loadRusParamNamesAll()).containsAllEntriesOf(names);
    }

    @Test
    public void testItFiltersAndSkipsNotFound() {
        Map<Long, String> names = Map.of(1L, "Test", 2L, "Test2");
        Mockito.when(parameterLoaderService.loadRusParamNames())
            .thenReturn(names);

        Assertions.assertThat(parameterController.loadRusParamNames(Set.of(1L, 3L)))
            .containsExactly(Map.entry(1L, "Test"));
    }

    @Test
    public void nullNamesMustBeSkipped() {
        Map<Long, String> names = new HashMap<>();
        names.put(1L, "Test");
        names.put(2L, "Test2");
        names.put(3L, null);
        Mockito.when(parameterLoaderService.loadRusParamNames())
            .thenReturn(names);

        Assertions.assertThat(parameterController.loadRusParamNames(Set.of(1L, 2L, 3L)))
            .containsExactly(Map.entry(1L, "Test"), Map.entry(2L, "Test2"));
    }
}
