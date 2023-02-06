package ru.yandex.market.mbo.licensor2.scheme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * @author ayratgdl
 * @date 14.01.18
 */
public class LicensorSchemeServiceTest {
    private static final Long LICENSOR1 = 101L;
    private static final Long LICENSOR2 = 102L;
    private static final Long FRANCHISE1 = 201L;
    private static final Long PERSONAGE1 = 301L;
    private static final Long VENDOR1 = 401L;
    private static final Long CATEGORY1 = 501L;
    private static final Long UID1 = 601L;

    private LicensorSchemeService schemeService;

    @Before
    public void setUp() {
        schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());
    }

    // Test method createLicensorCase

    @Test
    public void createLicensorCase() {
        LicensorCase licensorCase =
            new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
                .setRestoreByFP(true)
                .setRestoreByLP(true);

        schemeService.createLicensorCase(licensorCase);

        List<LicensorCase> expectedAllLicensorCases = Arrays.asList(licensorCase);
        Assert.assertEquals(expectedAllLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    // Test method createLicensorCase. Exists LF when exists free F

    @Test
    public void createLicensorCaseWhereCasesAreLFPAndFP() {
        LicensorCase lfpCase = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        LicensorCase fpCase = new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1));
        LicensorCase lfCase = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        schemeService.createLicensorCase(lfpCase);
        schemeService.createLicensorCase(fpCase);

        TreeSet<LicensorCase> expectedLicensorCases = new TreeSet<>(Arrays.asList(lfpCase, fpCase, lfCase));
        Assert.assertEquals(expectedLicensorCases, new TreeSet<>(schemeService.getScheme().getAllLicensorCases()));
    }

    @Test
    public void createLicensorCaseWhereCasesAreFPAndLFP() {
        LicensorCase fpCase = new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1));
        LicensorCase lfpCase = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        LicensorCase lfCase = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        schemeService.createLicensorCase(fpCase);
        schemeService.createLicensorCase(lfpCase);

        TreeSet<LicensorCase> expectedLicensorCases = new TreeSet<>(Arrays.asList(fpCase, lfpCase, lfCase));
        Assert.assertEquals(expectedLicensorCases, new TreeSet<>(schemeService.getScheme().getAllLicensorCases()));
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCase() {
        LicensorCase initialLicensorCase =
            new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
                .setRestoreByFP(true);
        schemeService.createLicensorCase(initialLicensorCase);

        LicensorCase updatedLicensorCase =
            new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
                .setRestoreByFP(true)
                .setRestoreByLP(true);
        schemeService.updateLicensorCase(updatedLicensorCase);

        List<LicensorCase> expectedAllLicensorCases = Arrays.asList(updatedLicensorCase);
        Assert.assertEquals(expectedAllLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCase() {
        schemeService.createLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<LicensorCase> expectedLicensorCases = Collections.emptyList();
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    // Test createVendorConstraint

    @Test
    public void createLVConstraint() {
        LicensorVendorConstraint lvConstraint =
            new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI);
        schemeService.createVendorConstraint(lvConstraint);

        List<LicensorVendorConstraint> expectedAllLVConstrains = Arrays.asList(lvConstraint);
        Assert.assertEquals(expectedAllLVConstrains, schemeService.getScheme().getAllVendorConstraints());
    }

    // Test deleteVendorConstraint

    @Test
    public void deleteLVConstraint() {
        LicensorVendorConstraint lvConstraint =
            new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI);
        schemeService.createVendorConstraint(lvConstraint);

        schemeService.deleteVendorConstraint(lvConstraint);

        List<LicensorVendorConstraint> expectedAllLVConstrains = Collections.emptyList();
        Assert.assertEquals(expectedAllLVConstrains, schemeService.getScheme().getAllVendorConstraints());
    }

    // deleting single L when deleting last LFP with this L

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndL() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, null, null));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<LicensorCase> expectedLicensorCases = Collections.emptyList();
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndLWithExtras() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        LicensorCase lSingleCase =
            new LicensorCase(LICENSOR1, null, null)
                .setExtras(Arrays.asList(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2)));
        schemeService.createLicensorCase(lSingleCase);

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<LicensorCase> expectedLicensorCases = Arrays.asList(lSingleCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    // deleting single F when deleting last LFP with this F

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndF() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, FRANCHISE1, null));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        List<LicensorCase> expectedLicensorCases = Collections.emptyList();
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndFWithExtras() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        LicensorCase fSingleCase =
            new LicensorCase(null, FRANCHISE1, null)
                .setExtras(Arrays.asList(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2)));
        schemeService.createLicensorCase(fSingleCase);

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        List<LicensorCase> expectedLicensorCases = Arrays.asList(fSingleCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    // deleting single P when deleting last LFP with this P

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndP() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        schemeService.createLicensorCase(new LicensorCase(null, null, PERSONAGE1));
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<LicensorCase> expectedLicensorCases = Collections.emptyList();
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void deleteLicensorCaseWhereCaseAreLFPAndPWithExtras() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        LicensorCase pSingleCase =
            new LicensorCase(null, null, PERSONAGE1)
                .setExtras(Arrays.asList(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2)));
        schemeService.createLicensorCase(pSingleCase);

        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<LicensorCase> expectedLicensorCases = Arrays.asList(pSingleCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }
}
