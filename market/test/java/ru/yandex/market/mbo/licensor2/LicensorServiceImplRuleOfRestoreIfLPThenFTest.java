package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplRuleOfRestoreIfLPThenFTest extends LicensorServiceImplBaseTest {

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseWhereLFPWithoutRestoreByLP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifLPThenF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereLFPWithRestoreByLP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByLP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifLPThenF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method updateLicensorCase

    @Test
    public void createLicensorCaseWhereLPThenSetRestoreByLP() {
        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByLP(true)
        );

        Assert.assertTrue(
            ruleOfRestoreHelper.ifLPThenF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    @Test
    public void createLicensorCaseWhereLPThenUnsetRestoreByLP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByLP(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByLP(false)
        );

        Assert.assertFalse(
            ruleOfRestoreHelper.ifLPThenF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseWhereLFPThenDeleteLFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByLP(true)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(
            ruleOfRestoreHelper.ifLPThenF().find(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)).isPresent()
        );
    }
}
