package ru.yandex.market.core.notification.model.data;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.core.framework.composer.JDOMConverter;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit тесты для {@link HasAgencyContainer}.
 *
 * @author avetokhin 13/03/18.
 */
public class HasAgencyContainerTest {

    @Test
    public void convert() {
        final String result = new JDOMConverter().convert(new HasAgencyContainer(true));
        assertThat(result, Matchers.equalTo("<has-agency>true</has-agency>"));
    }

}
