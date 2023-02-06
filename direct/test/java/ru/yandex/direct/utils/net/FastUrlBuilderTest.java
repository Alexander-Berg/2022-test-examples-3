package ru.yandex.direct.utils.net;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FastUrlBuilderTest {
    @Test
    public void complexUrlBuilding() throws Exception {
        String url = new FastUrlBuilder("URL/path")
                .addParam("p1", "val")
                .addParam("p1", "знач ение")
                .addParam("p2", 'x')
                .addParam("p3", 3123)
                .build();
        assertThat(url)
                .isEqualTo("URL/path?p1=val&p1=%D0%B7%D0%BD%D0%B0%D1%87+%D0%B5%D0%BD%D0%B8%D0%B5&p2=x&p3=3123");
    }
}
