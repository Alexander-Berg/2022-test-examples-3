package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplRuleOfRestoreIfPThenFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereFPWithoutRestoreByP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenF().find(new LFP(null, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereFPWithRestoreByP() {
        licensorService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifPThenF().find(new LFP(null, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseWhereFPThenSetRestoreByP() {
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));
        licensorService.updateLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifPThenF().find(new LFP(null, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void updateLicensorCaseWhereFPThenUnsetRestoreByP() {
        licensorService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(false)
        );

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenF().find(new LFP(null, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereFPThenDeleteFP() {
        licensorService.createLicensorCase(
            new LicensorCase(null, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.deleteLicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifPThenF().find(new LFP(null, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }
}
