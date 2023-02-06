package ru.yandex.market.yt.util.utils;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;

public class YtMrUtilsTest {

    private Yt yt;

    @Before
    public void setUp() {
        Cypress cypress = Mockito.mock(Cypress.class);

        Mockito.when(cypress.get(Mockito.any(Optional.class), Mockito.anyBoolean(),
            Mockito.any(YPath.class), Mockito.anyCollection()))
            .thenAnswer(invocation -> {
                YPath path = invocation.getArgument(2);
                YTreeBuilder builder = YTree.builder()
                    .beginAttributes()
                        .key("unflushed_timestamp").value(102)
                        .key("retained_timestamp").value(100)
                    .endAttributes()
                    .value(path.justPath().toTree());
                return builder.build();
            });

        yt = Mockito.mock(Yt.class);
        Mockito.when(yt.cypress()).thenReturn(cypress);
    }

    @Test
    public void getFlushedTablePath() {
        YPath flushedPath = YtMrUtils.getFlushedTablePath(Optional.empty(), yt, YPath.simple("//some/din-table"));
        Assert.assertEquals(YPath.simple("//some/din-table").withTimestamp(101), flushedPath);
    }

    @Test
    public void getFlushedTablePathWithAttributes() {
        YPath flushedPath = YtMrUtils.getFlushedTablePath(Optional.empty(), yt,
            YPath.simple("//some/din-table").sortedBy("test"));
        Assert.assertEquals(YPath.simple("//some/din-table").sortedBy("test").withTimestamp(101), flushedPath);
    }
}
