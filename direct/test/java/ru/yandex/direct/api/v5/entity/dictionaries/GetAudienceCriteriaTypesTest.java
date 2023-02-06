package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.dictionaries.AudienceCriteriaTypesItem;
import com.yandex.direct.api.v5.dictionaries.CanSelectEnum;
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

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.AUDIENCE_CRITERIA_TYPES;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.FAMILY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;

@Api5Test
@RunWith(Parameterized.class)
@Description("Получение типов социал-демо аудиторий")
public class GetAudienceCriteriaTypesTest {

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
    public AudienceCriteriaTypesItem audienceCriteriaTypesItem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Пол",
                        new Goal()
                                .withId(2499000021L)
                                .withType(SOCIAL_DEMO)
                                .withTankerNameKey("crypta_gender_name")
                                .withTankerDescriptionKey("crypta_gender_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("GENDER")
                                .withBlockElement("SOCIAL")
                                .withName("Gender")
                                .withDescription("Gender")
                                .withCanSelect(CanSelectEnum.EXCEPT_ALL)
                },
                {"Возраст",
                        new Goal()
                                .withId(2499000022L)
                                .withType(SOCIAL_DEMO)
                                .withTankerNameKey("crypta_age_segment_name")
                                .withTankerDescriptionKey("crypta_age_segment_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("AGE")
                                .withBlockElement("SOCIAL")
                                .withName("Age")
                                .withDescription("Age")
                                .withCanSelect(CanSelectEnum.EXCEPT_ALL)
                },
                {"Доход",
                        new Goal()
                                .withId(2499000023L)
                                .withType(SOCIAL_DEMO)
                                .withTankerNameKey("crypta_income_segment_name")
                                .withTankerDescriptionKey("crypta_income_segment_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("INCOME")
                                .withBlockElement("SOCIAL")
                                .withName("Income")
                                .withDescription("Income")
                                .withCanSelect(CanSelectEnum.EXCEPT_ALL)
                },
                {"Семейное положение",
                        new Goal()
                                .withId(2499000100L)
                                .withType(FAMILY)
                                .withTankerNameKey("crypta_family_status_name")
                                .withTankerDescriptionKey("crypta_family_status_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("MARITAL_STATUS")
                                .withBlockElement("EXTENDED_SOCIAL")
                                .withName("Marital status")
                                .withDescription("Marital status")
                                .withCanSelect(CanSelectEnum.EXCEPT_ALL)
                },
                {"Наличие детей",
                        new Goal()
                                .withId(2499000110L)
                                .withType(FAMILY)
                                .withTankerNameKey("crypta_has_children_name")
                                .withTankerDescriptionKey("crypta_has_children_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("CHILDREN")
                                .withBlockElement("EXTENDED_SOCIAL")
                                .withName("Children")
                                .withDescription("Children")
                                .withCanSelect(CanSelectEnum.ALL)
                },
                {"Профессии",
                        new Goal()
                                .withId(2499000120L)
                                .withType(FAMILY)
                                .withTankerNameKey("crypta_profession_name")
                                .withTankerDescriptionKey("crypta_profession_description"),
                        new AudienceCriteriaTypesItem()
                                .withType("OCCUPATION")
                                .withBlockElement("EXTENDED_SOCIAL")
                                .withName("Profession")
                                .withDescription("Professions")
                                .withCanSelect(CanSelectEnum.ALL)
                },
        };
        return Arrays.asList(data);
    }

    private DictionariesService dictionariesService;

    @Before
    public void before() {
        openMocks(this);

        DictionariesServiceBuilder builder = new DictionariesServiceBuilder(steps.applicationContext());

        CryptaSegmentRepository cryptaSegmentRepository = mock(CryptaSegmentRepository.class);

        when(cryptaSegmentRepository.getSocialDemoTypes())
                .thenReturn(Collections.singletonMap(goal.getId(), goal));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        dictionariesService = builder
                .withClientAuth(clientInfo)
                .withCryptaSegmentRepository(cryptaSegmentRepository)
                .build();
    }

    @Test
    @Description("Тип аудитории присутствует")
    public void get_audienceTypeTest() {
        GetResponse response =
                dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(AUDIENCE_CRITERIA_TYPES)));
        List<AudienceCriteriaTypesItem> items = response.getAudienceCriteriaTypes();

        assertThat(items, contains(beanDiffer(audienceCriteriaTypesItem)));
    }
}
