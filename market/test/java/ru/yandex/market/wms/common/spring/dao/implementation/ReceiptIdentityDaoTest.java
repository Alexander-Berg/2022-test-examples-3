package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailIdentity;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailIdentity.PK;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ReceiptIdentityDaoTest extends IntegrationTest {

    private final ReceiptDetailIdentityDao dao;
    private final UuidGenerator uuidGenerator;

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    @DatabaseSetup(value = "/db/dao/receipt-detail-identity/before.xml", connection = "wmwhseConnection", type =
            DatabaseOperation.REFRESH)
    void createAndFindByPK() {
        String receiptKey = "0000000101";
        final ReceiptDetailIdentity expected = ReceiptDetailIdentity.builder()
                .pk(PK.builder()
                        .type(TypeOfIdentity.of("CIS"))
                        .receiptDetailKey(new ReceiptDetailKey(receiptKey, "00001"))
                        .value("abc")
                        .build())
                .uuid(uuidGenerator.generate().toString())
                .addDate(LocalDateTime.of(2021, Month.FEBRUARY, 2,
                        1, 55, 27).toInstant(ZoneOffset.ofHours(3))
                )
                .editDate(LocalDateTime.of(2021, Month.FEBRUARY, 2,
                        14, 59, 27).toInstant(ZoneOffset.ofHours(3))
                )
                .build();
        dao.insert(singleton(expected));

        PK expectedIdentityPK = expected.getPk();
        final Optional<ReceiptDetailIdentity> receiptDetailIdentity = dao.findByPrimaryKey(expectedIdentityPK);
        assertThat("identity is found", receiptDetailIdentity.isPresent());
        ReceiptDetailIdentity actualIdentity = receiptDetailIdentity.get();
        assertEquals(expectedIdentityPK, actualIdentity.getPk());
        assertThat("add who", actualIdentity.getAddWho(), equalTo("tester"));
        assertThat("edit who", actualIdentity.getEditWho(), equalTo("tester"));
        assertThat("uuid", actualIdentity.getUuid(), equalTo(expected.getUuid()));
    }

    @Test
    void emptyOrNull() {
        assertNotNull(dao.findByPrimaryKey(null));
        assertThat(dao.findByPrimaryKey(null), notNullValue());
        dao.insert(emptySet());
        dao.insert(null);
        assertEquals(0, dao.deleteByReceiptKey(null));
    }
}
