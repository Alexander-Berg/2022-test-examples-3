package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;

/**
 * @author ayratgdl
 * @date 12.02.18
 */
public class LicensorServiceImplRuleOfExtrasTest extends LicensorServiceImplBaseTest {

    @Test
    public void createLicensorCaseWithExtraLicensor() {
        LFP lfp = new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1);
        LicensorCase.Extra extra = new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2);

        licensorService.createLicensorCase(new LicensorCase(lfp).addExtra(extra));

        Assert.assertEquals(ruleOfExtrasHelper.build(lfp, extra),
                            clearId(ruleOfExtrasHelper.find(lfp, extra).get())
        );
    }

    @Test
    public void updateLicensorCaseWithDeleteAdditionalLicensor() {
        LFP lfp = new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1);
        LicensorCase.Extra extra = new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2);

        licensorService.createLicensorCase(new LicensorCase(lfp).addExtra(extra));
        licensorService.updateLicensorCase(new LicensorCase(lfp));

        Assert.assertFalse(ruleOfExtrasHelper.find(lfp, extra).isPresent());
    }

    @Test
    public void updateLicensorCaseWithChangeExtras() {
        LFP lfp = new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1);
        LicensorCase.Extra extraL2 = new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2);
        LicensorCase.Extra extraF2 = new LicensorCase.Extra(LicensorCase.Extra.Type.FRANCHISE, FRANCHISE2);

        licensorService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorService.updateLicensorCase(new LicensorCase(lfp).addExtra(extraL2));
        licensorService.updateLicensorCase(new LicensorCase(lfp).addExtra(extraF2));

        Assert.assertFalse(ruleOfExtrasHelper.find(lfp, extraL2).isPresent());
        Assert.assertTrue(ruleOfExtrasHelper.find(lfp, extraF2).isPresent());
    }

    @Test
    public void deleteLicensorCaseWithExtras() {
        LFP lfp = new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1);
        LicensorCase.Extra extra = new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2);

        licensorService.createLicensorCase(new LicensorCase(lfp).addExtra(extra));
        licensorService.deleteLicensorCase(lfp);

        Assert.assertFalse(ruleOfExtrasHelper.find(lfp, extra).isPresent());
    }
}
