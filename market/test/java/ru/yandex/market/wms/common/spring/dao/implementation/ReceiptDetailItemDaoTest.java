package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailItem;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ReceiptDetailItemDaoTest extends IntegrationTest {

    @Autowired
    ReceiptDetailItemDao receiptDetailItemDao;

    @Test
    @DatabaseSetup("/db/dao/receipt-detail-item/before.xml")
    public void insertAndFindTest() {
        String uuid = "1";
        String receiptKey = "0000000101";
        final ReceiptDetailItem expected = ReceiptDetailItem.builder()
                .uuid(uuid)
                .receiptDetailKey(new ReceiptDetailKey("0000000101", "00002"))
                .returnReasonId("returnReasonId")
                .returnReason("comment1,comment2")
                .editDate(LocalDateTime.of(2021, Month.FEBRUARY, 2,
                        14, 59, 27).toInstant(ZoneOffset.ofHours(3))
                )
                .addDate(LocalDateTime.of(2021, Month.FEBRUARY, 2,
                        1, 55, 27).toInstant(ZoneOffset.ofHours(3)))
                .addWho("tester")
                .editWho("tester")
                .build();
        receiptDetailItemDao.insert(singleton(expected));

        final Optional<ReceiptDetailItem> receiptDetailItem = receiptDetailItemDao.findByUuid(uuid);
        assertThat("identity is found", receiptDetailItem.isPresent());
        ReceiptDetailItem actual = receiptDetailItem.get();
        assertEquals(uuid, actual.getUuid());
        assertThat("add who", actual.getAddWho(), equalTo("tester"));
        assertThat("edit who", actual.getEditWho(), equalTo("tester"));
        assertThat("receipt key", actual.getReceiptDetailKey().getReceiptKey(), equalTo(receiptKey));

    }

    @Test
    void emptyOrNull() {
        assertNotNull(receiptDetailItemDao.findByReceiptDetailKey(null));
        assertThat(receiptDetailItemDao.findByReceiptDetailKey(null), notNullValue());
        receiptDetailItemDao.insert(emptySet());
        receiptDetailItemDao.insert(null);
    }
}
