package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 17.01.18
 */
public class LicensorServiceImplRuleOfDeletedIfLNoFThenLTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndL() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, null, null));

        Assert.assertFalse(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLF1PAndLF2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE2, null));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreL1F1P1AndL2F2P2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE2, PERSONAGE2));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR2).isPresent());
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereCasesAreLFPDeleteLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }

    @Test
    public void deleteLicensorCaseWhereCasesAreLFPAndLDeleteL() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, null, null));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, null, null));

        Assert.assertTrue(ruleOfDeletedHelper.ifLNoFThenNoL().find(LICENSOR1).isPresent());
    }
}
