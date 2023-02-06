package ru.yandex.autotests.direct.cmd.bssynced.phrases;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении фразы через сохранение группы")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.PHRASES)
@Tag(ObjectTag.PHRASE)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class StatusBsSyncedAfterPhraseChangeTest extends StatusBsSyncedAfterPhraseChangeBaseTest {

    private File excelToUpload;
    private File tempExcel;

    public StatusBsSyncedAfterPhraseChangeTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.HIGHEST_POSITION_MAX_COVERAGE))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сброс bsSynced группы и фразы после изменения фразы. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList((Object[][]) new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    @Test
    @Description("Сброс bsSynced группы при добавлении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9349")
    public void checkBsSyncedCreatePhrase() {
        addPhrase(NEW_PHRASE);
        Long newPhraseId = bannersRule.getCurrentGroup().getPhrases().stream()
                .filter(f -> f.getPhrase().equals(NEW_PHRASE))
                .findFirst()
                .orElseThrow(IllegalStateException::new)
                .getId();

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при удалении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9350")
    public void checkBsSyncedDeletePhrase() {
        Group group = bannersRule.getCurrentGroup();
        group.getPhrases().remove(0);
        group.setRetargetings(Collections.emptyList());
        group.setTags(Collections.emptyMap());

        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(CLIENT, campaignId, group);
        bannersRule.saveGroup(groupsParameters);

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при изменении текста фразы на странице изменения кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9352")
    public void checkBsSyncedChangePhraseText() {
        Group group = bannersRule.getCurrentGroup();
        Phrase phrase = group.getPhrases().get(0);
        phrase.withPhrase("поменяли текст фразы");
        group.setRetargetings(Collections.emptyList());
        group.setTags(Collections.emptyMap());

        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(CLIENT, campaignId, group);
        bannersRule.saveGroup(groupsParameters);

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при изменении параметрa 1 фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10676")
    public void checkBsSyncedChangePhraseParameter1() {
        changeBidsParam(ExcelColumnsEnum.PARAM1);
        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при изменении параметрf 2 фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10677")
    public void checkBsSyncedChangePhraseParameter2() {
        changeBidsParam(ExcelColumnsEnum.PARAM2);
        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    private void changeBidsParam(ExcelColumnsEnum columnsEnum){
        tempExcel = cmdRule.cmdSteps().excelSteps().exportXlsCampaign(bannersRule.getCampaignId(), CLIENT);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }

        ExcelUtils.setCellValue(tempExcel, excelToUpload, columnsEnum, 1, "New Param");
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT, bannersRule.getCampaignId().toString(),
                ImportCampXlsRequest.DestinationCamp.OLD);
    }

    @After
    public void delete() {
        if (tempExcel != null) {
            FileUtils.deleteQuietly(tempExcel);
        }
        if (excelToUpload != null) {
            FileUtils.deleteQuietly(excelToUpload);
        }
    }
}
