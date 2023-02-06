package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import com.yandex.direct.api.v5.dictionaries.InterestsItem;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.INTERESTS;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Получение интересов")
public class GetInterestsTest {

    private static final long TARGETING_CATEGORY_ID = 11L;
    private static final long PARENT_ID = 22L;
    private static final String CATEGORY_NAME = "Health & fitness";
    private static final String ORIGINAL_CATEGORY_NAME = "HEALTH_AND_FITNESS";

    @Autowired
    private Steps steps;

    private DictionariesService dictionariesService;

    @Before
    public void before() {
        openMocks(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        TargetingCategoriesRepository targetingCategoriesRepository = mock(TargetingCategoriesRepository.class);

        TargetingCategory targetingCategory = new TargetingCategory(TARGETING_CATEGORY_ID, PARENT_ID, null,
                ORIGINAL_CATEGORY_NAME, null, true);
        when(targetingCategoriesRepository.getAll())
                .thenReturn(Collections.singletonList(targetingCategory));

        dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withTargetingCategoriesRepository(targetingCategoriesRepository)
                        .withClientAuth(clientInfo)
                        .build();
    }

    @Test
    @Description("Интерес присутствует")
    public void get_InterestFoundTest() {
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(INTERESTS)));
        List<InterestsItem> interests = response.getInterests();

        InterestsItem interest = new InterestsItem()
                .withInterestId(TARGETING_CATEGORY_ID)
                .withParentId(PARENT_ID)
                .withName(CATEGORY_NAME)
                .withIsTargetable(YesNoEnum.YES);

        assertThat(interests, contains(beanDiffer(interest)));
    }
}
