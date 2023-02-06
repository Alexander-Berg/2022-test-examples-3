package ru.yandex.market.crm.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConcurrentsTest {

    @Test
    public void checkTransformed() throws Exception {
        // настройка системы
        ExecutorService executorService = Executors.newFixedThreadPool(16);

        Function<String, String> transformer0 = (v) -> v + "_transformed";
        Function<String, Collection<String>> transformer = (v) -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 25));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return Collections.singleton(transformer0.apply(v));
        };

        // вызов системы
        List<String> origin = Lists.newArrayList();
        for (int i = 0; i < 1024; ++i) {
            origin.add("s_" + i);
        }

        Iterator<String> transformed = Concurrents.transform(executorService, 16, origin.iterator(), transformer);
        Thread.sleep(1000); // делаем задержку чтобы проверить как работает переполнение буфера преобразованных значений
        List<String> result = Lists.newArrayList(transformed);

        // Проверка утверждений
        Assertions.assertEquals(origin.size(), result.size(), "Не должено изменииться ко-во элементов");
        for (String originValue : origin) {
            String expected = transformer0.apply(originValue);
            Assertions.assertTrue(result.contains(expected), "Должно содержаться преобразованное значение " + expected);
        }

        // очистка системы
        executorService.shutdown();
    }
}
