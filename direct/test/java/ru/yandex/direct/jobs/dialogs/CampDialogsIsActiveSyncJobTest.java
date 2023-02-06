package ru.yandex.direct.jobs.dialogs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.dialogs.service.DialogsService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DialogInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dialogs.client.model.Skill.Error;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.mockito.Mockito.spy;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_SYNC_CAMP_DIALOGS_STATUS;
import static ru.yandex.direct.jobs.dialogs.SkillsParamsForMock.setSkills;

/**
 * Тесты на джобу {@link CampDialogsIsActiveSyncJob}
 */
@JobsTest
class CampDialogsIsActiveSyncJobTest {
    private CampDialogsIsActiveSyncJob job;
    private TestContextManager testContextManager;

    @Autowired
    private Steps steps;
    @Autowired
    private DialogsService dialogsService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private DialogsService mockDialogsService;

    static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        Arrays.asList(
                                // Не меняется name
                                new TestDialog()
                                        .withDialog("sId2", "bGuid2", "name0", true)
                                        .withExpected("bGuid2", "name2", true, StatusBsSynced.YES),
                                // Диалог не меняется
                                new TestDialog()
                                        .withDialog("sId3", "bGuid3", "name3", true)
                                        .withExpected("bGuid3", "name3", true, StatusBsSynced.YES)
                        ),
                        Arrays.asList(
                                new SkillsParamsForMock("sId1", "bGuid1", "name1", true, null),
                                new SkillsParamsForMock("sId2", "bGuid2", "name2", true, null),
                                new SkillsParamsForMock("sId3", "bGuid3", "name3", true, null)
                        )
                },
                {
                        Arrays.asList(
                                // Диалог меняется с активного не неактивный
                                new TestDialog()
                                        .withDialog("sId1", "bGuid1", "name1", false)
                                        .withExpected("bGuid1", "name1", true, StatusBsSynced.NO),
                                // Диалог был неактивным, приходит неактивный статус (botGuid == null)
                                new TestDialog()
                                        .withDialog("sId2", "bGuid2", "name2", false)
                                        .withExpected("bGuid2", "name2", false, StatusBsSynced.YES),
                                // Диалог был активным, приходит неактивный статус (botGuid == null)
                                new TestDialog()
                                        .withDialog("sId3", "bGuid3", "name3", true)
                                        .withExpected("bGuid3", "name3", false, StatusBsSynced.NO),
                                // Диалог был активным, приходит неактивный статус (onAir == false)
                                new TestDialog()
                                        .withDialog("sId4", "bGuid4", "name4", true)
                                        .withExpected("bGuid4", "name4", false, StatusBsSynced.NO),
                                // Диалог был неактивным, приходит неактивный статус (onAir == false)
                                new TestDialog()
                                        .withDialog("sId5", "bGuid5", "name5", false)
                                        .withExpected("bGuid5", "name5", false, StatusBsSynced.YES),
                                // Диалог был активным, приходит неактивный статус (botGuid == null && onAir == false)
                                new TestDialog()
                                        .withDialog("sId6", "bGuid6", "name6", true)
                                        .withExpected("bGuid6", "name6", false, StatusBsSynced.NO)
                        ),
                        Arrays.asList(
                                new SkillsParamsForMock("sId1", "bGuid1", "name1", true, null),
                                new SkillsParamsForMock("sId2", null, "name2", true, null),
                                new SkillsParamsForMock("sId3", null, "name3", true, null),
                                new SkillsParamsForMock("sId4", "bGuid4", "name4", false, null),
                                new SkillsParamsForMock("sId5", "bGuid5", "name5", false, null),
                                new SkillsParamsForMock("sId6", null, "name6", false, null)
                        )
                },
                {
                        Arrays.asList(
                                new TestDialog()
                                        .withDialog("eId1", "bGuid2", "name2", true)
                                        .withExpected("bGuid2", "name2", false, StatusBsSynced.NO),
                                new TestDialog()
                                        .withDialog("eId2", "bGuid3", "name3", true)
                                        .withExpected("bGuid3", "name3", false, StatusBsSynced.NO)
                        ),
                        Arrays.asList(
                                new SkillsParamsForMock(null, null, null, null,
                                        new Error().withCode(400).withMessage("not_correct").withSkillId("eId1")),
                                new SkillsParamsForMock(null, null, null, null,
                                        new Error().withCode(404).withMessage("not_found").withSkillId("eId2"))
                        )
                },
                {
                        Arrays.asList(
                                // Общий диалог у двух кампаний
                                new TestDialog()
                                        .withDialog("sId1", "bGuid1", "name1", true)
                                        .withExpected("bGuid1", "name1", false, StatusBsSynced.NO),
                                new TestDialog()
                                        .withDialog("sId1", "bGuid1", "name1", true)
                                        .withExpected("bGuid1", "name1", false, StatusBsSynced.NO)
                        ),
                        Collections.singletonList(
                                new SkillsParamsForMock("sId1", null, "name1", true, null)
                        )
                }
        });
    }

    @BeforeEach
    void before() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        this.mockDialogsService = spy(dialogsService);

        PpcProperty<Boolean> enableSyncCampDialogsStatus = ppcPropertiesSupport.get(ENABLE_SYNC_CAMP_DIALOGS_STATUS);
        enableSyncCampDialogsStatus.set(true);
    }


    @ParameterizedTest(name = "Dialogs: {0} Skills: {1}")
    @MethodSource("params")
    void syncDialogsTest(List<TestDialog> testDialogs, List<SkillsParamsForMock> skillsParamsForMock) {
        setSkills(mockDialogsService, skillsParamsForMock);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Map<String, Long> campaignIdBySkillIds = new HashMap<>();
        for (TestDialog testDialog : testDialogs) {
            DialogInfo dialogInfo = steps.dialogSteps().createCampWithDialog(clientInfo, testDialog.getDialog());
            campaignIdBySkillIds.put(dialogInfo.getDialog().getSkillId(), dialogInfo.getCampaignId());
        }

        job = new CampDialogsIsActiveSyncJob(1, mockDialogsService, ppcPropertiesSupport);
        executeJob();

        Map<Long, Dialog> id2dialogs = mockDialogsService.getDialogs(1, LimitOffset.maxLimited())
                .stream().collect(Collectors.toMap(Dialog::getId, Function.identity()));
        Map<Long, StatusBsSynced> cid2bs = campaignRepository.getCampaigns(1, campaignIdBySkillIds.values())
                .stream().collect(Collectors.toMap(Campaign::getId, Campaign::getStatusBsSynced));

        for (TestDialog testDialog : testDialogs) {
            SoftAssertions.assertSoftly(soft -> {
                Dialog result = id2dialogs.get(testDialog.getDialog().getId());
                soft.assertThat(result.getBotGuid())
                        .describedAs("botGuid")
                        .isEqualTo(testDialog.getExpectedBotGuid());
                soft.assertThat(result.getName())
                        .describedAs("name")
                        .isEqualTo(testDialog.getExpectedName());
                soft.assertThat(result.getIsActive())
                        .describedAs("isActive")
                        .isEqualTo(testDialog.getExpectedIsActive());
                soft.assertThat(cid2bs.get(campaignIdBySkillIds.get(testDialog.getDialog().getSkillId())))
                        .describedAs("statusBsSynced")
                        .isEqualTo(testDialog.getExpectedStatusBsSynced());
            });
        }

        for (TestDialog testDialog : testDialogs) {
            steps.dialogSteps()
                    .deleteDialog(clientInfo, campaignIdBySkillIds.get(testDialog.getDialog().getSkillId()),
                            testDialog.getDialog());
        }
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }
}
