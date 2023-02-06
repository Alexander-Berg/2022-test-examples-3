package ru.yandex.direct.core.entity.dialogs.service;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.dialogs.exception.InvalidDialogException;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dialogs.client.DialogsClient;
import ru.yandex.direct.dialogs.client.model.Skill;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DialogsServiceTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ClientDialogsRepository clientDialogsRepository;

    @Autowired
    private Steps steps;

    private DialogsClient dialogsClient;
    private DialogsService dialogsService;
    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void setUp() {
        initMocks(this);
        dialogsClient = mock(DialogsClient.class);
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        dialogsService = new DialogsService(campaignRepository, clientDialogsRepository, dialogsClient);
    }

    @Test
    public void addDialog_success() {
        Skill skill = new Skill();
        String skillId = "skillId";
        skill.setSkillId(skillId);
        skill.setBotGuid("botGuid");
        skill.setName("name");
        skill.setOnAir(true);

        doReturn(singletonList(skill)).when(dialogsClient).getSkills(anyList());

        Dialog expectedDialog = new Dialog()
                .withSkillId(skillId)
                .withBotGuid("botGuid")
                .withIsActive(true)
                .withName("name")
                .withClientId(clientInfo.getClientId().asLong());

        Result<Dialog> result = dialogsService.addDialog(clientInfo.getClientId(), "skillId");
        assertThat(result.getErrors(), hasSize(0));

        List<Dialog> dialogs = dialogsService.getDialogs(shard, new LimitOffset(100, 0));
        assertThat(dialogs, hasSize(greaterThan(0)));

        Map<String, Dialog> dialogBySkillId = listToMap(dialogs, Dialog::getSkillId);
        assumeThat(dialogBySkillId.get(skillId),
                beanDiffer(expectedDialog).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void addDialog_invalidBotGuid() {
        Skill skill = new Skill();
        String skillId = "skillId";
        skill.setSkillId(skillId);
        skill.setName("name");
        skill.setOnAir(true);

        doReturn(singletonList(skill)).when(dialogsClient).getSkills(anyList());

        assertThatThrownBy(() -> dialogsService.addDialog(clientInfo.getClientId(), "skillId"))
                .isInstanceOf(InvalidDialogException.class);
    }
}
