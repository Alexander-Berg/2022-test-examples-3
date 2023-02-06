package ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.StateEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class DynamicTextAdTargetStateConverterTest {

    @Parameterized.Parameter
    public DynamicTextAdTarget dynamicTextAdTarget;

    @Parameterized.Parameter(1)
    public StateEnum state;

    public GetResponseConverter responseConverter;

    @Autowired
    private TranslationService translationService;

    @Parameterized.Parameters(name = "check {1} state")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new DynamicTextAdTarget().withId(123L).withIsSuspended(false),
                        StateEnum.ON},
                {new DynamicTextAdTarget().withId(123L).withIsSuspended(true), StateEnum.SUSPENDED},
                {new DynamicTextAdTarget().withId(null), StateEnum.DELETED},
        };
    }

    @Before
    public void prepare() {
        responseConverter = new GetResponseConverter(translationService);
    }

    @Test
    public void test() {
        assertThat(responseConverter.convertState(dynamicTextAdTarget)).isEqualTo(state);
    }
}
