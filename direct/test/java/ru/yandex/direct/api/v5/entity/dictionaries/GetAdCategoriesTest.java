package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.AdCategoriesItem;
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

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.AD_CATEGORIES;
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
@Description("Получение категорий")
public class GetAdCategoriesTest {

    @Autowired
    private Steps steps;

    private List<AdCategoriesItem> adCategories;

    @Before
    public void before() {
        openMocks(this);

        DictionariesServiceBuilder builder = new DictionariesServiceBuilder(steps.applicationContext());

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService = builder
                .withClientAuth(clientInfo)
                .build();

        GetResponse response =
                dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(AD_CATEGORIES)));
        adCategories = response.getAdCategories();

        assumeThat("Получен словарь категорий", adCategories, hasSize(greaterThan(0)));
    }

    @Test
    @Description("Все названия категорий заполнены")
    public void get_propertiesAreFilledTest() {
        Set<String> values = adCategories.stream()
                .map(AdCategoriesItem::getAdCategory)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Все описания заполнены")
    public void get_valuesAreFilledTest() {
        Set<String> values = adCategories.stream()
                .map(AdCategoriesItem::getDescription)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Все сообщения заполнены")
    public void get_messagesAreFilledTest() {
        Set<String> values = adCategories.stream()
                .map(AdCategoriesItem::getMessage)
                .collect(Collectors.toSet());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Категория присутствует")
    public void get_categoryFoundTest() {
        AdCategoriesItem category = new AdCategoriesItem()
                .withAdCategory("DIETARY_SUPPLEMENTS")
                .withDescription("Dietary supplement")
                .withMessage("This is not a drug");

        assertThat(adCategories, hasItem(beanDiffer(category)));
    }

}
