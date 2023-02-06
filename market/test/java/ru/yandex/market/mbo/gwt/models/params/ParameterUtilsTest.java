package ru.yandex.market.mbo.gwt.models.params;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.market.mbo.gwt.models.GwtPair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author ayratgdl
 * @date 15.04.17
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ParameterUtilsTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void findSimilarOptionsEmptyTest() {
        Collection<GwtPair<Option, Option>> similarOptions = ParameterUtils.findSimilarOptions(
            Collections.emptyList(), Collections.emptyList()
        );
        assertTrue(similarOptions.isEmpty());
    }

    @Test
    public void findSimilarOptionsTest() {
        Option option11 = createOption(1, "similar1", "alias1");
        Option option12 = createOption(2, "name1_2", "alias2", "similar2");

        Option option21 = createOption(3, "SIMILAR1", "alias3");
        Option option22 = createOption(4, "name2_2", "alias3", "SIMILAR1");
        Option option23 = createOption(5, "SIMILAR2", "alias4");

        Collection<GwtPair<Option, Option>> expected = Arrays.asList(
            new GwtPair<>(option11, option21),
            new GwtPair<>(option11, option22),
            new GwtPair<>(option12, option23)
        );
        Collection<GwtPair<Option, Option>> actual = ParameterUtils.findSimilarOptions(
            Arrays.asList(option11, option12),
            Arrays.asList(option21, option22, option23)
        );
        assertArrayEquals(expected.toArray(new GwtPair[0]), actual.toArray(new GwtPair[0]));
    }

    @Test
    public void getBooleanOption() throws Exception {
        Option trueOption = createOption(1, "true");
        Option falseOption = createOption(2, "FALSE");
        CategoryParam param = mock(CategoryParam.class);
        when(param.getOptions()).thenReturn(Arrays.asList(trueOption, falseOption));

        assertThat(ParameterUtils.getBooleanOption(param, true), is(sameInstance(trueOption)));
        assertThat(ParameterUtils.getBooleanOption(param, false), is(sameInstance(falseOption)));
    }


    @Test
    public void getBooleanOptionThrows() throws Exception {
        Option trueOption = createOption(1, "true1");
        Option falseOption = createOption(2, "FALSE0");
        CategoryParam param = mock(CategoryParam.class);
        when(param.getOptions()).thenReturn(Arrays.asList(trueOption, falseOption));
        when(param.getId()).thenReturn(42L);

        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("option false not found in parameter #42");
        ParameterUtils.getBooleanOption(param, false);
    }

    private static Option createOption(long id, String name, String... aliases) {
        Option option = new OptionImpl(id, name);
        for (String alias : aliases) {
            option.addAlias(new EnumAlias(0, 225, alias));
        }
        return option;
    }

}
