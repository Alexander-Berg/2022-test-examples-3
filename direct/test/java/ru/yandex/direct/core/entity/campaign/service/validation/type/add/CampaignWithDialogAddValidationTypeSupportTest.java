package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDialog;
import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.dialogs.service.DialogsService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithDialogAddValidationTypeSupportTest {
    private static final long DEFAULT_DIALOG_ID = 1L;
    @Mock
    private DialogsService dialogsService;

    @InjectMocks
    private CampaignWithDialogAddValidationTypeSupport typeSupport;

    private static ClientId clientId;
    private static Long uid;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();

        Dialog dialog = new Dialog().withId(DEFAULT_DIALOG_ID);
        doReturn(List.of(dialog)).when(dialogsService).getDialogsByClientId(clientId);
    }

    @Test
    public void testValidateSuccessfully() {
        var campaign = new TextCampaign()
                .withClientDialogId(DEFAULT_DIALOG_ID);
        ValidationResult<List<CampaignWithDialog>, Defect> result = typeSupport
                .validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidationError() {
        var campaign = new TextCampaign()
                .withClientDialogId(555L);
        ValidationResult<List<CampaignWithDialog>, Defect> result =
                typeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasDefectDefinitionWith(validationError(dialogPath(), inCollection())));
    }

    private static Path dialogPath() {
        return path(index(0), field(CampaignWithDialog.CLIENT_DIALOG_ID));
    }
}
