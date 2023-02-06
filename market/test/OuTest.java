package ru.yandex.market.jmf.module.ou.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpConstants;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.RequiredAttributesValidationException;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@SpringJUnitConfig(classes = ModuleOuTestConfiguration.class)
public class OuTest {

    @Inject
    BcpService bcpService;
    @Inject
    EntityService entityService;
    @Inject
    DbService dbService;
    @Inject
    TxService txService;
    @Inject
    OuTestUtils ouTestUtils;
    @Inject
    EmployeeTestUtils employeeTestUtils;

    /**
     * Проверяем создание отдела, у которого указано только название
     */
    @Test
    @Transactional
    public void createOu_default() {
        String title = Randoms.string();

        Entity ou = ouTestUtils.createOu(title);

        Object result = ou.getAttribute(Ou.TITLE);
        Assertions.assertEquals(title, result);
    }

    /**
     * Проверяем изменение parent-а на сам отдел.
     * <p>
     * Должны получить ошибку т.к. нельзя делаь родителем самого себя.
     */
    @Test
    @Transactional
    public void parent_self() {
        Assertions.assertThrows(ValidationException.class, () -> {
            Entity ou = ouTestUtils.createOu();

            bcpService.edit(ou, ImmutableMap.of(Ou.PARENT, ou));
        });
    }

    /**
     * Проверяем изменение parent-а на циклического родителя.
     * <p>
     * Должны получить ошибку т.к. нельзя делаь родителем самого себя.
     */
    @Test
    @Transactional
    public void parent_cycle() {
        Assertions.assertThrows(ValidationException.class, () -> {
            Entity ou0 = ouTestUtils.createOu();
            Entity ou1 = ouTestUtils.createOu(ou0);

            bcpService.edit(ou0, ImmutableMap.of(Ou.PARENT, ou1));
        });
    }


    @Test
    @Transactional
    public void moveToOtherParent() {
        Entity ou0 = ouTestUtils.createOu();
        Entity ou1 = ouTestUtils.createOu(ou0);
        Entity ou2 = ouTestUtils.createOu();

        Entity result = bcpService.edit(ou1, ImmutableMap.of(Ou.PARENT, ou2));

        Object newValue = result.getAttribute(Ou.PARENT);
        Assertions.assertEquals(ou2, newValue);
    }


    /**
     * Проверяем невозможность создание отдела без названия (с пустым названием)
     */
    @Test
    @Transactional
    public void createOu_default_noTitle() {
        Assertions.assertThrows(RequiredAttributesValidationException.class, () -> ouTestUtils.createOu(""));
    }

    /**
     * Проверяем создание вложенного отдела
     */
    @Test
    @Transactional
    public void createSubOu_default() {
        Entity parent = ouTestUtils.createOu();

        Entity ou = ouTestUtils.createOu(parent);

        Object result = ou.getAttribute(Ou.PARENT);
        Assertions.assertEquals(parent, result);
    }

    /**
     * Проверяем получение списка вложенных отделов
     */
    @Test
    @Transactional
    public void getSubOu_default() {
        // настройка системы
        Entity parent = ouTestUtils.createOu();
        Entity ou = ouTestUtils.createOu(parent);
        // вызов системы

        Entity entity = dbService.get(parent.getGid());
        Collection<?> result = new ArrayList<>(entity.getAttribute(Ou.CHILDREN));

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(ou));
    }

    @Test
    @Transactional
    public void createEmployee() {
        Entity ou = ouTestUtils.createOu();
        employeeTestUtils.createEmployee(ou);
    }

    @Test
    @Transactional
    public void hierarchicalOuFilter() {
        // настройка системы
        InitHierarchy ctx = new InitHierarchy().invoke();

        // вызов системы
        Query q = Query.of(Employee.FQN)
                .withFilters(Filters.hierarchical(Employee.OU, ctx.getOu0().getGid()));
        List<Entity> result = dbService.list(q);

        // проверка утверждений
        Assertions.assertTrue(result.contains(ctx.getEmployee0()));
        Assertions.assertTrue(result.contains(ctx.getEmployee1()));
        Assertions.assertTrue(result.contains(ctx.getEmployee2()));
        Assertions.assertFalse(result.contains(ctx.getEmployee3()));
        Assertions.assertEquals(3, result.size());
    }

    @Test
    @Transactional
    public void hierarchicalEmployeeFilter() {
        // настройка системы
        InitHierarchy ctx = new InitHierarchy().invoke();

        bcpService.edit(ctx.getOu1(), Maps.of(Ou.HEAD, ctx.getEmployee3()));
        bcpService.edit(ctx.getOu3(), Maps.of(Ou.HEAD, ctx.getEmployee1()));

        // вызов системы
        Query q = Query.of(Ou.FQN)
                .withFilters(Filters.hierarchical(Ou.HEAD, ctx.getOu0().getGid()));
        List<Entity> result = dbService.list(q);

        // проверка утверждений
        Assertions.assertTrue(
                result.contains(ctx.getOu3()), "Должны получить ou3 отдел т.к. его руководитель находится внутри ou0");
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void hierarchicalOuFilterOnLinkedObject() {
        // настройка системы
        InitHierarchy ctx = txService.doInNewTx(() -> new InitHierarchy().invoke());

        txService.doInNewTx(() -> {
            bcpService.edit(ctx.getOu1(), Maps.of(Ou.HEAD, ctx.getEmployee3()));
            bcpService.edit(ctx.getOu3(), Maps.of(Ou.HEAD, ctx.getEmployee1()));
            return null;
        });

        // вызов системы
        Query q = Query.of(Ou.FQN)
                .withFilters(Filters.hierarchical("ou@head.employee@ou", ctx.getOu0().getGid()));
        List<Entity> result = txService.doInNewTx(() -> dbService.list(q));

        // проверка утверждений
        Assertions.assertTrue(result.contains(ctx.getOu3()));
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void hierarchicalEmployeeFilterOnLinkedObject() {
        // настройка системы
        InitHierarchy ctx = txService.doInNewTx(() -> new InitHierarchy().invoke());

        txService.doInNewTx(() -> {
            bcpService.edit(ctx.getOu1(), Maps.of(Ou.HEAD, ctx.getEmployee3()));
            bcpService.edit(ctx.getOu3(), Maps.of(Ou.HEAD, ctx.getEmployee1()));
            return null;
        });

        // вызов системы
        Query q = Query.of(Employee.FQN)
                .withFilters(Filters.hierarchical("employee@ou.ou@head", ctx.getOu0().getGid()));
        List<Entity> result = txService.doInNewTx(() -> dbService.list(q));

        // проверка утверждений
        Assertions.assertTrue(result.contains(ctx.getEmployee3()));
        Assertions.assertEquals(1, result.size());
    }

    /**
     * Проверяем, что павильно работает {@link Query#withAttributes(String...)}. Должны проинициализировать указанные
     * поля сразу в запросе, а не
     * подтягивать их лениво. См. обратный тест {@link #queryEmployee_withoutAttributes()}.
     *
     * @see #queryEmployee_withoutAttributes()
     */
    @Test
    @Transactional
    public void queryEmployee_withAttributes() {
        String ouTitle = Randoms.string();
        String employeeTitle = Randoms.string();
        Entity ou = ouTestUtils.createOu(ouTitle);
        Entity employee = employeeTestUtils.createEmployee(employeeTitle, ou);

        Query q = Query.of(Employee.FQN)
                .withFilters(Filters.eq(Employee.TITLE, employeeTitle))
                .withAttributes(Employee.OU);
        List<Entity> result = dbService.list(q);

        Assertions.assertEquals(1, result.size(), "Создали только одного сотрудника в отделе");
        Entity entity = result.get(0);
        Assertions.assertEquals(employee, entity, "Должны получить созданного сотрудника");
        Entity entityOu = entity.getAttribute(Employee.OU);
        Assertions.assertEquals(ou, entityOu, "Должны получить отдел, в котором создавали сотрудника");
        String entityOuTitle = entityOu.getAttribute(Ou.TITLE);
        Assertions.assertEquals(ouTitle, entityOuTitle, "Должны получить название отдела даже без транзакции т.к. в " +
                "запросе мы явно указали необходимость поднять отдел сотрудника сразу, а не делат его Lazy");
    }

    /**
     * Обратный тест к {@link #queryEmployee_withAttributes()}. Т.к. в запросе не указали, что необходимо сразу
     * поднять из базы аттрибут {@code ou},
     * то получаем ошибку {@link LazyInitializationException}.
     */
    @Test
    public void queryEmployee_withoutAttributes() {
        Assertions.assertThrows(LazyInitializationException.class, () -> {
            Entity ouRef = null;
            Entity employeeRef = null;

            // Сам тест внутри try
            try {
                String ouTitle = Randoms.string();
                String employeeTitle = Randoms.string();
                var ou = txService.doInNewTx(() -> ouTestUtils.createOu(ouTitle));
                ouRef = ou;
                var employee = txService.doInNewTx(() -> employeeTestUtils.createEmployee(employeeTitle, ou));
                employeeRef = employee;

                Query q = Query.of(Employee.FQN)
                        .withFilters(Filters.eq(Employee.TITLE, employeeTitle));
                List<Entity> result = txService.doInNewTx(() -> dbService.list(q));

                Assertions.assertEquals(1, result.size(), "Создали только одного сотрудника в отделе");
                Entity entity = result.get(0);
                Assertions.assertEquals(employee, entity, "Должны получить созданного сотрудника");
                Entity entityOu = entityService.getAttribute(entity, Employee.OU);
                Assertions.assertEquals(ou, entityOu, "Должны получить отдел, в котором создавали сотрудника");
                entityService.getAttribute(entityOu, Ou.TITLE);
            } finally {
                // А тут чистим за собой
                var ou = ouRef;
                var employee = employeeRef;
                txService.runInTx(() -> {
                    bcpService.delete(employee, Map.of(
                            BcpConstants.Attributes.ATTRIBUTE_SKIP_DEPENDENCY, true
                    ));
                    bcpService.delete(ou);
                });
            }
        });
    }

    @Test
    @Transactional
    public void employeeOus() {
        Entity other = ouTestUtils.createOu();
        Entity root = ouTestUtils.createOu();
        Entity ou = ouTestUtils.createOu(root);
        Employee employee = employeeTestUtils.createEmployee(ou);

        Collection<Ou> result = employee.getOus();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(ou));
        Assertions.assertTrue(result.contains(root));
    }

    private class InitHierarchy {
        private Entity ou0;
        private Entity ou1;
        private Entity ou2;
        private Entity ou3;
        private Entity employee0;
        private Entity employee1;
        private Entity employee2;
        private Entity employee3;

        public Entity getOu0() {
            return ou0;
        }

        public Entity getOu1() {
            return ou1;
        }

        public Entity getOu2() {
            return ou2;
        }

        public Entity getOu3() {
            return ou3;
        }

        public Entity getEmployee0() {
            return employee0;
        }

        public Entity getEmployee1() {
            return employee1;
        }

        public Entity getEmployee2() {
            return employee2;
        }

        public Entity getEmployee3() {
            return employee3;
        }

        public InitHierarchy invoke() {
            ou0 = ouTestUtils.createOu();
            ou1 = ouTestUtils.createOu(ou0);
            ou2 = ouTestUtils.createOu(ou0);
            ou3 = ouTestUtils.createOu();

            employee0 = employeeTestUtils.createEmployee(ou0);
            employee1 = employeeTestUtils.createEmployee(ou1);
            employee2 = employeeTestUtils.createEmployee(ou2);
            employee3 = employeeTestUtils.createEmployee(ou3);
            return this;
        }
    }
}
