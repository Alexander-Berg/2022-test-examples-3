package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplRuleOfRestoreIfPThenLFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereLFPWithoutRestoreByP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenLF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereLFPWithRestoreByP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifPThenLF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseWhereLFPThenSetRestoreByP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifPThenLF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereLFPThenUnsetRestoreByP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(false)
        );

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenLF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereLFPThenDeleteLFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenLF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }
}
