package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.dictionaries.AudienceInterestsItem;
import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.AUDIENCE_INTERESTS;
import static com.yandex.direct.api.v5.dictionaries.InterestTypeEnum.ANY;
import static com.yandex.direct.api.v5.dictionaries.InterestTypeEnum.LONG_TERM;
import static com.yandex.direct.api.v5.dictionaries.InterestTypeEnum.SHORT_TERM;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.ANY_TERM_PREFIX;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.LONG_TERM_PREFIX;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.MAX_ID_FOR_PREFIX;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.SHORT_TERM_PREFIX;

@Api5Test
@RunWith(Parameterized.class)
@Description("Получение интересов аудиторий")
public class GetAudienceInterestsTest {
    private static final long PARENT_ID = 2499001255L; // Животные
    private static final long ID = 2499001254L; // Корма для животных

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Goal goal;

    @Parameterized.Parameter(2)
    public AudienceInterestsItem shortInterest;

    @Parameterized.Parameter(3)
    public AudienceInterestsItem longInterest;

    @Parameterized.Parameter(4)
    public AudienceInterestsItem anyInterest;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Животные (parentId = 0)",
                        new Goal()
                                .withId(PARENT_ID)
                                .withParentId(0L)
                                .withTankerNameKey("crypta_interest_pets_name")
                                .withTankerDescriptionKey("crypta_interest_pets_description"),

                        new AudienceInterestsItem()
                                .withInterestKey(PARENT_ID)
                                .withId(SHORT_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withParentId(0)
                                .withInterestType(SHORT_TERM)
                                .withName("Animals")
                                .withDescription("People interested in pets."),
                        new AudienceInterestsItem()
                                .withInterestKey(PARENT_ID)
                                .withId(LONG_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withParentId(0)
                                .withInterestType(LONG_TERM)
                                .withName("Animals")
                                .withDescription("People interested in pets."),
                        new AudienceInterestsItem()
                                .withInterestKey(PARENT_ID)
                                .withId(ANY_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withParentId(0)
                                .withInterestType(ANY)
                                .withName("Animals")
                                .withDescription("People interested in pets.")

                },
                {"Корма для животных (parentId != 0)",
                        new Goal()
                                .withId(ID)
                                .withParentId(PARENT_ID)
                                .withTankerNameKey("crypta_interest_pet_food_name")
                                .withTankerDescriptionKey("crypta_interest_pet_food_description"),

                        new AudienceInterestsItem()
                                .withInterestKey(ID)
                                .withId(SHORT_TERM_PREFIX * MAX_ID_FOR_PREFIX + ID)
                                .withParentId(SHORT_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withInterestType(SHORT_TERM)
                                .withName("Pet food")
                                .withDescription("Interested in food for cats, dogs, birds, fish and gerbils."),
                        new AudienceInterestsItem()
                                .withInterestKey(ID)
                                .withId(LONG_TERM_PREFIX * MAX_ID_FOR_PREFIX + ID)
                                .withParentId(LONG_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withInterestType(LONG_TERM)
                                .withName("Pet food")
                                .withDescription("Interested in food for cats, dogs, birds, fish and gerbils."),
                        new AudienceInterestsItem()
                                .withInterestKey(ID)
                                .withId(ANY_TERM_PREFIX * MAX_ID_FOR_PREFIX + ID)
                                .withParentId(ANY_TERM_PREFIX * MAX_ID_FOR_PREFIX + PARENT_ID)
                                .withInterestType(ANY)
                                .withName("Pet food")
                                .withDescription("Interested in food for cats, dogs, birds, fish and gerbils.")
                },
        };
        return asList(data);
    }

    private DictionariesService dictionariesService;

    @Before
    public void before() {
        openMocks(this);

        DictionariesServiceBuilder builder = new DictionariesServiceBuilder(steps.applicationContext());

        CryptaSegmentRepository cryptaSegmentRepository = mock(CryptaSegmentRepository.class);

        when(cryptaSegmentRepository.getInterests())
                .thenReturn(Collections.singletonMap(goal.getId(), goal));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        dictionariesService = builder
                .withClientAuth(clientInfo)
                .withCryptaSegmentRepository(cryptaSegmentRepository)
                .build();
    }

    @Test
    @Description("Интересы всей длительности присутствуют")
    public void get_audienceTypeTest() {
        GetResponse response = dictionariesService.get(
                new GetRequest().withDictionaryNames(singletonList(AUDIENCE_INTERESTS)));
        List<AudienceInterestsItem> items = response.getAudienceInterests();

        assertThat(items, contains(Arrays.asList(beanDiffer(shortInterest), beanDiffer(longInterest),
                beanDiffer(anyInterest))));
    }
}
