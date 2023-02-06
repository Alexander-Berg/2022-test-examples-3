package ru.yandex.market.mbi.yt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class YtTemplateTest {

    @DisplayName("Проверяем корректный возврат результата из второго кластера, когда первый упал")
    @Test
    void test_getFromYt() {
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

        Assert.assertEquals(1, result.intValue());
        Assert.assertEquals(Arrays.asList("test1", "test2"), called);
    }

    @DisplayName("Проверяем корректное выполенение во втором кластере, когда первый упал")
    @Test
    void test_runInYt() {
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

        Assert.assertEquals(Arrays.asList("test1", "test2"), called);
        Assert.assertFalse("Marker must be reset in retry handler", contextResetMarker.get());
    }

    @DisplayName("При успешном выполнении запросов, не вызыватся обработчик retry на кластере ")
    @Test
    void test_runInYt_when_ok_then_no_retryHandlerCall() {
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

        Assert.assertEquals(Arrays.asList("test1"), called);
        Assert.assertTrue("Marker must NOT be reset in retry handler", contextResetMarker.get());
    }

    @DisplayName("Если кластер в пуле только 1, то при возникновении ошибки retry обработчки не вызывается")
    @Test
    void test_runInYt_when_ok_errorAndOneCluster_no_retryHandlerCall() {
        YtTemplate ytTemplate = new YtTemplate(new YtCluster[]{
                new YtCluster("test1", null),
        });

        List<String> called = new ArrayList<>();
        AtomicBoolean contextResetMarker = new AtomicBoolean(true);
        RuntimeException ex = Assertions.assertThrows(
                RuntimeException.class,
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
        );

        Assert.assertEquals(Arrays.asList("test1"), called);
        Assert.assertEquals(ex.getMessage(), "test1");
        Assert.assertTrue("Marker must NOT be reset in retry handler", contextResetMarker.get());
    }

}