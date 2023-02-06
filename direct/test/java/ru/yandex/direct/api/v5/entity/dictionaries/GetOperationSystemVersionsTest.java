package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import com.yandex.direct.api.v5.dictionaries.OperationSystemVersionsItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.OPERATION_SYSTEM_VERSIONS;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Получение версий операционных систем")
public class GetOperationSystemVersionsTest {

    @Autowired
    private Steps steps;

    private List<OperationSystemVersionsItem> operationSystemVersions;

    @Before
    public void before() {
        openMocks(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withClientAuth(clientInfo)
                        .build();

        GetResponse response = dictionariesService.get(
                new GetRequest().withDictionaryNames(singletonList(OPERATION_SYSTEM_VERSIONS)));
        operationSystemVersions = response.getOperationSystemVersions();

        assumeThat("Получен словарь версий операционных систем", operationSystemVersions, hasSize(greaterThan(0)));
    }

    @Test
    @Description("Все названия заполнены")
    public void get_namesAreFilledTest() {
        Set<String> values = operationSystemVersions.stream()
                .map(OperationSystemVersionsItem::getOsName)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Все значения версий заполнены")
    public void get_versionsAreFilledTest() {
        Set<String> values = operationSystemVersions.stream()
                .map(OperationSystemVersionsItem::getOsVersion)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Версия присутствует")
    public void get_versionFoundTest() {
        OperationSystemVersionsItem constant = new OperationSystemVersionsItem()
                .withOsName("Android")
                .withOsVersion("3.1");

        assertThat(operationSystemVersions, hasItem(beanDiffer(constant)));
    }

}
