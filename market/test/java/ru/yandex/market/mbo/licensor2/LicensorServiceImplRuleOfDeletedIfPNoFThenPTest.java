package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 17.01.18
 */
public class LicensorServiceImplRuleOfDeletedIfPNoFThenPTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, null, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE1).isPresent());
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereCasesAreLF1PAndLF1PThenDeleteLF2P() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE2, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE2, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE1).isPresent());
    }

    @Test
    public void deleteLicensorCaseWhereCasesAreLFP1AndLFP2ThenDeleteLFP2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2));

        Assert.assertTrue(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE1).isPresent());
        Assert.assertFalse(ruleOfDeletedHelper.ifPNoFThenNoP().find(PERSONAGE2).isPresent());
    }
}
