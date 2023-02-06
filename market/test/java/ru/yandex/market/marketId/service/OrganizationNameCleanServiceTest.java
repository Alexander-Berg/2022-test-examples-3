package ru.yandex.market.marketId.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.marketId.service.implementation.OrganizationNameCleanServiceImpl;

@RunWith(Parameterized.class)
public class OrganizationNameCleanServiceTest {

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static List<String[]> data() {
        return Arrays.asList(
                new String[]{"ООО Джон", "Джон"},
                new String[]{"ООО.Джон", "Джон"},
                new String[]{"О.О.О.Джон", "Джон"},
                new String[]{"Общество с ограниченной ответственностью Джон", "Джон"},
                new String[]{"ОАО Джон", "Джон"},
                new String[]{"ОАО Джон", "Джон"},
                new String[]{"О.А.О. Джон", "Джон"},
                new String[]{"Открытое акционерное общество Джон", "Джон"},
                new String[]{"ЗАО Джон", "Джон"},
                new String[]{"ЗАО. Джон", "Джон"},
                new String[]{"З.А.О. Джон", "Джон"},
                new String[]{"Закрытое акционерное общество Джон", "Джон"},
                new String[]{"АО Джон", "Джон"},
                new String[]{"АО. Джон", "Джон"},
                new String[]{"А.О. Джон", "Джон"},
                new String[]{"Акционерное общество Джон", "Джон"},
                new String[]{"ИП Джон", "Джон"},
                new String[]{"ИП. Джон", "Джон"},
                new String[]{"И.П. Джон", "Джон"},
                new String[]{"ип Джон", "Джон"},
                new String[]{"индивидуальный предприниматель Джон", "Джон"},
                new String[]{"Индивидуальный предприниматель Джон", "Джон"},
                new String[]{"ЧП Джон", "Джон"},
                new String[]{"ЧП. Джон", "Джон"},
                new String[]{"Ч.П. Джон", "Джон"},
                new String[]{"Частный предприниматель Джон", "Джон"},
                new String[]{"СП Джон", "Джон"},
                new String[]{"СПДжон", "СПДжон"},
                new String[]{"ЗАО \"Джон\"", "Джон"},
                new String[]{"\"ЗАО Джон\"", "Джон"},
                new String[]{"ЗАО", "ЗАО"},
                new String[]{"  ЗАО  ", "ЗАО"},
                new String[]{"\"ЗАО\"", "ЗАО"});
    }

    @Parameterized.Parameter(0)
    public String rawName;

    @Parameterized.Parameter(1)
    public String expectedName;

    private OrganizationNameCleanService service = new OrganizationNameCleanServiceImpl(null, null);

    @Test
    public void testCleanName() {
        Assertions.assertEquals(expectedName, service.cleanOrganizationName(rawName));
    }
}
