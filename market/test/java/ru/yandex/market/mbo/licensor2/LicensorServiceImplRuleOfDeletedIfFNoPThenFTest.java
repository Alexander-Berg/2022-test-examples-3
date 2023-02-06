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
public class LicensorServiceImplRuleOfDeletedIfFNoPThenFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereCasesAreLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndFP() {
        licensorService.createLicensorCase(new LicensorCase(FRANCHISE1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void createLicensorCaseWhereCasesAreLFP1AndFP2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoPThenNoF().build(FRANCHISE1, Arrays.asList(PERSONAGE1, PERSONAGE2)),
            clearId(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).get())
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereCasesAreLFPThenDeleteLFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void deleteLicensorCaseWhereCasesAreLFP1AndLFP2ThenDeleteLFP2() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoPThenNoF().build(FRANCHISE1, Arrays.asList(PERSONAGE1)),
            clearId(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).get())
        );
    }

    @Test
    public void deleteLicensorCaseWhereCasesAreLFP1AndLFAndFP2ThenDeleteLFP1AndLF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE2));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        Assert.assertTrue(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoPThenNoF().build(FRANCHISE1, Arrays.asList(PERSONAGE2)),
            clearId(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).get())
        );
    }

    @Test
    public void addSingleFWhereCasesAreFP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));

        Assert.assertFalse(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).isPresent());
    }

    @Test
    public void deleteSingleFWhereCasesAreFPAndF() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        licensorService.deleteLicensorCase(new LFP(null, FRANCHISE1, null));

        Assert.assertEquals(
            ruleOfDeletedHelper.ifFNoPThenNoF().build(FRANCHISE1, Arrays.asList(PERSONAGE1)),
            clearId(ruleOfDeletedHelper.ifFNoPThenNoF().find(FRANCHISE1).get())
        );
    }
}
