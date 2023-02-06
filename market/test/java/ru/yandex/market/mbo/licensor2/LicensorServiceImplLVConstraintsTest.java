package ru.yandex.market.mbo.licensor2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkDefinition;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.licensor2.scheme.LFP;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 18.01.18
 */
public class LicensorServiceImplLVConstraintsTest extends LicensorServiceImplBaseTest {

    // Test method onCreateVendorConstraint

    @Test
    public void addLVConstrainToL() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void addLVConstraintToLFWithRestoreByF() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_FRANCHISE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.FRANCHISE_PARAM_ID, FRANCHISE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void addLVConstraintToLFPWithResultByFP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_FRANCHISE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.FRANCHISE_PARAM_ID, FRANCHISE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method onDeleteVendorConstraint

    @Test
    public void removeLVConstraintFromL() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.deleteVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Collections.emptyList();
        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void removeLVConstraintFromLFWithRestoreByF() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.deleteVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Collections.emptyList();
        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void removeLVConstraintFromLFPWithRestoreByFP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );
        licensorService.deleteVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Collections.emptyList();
        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseLFWithRestoreByFWhereExistsLVConstraint() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_FRANCHISE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.FRANCHISE_PARAM_ID, FRANCHISE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void createLicensorCaseLFWithoutRestoreByFWhereExistsLVConstraint() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void createLicensorCaseLFPWithRestoreByFPWhereExistsLVConstraint() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByFP(true)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_FRANCHISE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.FRANCHISE_PARAM_ID, FRANCHISE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void createLicensorCaseLFPWithoutRestoreByFPWhereExistsLVConstraint() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseLFSetRestoreByF() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(false)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_FRANCHISE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.FRANCHISE_PARAM_ID, FRANCHISE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void updateLicensorCaseLFUnsetRestoreByF() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(false)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method deleteLicensorCase

    @Test
    public void deleteLicensorCaseLF() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, null).setRestoreByF(true)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, null));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );
        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void deleteLicensorCaseLFP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1)
        );
        licensorService.deleteLicensorCase(new LFP(LICENSOR1, FRANCHISE1, PERSONAGE1));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );
        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }
}
