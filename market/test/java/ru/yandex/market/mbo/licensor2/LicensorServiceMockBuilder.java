package ru.yandex.market.mbo.licensor2;

import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceMock;
import ru.yandex.market.mbo.db.params.GLRulesServiceInterface;
import ru.yandex.market.mbo.db.params.GLRulesServiceMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCaseDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorExtraDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorSchemeService;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraintDAOMock;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedHelper;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasHelper;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreHelper;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorVendorLinkUpdater;
import ru.yandex.market.mbo.user.AutoUser;

/**
 * @author ayratgdl
 * @since 01.10.18
 */
public class LicensorServiceMockBuilder {
    private LicensorServiceImpl licensorService;
    private LicensorRuleOfDeletedHelper ruleOfDeletedHelper;
    private LicensorRuleOfRestoreHelper ruleOfRestoreHelper;
    private LicensorRuleOfExtrasHelper ruleOfExtrasHelper;
    private ValueLinkServiceInterface valueLinkService;
    private long autoUid = 0;

    public LicensorServiceMockBuilder setAutoUid(long autoUid) {
        this.autoUid = autoUid;
        return this;
    }

    public LicensorServiceMockBuilder build() {
        licensorService = new LicensorServiceImpl();

        LicensorSchemeService schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());
        licensorService.setSchemeService(schemeService);

        AutoUser autoUser = new AutoUser(autoUid);

        LicensorRuleOfRestoreUpdater ruleOfRestoreUpdater = new LicensorRuleOfRestoreUpdater();
        GLRulesServiceInterface glRulesService = new GLRulesServiceMock();
        ruleOfRestoreUpdater.setGlRulesService(glRulesService);
        ruleOfRestoreUpdater.setAutoUser(autoUser);
        ruleOfRestoreUpdater.init();
        licensorService.setRuleOfRestoreUpdater(ruleOfRestoreUpdater);

        LicensorRuleOfDeletedUpdater ruleOfDeletedUpdater = new LicensorRuleOfDeletedUpdater();
        ruleOfDeletedUpdater.setGlRulesService(glRulesService);
        ruleOfDeletedUpdater.setAutoUser(autoUser);
        licensorService.setRuleOfDeletedUpdater(ruleOfDeletedUpdater);

        LicensorRuleOfExtrasUpdater ruleOfAdditionUpdater = new LicensorRuleOfExtrasUpdater();
        ruleOfAdditionUpdater.setGlRulesService(glRulesService);
        ruleOfAdditionUpdater.setAutoUser(autoUser);
        licensorService.setRuleOfExtrasUpdater(ruleOfAdditionUpdater);

        LicensorVendorLinkUpdater lvLinkUpdater = new LicensorVendorLinkUpdater();
        valueLinkService = new ValueLinkServiceMock();
        lvLinkUpdater.setValueLinkService(valueLinkService);
        licensorService.setVendorLinkUpdater(lvLinkUpdater);

        ruleOfDeletedHelper = new LicensorRuleOfDeletedHelper();
        ruleOfDeletedHelper.setGlRulesService(glRulesService);

        ruleOfRestoreHelper = new LicensorRuleOfRestoreHelper();
        ruleOfRestoreHelper.setGlRulesService(glRulesService);

        ruleOfExtrasHelper = new LicensorRuleOfExtrasHelper();
        ruleOfExtrasHelper.setGlRulesService(glRulesService);

        return this;
    }

    public LicensorServiceImpl getLicensorService() {
        return licensorService;
    }

    public LicensorRuleOfDeletedHelper getRuleOfDeletedHelper() {
        return ruleOfDeletedHelper;
    }

    public LicensorRuleOfRestoreHelper getRuleOfRestoreHelper() {
        return ruleOfRestoreHelper;
    }

    public LicensorRuleOfExtrasHelper getRuleOfExtrasHelper() {
        return ruleOfExtrasHelper;
    }

    public ValueLinkServiceInterface getValueLinkService() {
        return valueLinkService;
    }
}
