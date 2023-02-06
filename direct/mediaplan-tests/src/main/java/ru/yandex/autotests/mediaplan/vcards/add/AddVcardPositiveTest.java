package ru.yandex.autotests.mediaplan.vcards.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_vcards.Api5AddVcardsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_vcards.ParamsApi5AddVcards;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.datafactories.AddVCardFactory.*;
import static ru.yandex.autotests.mediaplan.TestFeatures.VCARDS_ADD;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(VCARDS_ADD)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Создание визитной карточки для медиаплана")
@RunWith(Parameterized.class)
public class AddVcardPositiveTest {
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddVcards paramsApi5AddVcards;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна визитная каротчка", oneVCard()},
                {"Две визитные карточки", twoVCard()},
                {"Тысяча визиток", thousandVCard()}
        });
    }

    @Rule
    public AdgroupRule mediaplanRule = new AdgroupRule();

    @Test
    public void addVcard() {
        Api5AddVcardsResult vcardsResult = mediaplanRule.getUserSteps().vcardsSteps().api5VcardsAdd(
                paramsApi5AddVcards.withMediaplanId(mediaplanRule.getMediaplanId())
                        .withClientId(getClient()).withTimestamp(mediaplanRule.getLastUpdateTimestamp())
        );
        assertThat("Ответ на запрос сохранения визиток соответсвует ожиданиям", vcardsResult.getAddResults(),
                hasSize(paramsApi5AddVcards.getVCards().size()));
    }
}
