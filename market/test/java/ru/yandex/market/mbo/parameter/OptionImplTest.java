package ru.yandex.market.mbo.parameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;

import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;

/**
 * @author Alexander Ayoupov (ayoupov@yandex-team.ru)
 * @date 13.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class OptionImplTest {
    @Test
    public void testChangesMerge() {
        Option parent = new OptionImpl();
        parent.setDisplayName("Some name");
        parent.setCode("some code");

        OptionImpl changer = new OptionImpl();
        changer.setCode("another code");
        changer.setDisplayName("Some name");
        changer.setDefaultValue(true);

        OptionImpl child = new OptionImpl(parent);
        child.inheritFrom(changer);
        child.setParent(null);

        assertThat(child.getCode()).isEqualTo("another code");
        assertThat(child.getDisplayName()).isNull();
        assertThat(child.isDefaultValue()).isTrue();
    }

    @Test
    public void testChangesMergeNoParent() {

        OptionImpl changer = new OptionImpl();
        changer.setCode("another code");
        changer.setDisplayName("Some name");
        changer.setDefaultValue(true);

        OptionImpl child = new OptionImpl();
        child.setDisplayName("a name");
        child.inheritFrom(changer);

        assertThat(child.getCode()).isEqualTo("another code");
        assertThat(child.getDisplayName()).isEqualTo("Some name");
        assertThat(child.getTag()).isNull();
        assertThat(child.isDefaultValue()).isTrue();
    }
}
