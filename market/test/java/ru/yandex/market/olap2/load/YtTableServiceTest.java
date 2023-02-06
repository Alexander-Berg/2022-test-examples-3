package ru.yandex.market.olap2.load;

import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.market.olap2.model.RejectException;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.yt.YtTableService;
import ru.yandex.market.olap2.yt.YtWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtTableServiceTest {

    private YtWrapper ytWrapper = mock(YtWrapper.class);
    private Yt yt = mock(Yt.class);
    private Cypress cypress = mock(Cypress.class);

    private final YtTableService service = new YtTableService(ytWrapper);

    @Before
    public void before() {
        when(ytWrapper.yt(any())).thenReturn(yt);
        when(yt.cypress()).thenReturn(cypress);
    }

    @Test(expected = RejectException.class)
    public void testAttributeNotFound() {
        when(cypress.get(any(YPath.class))).thenThrow(YtErrorMapping.ResolveError.class);
        service.getYtAttribute(new YtCluster("hahn"), "//test/path", "someattribute");
    }

    @Test
    public void testAttributeFound() {
        var expected = new YTreeIntegerNodeImpl(false, 100500, new HashMap<>());
        when(cypress.get(any(YPath.class))).thenReturn(expected);
        Assertions.assertThat(service.getYtAttribute(new YtCluster("hahn"), "//test/path", "someattribute"))
                .isEqualTo(expected);
    }
}
