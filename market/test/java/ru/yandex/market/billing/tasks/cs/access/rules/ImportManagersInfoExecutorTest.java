package ru.yandex.market.billing.tasks.cs.access.rules;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryOperations;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tasks.cs.access.rules.misc.AccessRulesManagerParser;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.staff.Chief;
import ru.yandex.market.core.staff.Employee;
import ru.yandex.market.core.staff.EmployeeGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link ImportManagersInfoExecutor}.
 *
 * @author Vladislav Bauer
 */
class ImportManagersInfoExecutorTest extends FunctionalTest {
    @Autowired
    private ImportManagersInfoExecutor executor;

    @Test
    @DbUnitDataSet(before = "testDeleteRetiredManagers.before.csv", after = "testDeleteRetiredManagers.after.csv")
    void testParserAndJob() {
        var json = StringTestUtil.getString(getClass(), "getUsers.json");
        var employees = AccessRulesManagerParser.parseEmployees(json);
        assertThat(employees).usingRecursiveFieldByFieldElementComparator().containsExactly(
                new Employee.Builder()
                        .setUid(123)
                        .setGroup(EmployeeGroup.SUPPORT_MANAGER)
                        .setName("Вишес Сид")
                        .setEmail("sid@yandex-team.ru")
                        .setLogin("sido-o")
                        .setLdLogin("sid-o")
                        .setPhone(null)
                        .setCrmEmail("crm@yandex.ru")
                        .setChief(new Chief("pupkin", "pupkin@yandex-team.ru"))
                        .build(),
                new Employee.Builder()
                        .setUid(321)
                        .setGroup(EmployeeGroup.SUPPORT_MANAGER)
                        .setName("Спанджен Нэнси")
                        .setEmail("nancy@yandex-team.ru")
                        .setLogin("nancy-s")
                        .setLdLogin("nancy-s")
                        .setPhone("123321")
                        .setTelegramAccount("nancy_telegram")
                        .setCrmEmail(null)
                        .build()
        );

        var retryTemplateMock = mock(RetryOperations.class);
        doReturn(employees).when(retryTemplateMock).execute(any(RetryCallback.class));
        executor.setRetryTemplate(retryTemplateMock);
        executor.doJob(null);
        executor.doJob(null); // run again to check for idempotency
    }
}
