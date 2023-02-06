package ru.yandex.market.mbo.licensor2.scheme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 12.02.18
 */
public class LicensorSchemeServiceExtrasTest {
    private static final Long LICENSOR1 = 101L;
    private static final Long LICENSOR2 = 102L;
    private static final Long FRANCHISE1 = 201L;
    private static final Long PERSONAGE1 = 301L;

    private LicensorSchemeService schemeService;
    private LicensorExtraDAO extraLfpDAO;

    @Before
    public void setUp() {
        schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        extraLfpDAO = new LicensorExtraDAOMock();
        schemeService.setExtraLfpDAO(extraLfpDAO);
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());
    }

    @Test
    public void createLicensorCaseWithExtras() {
        LicensorCase licensorCase = new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
            .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2));
        schemeService.createLicensorCase(licensorCase);

        List<LicensorCase> expectedLicensorCases = Arrays.asList(licensorCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void updateLicensorCaseWithExtras() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        LicensorCase updatedCase = new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
            .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2));
        schemeService.updateLicensorCase(updatedCase);

        List<LicensorCase> expectedLicensorCases = Arrays.asList(updatedCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void deleteLicensorCaseWithExtras() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
            .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertEquals(Collections.emptyMap(), extraLfpDAO.getAllExtras());
    }

    @Test
    public void updateExtrasToSingleL() {
        LicensorCase lSingleCase = new LicensorCase(LICENSOR1, null, null)
            .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2));

        schemeService.createLicensorCase(lSingleCase);

        List<LicensorCase> expectedLicensorCases = Arrays.asList(lSingleCase);
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void updateLicensorCaseWhereCaseIsSinglePAndDeleteLastExtra() {
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, null, null)
            .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, null, null));

        List<LicensorCase> expectedLicensorCases = Collections.emptyList();
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }

    @Test
    public void updateLicensorCaseWhereCasesAreLFPAndSinglePAndDeleteLastExtra() {
        schemeService.createLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        schemeService.createLicensorCase(
            new LicensorCase(LICENSOR1, null, null)
                .addExtra(new LicensorCase.Extra(LicensorCase.Extra.Type.LICENSOR, LICENSOR2))
        );
        schemeService.updateLicensorCase(new LicensorCase(LICENSOR1, null, null));

        List<LicensorCase> expectedLicensorCases = Arrays.asList(
            new LicensorCase(LICENSOR1, FRANCHISE1, null),
            new LicensorCase(LICENSOR1, null, null)
        );
        Assert.assertEquals(expectedLicensorCases, schemeService.getScheme().getAllLicensorCases());
    }
}
