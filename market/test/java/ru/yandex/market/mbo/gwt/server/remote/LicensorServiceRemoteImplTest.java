package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceMock;
import ru.yandex.market.mbo.db.params.GLRulesServiceInterface;
import ru.yandex.market.mbo.db.params.GLRulesServiceMock;
import ru.yandex.market.mbo.gwt.client.models.licensor.LicensorExtra;
import ru.yandex.market.mbo.gwt.client.models.licensor.LicensorRow;
import ru.yandex.market.mbo.gwt.client.models.licensor.LicensorRowFilter;
import ru.yandex.market.mbo.gwt.client.models.licensor.LicensorRowSorting;
import ru.yandex.market.mbo.gwt.client.models.licensor.vendor.LicensorVendorRow;
import ru.yandex.market.mbo.gwt.client.models.licensor.vendor.LicensorVendorRowFilter;
import ru.yandex.market.mbo.gwt.client.models.licensor.vendor.LicensorVendorRowSorting;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.licensor2.LicensorServiceImpl;
import ru.yandex.market.mbo.licensor2.name.NameLicensorServiceMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCaseDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorExtraDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorScheme;
import ru.yandex.market.mbo.licensor2.scheme.LicensorSchemeService;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraintDAOMock;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorVendorLinkUpdater;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author ayratgdl
 * @date 10.11.17
 */
public class LicensorServiceRemoteImplTest {
    private static final IdAndName LICENSOR1 = new IdAndName(101, "Лицензор 01");
    private static final IdAndName LICENSOR2 = new IdAndName(102, "Лицензор 02");
    private static final IdAndName FRANCHISE1 = new IdAndName(201, "Франшиза 01");
    private static final IdAndName FRANCHISE2 = new IdAndName(202, "Франшиза 02");
    private static final IdAndName FRANCHISE3 = new IdAndName(203, "Франшиза 03");
    private static final IdAndName FRANCHISE4 = new IdAndName(204, "Франшиза 04");
    private static final IdAndName FRANCHISE5 = new IdAndName(205, "Франшиза 05");
    private static final IdAndName PERSONAGE1 = new IdAndName(301, "Персонаж 01");
    private static final IdAndName PERSONAGE2 = new IdAndName(302, "Персонаж 02");
    private static final IdAndName PERSONAGE3 = new IdAndName(303, "Персонаж 03");
    private static final IdAndName PERSONAGE4 = new IdAndName(304, "Персонаж 04");
    private static final IdAndName PERSONAGE5 = new IdAndName(305, "Персонаж 05");
    private static final IdAndName VENDOR1 = new IdAndName(401, "Производитель 01");
    private static final IdAndName VENDOR2 = new IdAndName(402, "Производитель 02");
    private static final IdAndName CATEGORY1 = new IdAndName(501, "Категория 01");
    private static final IdAndName CATEGORY2 = new IdAndName(502, "Категория 02");
    private static final IdAndName CATEGORY3 = new IdAndName(503, "Категория 03");
    private static final long AUTO_UID = 0;
    private static final long UID1 = 401;

    private LicensorServiceRemoteImpl licensorServiceRemote;
    private LicensorServiceImpl licensorService;
    private NameLicensorServiceMock nameLicensorService;

    @Before
    public void setUp() {
        licensorServiceRemote = new LicensorServiceRemoteImpl();

        licensorService = new LicensorServiceImpl();

        LicensorSchemeService schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());
        licensorService.setSchemeService(schemeService);

        LicensorRuleOfRestoreUpdater ruleOfRestoreUpdater = new LicensorRuleOfRestoreUpdater();
        GLRulesServiceInterface glRulesService = new GLRulesServiceMock();
        ruleOfRestoreUpdater.setGlRulesService(glRulesService);
        ruleOfRestoreUpdater.setAutoUser(new AutoUser(AUTO_UID));
        ruleOfRestoreUpdater.init();
        licensorService.setRuleOfRestoreUpdater(ruleOfRestoreUpdater);

        LicensorRuleOfDeletedUpdater ruleOfDeletedUpdater = new LicensorRuleOfDeletedUpdater();
        ruleOfDeletedUpdater.setGlRulesService(glRulesService);
        ruleOfDeletedUpdater.setAutoUser(new AutoUser(AUTO_UID));
        licensorService.setRuleOfDeletedUpdater(ruleOfDeletedUpdater);

        LicensorRuleOfExtrasUpdater ruleOfExtrasUpdater = new LicensorRuleOfExtrasUpdater();
        ruleOfExtrasUpdater.setGlRulesService(glRulesService);
        ruleOfExtrasUpdater.setAutoUser(new AutoUser(AUTO_UID));
        licensorService.setRuleOfExtrasUpdater(ruleOfExtrasUpdater);

        LicensorVendorLinkUpdater lvLinkUpdater = new LicensorVendorLinkUpdater();
        ValueLinkServiceInterface valueLinkService = new ValueLinkServiceMock();
        lvLinkUpdater.setValueLinkService(valueLinkService);
        licensorService.setVendorLinkUpdater(lvLinkUpdater);

        licensorServiceRemote.setLicensorService(licensorService);

        nameLicensorService = new NameLicensorServiceMock();
        licensorServiceRemote.setNameLicensorService(nameLicensorService);
    }

    @Test
    public void loadLicensorRowsWhenEmptyData() {
        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());

        Assert.assertEquals(Collections.emptyList(), actualRows);
    }

    @Test
    public void loadLicensorRowsWhenLonelyLFP() {
        nameLicensorService
            .addLicensors(LICENSOR1, LICENSOR2)
            .addFranchises(FRANCHISE1, FRANCHISE2)
            .addPersonages(PERSONAGE1, PERSONAGE2);

        List<LicensorRow> expectedRows = Arrays.asList(
            new LicensorRow(LICENSOR1, null, null),
            new LicensorRow(LICENSOR2, null, null),
            new LicensorRow(null, FRANCHISE1, null),
            new LicensorRow(null, FRANCHISE2, null),
            new LicensorRow(null, null, PERSONAGE1),
            new LicensorRow(null, null, PERSONAGE2)
        );

        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());

        Assert.assertEquals(expectedRows, actualRows);
    }

    @Test
    public void loadLicensorFullExample() {
        nameLicensorService
            .addLicensors(LICENSOR1, LICENSOR2)
            .addFranchises(FRANCHISE1, FRANCHISE2, FRANCHISE3, FRANCHISE4, FRANCHISE5)
            .addPersonages(PERSONAGE1, PERSONAGE2, PERSONAGE3, PERSONAGE4, PERSONAGE5);

        List<LicensorRow> expectedRows = Arrays.asList(
            new LicensorRow(LICENSOR1, null, null),
            new LicensorRow(LICENSOR2, FRANCHISE2, null),
            new LicensorRow(LICENSOR2, FRANCHISE3, PERSONAGE1),
            new LicensorRow(LICENSOR2, FRANCHISE3, PERSONAGE2),
            new LicensorRow(LICENSOR2, FRANCHISE4, PERSONAGE2).setRestoreByFP(true).setRestoreByLP(true),
            new LicensorRow(LICENSOR2, FRANCHISE4, PERSONAGE3).setRestoreByP(true),
            new LicensorRow(null, FRANCHISE1, null),
            new LicensorRow(null, FRANCHISE5, PERSONAGE4),
            new LicensorRow(null, null, PERSONAGE5)
        );


        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE5.getId(), PERSONAGE4.getId()));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR2.getId(), FRANCHISE4.getId(), PERSONAGE3.getId()).setRestoreByP(true)
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR2.getId(), FRANCHISE4.getId(), PERSONAGE2.getId())
                .setRestoreByFP(true)
                .setRestoreByLP(true)
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR2.getId(), FRANCHISE3.getId(), PERSONAGE2.getId()));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR2.getId(), FRANCHISE3.getId(), PERSONAGE1.getId()));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR2.getId(), FRANCHISE2.getId(), null));


        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());

        Assert.assertEquals(expectedRows, actualRows);
    }

    @Test
    public void updateExtrasAddLicensorExtra() {
        nameLicensorService
            .addLicensors(LICENSOR1, LICENSOR2)
            .addFranchises(FRANCHISE1)
            .addPersonages(PERSONAGE1);

        licensorServiceRemote.addLicensorRow(new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1));
        licensorServiceRemote.updateLicensorRow(
            new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1)
            .addExtra(new LicensorExtra(LicensorExtra.Type.LICENSOR, LICENSOR2))
        );

        LicensorRow expectedL1F1P1Row =
            new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .addExtra(new LicensorExtra(LicensorExtra.Type.LICENSOR, LICENSOR2));

        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());
        LicensorRow actualL1F1P1Row = getRow(LICENSOR1, FRANCHISE1, PERSONAGE1, actualRows);

        Assert.assertEquals(expectedL1F1P1Row, actualL1F1P1Row);
    }

    @Test
    public void updateExtrasDeleteFranchiseExtra() {
        nameLicensorService
            .addLicensors(LICENSOR1)
            .addFranchises(FRANCHISE1, FRANCHISE2)
            .addPersonages(PERSONAGE1);

        licensorServiceRemote.addLicensorRow(
            new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .addExtra(new LicensorExtra(LicensorExtra.Type.FRANCHISE, FRANCHISE2))
        );

        licensorServiceRemote.updateLicensorRow(new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1));

        LicensorRow expectedL1F1P1Row = new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1);

        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());
        LicensorRow actualL1F1P1Row = getRow(LICENSOR1, FRANCHISE1, PERSONAGE1, actualRows);

        Assert.assertEquals(expectedL1F1P1Row, actualL1F1P1Row);
    }

    @Test
    public void updateExtrasChangeExtras() {
        nameLicensorService
            .addLicensors(LICENSOR1, LICENSOR2)
            .addFranchises(FRANCHISE1, FRANCHISE2)
            .addPersonages(PERSONAGE1, PERSONAGE2);

        LicensorRow oldL1F1P1Row =
            new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .addExtra(new LicensorExtra(LicensorExtra.Type.LICENSOR, LICENSOR2))
                .addExtra(new LicensorExtra(LicensorExtra.Type.PERSONAGE, PERSONAGE2));
        licensorServiceRemote.addLicensorRow(oldL1F1P1Row);

        LicensorRow newL1F1P1Row =
            new LicensorRow(LICENSOR1, FRANCHISE1, PERSONAGE1)
                .addExtra(new LicensorExtra(LicensorExtra.Type.FRANCHISE, FRANCHISE2))
                .addExtra(new LicensorExtra(LicensorExtra.Type.PERSONAGE, PERSONAGE2));
        licensorServiceRemote.updateLicensorRow(newL1F1P1Row);

        List<LicensorRow> actualRows = licensorServiceRemote
            .loadLicensorRows(0, Integer.MAX_VALUE, new LicensorRowFilter(), new LicensorRowSorting());
        LicensorRow actualL1F1P1Row = getRow(LICENSOR1, FRANCHISE1, PERSONAGE1, actualRows);

        Assert.assertEquals(newL1F1P1Row, actualL1F1P1Row);
    }

    @Test
    public void addVendorConstraint() {
        licensorServiceRemote.addLicensorVendorRow(UID1, LICENSOR1.getId(), VENDOR1.getId(), CATEGORY1.getId());
        LicensorScheme expectedScheme = new LicensorScheme()
            .addVendorConstraint(
                new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR1.getId(), CATEGORY1.getId(), UID1, Source.MBO_UI)
            );
        Assert.assertEquals(expectedScheme, licensorService.getLicensorScheme());
    }

    @Test
    public void loadLicensorVendorRowsWhenEmptyData() {
        List<LicensorVendorRow> actualRows =
            licensorServiceRemote.loadLicensorVendorRows(0, Integer.MAX_VALUE,
                                                         new LicensorVendorRowFilter(),
                                                         new LicensorVendorRowSorting());
        Assert.assertEquals(Collections.emptyList(), actualRows);

    }

    @Test
    public void loadLicensorVendorRows() {
        nameLicensorService
            .addLicensors(LICENSOR1)
            .addVendors(VENDOR1, VENDOR2)
            .addCategories(CATEGORY1, CATEGORY2, CATEGORY3);

        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR1.getId(), CATEGORY1.getId(),
                                         UID1, Source.UNKNOWN));
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR1.getId(), CATEGORY2.getId(),
                                         UID1, Source.MBO_UI));
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR1.getId(), CATEGORY3.getId(),
                                         UID1, Source.VENDOR_OFFICE));
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR2.getId(), CATEGORY1.getId(),
                                         UID1, Source.UNKNOWN_THROUGH_HTTP_API));
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR1.getId(), VENDOR2.getId(), CATEGORY2.getId(),
                                         UID1, Source.TOOL));

        List<LicensorVendorRow> expectedRows = Arrays.asList(
            new LicensorVendorRow().setLicensor(LICENSOR1).setVendor(VENDOR1).setCategory(CATEGORY1)
                .setSource(LicensorVendorRow.Source.UNKNOWN),
            new LicensorVendorRow().setLicensor(LICENSOR1).setVendor(VENDOR1).setCategory(CATEGORY2)
                .setSource(LicensorVendorRow.Source.MBO_UI),
            new LicensorVendorRow().setLicensor(LICENSOR1).setVendor(VENDOR1).setCategory(CATEGORY3)
                .setSource(LicensorVendorRow.Source.VENDOR_OFFICE),
            new LicensorVendorRow().setLicensor(LICENSOR1).setVendor(VENDOR2).setCategory(CATEGORY1)
                .setSource(LicensorVendorRow.Source.UNKNOWN),
            new LicensorVendorRow().setLicensor(LICENSOR1).setVendor(VENDOR2).setCategory(CATEGORY2)
                .setSource(LicensorVendorRow.Source.UNKNOWN)
        );

        List<LicensorVendorRow> actualRows =
            licensorServiceRemote.loadLicensorVendorRows(0, Integer.MAX_VALUE,
                                                         new LicensorVendorRowFilter(),
                                                         new LicensorVendorRowSorting());
        Assert.assertEquals(expectedRows, actualRows);

    }

    private static LicensorRow getRow(IdAndName licensor, IdAndName franchise, IdAndName personage,
                                      List<LicensorRow> rows) {
        for (LicensorRow row : rows) {
            if (Objects.equals(licensor, row.getLicensor())
                && Objects.equals(franchise, row.getFranchise())
                && Objects.equals(personage, row.getPersonage())) {

                return row;
            }
        }
        return null;
    }
}
