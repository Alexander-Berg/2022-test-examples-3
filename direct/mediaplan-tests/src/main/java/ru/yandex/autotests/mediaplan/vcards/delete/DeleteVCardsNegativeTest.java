package ru.yandex.autotests.mediaplan.vcards.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_vcards.ParamsApi5AddVcards;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_vcards.Api5DeleteVcardsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_vcards.ParamsApi5DeleteVcards;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_vcards.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.VCARDS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddVCardFactory.oneVCard;
import static ru.yandex.autotests.mediaplan.datafactories.AddVCardFactory.twoVCard;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(VCARDS_DELETE)
@Description("Удаление несуществующих визитных карточек из медиаплана")
@RunWith(Parameterized.class)
public class DeleteVCardsNegativeTest {
    private ParamsApi5AddVcards paramsApi5AddVcards;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна визитная каротчка", oneVCard()},
                {"Две визитные карточки", twoVCard()},
        });
    }

    @Rule
    public AdgroupRule adgroupRule;

    public DeleteVCardsNegativeTest(String text, ParamsApi5AddVcards paramsApi5AddVcards) {
        this.paramsApi5AddVcards = paramsApi5AddVcards;
        adgroupRule = new AdgroupRule().withVCards(paramsApi5AddVcards);
    }

    @Test
    public void deleteVCard() {
        adgroupRule.getUserSteps().vcardsSteps().api5VcardsDelete(new ParamsApi5DeleteVcards()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()).withSelectionCriteria(new SelectionCriteria()
                        .withIds(adgroupRule.getSitelinksIds())));
        Api5DeleteVcardsResult api5DeleteSitelinksResult = adgroupRule.getUserSteps().vcardsSteps().api5VcardsDelete(new ParamsApi5DeleteVcards()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()).withSelectionCriteria(new SelectionCriteria()
                        .withIds(Collections.singletonList(123123l))));
        assertThat("Ссылки удалились", api5DeleteSitelinksResult.getDeleteResults().stream()
                        .filter(x->!x.getErrors().isEmpty()).collect(Collectors.toList()),
                hasSize(paramsApi5AddVcards.getVCards().size()));
    }
}
