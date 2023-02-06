package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.dictionaries.AudienceDemographicProfilesItem;
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

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.AUDIENCE_DEMOGRAPHIC_PROFILES;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Api5Test
@RunWith(Parameterized.class)
@Description("Получение социал-демо аудиторий")
public class GetAudienceDemographicProfilesTest {
    public static final long ID = 123L;

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
    public AudienceDemographicProfilesItem audienceDemographicProfilesItem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Мужчины",
                        new Goal()
                                .withId(ID)
                                .withParentId(2499000021L) // parentId определяет тип
                                .withTankerNameKey("crypta_gender_male_name")
                                .withTankerDescriptionKey("crypta_gender_male_description"),
                        new AudienceDemographicProfilesItem()
                                .withId(ID)
                                .withType("GENDER")
                                .withName("Male")
                                .withDescription("Male. Gender is determined by online behavior.")
                },
                {"Есть дети до 1 года",
                        new Goal()
                                .withId(ID)
                                .withParentId(2499000110L)
                                .withTankerNameKey("crypta_family_has_infants_name")
                                .withTankerDescriptionKey("crypta_family_has_infants_description"),
                        new AudienceDemographicProfilesItem()
                                .withId(ID)
                                .withType("CHILDREN")
                                .withName("With children less than 1 year of age")
                                .withDescription("People who, based on internet behavior, have infants.")
                },
                {"Дизайнеры",
                        new Goal()
                                .withId(ID)
                                .withParentId(2499000120L)
                                .withTankerNameKey("crypta_profession_designer_name")
                                .withTankerDescriptionKey("crypta_profession_designer_description"),
                        new AudienceDemographicProfilesItem()
                                .withId(ID)
                                .withType("OCCUPATION")
                                .withName("Designers")
                                .withDescription("People who work as professional designers. Occupation is determined" +
                                " by online behavior.")
                },
                {"Мотоциклисты",
                        new Goal()
                                .withId(ID)
                                .withParentId(2499000260L)
                                .withTankerNameKey("crypta_segment-2d55a577_name")
                                .withTankerDescriptionKey("crypta_segment-2d55a577_description"),
                        new AudienceDemographicProfilesItem()
                                .withId(ID)
                                .withType("TRANSPORTATION")
                                .withName("Motorcyclists")
                                .withDescription("People who often visit websites for motorcyclists.")
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

        when(cryptaSegmentRepository.getSocialDemo())
                .thenReturn(Collections.singletonMap(goal.getId(), goal));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        dictionariesService = builder
                .withClientAuth(clientInfo)
                .withCryptaSegmentRepository(cryptaSegmentRepository)
                .build();
    }

    @Test
    @Description("Элемент аудитории присутствует")
    public void get_audienceTypeTest() {
        GetResponse response = dictionariesService.get(
                new GetRequest().withDictionaryNames(singletonList(AUDIENCE_DEMOGRAPHIC_PROFILES)));
        List<AudienceDemographicProfilesItem> items = response.getAudienceDemographicProfiles();

        assertThat(items, contains(beanDiffer(audienceDemographicProfilesItem)));
    }
}
