package ru.yandex.market.wms.common.spring.dao;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotAttribute;
import ru.yandex.market.wms.common.spring.dao.implementation.LotAttributeDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LotAttributeDaoTest extends IntegrationTest {

    @Autowired
    private LotAttributeDao lotAttributeDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/after-save-surplus-with-put-away-class.xml",
            assertionMode = NON_STRICT)
    public void saveSurplusWithPutAwayClass() {
        save(true, "ALL");
    }

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/after-save-not-surplus-without-put-away-class.xml",
            assertionMode = NON_STRICT)
    public void saveNotSurplusWithoutPutAwayClass() {
        save(false, null);
    }

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/after-save-without-shelf-life-dates.xml",
            assertionMode = NON_STRICT)
    public void saveWithoutShelfLifeDates() {
        save(true, null, null, null);
    }

    @Test
    @DatabaseSetup("/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml",
            assertionMode = NON_STRICT)
    public void findShippedBySerialNumberWhenExistsOne() {
        Optional<LotAttribute> shippedBySerialNumber = lotAttributeDao.findShippedBySerialNumber("1234567890");
        assertions.assertThat(shippedBySerialNumber).isPresent();
        LotAttribute attributes = shippedBySerialNumber.get();
        assertAttributesExpected(attributes, "0000012345");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml",
            assertionMode = NON_STRICT)
    public void findShippedBySerialNumberWhenExistsTwo() {
        Optional<LotAttribute> shippedBySerialNumber = lotAttributeDao.findShippedBySerialNumber("1234567891");
        assertions.assertThat(shippedBySerialNumber).isPresent();
        LotAttribute attributes = shippedBySerialNumber.get();
        assertAttributesExpected(attributes, "0000012347");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/before-find-by-shipped-serial-number.xml",
            assertionMode = NON_STRICT)
    public void findShippedBySerialNumberWhenNotExists() {
        Optional<LotAttribute> shippedBySerialNumber = lotAttributeDao.findShippedBySerialNumber("1234567892");
        assertions.assertThat(shippedBySerialNumber).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/lot-attribute/before-find-by-serial-number.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/before-find-by-serial-number.xml", assertionMode = NON_STRICT)
    public void findBySerialNumberWhenExists() {
        Optional<LotAttribute> shippedBySerialNumber = lotAttributeDao.findBySerialNumber("1234567890");
        assertions.assertThat(shippedBySerialNumber).isPresent();
        LotAttribute attributes = shippedBySerialNumber.get();
        assertAttributesExpected(attributes, "0000012345");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-attribute/before-find-by-serial-number.xml")
    @ExpectedDatabase(value = "/db/dao/lot-attribute/before-find-by-serial-number.xml", assertionMode = NON_STRICT)
    public void findBySerialNumberWhenNotExists() {
        Optional<LotAttribute> shippedBySerialNumber = lotAttributeDao.findBySerialNumber("1234567892");
        assertions.assertThat(shippedBySerialNumber).isEmpty();
    }

    private void save(boolean surplus, @Nullable String putAwayClass) {
        Instant creationDateTime = Instant.parse("2020-04-17T12:00:00.000Z");
        Instant expirationDateTime = Instant.parse("2020-04-25T15:00:00.000Z");
        save(surplus, putAwayClass, creationDateTime, expirationDateTime);
    }

    private void save(boolean surplus, @Nullable String putAwayClass,
                      @Nullable Instant creationDateTime, @Nullable Instant expirationDateTime) {
        LotAttribute lotAttribute = LotAttribute.builder()
                .storerKey("465852")
                .sku("ROV0000000000000001456")
                .lot("0000012345")
                .expirationDateTime(expirationDateTime)
                .creationDateTime(creationDateTime)
                .surplus(surplus)
                .putAwayClass(putAwayClass)
                .addWho("TEST")
                .editWho("TEST")
                .originalLot("0000012345")
                .build();
        lotAttributeDao.save(lotAttribute);
    }

    private void assertAttributesExpected(LotAttribute attributes, String lot) {
        Instant creationDateTime = Instant.parse("2020-04-17T12:00:00.000Z");
        Instant expirationDateTime = Instant.parse("2020-04-25T15:00:00.000Z");
        assertions.assertThat(attributes.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(attributes.getSku()).isEqualTo("ROV0000000000000001456");
        assertions.assertThat(attributes.getLot()).isEqualTo(lot);
        assertions.assertThat(attributes.getOriginalLot()).isEqualTo(lot);
        assertions.assertThat(attributes.getExpirationDateTime()).isEqualTo(expirationDateTime);
        assertions.assertThat(attributes.getCreationDateTime()).isEqualTo(creationDateTime);
        assertions.assertThat(attributes.isSurplus()).isFalse();
        assertions.assertThat(attributes.getPutAwayClass()).isEqualTo("ALL");
        assertions.assertThat(attributes.getAddWho()).isEqualTo("TEST");
        assertions.assertThat(attributes.getEditWho()).isEqualTo("TEST");
    }
}
