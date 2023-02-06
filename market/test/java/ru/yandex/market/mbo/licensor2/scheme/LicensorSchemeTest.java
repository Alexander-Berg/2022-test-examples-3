package ru.yandex.market.mbo.licensor2.scheme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;

import java.util.Arrays;
import java.util.List;

/**
 * @author ayratgdl
 * @date 19.01.18
 */
public class LicensorSchemeTest {
    private static final Long LICENSOR1 = 101L;
    private static final Long LICENSOR2 = 102L;
    private static final Long FRANCHISE1 = 201L;
    private static final Long FRANCHISE2 = 202L;
    private static final Long FRANCHISE3 = 203L;
    private static final Long PERSONAGE1 = 301L;
    private static final Long PERSONAGE2 = 302L;
    private static final Long PERSONAGE3 = 303L;
    private static final Long VENDOR1 = 401L;
    private static final Long VENDOR2 = 402L;
    private static final Long CATEGORY1 = 501L;
    private static final Long CATEGORY2 = 502L;
    private static final Long UID1 = 601L;

    private LicensorScheme scheme;

    @Before
    public void setUp() {
        scheme = new LicensorScheme();
    }

    // Test method getLicensorCase

    @Test
    public void getLicensorCase() {
        LicensorCase licensorCase = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        scheme.addLicensorCase(licensorCase);

        Assert.assertEquals(licensorCase,
                            scheme.getLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
        );
    }

    // Test method existsLicensorCase

    @Test
    public void existsLicensorCaseWithResultTrue() {
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));

        Assert.assertTrue(
            scheme.existsLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
        );
    }

    @Test
    public void existsLicensorCaseWithResultFalse() {
        Assert.assertFalse(
            scheme.existsLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1))
        );
    }

    // Test method getLicensorCasesByLicensor

    @Test
    public void getLicensorCasesByLicensor() {
        LicensorCase licensorCaseL1F1P1 = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        LicensorCase licensorCaseL1F1P2 = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2));
        LicensorCase licensorCaseL2F2P1 = new LicensorCase(new LFP(LICENSOR2, FRANCHISE2, PERSONAGE1));

        scheme
            .addLicensorCase(licensorCaseL1F1P1)
            .addLicensorCase(licensorCaseL1F1P2)
            .addLicensorCase(licensorCaseL2F2P1);

        List<LicensorCase> expectedLicensorCasesByLicensor1 =
            Arrays.asList(licensorCaseL1F1P1, licensorCaseL1F1P2);

        Assert.assertEquals(expectedLicensorCasesByLicensor1,
                            scheme.getLicensorCasesByLicensor(LICENSOR1)
        );
    }

    // Test method getLicensorCasesByFranchise

    @Test
    public void getLicensorCasesByFranchise() {
        LicensorCase licensorCaseL1F1P1 = new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));
        LicensorCase licensorCaseF1P1 = new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1));
        LicensorCase licensorCaseL1F2P1 = new LicensorCase(new LFP(LICENSOR1, FRANCHISE2, PERSONAGE1));
        scheme
            .addLicensorCase(licensorCaseL1F1P1)
            .addLicensorCase(licensorCaseF1P1)
            .addLicensorCase(licensorCaseL1F2P1);

        List<LicensorCase> expectedLicensorCasesByFranchiseF1 = Arrays.asList(
            licensorCaseL1F1P1,
            licensorCaseF1P1
        );

        Assert.assertEquals(expectedLicensorCasesByFranchiseF1,
                            scheme.getLicensorCasesByFranchise(FRANCHISE1)
        );
    }

    // Test method getLicensorByFranchise

    @Test
    public void getLicensorByFranchiseWithResultLicensor() {
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));
        scheme.addLicensorCase(new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1)));

        Assert.assertEquals(LICENSOR1, scheme.getLicensorByFranchise(FRANCHISE1));
    }

    @Test
    public void getLicensorByFranchiseWithResultNull() {
        Assert.assertEquals(null, scheme.getLicensorByFranchise(FRANCHISE1));
    }

    // Test method getFranchisesByLicensor

    @Test
    public void getFranchisesByLicensorTest() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, null));
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE2, PERSONAGE2));
        scheme.addLicensorCase(new LicensorCase(LICENSOR2, FRANCHISE3, PERSONAGE3));

        List<Long> expectedFranchises = Arrays.asList(FRANCHISE1, FRANCHISE2);
        Assert.assertEquals(expectedFranchises, scheme.getFranchisesByLicensor(LICENSOR1));
    }

    // Test method getPersonagesByLF

    @Test
    public void getPersonagesByLFWhereLicensorAndFranchiseAreNotNull() {
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE2)));
        scheme.addLicensorCase(new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE3)));

        List<Long> expectedPersonages = Arrays.asList(PERSONAGE1, PERSONAGE2);
        Assert.assertEquals(expectedPersonages, scheme.getPersonagesByLF(LICENSOR1, FRANCHISE1));
    }

    @Test
    public void getPersonagesByFLWhereLicensorIsNull() {
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));
        scheme.addLicensorCase(new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE2)));

        List<Long> expectedPersonages = Arrays.asList(PERSONAGE2);
        Assert.assertEquals(expectedPersonages, scheme.getPersonagesByLF(null, FRANCHISE1));
    }

    // getFranchisesByPersonage

    @Test
    public void getFranchisesByPersonage() {
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1)));
        scheme.addLicensorCase(new LicensorCase(new LFP(null, FRANCHISE1, PERSONAGE1)));
        scheme.addLicensorCase(new LicensorCase(new LFP(LICENSOR1, FRANCHISE2, PERSONAGE1)));

        List<Long> expectedFranchises = Arrays.asList(FRANCHISE1, FRANCHISE2);
        Assert.assertEquals(expectedFranchises, scheme.getFranchisesByPersonage(PERSONAGE1));
    }

    // Test method existsFreeFranchise

    @Test
    public void existsFreeFranchiseWhereNoExistsFree() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertFalse(scheme.existsFreeFranchise(FRANCHISE1));
    }

    @Test
    public void existsFreeFranchiseWhereExistsNoFree() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));
        scheme.addLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(scheme.existsFreeFranchise(FRANCHISE1));
    }

    @Test
    public void existsFreeFranchiseWhereNoExistsNoFree() {
        scheme.addLicensorCase(new LicensorCase(null, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(scheme.existsFreeFranchise(FRANCHISE1));
    }

    // Test hasLicensor

    @Test
    public void hasLicensor() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(scheme.hasLicensor(LICENSOR1));
    }

    // Test hasFranchise

    @Test
    public void hasFranchise() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(scheme.hasFranchise(FRANCHISE1));
    }

    // Test hasPersonage

    @Test
    public void hasPersonage() {
        scheme.addLicensorCase(new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1));

        Assert.assertTrue(scheme.hasPersonage(PERSONAGE1));
    }

    // getAllVendorConstraints

    @Test
    public void getAllVendorConstraints() {
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI));

        List<LicensorVendorConstraint> expectedConstrains = Arrays.asList(
            new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI)
        );
        Assert.assertEquals(expectedConstrains, scheme.getAllVendorConstraints()
        );
    }

    // Test method getVendorConstraintsByLicensor

    @Test
    public void getVendorConstraintsByLicensor() {
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI));
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR2, VENDOR1, CATEGORY1, UID1, Source.MBO_UI));
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR2, CATEGORY2, UID1, Source.MBO_UI));

        List<LicensorVendorConstraint> expectedConstrains = Arrays.asList(
            new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI),
            new LicensorVendorConstraint(LICENSOR1, VENDOR2, CATEGORY2, UID1, Source.MBO_UI)
        );
        Assert.assertEquals(expectedConstrains, scheme.getVendorConstraintsByLicensor(LICENSOR1));
    }

    // Test method getVendorIds

    @Test
    public void getVendorIds() {
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1, UID1, Source.MBO_UI));
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR2, VENDOR1, CATEGORY1, UID1, Source.MBO_UI));
        scheme.addVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR2, CATEGORY2, UID1, Source.MBO_UI));

        List<Long> expectedVendorIds = Arrays.asList(VENDOR1, VENDOR2);
        Assert.assertEquals(expectedVendorIds, scheme.getVendorIds());
    }
}
