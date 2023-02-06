package ru.yandex.direct.core.entity.grants.service.validation;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.grants.model.Grants;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GrantsValidationServiceTest {

    private GrantsValidationService grantsValidationService;

    @Before
    public void before() {
        grantsValidationService = new GrantsValidationService();
    }

    @Test
    public void validAllowAll() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(validGrantsAllowAll());
        assertThat("Ошибки отсутствуют", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validDenyAll() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(validGrantsDenyAll());
        assertThat("Ошибки отсутствуют", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void nullClientId() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(
                validGrantsAllowAll().withClientId(null));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("clientId")), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void nullAllowEditCampaigns() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(
                validGrantsAllowAll().withAllowEditCampaigns(null));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("allowEditCampaigns")), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void nullAllowTransferMoney() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(
                validGrantsAllowAll().withAllowTransferMoney(null));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("allowTransferMoney")), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void nullAllowImportXls() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(
                validGrantsAllowAll().withAllowImportXls(null));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("allowImportXls")), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void inconsistentAllowEditCampaignAndAllowImportXls() throws Exception {
        ValidationResult<Grants, Defect> vr = grantsValidationService.validate(
                validGrantsAllowAll()
                        .withAllowEditCampaigns(false)
                        .withAllowImportXls(true));
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("allowImportXls")),
                        GrantsDefectIds.Gen.INCONSISTENT_STATE_ALLOW_EDIT_CAMPAIGN_AND_ALLOW_IMPORT_XLS)));
    }

    private Grants validGrantsAllowAll() {
        return new Grants()
                .withClientId(1L)
                .withAllowEditCampaigns(true)
                .withAllowTransferMoney(true)
                .withAllowImportXls(true);
    }

    private Grants validGrantsDenyAll() {
        return new Grants()
                .withClientId(1L)
                .withAllowEditCampaigns(false)
                .withAllowTransferMoney(false)
                .withAllowImportXls(false);
    }
}
