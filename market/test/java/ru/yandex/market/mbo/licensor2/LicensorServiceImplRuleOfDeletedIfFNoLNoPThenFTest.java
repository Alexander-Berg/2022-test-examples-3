package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

import java.util.Arrays;

/**
 * @author ayratgdl
 * @date 17.01.18
 */
public class LicensorServiceImplRuleOfDeletedIfFNoLNoPThenFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFPAndLFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFP1AndFP2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE2));

        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoLNoPThenNoF().build(FRANCHISE1, LICENSOR1, Arrays.asList(PERSONAGE2)),
            clearId(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).get())
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereCasesAreLFPAndFPThenDeleteFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void deleteLicensorCasesWhereCasesAreLFPAndFPThenDeleteLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoLNoPThenNoF().find(FRANCHISE1).isPresent());
    }
}
