package ru.yandex.market.mbo.licensor2.scheme;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.processing.OperationException;

import static ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source.MBO_UI;
import static ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source.UNKNOWN;

/**
 * @author ayratgdl
 * @date 22.01.18
 */
public class LicensorSchemeServiceValidateTest {
    private static final Long LICENSOR1 = 101L;
    private static final Long LICENSOR2 = 102L;
    private static final Long FRANCHISE1 = 201L;
    private static final Long FRANCHISE2 = 202L;
    private static final Long PERSONAGE1 = 301L;
    private static final Long VENDOR1 = 401L;
    private static final Long CATEGORY1 = 501L;
    private static final Long UID1 = 601L;
    private static final Long UID2 = 602L;

    private LicensorSchemeService schemeService;

    @Before
    public void setUp() {
        schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());
    }

    // ####### Test validate create/update not valid LFP

    @Test(expected = IllegalArgumentException.class)
    public void createLicensorCaseWhereCaseIsLP() {
        LicensorCase licensorCase = new LicensorCase(new LFP(LICENSOR1, null, PERSONAGE1));
        schemeService.createLicensorCase(licensorCase);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateCreateLicensorCaseWithResultLfpIsAllNull() {
        LicensorCase licensorCase = new LicensorCase(new LFP(null, null, null));
        schemeService.createLicensorCase(licensorCase);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createLicensorCaseWhereLFWithRestoreByFP() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByFP(true)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLicensorCaseWhereLFWithRestoreByFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        schemeService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByFP(true)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void createLicensorCaseWhereFPWithRestoreByFP() {
        schemeService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1)
                .setRestoreByFP(true)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLicensorCaseWhereFPWithRestoreByFP() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.updateLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1)
                .setRestoreByFP(true)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLicensorCaseWhereLFPWithRestoreByPAndLP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .setRestoreByP(true)
                .setRestoreByLP(true)
        );
    }

    // Test validate duplicate

    @Test(expected = OperationException.class)
    public void createLicensorCaseWhereDuplicateLF() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
    }

    // Test validate franchise belong two licensors

    @Test(expected = OperationException.class)
    public void createLicensorCaseWithResultFranchiseBelongTwoLicensors() {
        schemeService.createLicensorCase(
            new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, null))
        );
        schemeService.createLicensorCase(
            new LicensorCase(new LFP(LICENSOR2, FRANCHISE1, null))
        );
    }

    // ###### Test validate update not exists LFP

    @Test(expected = IllegalArgumentException.class)
    public void updateLicensorCaseWhereNotExistsCase() {
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
    }


    // ###### Test validate exists rule of restore L by F when exists free F and vice versa

    @Test(expected = OperationException.class)
    public void createLicensorCaseExistsFreeFranchiseThenRuleRestoreByF() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
    }

    @Test(expected = OperationException.class)
    public void createLicensorCaseExistsFreeFranchiseThenRuleRestoreByFP() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .setRestoreByFP(true)
        );
    }

    @Test(expected = OperationException.class)
    public void createLicensorCaseExistsRuleRestoreByFThenAddFreeFranchise() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
    }

    @Test(expected = OperationException.class)
    public void createLicensorCaseExistsRuleRestoreByFPThenAddFreeFranchise() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
    }

    @Test(expected = OperationException.class)
    public void createLicensorCaseExistsRuleRestoreByFPThenAddFreeFranchiseWithPersonage() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
    }

    @Test(expected = OperationException.class)
    public void updateLicensorCaseWhereExistsFreeFranchiseThenRuleRestoreByF() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        schemeService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
    }

    @Test(expected = OperationException.class)
    public void updateLicensorCaseWhereExistsFreeFranchiseThenRuleRestoreByFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        schemeService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .setRestoreByFP(true)
        );
    }

    // Test validate delete LF when exists free F

    @Test
    public void validateDeleteLicensorCaseDeleteLFWhenExistsFreeFAndNoExistsRestoreL() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        // no exception. It's OK
    }

    @Test(expected = OperationException.class)
    public void validateDeleteLicensorCaseDeleteLFWhenExistsRestoreFAndFreeF() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
                .setRestoreByF(true)
        );
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));
    }

    @Test(expected = OperationException.class)
    public void validateDeleteLicensorCaseDeleteLFWhenExistsRestoreFPAndFreeF() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .setRestoreByFP(true)
        );
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));
    }

    // Test validate если P востанавливает LFP, тогда P принадлежит только лицензиару L

    @Test
    public void validateUniquePWhenNoExistsRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE2, PERSONAGE1));

        // no exception. It is Ok.
    }

    @Test
    public void validateUniquePWhenFPThenUpdateFPToRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.updateLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true));

        // No exception. It is Ok.
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenL1FPWithRestoreByPThenAddL2FP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE2, PERSONAGE1));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenL2FPThenAddL1FPWithRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE2, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenL1FPAndL2FPThenUpdateL1FPWhitRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE2, PERSONAGE1));
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenExistsRestoreByPThenAddFreeP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
        schemeService.createLicensorCase(new LicensorCase(null, null, PERSONAGE1));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenLFPAndFreePThenUpdateLFPWithRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, null, PERSONAGE1));
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenExistsRestoreByPThenAddFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenFPThenAddLFPWithRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
    }

    @Test(expected = OperationException.class)
    public void validateUniquePWhenFPAndLFPThenUpdateLFPWithRestoreByP() {
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true));
    }

    @Test
    public void validateUniquePWhereFPWithRestoreByPThenUpdateFP() {
        schemeService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        schemeService.updateLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        // No exception. It's ok.
    }

    // Test validate create vendor constraint

    @Test(expected = OperationException.class)
    public void addLVLinkWithResultDuplicate() {
        schemeService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                          UID1, MBO_UI));
        schemeService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                          UID2, UNKNOWN));
    }

    // Validation tests of creating and deleting single L/F/P

    @Test(expected = OperationException.class)
    public void deleteSingleL() {
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, null, null));
    }

    @Test
    public void createAndDeleteSingleLWhenExistedLFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, null, null));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, null, null));
    }

    @Test(expected = OperationException.class)
    public void deleteSingleLWithExtras() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, null, null)
                .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, null, null));
    }

    @Test(expected = OperationException.class)
    public void deleteSingleF() {
        schemeService.deleteLicensorCase(new LFP(null, FRANCHISE1, null));
    }

    @Test
    public void createAndDeleteSingleFWhenExistedLFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        schemeService.deleteLicensorCase(new LFP(null, FRANCHISE1, null));
    }

    @Test(expected = OperationException.class)
    public void deleteSingleFWithExtras() {
        schemeService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, null)
                .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.deleteLicensorCase(new LFP(null, FRANCHISE1, null));
    }

    @Test(expected = OperationException.class)
    public void deleteSingleP() {
        schemeService.deleteLicensorCase(new LFP(null, null, PERSONAGE1));
    }

    @Test
    public void createAndDeleteSinglePWhenExistedLFP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, null, PERSONAGE1));
        schemeService.deleteLicensorCase(new LFP(null, null, PERSONAGE1));
    }

    @Test(expected = OperationException.class)
    public void deleteSinglePWithExtras() {
        schemeService.createLicensorCase(
            new LicensorCase(null, null, PERSONAGE1)
                .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.deleteLicensorCase(new LFP(null, null, PERSONAGE1));
    }
}
