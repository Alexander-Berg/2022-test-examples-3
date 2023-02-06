package ru.yandex.market.ff.service.implementation;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.entity.EnvironmentParam;
import ru.yandex.market.ff.repository.EnvironmentParamRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnvironmentParamServiceImplTest {
    private EnvironmentParamRepository environmentParamRepository = mock(EnvironmentParamRepository.class);
    private EnvironmentParamServiceImpl environmentParamService =
        new EnvironmentParamServiceImpl(environmentParamRepository);

    @Test
    void callCount() {
        when(environmentParamRepository.findAllByNameOrderById("key"))
            .thenReturn(Collections.singletonList(getEnvironmentParam()));
        environmentParamService.setParam("key", Collections.singletonList("true"));
        Boolean key = environmentParamService.getBooleanParam("key");
        Boolean secondKey = environmentParamService.getBooleanParam("key");

        verify(environmentParamRepository, times(1)).findAllByNameOrderById("key");
        assertEquals(true, key);
        assertEquals(true, secondKey);
    }

    @NotNull
    private EnvironmentParam getEnvironmentParam() {
        EnvironmentParam environmentParam = new EnvironmentParam();
        environmentParam.setName("key");
        environmentParam.setValue("true");
        return environmentParam;
    }

}
