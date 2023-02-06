package ru.yandex.autotests.mediaplan.adgroups;

import org.junit.runners.Parameterized;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.ParamsApi5AddAdgroups;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.*;

public class AdGroupsPositiveBaseTest {
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddAdgroups addAdGroupsInputData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна группа", oneAdgroup()},
                {"Две одинаковые группы", twoSameAdgroups()},
                {"Две разные группы", twoRandomAdgroups()},
                {"Тысяча разных групп", thousandRandomAdgroups()},
                {"Одна группа с необязательными полями", oneAdgroupWithOptionalParams()},
                {"Две группы с необязательными полями", twoAdgroupWithOptionalParams()},
                {"Одна группа с необязательными полями вторая без", twoAdgroupHalfWithOptionalParams()},
        });
    }

}
