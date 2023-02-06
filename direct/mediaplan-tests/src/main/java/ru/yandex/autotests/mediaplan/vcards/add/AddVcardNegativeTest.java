package ru.yandex.autotests.mediaplan.vcards.add;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_vcards.Api5AddVcardsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_vcards.ParamsApi5AddVcards;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.VCARDS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddVCardFactory.tooMuchVCard;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(VCARDS_ADD)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Создание 1001 визитной карточки для медиаплана")
public class AddVcardNegativeTest {
    public ParamsApi5AddVcards paramsApi5AddVcards = tooMuchVCard();
    @Rule
    public AdgroupRule mediaplanRule = new AdgroupRule();

    @Test
    public void addVcard() {
        Api5AddVcardsResult vcardsResult = mediaplanRule.getUserSteps().vcardsSteps().api5VcardsAdd(
                paramsApi5AddVcards.withMediaplanId(mediaplanRule.getMediaplanId())
                        .withClientId(getClient()).withTimestamp(mediaplanRule.getLastUpdateTimestamp())
        );
        assertThat("вернулась ошибка на лишнуюю визитную карточку", vcardsResult.getAddResults().stream()
                        .filter(x -> !x.getErrors().isEmpty()).collect(Collectors.toList()),
                hasSize(1));
    }
}
