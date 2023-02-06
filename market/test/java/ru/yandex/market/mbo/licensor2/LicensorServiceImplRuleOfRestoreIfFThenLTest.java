package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplRuleOfRestoreIfFThenLTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereLFWithoutRestoreByF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereLFWithRestoreByF() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseWhereLFThenSetRestoreByF() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereLFThenUnsetRestoreByF() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(false)
        );

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereLF1AndLF2ThenUnsetRestoreByF2() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE2, null).setRestoreByF(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE2, null).setRestoreByF(false)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
        Assert.assertFalse(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE2, null)).isPresent()
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void updateLicensorCaseWhereLFThenDeleteLF() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifFThenL().find(new LFP(LICENSOR1, FRANCHISE1, null)).isPresent()
        );
    }
}
