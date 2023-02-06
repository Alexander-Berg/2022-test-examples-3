package ru.yandex.market.mbo.licensor2;

import org.junit.Before;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedHelper;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasHelper;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreHelper;

import java.util.List;

/**
 * @author ayratgdl
 * @date 17.01.18
 */
public abstract class LicensorServiceImplBaseTest {
    protected static final Long LICENSOR1 = 101L;
    protected static final Long LICENSOR2 = 102L;
    protected static final Long FRANCHISE1 = 201L;
    protected static final Long FRANCHISE2 = 202L;
    protected static final Long PERSONAGE1 = 301L;
    protected static final Long PERSONAGE2 = 302L;
    protected static final Long VENDOR1 = 401L;
    protected static final Long CATEGORY1 = 501L;
    protected static final Long AUTO_UID = 0L;
    protected static final Long UID1 = 601L;

    protected LicensorServiceImpl licensorService;
    protected LicensorRuleOfDeletedHelper ruleOfDeletedHelper;
    protected LicensorRuleOfRestoreHelper ruleOfRestoreHelper;
    protected LicensorRuleOfExtrasHelper ruleOfExtrasHelper;
    protected ValueLinkServiceInterface valueLinkService;

    @Before
    public void setUp() {
        LicensorServiceMockBuilder mockBuilder = new LicensorServiceMockBuilder()
            .setAutoUid(AUTO_UID)
            .build();

        licensorService = mockBuilder.getLicensorService();

        valueLinkService = mockBuilder.getValueLinkService();

        ruleOfDeletedHelper = mockBuilder.getRuleOfDeletedHelper();

        ruleOfRestoreHelper = mockBuilder.getRuleOfRestoreHelper();

        ruleOfExtrasHelper = mockBuilder.getRuleOfExtrasHelper();
    }

    protected static GLRule clearId(GLRule rule) {
        rule.setId(0);
        return rule;
    }

    protected static List<ValueLink> clearIds(List<ValueLink> links) {
        links.forEach(link -> link.setId(null));
        return links;
    }
}
