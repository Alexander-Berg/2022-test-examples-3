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
public class LicensorServiceImplLVConstraintsForRestoreByPTest extends LicensorServiceImplBaseTest {

    // Test method onCreateVendorConstraint

    @Test
    public void addLVConstraintToLFPWithResultByP() {
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_PERSONAGE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.PERSONAGE_PARAM_ID, PERSONAGE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method onDeleteVendorConstraint

    @Test
    public void removeLVConstraintFromLFPWithRestoreByP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.deleteVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));

        List<ValueLink> expectedValueLinks = Collections.emptyList();
        Assert.assertEquals(expectedValueLinks,
                            valueLinkService.findValueLinks(new ValueLinkSearchCriteria())
        );
    }

    // Test method createLicensorCase

    @Test
    public void createLicensorCaseLFPWithRestoreByPWhereExistsLVConstraint() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_PERSONAGE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.PERSONAGE_PARAM_ID, PERSONAGE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    // Test method updateLicensorCase

    @Test
    public void updateLicensorCaseLFPSetRestoreByP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(false)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );

        List<ValueLink> expectedValueLinks = Arrays.asList(
            ValueLinkDefinition.VENDOR_LICENSOR_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.LICENSOR_PARAM_ID, LICENSOR1)
                .setCategoryId(CATEGORY1)
                .buildLink(),
            ValueLinkDefinition.VENDOR_PERSONAGE_LINK.newBuilder()
                .addValue(KnownIds.VENDOR_PARAM_ID, VENDOR1)
                .addValue(KnownIds.PERSONAGE_PARAM_ID, PERSONAGE1)
                .setCategoryId(CATEGORY1)
                .buildLink()
        );

        Assert.assertEquals(expectedValueLinks,
                            clearIds(valueLinkService.findValueLinks(new ValueLinkSearchCriteria()))
        );
    }

    @Test
    public void updateLicensorCaseLFUnsetRestoreByP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
        );
        licensorService.updateLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByF(false)
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
    public void deleteLicensorCaseLFP() {
        licensorService.createVendorConstraint(new LicensorVendorConstraint(LICENSOR1, VENDOR1, CATEGORY1,
                                                                            UID1, Source.MBO_UI));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR1, FRANCHISE1, PERSONAGE1).setRestoreByP(true)
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
