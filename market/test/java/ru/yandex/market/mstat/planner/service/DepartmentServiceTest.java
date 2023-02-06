package ru.yandex.market.mstat.planner.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ru.yandex.market.mstat.planner.config.StaffConfig;
import ru.yandex.market.mstat.planner.model.Department;

import static ru.yandex.market.mstat.planner.service.DepartmentService.DEP_TREE_SQL;

public class DepartmentServiceTest {
    @Test
    public void testDepTree() {
        Department root = new Department();
        root.id = StaffConfig.MARKET_DEV_DEP_ID;
        LinkedList<Department> breadcrumbs = new LinkedList<>();
        breadcrumbs.add(root);

        List<Department> rows = new ArrayList<>();
        rows.add(d(117, 96, "117"));
        rows.add(d(30073, 117, "117,30073"));
        rows.add(d(58540, 30073, "117,30073,58540"));
        rows.add(d(55657, 117, "117,55657"));
        rows.add(d(107677, 55657, "117,55657,107677"));
        rows.add(d(107678, 55657, "117,55657,107678"));
        rows.add(d(57241, 117, "117,57241"));
        rows.add(d(58612, 117, "117,58612"));
        rows.add(d(58613, 58612, "117,58612,58613"));
        rows.add(d(58614, 58612, "117,58612,58614"));
        rows.add(d(77364, 58612, "117,58612,77364"));
        rows.add(d(66883, 117, "117,66883"));
        rows.add(d(66884, 117, "117,66884"));
        rows.add(d(145, 96, "145"));
        rows.add(d(66834, 145, "145,66834"));
        rows.add(d(74380, 145, "145,74380"));
        rows.add(d(93047, 145, "145,93047"));
        rows.add(d(55658, 93047, "145,93047,55658"));
        rows.add(d(67328, 93047, "145,93047,67328"));


        for(Department department: rows) {
            // System.out.println(department.name);
            while (department.parent_id != breadcrumbs.getLast().id) {
                breadcrumbs.removeLast();
            }
            breadcrumbs.getLast().children.add(department);
            breadcrumbs.add(department);
        }

        // pprintDep(root, 0);
    }

    private static void pprintDep(Department d, int level) {
        System.out.println(StringUtils.repeat('\t', level) + d.name);
        for(Department child: d.children) {
            pprintDep(child, level + 1);
        }
    }

    private Department d(long id, long pid, String name) {
        return new Department(id, pid, "head", name, false);
    }

    @Test
    public void t() {
        String sql = DEP_TREE_SQL
            .replace("${recursion_init}", "cast(department_id as text) as dep_path")
            .replace("${recursion_recursion}", "concat(dep_path, ',', departments.department_id) as dep_path") +
            "\norder by dep_path asc";
        sql = sql.replace(":root", Long.toString(StaffConfig.MARKET_DEV_DEP_ID));
        System.out.println(sql);
    }
}
