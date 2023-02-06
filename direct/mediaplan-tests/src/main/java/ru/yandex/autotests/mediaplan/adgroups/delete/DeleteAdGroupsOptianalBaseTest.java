package ru.yandex.autotests.mediaplan.adgroups.delete;

import org.junit.runners.Parameterized;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.ParamsApi5AddAdgroups;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroupWithOptionalParams;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.twoAdgroupHalfWithOptionalParams;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.twoAdgroupWithOptionalParams;

public class DeleteAdGroupsOptianalBaseTest {
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddAdgroups addAdGroupsInputData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна группа с необязательными полями", oneAdgroupWithOptionalParams()},
                {"Две группы с необязательными полями", twoAdgroupWithOptionalParams()},
                {"Одна группа с необязательными полями вторая без", twoAdgroupHalfWithOptionalParams()},
        });
    }

}
