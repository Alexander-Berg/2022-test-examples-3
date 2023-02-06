package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplRuleOfRestoreIfFPThenLTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereLFPWithoutRestoreByFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereLFPWithRestoreByFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseWhereLFPThenSetRestoreByFP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereLFPThenUnsetRestoreByFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );
        licensorService.updateLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereLFP1AndLFP2ThenUnsetRestoreByFP2() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2).setRestoreByFP(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE2).setRestoreByFP(false)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
        Assert.assertFalse(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2)).isPresent()
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereLFPThenDeleteLFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFPThenL().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }
}
