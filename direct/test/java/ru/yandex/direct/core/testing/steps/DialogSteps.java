package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.UUID;

import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DialogInfo;
import ru.yandex.direct.core.testing.repository.TestClientDialogsRepository;

import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

public class DialogSteps {
    private final CampaignSteps campaignSteps;
    private final ClientDialogsRepository clientDialogsRepository;
    private final TestClientDialogsRepository testClientDialogsRepository;

    public DialogSteps(CampaignSteps campaignSteps,
                       ClientDialogsRepository clientDialogsRepository,
                       TestClientDialogsRepository testClientDialogsRepository) {
        this.campaignSteps = campaignSteps;
        this.clientDialogsRepository = clientDialogsRepository;
        this.testClientDialogsRepository = testClientDialogsRepository;
    }

    public DialogInfo createCampWithDialog(ClientInfo clientInfo, Dialog dialog) {
        CampaignInfo campaignInfo = new CampaignInfo()
                .withCampaign(activeTextCampaign(null, null))
                .withClientInfo(clientInfo);

        dialog.setClientId(clientInfo.getClientId().asLong());
        dialog.setLastSyncTime(LocalDateTime.now().minusDays(1));

        DialogInfo dialogInfo = new DialogInfo()
                .withCampaignInfo(campaignInfo)
                .withDialog(dialog);

        campaignSteps.createCampaign(dialogInfo.getCampaignInfo());
        clientDialogsRepository.addDialog(dialogInfo.getShard(), dialogInfo.getCampaignId(), dialogInfo.getDialog());

        return dialogInfo;
    }

    public void deleteDialog(ClientInfo clientInfo, Long campaignId, Dialog dialog) {
        testClientDialogsRepository.deleteDialog(clientInfo.getShard(), campaignId, dialog);
    }

    public DialogInfo createDefaultDialog(ClientInfo clientInfo, String skillId) {
        Dialog defaultDialog = getDefaultDialog(clientInfo, skillId);
        return createCampWithDialog(clientInfo, defaultDialog);
    }

    public DialogInfo createStandaloneDefaultDialog(ClientInfo clientInfo) {
        Dialog dialog = getDefaultDialog(clientInfo, UUID.randomUUID().toString());
        dialog.setClientId(clientInfo.getClientId().asLong());
        dialog.setLastSyncTime(LocalDateTime.now().minusDays(1));

        dialog.withId(clientDialogsRepository.addOrUpdateDialogToClient(dialog));

        return new DialogInfo().withDialog(dialog);
    }

    private Dialog getDefaultDialog(ClientInfo clientInfo, String skillId) {
        return new Dialog()
                .withClientId(clientInfo.getClientId().asLong())
                .withBotGuid("botGuid")
                .withSkillId(skillId == null ? "skillId" : skillId)
                .withIsActive(true)
                .withName("dialogName");
    }
}
