package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 17.01.18
 */
public class LicensorServiceImplRuleOfDeletedIfFNoLThenFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFPAndLFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereCasesAreLFPThenDeleteLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void deleteLicensorCaseWhereCasesAreLFP1AndLFP2ThenDeleteLFP2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void addSingleFWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void deleteSingleFWhereCasesAreFPAndF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        licensorService.deleteLicensorCase(new LFP(null, FRANCHISE1, null));

        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoLThenNoF().build(FRANCHISE1, LICENSOR1),
            clearId(ruleOfDeletedHelper.ifFNoLThenNoF().find(FRANCHISE1).get())
        );
    }
}
