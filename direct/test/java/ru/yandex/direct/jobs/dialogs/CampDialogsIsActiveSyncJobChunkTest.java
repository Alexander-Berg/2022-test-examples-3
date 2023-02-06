package ru.yandex.direct.jobs.dialogs;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
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
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_SYNC_CAMP_DIALOGS_STATUS;
import static ru.yandex.direct.jobs.dialogs.SkillsParamsForMock.setSkills;

/**
 * Тесты на джобу {@link CampDialogsIsActiveSyncJob}
 */
@JobsTest
class CampDialogsIsActiveSyncJobChunkTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DialogsService dialogsService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private DialogsService mockDialogsService;

    private int shard = 1;
    private static int chunkSize = 2;
    private static int hoursBetweenSync = 22;

    @BeforeEach
    void before() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        this.mockDialogsService = spy(dialogsService);

        PpcProperty<Boolean> enableSyncCampDialogsStatus = ppcPropertiesSupport.get(ENABLE_SYNC_CAMP_DIALOGS_STATUS);
        enableSyncCampDialogsStatus.set(true);
    }

    static Collection<Object[]> syncDialogsByChunk() {
        return asList(new Object[][]{
                {
                        asList(
                                new TestDialog()
                                        .withDialog("sId1", "bGuid1", "name1", true)
                                        .withExpected("bGuid1", "name1", true, StatusBsSynced.YES),
                                new TestDialog()
                                        .withDialog("sId2", "bGuid2", "name2", true)
                                        .withExpected("bGuid2", "name2", true, StatusBsSynced.YES),
                                new TestDialog()
                                        .withDialog("sId3", "bGuid3", "name3", true)
                                        .withExpected("bGuid0", "name0", true, StatusBsSynced.NO),
                                new TestDialog()
                                        .withDialog("sId4", "bGuid4", "name4", true)
                                        .withExpected("bGuid0", "name0", true, StatusBsSynced.NO)
                        ),
                        asList(
                                new SkillsParamsForMock("sId1", "bguid0", "name0", true, null),
                                new SkillsParamsForMock("sId2", "bGuid0", "name0", true, null),
                                new SkillsParamsForMock("sId3", "bGuid0", "name0", true, null),
                                new SkillsParamsForMock("sId4", "bGuid0", "name0", true, null)
                        )
                }

        });
    }

    static Collection<Object[]> incorrectName() {
        return asList(new Object[][]{
                {
                        singletonList(
                                new TestDialog()
                                        .withDialog("sId1", "bGuid1", "name0", true)
                                        .withExpected("bGuid1", "\uFFFD", true, StatusBsSynced.YES)
                        ),
                        singletonList(
                                new SkillsParamsForMock("sId1", "bGuid1", "\uD83D\uDC15", true, null)
                        )
                },

        });
    }

    @ParameterizedTest(name = "Dialogs: {0} Skills: {1} ")
    @MethodSource("syncDialogsByChunk")
    void syncDialogsByChunkTest(List<TestDialog> testDialogs, List<SkillsParamsForMock> skillsParamsForMock) {
        // Проверяем, что при ошибке в одном чанке, остальные чанки успешно выполнятся
        List<Dialog> dialogs = StreamEx.of(testDialogs)
                .map(TestDialog::getDialog)
                .toList();
        doThrow(new RuntimeException()).when(mockDialogsService)
                .updateDialogsBySkills(anyInt(), anyList(),
                        argThat( arg -> arg.containsKey(skillsParamsForMock.get(0).getSkillId())));
        doCallRealMethod().when(mockDialogsService)
                .updateDialogsBySkills(anyInt(), eq(dialogs.subList(chunkSize, dialogs.size())),
                        argThat( arg -> !arg.containsKey(skillsParamsForMock.get(0).getSkillId())));

        doJobAndCheck(testDialogs, skillsParamsForMock);
    }

    @ParameterizedTest(name = "Dialogs: {0} Skills: {1} ")
    @MethodSource("incorrectName")
    void incorrectNameTest(List<TestDialog> testDialogs, List<SkillsParamsForMock> skillsParamsForMock) {
        doJobAndCheck(testDialogs, skillsParamsForMock);
    }

    void doJobAndCheck(List<TestDialog> testDialogs, List<SkillsParamsForMock> skillsParamsForMock) {
        setSkills(mockDialogsService, skillsParamsForMock);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        Map<String, Long> campaignIdBySkillIds = new HashMap<>();
        for (TestDialog testDialog : testDialogs) {
            DialogInfo dialogInfo = steps.dialogSteps().createCampWithDialog(clientInfo, testDialog.getDialog());
            campaignIdBySkillIds.put(dialogInfo.getDialog().getSkillId(), dialogInfo.getCampaignId());
        }

        CampDialogsIsActiveSyncJob job = new CampDialogsIsActiveSyncJob(shard, mockDialogsService, ppcPropertiesSupport);
        job.syncDialogsByChunk(chunkSize, hoursBetweenSync);

        Map<Long, Dialog> id2dialogs = mockDialogsService.getDialogs(shard, LimitOffset.maxLimited())
                .stream().collect(Collectors.toMap(Dialog::getId, Function.identity()));
        Map<Long, StatusBsSynced> cid2bs = campaignRepository.getCampaigns(shard, campaignIdBySkillIds.values())
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
}
