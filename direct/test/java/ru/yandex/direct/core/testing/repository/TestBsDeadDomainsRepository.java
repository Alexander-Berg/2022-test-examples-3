package ru.yandex.direct.core.testing.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStatusmetricacontrol;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BS_DEAD_DOMAINS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;

@Component
@ParametersAreNonnullByDefault
public class TestBsDeadDomainsRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestBsDeadDomainsRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void addDeadDomain(DomainInfo domainInfo) {
        dslContextProvider.ppc(domainInfo.getShard())
                .insertInto(BS_DEAD_DOMAINS)
                .columns(BS_DEAD_DOMAINS.DOMAIN_ID, BS_DEAD_DOMAINS.LAST_CHANGE)
                .values(DSL.val(domainInfo.getDomainId()), DSL.currentLocalDateTime())
                .execute();
    }

    public void setStatusMetricaControl(CampaignInfo campaignInfo, boolean value) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STATUS_METRICA_CONTROL, value
                        ? CampOptionsStatusmetricacontrol.Yes
                        : CampOptionsStatusmetricacontrol.No)
                .where(CAMP_OPTIONS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

}
