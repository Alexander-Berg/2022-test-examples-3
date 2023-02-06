package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;

import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import com.yandex.direct.api.v5.dictionaries.SupplySidePlatformsItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.SUPPLY_SIDE_PLATFORMS;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Получение внешних сетей")
public class GetSupplySidePlatformsTest {

    private static final String ADS_NATIVE = "AdsNative";

    @Autowired
    private Steps steps;

    private List<SupplySidePlatformsItem> supplySidePlatforms;

    @Before
    public void before() {
        openMocks(this);

        steps.sspPlatformsSteps().addSspPlatforms(singletonList(ADS_NATIVE));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withClientAuth(clientInfo)
                        .build();

        GetResponse response = dictionariesService.get(
                new GetRequest().withDictionaryNames(singletonList(SUPPLY_SIDE_PLATFORMS)));
        supplySidePlatforms = response.getSupplySidePlatforms();
    }

    @Test
    @Description("Внешняя сеть присутствует")
    public void get_platformFoundTest() {
        SupplySidePlatformsItem item = new SupplySidePlatformsItem()
                .withTitle(ADS_NATIVE);

        assertThat(supplySidePlatforms, hasItem(beanDiffer(item)));
    }
}
