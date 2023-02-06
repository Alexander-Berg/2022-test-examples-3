package ru.yandex.market.stat.dicts.services;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by kateleb on 12.05.17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DictionariesITestConfig.class)
public class YtFileRemover {

    public static final String TESTING_MSTAT_DICTIONARIES = "//home/market/testing/mstat/dictionaries";
    @Autowired
    private DictionaryYtService yt;


    @Test
    @Ignore
    public void tearDown() {
        List<YPath> paths = yt.list(YPath.simple(TESTING_MSTAT_DICTIONARIES)).stream().map(d -> d.getPath()).collect(Collectors.toList());
        List<YPath> pathsToDelete = paths.stream().filter(p -> (p.toString().contains("build_") || p.toString().contains("dev_")) && !p.toString().contains("build_714")).collect(Collectors.toList());
        System.out.println("Will remove following paths:\n" + pathsToDelete.stream().map(p -> p.toString()).collect(joining(",")));
        pathsToDelete.forEach(p -> yt.removePath(p));
    }

    @Test
    @Ignore
    public void unlock() {
        yt.getYt().transactions().abort(GUID.valueOf("6373-302b5f-3fe0001-684d12a4"));
    }

}
