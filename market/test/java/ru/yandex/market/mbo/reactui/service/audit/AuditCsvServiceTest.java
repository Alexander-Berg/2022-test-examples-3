package ru.yandex.market.mbo.reactui.service.audit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ru.yandex.market.mbo.statistic.AuditTestHelper.createAuditAction;

public class AuditCsvServiceTest {

    private static final Long PARAM_1_ID = 1L;
    private static final Long PARAM_2_ID = 2L;
    private static final String PARAM_1_NAME = "параметр1";
    private static final String PARAM_2_NAME = "параметр2";
    private static final MboUser MBO_USER_1 =
        new MboUser("login1", 1L, "fullname1", "", "staff");
    private static final MboUser MBO_USER_2 =
        new MboUser("login2", 2L, "fullname2", "", "staff");

    private AuditCsvService auditCsvService;
    private IParameterLoaderService parameterLoaderService;

    @Before
    public void init() {
        UserManager userManager = Mockito.mock(UserManager.class);
        parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        Mockito.when(parameterLoaderService.loadRusParamNames()).thenReturn(Map.of(
            PARAM_1_ID, PARAM_1_NAME,
            PARAM_2_ID, PARAM_2_NAME
        ));
        Mockito.when(userManager.getAllMboUser()).thenReturn(Arrays.asList(MBO_USER_1, MBO_USER_2));
        auditCsvService = new AuditCsvService(userManager, parameterLoaderService);
    }

    @Test
    public void test() throws IOException {
        String result = auditCsvService.getCsv(Arrays.asList(
            createAuditAction(1L, AuditAction.EntityType.PARAMETER, AuditAction.ActionType.UPDATE, PARAM_1_ID,
                "parameterXslName", "oldValue", "newValue",
                1L, AuditAction.BillingMode.BILLING_MODE_NONE, MBO_USER_1.getUid()),
            createAuditAction(2L, AuditAction.EntityType.PARAMETER, AuditAction.ActionType.UPDATE, PARAM_2_ID,
                "parameterXslName", "oldValue", "newValue",
                1L, AuditAction.BillingMode.BILLING_MODE_NONE, MBO_USER_2.getUid())
        ));

        List<String[]> actual = List.of(
            new String[]{"Дата", "ID объекта", "Источник", "ID источника", "Имя объекта", "Тип объекта",
                "MBO пользователь", "staff", "ID категории", "Параметр", "Действие", "Биллинг",
                "Свойство объекта", "Прежнее значение", "Новое значение"},
            new String[]{"1970-01-01T03:00:00+03:00", "1", "Задача в ЯНГ", "1234", "", "Параметр", "fullname1",
                "", "1", "параметр1", "Изменение", "Нет", "parameterXslName", "oldValue", "newValue"},
            new String[]{"1970-01-01T03:00:01+03:00", "2", "Задача в ЯНГ", "1234", "", "Параметр", "fullname2",
                "", "1", "параметр2", "Изменение", "Нет", "parameterXslName", "oldValue", "newValue"}
        );
        actual.forEach(this::addQuotes);
        assertActions(actual, result);
    }

    private void assertActions(List<String[]> expected, String csv) throws IOException {
        BufferedReader csvReader = new BufferedReader(new StringReader(csv));
        String row = "";
        Iterator<String[]> itExpected = expected.iterator();
        while ((row = csvReader.readLine()) != null) {
            String[] actual = row.split(AuditCsvService.SEPARATOR + "");
            assertArrays(itExpected.next(), actual);
        }
        csvReader.close();
    }

    private void assertArrays(String[] expected, String[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 1; i < actual.length; i++) {
            Assert.assertEquals(expected[i],  actual[i]);
        }
    }

    private void addQuotes(String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].length() > 0) {
                values[i] = AuditCsvService.QUOTE_AND_ESCAPE + values[i] + AuditCsvService.QUOTE_AND_ESCAPE;
            }
        }
    }
}
