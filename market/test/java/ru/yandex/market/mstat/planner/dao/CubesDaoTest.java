package ru.yandex.market.mstat.planner.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.mstat.planner.model.ProjectColor;

public class CubesDaoTest {

    @Test
    public void tt() {
        System.out.println(CubesDao.COLOR_GROUPS_CASE);
    }


    @Test
    public void ttt() {
        Map<String, Set<ProjectColor>> m = new HashMap<>();
        ProjectColor.COLOR_GROUPS_MAPPING.forEach((k, v) -> {
            m.putIfAbsent(v, new HashSet<>());
            m.get(v).add(k);
        });
        System.out.println(m);
    }

    @Test
    public void mainCubeSql() {
        System.out.println("" +
                "\\set from '2019-01-01'\n" +
                "\\set to   '2019-01-31'\n" +
                "\\set dep_root 96\n\n" +
                CubesDao.CUBE_SQL
                        .replace(":from", ":'from'")
                        .replace(":to", ":'to'"));
    }

    @Test
    public void secondCubePage_cubeForSingleDepTo() {
        System.out.println("" +
                "\\set from '2019-01-01'\n" +
                "\\set to   '2019-01-31'\n" +
                "\\set department_id_to 117\n" +
                "\\set dep_root 96\n\n" +
                CubesDao.CUBE_FOR_DEP_SQL
                        .replace(":from", ":'from'")
                        .replace(":to", ":'to'"));
    }

    @Test
    // mainCubeSql groups by toDeps
    public void cubeDataForAllFromDeps() {
        System.out.println("" +
                "\\set from '2019-01-01'\n" +
                "\\set to   '2019-01-31'\n" +
                "\\set dep_root 96\n\n" +
                CubesDao.CUBE_WITH_FROM_DEP_SQL
                        .replace(":from", ":'from'")
                        .replace(":to", ":'to'"));
    }
}
