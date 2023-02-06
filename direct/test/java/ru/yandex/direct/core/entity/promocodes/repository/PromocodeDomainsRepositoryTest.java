package ru.yandex.direct.core.entity.promocodes.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.jooq.Record3;
import org.jooq.Record4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.promocodes.model.PromocodeClientDomain;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.TestUtils;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.dbschema.ppcdict.tables.PromocodeDomains.PROMOCODE_DOMAINS;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodeDomainsRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private PromocodeDomainsRepository repository;

    private static final String UNICODE_DOMAIN = "яндекс.рф";

    @Before
    public void addIrrelevantData() {
        String code = TestUtils.randomName("", 20).toUpperCase();
        String domain = TestUtils.randomName("", 40);
        long clientIdLong = nextPositiveInteger();

        dslContextProvider.ppcdict().insertInto(PROMOCODE_DOMAINS)
                .set(PROMOCODE_DOMAINS.PROMOCODE, code)
                .set(PROMOCODE_DOMAINS.DOMAIN, domain)
                .set(PROMOCODE_DOMAINS.CLIENT_ID, clientIdLong)
                .execute();
    }

    @Test
    public void getPromocodeDomain_unicodeDomain_checkFields() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        long clientIdLong = nextPositiveInteger();

        dslContextProvider.ppcdict().insertInto(PROMOCODE_DOMAINS)
                .set(PROMOCODE_DOMAINS.PROMOCODE, code)
                .set(PROMOCODE_DOMAINS.DOMAIN, UNICODE_DOMAIN)
                .set(PROMOCODE_DOMAINS.CLIENT_ID, clientIdLong)
                .execute();
        PromocodeClientDomain promocodeClientDomain = repository.getPromocodeDomains(List.of(code)).get(code);
        assertNotNull(promocodeClientDomain);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(promocodeClientDomain.getDomain()).isEqualTo(UNICODE_DOMAIN);
            softly.assertThat(promocodeClientDomain.getClientId()).isEqualTo(ClientId.fromLong(clientIdLong));
        });
    }

    @Test
    public void getPromocodeDomain_multipleRecords_checkFields() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        String code2 = TestUtils.randomName("", 14).toUpperCase();
        long clientIdLong = nextPositiveInteger();
        long clientIdLong2 = nextPositiveInteger();
        String domain = TestUtils.randomName("", 40);

        dslContextProvider.ppcdict().insertInto(PROMOCODE_DOMAINS)
                .columns(PROMOCODE_DOMAINS.PROMOCODE, PROMOCODE_DOMAINS.DOMAIN, PROMOCODE_DOMAINS.CLIENT_ID)
                .values(code, domain, clientIdLong)
                .values(code2, UNICODE_DOMAIN, clientIdLong2)
                .execute();
        Map<String, PromocodeClientDomain> promocodeDomains = repository.getPromocodeDomains(List.of(code, code2));
        assertTrue(promocodeDomains.containsKey(code));
        assertTrue(promocodeDomains.containsKey(code2));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(promocodeDomains.get(code).getDomain()).isEqualTo(domain);
            softly.assertThat(promocodeDomains.get(code).getClientId()).isEqualTo(ClientId.fromLong(clientIdLong));
            softly.assertThat(promocodeDomains.get(code2).getDomain()).isEqualTo(UNICODE_DOMAIN);
            softly.assertThat(promocodeDomains.get(code2).getClientId()).isEqualTo(ClientId.fromLong(clientIdLong2));
        });
    }

    @Test
    public void deleteOldPromocodeDomains_newRecord_notDeleted() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        long clientIdLong = nextPositiveInteger();

        dslContextProvider.ppcdict().insertInto(PROMOCODE_DOMAINS)
                .set(PROMOCODE_DOMAINS.PROMOCODE, code)
                .set(PROMOCODE_DOMAINS.DOMAIN, UNICODE_DOMAIN)
                .set(PROMOCODE_DOMAINS.CLIENT_ID, clientIdLong)
                .execute();
        int count = repository.deleteOldPromocodeDomains(LocalDateTime.now().minusDays(1));
        PromocodeClientDomain promocodeClientDomain = repository.getPromocodeDomains(List.of(code)).get(code);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(count).isEqualTo(0);
            softly.assertThat(promocodeClientDomain).isNotNull();
        });
    }

    @Test
    public void deleteOldPromocodeDomains_oldRecord_deleted() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        long clientIdLong = nextPositiveInteger();

        dslContextProvider.ppcdict().insertInto(PROMOCODE_DOMAINS)
                .set(PROMOCODE_DOMAINS.PROMOCODE, code)
                .set(PROMOCODE_DOMAINS.DOMAIN, UNICODE_DOMAIN)
                .set(PROMOCODE_DOMAINS.CLIENT_ID, clientIdLong)
                .set(PROMOCODE_DOMAINS.IMPORT_TIME, LocalDateTime.now().minusDays(2))
                .execute();
        int count = repository.deleteOldPromocodeDomains(LocalDateTime.now().minusDays(1));
        PromocodeClientDomain promocodeClientDomain = repository.getPromocodeDomains(List.of(code)).get(code);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(count).isEqualTo(1);
            softly.assertThat(promocodeClientDomain).isNull();
        });
    }

    @Test
    public void addPromocodeDomain_newRecord_checkFields() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        long clientIdLong = nextPositiveInteger();
        repository.addPromocodeDomain(new PromocodeClientDomain()
                .withPromocode(code)
                .withDomain(UNICODE_DOMAIN)
                .withClientId(ClientId.fromLong(clientIdLong))
        );
        Record3<String, String, Long> record =
                dslContextProvider.ppcdict().select(PROMOCODE_DOMAINS.PROMOCODE,
                        PROMOCODE_DOMAINS.DOMAIN,
                        PROMOCODE_DOMAINS.CLIENT_ID)
                        .from(PROMOCODE_DOMAINS)
                        .where(PROMOCODE_DOMAINS.PROMOCODE.eq(code)).fetchOne();
        assertNotNull(record);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(record.value1()).isEqualTo(code);
            softly.assertThat(record.value2()).isEqualTo(UNICODE_DOMAIN);
            softly.assertThat(record.value3()).isEqualTo(clientIdLong);
        });
    }

    @Test
    public void addPromocodeDomain_sameCode_checkUpdatedFields() {
        String code = TestUtils.randomName("", 16).toUpperCase();
        long clientIdLong = nextPositiveInteger();
        repository.addPromocodeDomain(new PromocodeClientDomain()
                .withPromocode(code)
                .withDomain(UNICODE_DOMAIN)
                .withClientId(ClientId.fromLong(clientIdLong))
        );
        LocalDateTime oldTime = LocalDateTime.now().minusMinutes(15);
        dslContextProvider.ppcdict().update(PROMOCODE_DOMAINS)
                .set(PROMOCODE_DOMAINS.IMPORT_TIME, oldTime)
                .where(PROMOCODE_DOMAINS.PROMOCODE.eq(code))
                .execute();

        String domain = TestUtils.randomName("", 30);
        long clientIdLong2 = nextPositiveInteger();
        repository.addPromocodeDomain(new PromocodeClientDomain()
                .withPromocode(code)
                .withDomain(domain)
                .withClientId(ClientId.fromLong(clientIdLong2))
        );
        Record4<String, String, Long, LocalDateTime> record = dslContextProvider.ppcdict()
                .select(PROMOCODE_DOMAINS.PROMOCODE,
                        PROMOCODE_DOMAINS.DOMAIN,
                        PROMOCODE_DOMAINS.CLIENT_ID,
                        PROMOCODE_DOMAINS.IMPORT_TIME)
                        .from(PROMOCODE_DOMAINS)
                        .where(PROMOCODE_DOMAINS.PROMOCODE.eq(code)).fetchOne();
        assertNotNull(record);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(record.value1()).isEqualTo(code);
            softly.assertThat(record.value2()).isEqualTo(domain);
            softly.assertThat(record.value3()).isEqualTo(clientIdLong2);
            softly.assertThat(record.value4())
                    .isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(3, MINUTES));
        });
    }
}
