package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDialog;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.validation.result.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithDialogValidatorTest {
    @Test
    public void testSuccess() {
        var campaign = new TextCampaign().withClientDialogId(1L);
        var validator = new CampaignWithDialogValidator(Set.of(1L));
        var result = validator.apply(campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testNoAvailableDialogs() {
        var campaign = new TextCampaign().withClientDialogId(1L);
        var validator = new CampaignWithDialogValidator(Set.of());
        var result = validator.apply(campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(dialogPath(), inCollection())));
    }

    @Test
    public void testWrongDialogId() {
        var campaign = new TextCampaign().withClientDialogId(1L);
        var validator = new CampaignWithDialogValidator(Set.of(2L));
        var result = validator.apply(campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(dialogPath(), inCollection())));
    }

    @Test
    public void testCampaignWithoutDialog() {
        var campaign = new TextCampaign();
        var validator = new CampaignWithDialogValidator(Set.of(1L));
        var result = validator.apply(campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    private static Path dialogPath() {
        return path(field(CampaignWithDialog.CLIENT_DIALOG_ID));
    }
}
