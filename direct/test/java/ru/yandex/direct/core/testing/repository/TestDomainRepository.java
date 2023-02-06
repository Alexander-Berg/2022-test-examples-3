package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.model.FilterDomain;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.tables.Domains.DOMAINS;
import static ru.yandex.direct.dbschema.ppc.tables.FilterDomain.FILTER_DOMAIN;
import static ru.yandex.direct.dbschema.ppcdict.tables.ApiDomainStat.API_DOMAIN_STAT;
import static ru.yandex.direct.dbschema.ppcdict.tables.DomainsDict.DOMAINS_DICT;
import static ru.yandex.direct.dbschema.ppcdict.tables.Mirrors.MIRRORS;
import static ru.yandex.direct.dbschema.ppcdict.tables.MirrorsCorrection.MIRRORS_CORRECTION;

@Repository
@QueryWithoutIndex("тестовый репозиторий")
public class TestDomainRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void addToPpc(int shard, Domain domain) {
        dslContextProvider.ppc(shard)
                .insertInto(DOMAINS)
                .set(DOMAINS.DOMAIN_ID, domain.getId())
                .set(DOMAINS.DOMAIN, domain.getDomain())
                .set(DOMAINS.REVERSE_DOMAIN, domain.getReverseDomain())
                .execute();
    }

    public Long addToPpcDict(String domain) {
        return dslContextProvider.ppcdict()
                .insertInto(DOMAINS_DICT)
                .set(DOMAINS_DICT.DOMAIN, domain)
                .returning()
                .fetchOne()
                .getDomainId();
    }

    public Map<String, Long> getDomainIdByDomain(int shard, List<String> domains) {
        return dslContextProvider.ppc(shard)
                .select(DOMAINS.DOMAIN, DOMAINS.DOMAIN_ID)
                .from(DOMAINS)
                .where(DOMAINS.DOMAIN.in(domains))
                .fetchMap(DOMAINS.DOMAIN, DOMAINS.DOMAIN_ID);
    }

    public void deleteDomains(int shard, Collection<String> domains) {
        dslContextProvider.ppcdict()
                .deleteFrom(DOMAINS_DICT)
                .where(DOMAINS_DICT.DOMAIN.in(domains))
                .execute();
        dslContextProvider.ppc(shard)
                .deleteFrom(DOMAINS)
                .where(DOMAINS.DOMAIN.in(domains))
                .execute();
    }

    public void deleteDomains(int shard) {
        dslContextProvider.ppcdict()
                .deleteFrom(DOMAINS_DICT)
                .execute();
        dslContextProvider.ppc(shard)
                .deleteFrom(DOMAINS)
                .execute();
    }

    public static Domain testDomain() {
        String domain = "test_" + RandomStringUtils.randomAlphanumeric(6);
        return new Domain()
                .withDomain(domain)
                .withReverseDomain(StringUtils.reverse(domain));
    }

    public void addFilterDomains(int shard, List<FilterDomain> filterDomains) {
        if (filterDomains.isEmpty()) {
            return;
        }
        for (FilterDomain filterDomain : filterDomains) {
            dslContextProvider.ppc(shard)
                    .insertInto(FILTER_DOMAIN,
                            FILTER_DOMAIN.DOMAIN,
                            FILTER_DOMAIN.FILTER_DOMAIN_)
                    .values(filterDomain.getDomain(),
                            filterDomain.getFilterDomain())
                    .onDuplicateKeyUpdate()
                    .set(FILTER_DOMAIN.FILTER_DOMAIN_, MySQLDSL.values(FILTER_DOMAIN.FILTER_DOMAIN_))
                    .execute();
        }
    }

    public Map<String, String> getFilterDomains(int shard, List<String> domains) {
        return dslContextProvider.ppc(shard)
                .select(FILTER_DOMAIN.DOMAIN, FILTER_DOMAIN.FILTER_DOMAIN_)
                .from(FILTER_DOMAIN)
                .where(FILTER_DOMAIN.DOMAIN.in(domains))
                .fetchMap(FILTER_DOMAIN.DOMAIN, FILTER_DOMAIN.FILTER_DOMAIN_);
    }

    public void addDomainStat(String domain, Long showsApprox) {
        dslContextProvider.ppcdict()
                .insertInto(API_DOMAIN_STAT)
                .set(API_DOMAIN_STAT.FILTER_DOMAIN, domain)
                .set(API_DOMAIN_STAT.SHOWS_APPROX, showsApprox)
                .execute();
    }

    public void addMainMirror(String domain, String mirror) {
        dslContextProvider.ppcdict()
                .insertInto(MIRRORS, MIRRORS.DOMAIN, MIRRORS.MIRROR)
                .values(domain, mirror)
                .onDuplicateKeyUpdate()
                .set(MIRRORS.MIRROR, MySQLDSL.values(MIRRORS.MIRROR))
                .execute();
    }

    public void addMirrorCorrection(String domain, String mirror) {
        dslContextProvider.ppcdict()
                .insertInto(MIRRORS_CORRECTION, MIRRORS_CORRECTION.DOMAIN, MIRRORS_CORRECTION.REDIRECT_DOMAIN)
                .values(domain, mirror)
                .onDuplicateKeyUpdate()
                .set(MIRRORS_CORRECTION.REDIRECT_DOMAIN, MySQLDSL.values(MIRRORS_CORRECTION.REDIRECT_DOMAIN))
                .execute();
    }

    public void deleteDomainStat(String domain) {
        dslContextProvider.ppcdict()
                .deleteFrom(API_DOMAIN_STAT)
                .where(API_DOMAIN_STAT.FILTER_DOMAIN.eq(domain))
                .execute();
    }

    public void deleteMainMirror(String domain) {
        dslContextProvider.ppcdict()
                .deleteFrom(MIRRORS)
                .where(MIRRORS.DOMAIN.eq(domain))
                .execute();
    }

    public void deleteFilterDomain(int shard, String domain) {
        dslContextProvider.ppc(shard)
                .deleteFrom(FILTER_DOMAIN)
                .where(FILTER_DOMAIN.DOMAIN.eq(domain))
                .execute();
    }
}
