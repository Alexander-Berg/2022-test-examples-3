package ru.yandex.direct.ytwrapper.dynamic.selector;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttributeBasedClusterChooserTest {
    public static final String TESTPATH = "//testpath";
    public static final String ATTRIBUTE = "@creation_text_instant";
    private static final Comparator<YTreeNode> COMPARATOR = Comparator.comparing(a -> Instant.parse(a.stringValue()));
    private Map<YtCluster, String> clusters;
    private List<TestYt> testYts;
    private YtProvider ytProvider;
    private Timer timer;
    private Supplier<Boolean> getShouldThrowException;

    @Before
    public void setUp() throws Exception {
        ytProvider = mock(YtProvider.class);
        timer = new Timer();
        getShouldThrowException = () -> false;

        testYts = new ArrayList<>();
        clusters = new HashMap<>();
        clusters.put(YtCluster.HAHN, "2017-07-18T12:54:25.497475Z");
        clusters.put(YtCluster.ZENO, "2016-07-18T12:54:25.497475Z");
        clusters.forEach((cluster, attribute) -> {
            TestYt testYt = new TestYt(cluster, attribute);
            testYts.add(testYt);
            when(ytProvider.get(cluster)).thenReturn(testYt.yt);
        });
    }

    @After
    public void tearDown() {
        timer.cancel();
    }

    @Test
    public void chooseCluster() {
        AttributeBasedClusterChooser chooser = new AttributeBasedClusterChooser(
                timer,
                Duration.ofHours(1),
                ytProvider,
                clusters.keySet(),
                YPath.simple(TESTPATH),
                ATTRIBUTE,
                COMPARATOR.reversed());
        chooser.chooseCluster();
        assertThat(chooser.getCluster())
                .isNotEmpty()
                .hasValue(YtCluster.HAHN);

        testYts.forEach(yt -> {
            verify(yt.yt, atLeastOnce()).cypress();
            verify(yt.cypress, atLeastOnce()).get(YPath.simple(TESTPATH + "/" + ATTRIBUTE));
        });
    }

    @Test
    public void getClusterReturnsEmptyOnException() {
        AttributeBasedClusterChooser chooser = new AttributeBasedClusterChooser(
                timer,
                Duration.ofMillis(50),
                ytProvider,
                clusters.keySet(),
                YPath.simple(TESTPATH),
                ATTRIBUTE,
                COMPARATOR);
        getShouldThrowException = () -> true;

        Optional<YtCluster> cluster = chooser.getCluster();
        assertThat(cluster).isEmpty();
    }

    @Test
    public void getClusterWithReusedTimer() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        AttributeBasedClusterChooser chooser1 = new AttributeBasedClusterChooser(
                timer,
                Duration.ofMillis(50),
                ytProvider,
                clusters.keySet(),
                YPath.simple(TESTPATH),
                ATTRIBUTE,
                COMPARATOR.reversed());
        getShouldThrowException = () -> counter.incrementAndGet() > 2;

        assertThatCode(chooser1::getCluster).doesNotThrowAnyException();
        // спим, чтобы сработал таймер и получил исключение
        Thread.sleep(250);

        getShouldThrowException = () -> false;

        AttributeBasedClusterChooser chooser2 = new AttributeBasedClusterChooser(
                timer,
                Duration.ofMillis(50),
                ytProvider,
                clusters.keySet(),
                YPath.simple(TESTPATH),
                ATTRIBUTE,
                COMPARATOR.reversed());

        assertThat(chooser2.getCluster())
                .isNotEmpty()
                .hasValue(YtCluster.HAHN);
    }

    private class TestYt {
        final YtCluster ytCluster;
        final Yt yt;
        final Cypress cypress;
        final String attribute;

        TestYt(YtCluster ytCluster, String attribute) {
            this.ytCluster = ytCluster;
            this.yt = mock(Yt.class);
            this.cypress = mock(Cypress.class);
            this.attribute = attribute;

            when(yt.cypress()).thenReturn(cypress);
            when(cypress.get(YPath.simple(TESTPATH).child(ATTRIBUTE)))
                    .thenAnswer(ignored -> {
                        if (getShouldThrowException.get()) {
                            throw new RuntimeException("test");
                        }
                        return new YTreeStringNodeImpl(attribute, Cf.map());
                    });
        }
    }
}
