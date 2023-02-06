package ru.yandex.autotests.mediaplan.adgroups.add;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.AddResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.Api5AddAdgroupsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.ParamsApi5AddAdgroups;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_mediaplans.Api5AddMediaplansResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_mediaplans.Mediaplan;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.*;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_ADD)
@Description("Создание некоректных групп баннеров для медиаплана")

@Tag(MasterTags.MASTER)
public class AddAdGroupsNegativeTest {
    @Rule
    public MediaplanRule mediaplanRule = new MediaplanRule();

    @Test
    @Title("Добавление 1001 группы")
    public void addTooMuchAdGroup() {

        ParamsApi5AddAdgroups addAdGroupsInputData = tooMuchRandomAdgroups();
        Long timestamp = mediaplanRule.getLastUpdateTimestamp();
        Api5AddAdgroupsResult adGroups = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                addAdGroupsInputData
                        .withMediaplanId(mediaplanRule.getMediaplanId())
                        .withTimestamp(timestamp)
                        .withClientId(getClient()));
        List<Long> ids = adGroups.getAddResults().stream()
                .map(AddResult::getId)
                .collect(Collectors.toList());

        Api5ChangesCheckVerboseResult changes = mediaplanRule.getUserSteps().changesSteps()
                .adGroupChanges(ids.subList(0, ids.size() - 1), getClient(), mediaplanRule.getMediaplanId());
        changes.getModified().withAdGroups(changes.getModified().getAdGroups().stream()
                .map(x -> !x.getNegativeKeywords().getItems().isEmpty() ? x.withId(null) : x.withNegativeKeywords(null).withId(null))
                .collect(Collectors.toList()));
//        assumeThat("Сохраненные группы соотвествуют ожиданиям", changes.getModified().getAdGroups().subList(0, addAdGroupsInputData.getAdGroups().size() - 2),
//                is(addAdGroupsInputData.getAdGroups().subList(0, addAdGroupsInputData.getAdGroups().size())));
        assertThat("Сохранение 1001го  элемента отдало ошибку", adGroups.getAddResults().get(adGroups.getAddResults().size() - 1).getErrors(),
                notNullValue());

    }

    @Test
    @Title("Добавление группы в чужой медиаплан")
    public void addAdGroupToOtherMediaplan() {
        Api5AddMediaplansResult otherMediaplan = mediaplanRule.getUserSteps().mediaplansSteps().api5MediaplansAdd(
                new Mediaplan()
                        .withMediaplannerUID(getClient())
                        .withRequestId(mediaplanRule.getRequestId())
                        .withClientId(getClient())
        );
        ParamsApi5AddAdgroups addAdGroupsInputData = oneAdgroup();
        Long timestamp = mediaplanRule.getLastUpdateTimestamp();
        Api5AddAdgroupsResult adGroups = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                addAdGroupsInputData.withMediaplanId(otherMediaplan.getAddResults().get(0).getId()).withTimestamp(timestamp).withClientId(getClient()));
        List<Long> ids = adGroups.getAddResults().stream()
                .map(AddResult::getId)
                .collect(Collectors.toList());

        mediaplanRule.getUserSteps().changesSteps().adGroupChanges(ids, getClient(), mediaplanRule.getMediaplanId());
        assertThat("Сохранение в чужой медиаплан отдало ошибку", adGroups.getAddResults().get(0).getErrors(),
                not(hasSize(0)));

    }

    @Test
    @Title("Добавление группы в чужой медиаплан")
    public void addAdGroupToOtherClient() {
        ParamsApi5AddAdgroups addAdGroupsInputData = oneAdgroup();
        Long timestamp = mediaplanRule.getLastUpdateTimestamp();
        Api5AddAdgroupsResult adGroups = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                addAdGroupsInputData.withMediaplanId(mediaplanRule.getMediaplanId()).withTimestamp(timestamp).withClientId(2L));
        List<Long> ids = adGroups.getAddResults().stream()
                .map(AddResult::getId)
                .collect(Collectors.toList());

        mediaplanRule.getUserSteps().changesSteps().adGroupChanges(ids, getClient(), mediaplanRule.getMediaplanId());
        assertThat("Сохранение в чужой медиаплан отдало ошибку", adGroups.getAddResults().get(0).getErrors(),
                not(hasSize(0)));

    }


    @Test
    @Title("Добавление группы c очень длинным именем")
    public void addAdGroupWithLongName() {
        ParamsApi5AddAdgroups addAdGroupsInputData = oneAdgroupWithLongName();
        Long timestamp = mediaplanRule.getLastUpdateTimestamp();
        Api5AddAdgroupsResult adGroups = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                addAdGroupsInputData.withMediaplanId(mediaplanRule.getMediaplanId()).withTimestamp(timestamp).withClientId(2L));
        List<Long> ids = adGroups.getAddResults().stream()
                .map(AddResult::getId)
                .collect(Collectors.toList());

        mediaplanRule.getUserSteps().changesSteps().adGroupChanges(ids, getClient(), mediaplanRule.getMediaplanId());
        assertThat("Сохранение группы с длинным именем отдало ошибку", adGroups.getAddResults().get(0).getErrors(),
                not(hasSize(0)));

    }



}
