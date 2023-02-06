package ru.yandex.market.yt.util.client;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class JarCleanerTest {

    private final Yt yt = Mockito.mock(Yt.class);

    private final List<YTreeStringNode> list = new ArrayList<>();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        Cypress cypress = Mockito.mock(Cypress.class);
        Mockito.when(yt.cypress()).thenReturn(cypress);

        Mockito.doAnswer(invocation -> true)
            .when(cypress).exists(Mockito.any(YPath.class));

        Mockito.doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            list.removeIf(node -> node.getValue().equals(path.name()));
            return null;
        }).when(cypress).remove(Mockito.any(YPath.class));

        Mockito.doAnswer(invocation -> new ArrayList<>(list))
                .when(cypress).list(Mockito.any(YPath.class), Mockito.anyCollection());

        list.clear();
    }

    @Test
    public void testDateFormat() {
        list.add(node("2017-10-10.jar", "2017-10-10T22:24:24.714834Z"));

        JarCleaner jarCleaner = new JarCleaner(yt, YPath.simple("//tmp"), Duration.ofDays(1));
        jarCleaner.removeObsoleteJars();
    }


    @Test(expected = IllegalArgumentException.class)
    public void testNegativePeriod() {
        new JarCleaner(yt, YPath.simple("//tmp"), Duration.ofDays(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroPeriod() {
        new JarCleaner(yt, YPath.simple("//tmp"), Duration.ofDays(0));
    }

    @Test
    public void testPositivePeriod() {
        JarCleaner jarCleaner = new JarCleaner(yt, "//tmp", Duration.ofDays(1));

        Assert.assertEquals(YPath.simple("//tmp"), jarCleaner.getJarsDir());
    }

    @Test
    public void testClientFactoryConstructor() {
        YPath path = YPath.simple("//tmp/jars");

        YtHttpClientFactory factory = Mockito.mock(YtHttpClientFactory.class);
        Mockito.when(factory.getJarsDir()).thenReturn(path);
        Mockito.when(factory.getInstance()).thenReturn(yt);

        JarCleaner jarCleaner = new JarCleaner(factory, Duration.ofDays(1));

        Assert.assertEquals(path, jarCleaner.getJarsDir());
    }

    @Test
    public void testRemoveObsoleteJars() {
        // LocalDateTime.now() is unprecise, so subtract a second to account for that
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .appendLiteral('Z')
                .toFormatter();

        list.add(node("2017-10-13.jar", formatter.format(now.minusDays(0))));
        list.add(node("2017-10-12.jar", formatter.format(now.minusDays(1))));
        list.add(node("2017-10-11.jar", formatter.format(now.minusDays(2))));
        list.add(node("2017-10-10.jar", formatter.format(now.minusDays(3))));

        JarCleaner jarCleaner = new JarCleaner(yt, YPath.simple("//tmp"), Duration.ofDays(2));
        jarCleaner.removeObsoleteJars();

        Assert.assertEquals(Arrays.asList(
                node("2017-10-13.jar", formatter.format(now.minusDays(0))),
                node("2017-10-12.jar", formatter.format(now.minusDays(1)))
        ), list);
    }

    @Test
    public void repeatedRemoveObsoleteJars() {
        for (int i = 0; i < 1000; i++) {
            list.clear();
            testRemoveObsoleteJars();
        }
    }

    private static YTreeStringNode node(String name, String accessTime) {
        return new YTreeStringNodeImpl(name, Cf.map("access_time", YTree.stringNode(accessTime)));
    }
}
