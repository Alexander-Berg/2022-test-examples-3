package ru.yandex.market.checker.dao;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.EmptyTest;
import ru.yandex.market.checker.core.CheckerTask;
import ru.yandex.market.checker.zora.util.Platform;

/**
 * @author imelnikov
 * @since 04.03.2021
 */
public class CheckerDaoTest extends EmptyTest {

    @Autowired
    CheckerDao checkerDao;

    @Test
    void saveBulk() {
        List tasks = IntStream.iterate(1, IntUnaryOperator.identity())
                .mapToObj(i -> new CheckerTask<>("url", 0, 1, Platform.DESKTOP))
                .limit(100)
                .collect(Collectors.toList());
        checkerDao.addNewTasks(tasks);
    }
}
