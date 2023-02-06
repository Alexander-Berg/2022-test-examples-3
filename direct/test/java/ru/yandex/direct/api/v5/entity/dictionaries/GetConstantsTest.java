package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.ConstantsItem;
import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.CONSTANTS;
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
@Description("Получение констант")
public class GetConstantsTest {

    @Autowired
    private Steps steps;

    private List<ConstantsItem> constants;

    @Before
    public void before() {
        openMocks(this);

        DictionariesServiceBuilder builder = new DictionariesServiceBuilder(steps.applicationContext());

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService = builder
                .withClientAuth(clientInfo)
                .build();

        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(CONSTANTS)));
        constants = response.getConstants();

        assumeThat("Получен словарь констант", constants, hasSize(greaterThan(0)));
    }


    @Test
    @Description("Все названия свойств заполнены")
    public void get_propertiesAreFilledTest() {
        Set<String> values = constants.stream()
                .map(ConstantsItem::getName)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Все значения свойств заполнены")
    public void get_valuesAreFilledTest() {
        Set<String> values = constants.stream()
                .map(ConstantsItem::getValue)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Константа присутствует")
    public void get_constantFoundTest() {
        ConstantsItem constant = new ConstantsItem()
                .withName("MaximumAdTextLength")
                .withValue("81");

        assertThat(constants, hasItem(beanDiffer(constant)));
    }

}
