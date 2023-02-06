package ru.yandex.market.yt.util.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class YtTemplateTest {

    /**
     * Проверяем корректный возврат результата из второго кластера, когда первый упал")
     */
    @Test
    public void test_getFromYt() {
        YtTemplate ytTemplate = new YtTemplate(new YtCluster[]{
            new YtCluster("test1", null),
            new YtCluster("test2", null)
        });

        List<String> called = new ArrayList<>();

        Integer result = ytTemplate.getFromYt(cluster -> {
            called.add(cluster.getName());
            if (cluster.getName().equals("test1")) {
                throw new RuntimeException("test1");
            } else {
                return 1;
            }
        });

        assertThat(result.intValue()).isEqualTo(1);
        assertThat(called).isEqualTo(Arrays.asList("test1", "test2"));
    }

    /**
     * Проверяем корректное выполенение во втором кластере, когда первый упал
     */
    @Test
    public void test_runInYt() {
        YtTemplate ytTemplate = new YtTemplate(new YtCluster[]{
            new YtCluster("test1", null),
            new YtCluster("test2", null)
        });

        List<String> called = new ArrayList<>();
        AtomicBoolean contextResetMarker = new AtomicBoolean(true);
        ytTemplate.runInYt(
            new YtTemplate.ClusterSwitchAwareConsumer() {
                @Override
                public void runAtCluster(YtCluster cluster) {
                    called.add(cluster.getName());
                    if (cluster.getName().equals("test1")) {
                        throw new RuntimeException("test1");
                    }
                }

                @Override
                public void beforeRetry() {
                    contextResetMarker.compareAndSet(true, false);
                }
            }
        );

        assertThat(called).isEqualTo(Arrays.asList("test1", "test2"));
        assertThat(contextResetMarker.get()).as("Marker must be reset in retry handler").isFalse();
    }

    /**
     * При успешном выполнении запросов, не вызыватся обработчик retry на кластере "
     */
    @Test
    public void test_runInYt_when_ok_then_no_retryHandlerCall() {
        YtTemplate ytTemplate = new YtTemplate(new YtCluster[]{
            new YtCluster("test1", null),
            new YtCluster("test2", null)
        });

        List<String> called = new ArrayList<>();
        AtomicBoolean contextResetMarker = new AtomicBoolean(true);
        ytTemplate.runInYt(
            new YtTemplate.ClusterSwitchAwareConsumer() {
                @Override
                public void runAtCluster(YtCluster cluster) {
                    called.add(cluster.getName());
                }

                @Override
                public void beforeRetry() {
                    contextResetMarker.compareAndSet(true, false);
                }
            }
        );

        assertThat(called).isEqualTo(Arrays.asList("test1"));
        assertThat(contextResetMarker.get()).as("Marker must NOT be reset in retry handler").isTrue();
    }

    /**
     * Если кластер в пуле только 1, то при возникновении ошибки retry обработчки не вызывается
     */
    @Test
    public void test_runInYt_when_ok_errorAndOneCluster_no_retryHandlerCall() {
        YtTemplate ytTemplate = new YtTemplate(new YtCluster[]{
            new YtCluster("test1", null),
        });

        List<String> called = new ArrayList<>();
        AtomicBoolean contextResetMarker = new AtomicBoolean(true);
        Assertions.assertThatThrownBy(
                () ->
                    ytTemplate.runInYt(
                        new YtTemplate.ClusterSwitchAwareConsumer() {
                            @Override
                            public void runAtCluster(YtCluster cluster) {
                                called.add(cluster.getName());
                                if (cluster.getName().equals("test1")) {
                                    throw new RuntimeException("test1");
                                }
                            }

                            @Override
                            public void beforeRetry() {
                                contextResetMarker.compareAndSet(true, false);
                            }
                        }
                    )
            ).isInstanceOf(RuntimeException.class)
            .hasMessage("test1");

        assertThat(called).isEqualTo(Arrays.asList("test1"));
        assertThat(contextResetMarker.get()).as("Marker must NOT be reset in retry handler").isTrue();
    }

}
