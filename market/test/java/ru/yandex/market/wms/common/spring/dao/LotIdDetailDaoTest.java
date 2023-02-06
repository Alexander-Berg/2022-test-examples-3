package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotIdDetail;
import ru.yandex.market.wms.common.spring.dao.entity.LotIdDetailId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LotIdDetailDaoTest extends IntegrationTest {

    @Autowired
    private LotIdDetailDao lotIdDetailDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/after-insert.xml", assertionMode = NON_STRICT)
    public void simpleInsert() {
        LotIdDetail detail = LotIdDetail.builder()
                .lotIdKey("0023117444")
                .lotIdLineNumber("00001")
                .serialNumber("6269914266")
                .sku("ROV0000000000000223330")
                .ioFlag("I")
                .lot("0000404946")
                .id("PLT0244413")
                .sourceKey("B002171137")
                .sourceLineNumber("00020")
                .pickDetailKey("0027528416")
                .quantity(BigDecimal.TEN)
                .addWho("TEST1")
                .editWho("TEST2")
                .build();
        lotIdDetailDao.insert(detail);
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/before-select.xml", assertionMode = NON_STRICT)
    public void findIdByLotIdKeyAndSerialNumberIfExists() {
        Optional<LotIdDetailId> maybeLotIdDetailId =
                lotIdDetailDao.findIdByLotIdKeyAndSerialNumber("0023117444", "6269914267");
        assertions.assertThat(maybeLotIdDetailId).isPresent();
        LotIdDetailId lotIdDetailId = maybeLotIdDetailId.get();
        assertions.assertThat(lotIdDetailId.getLotIdKey()).isEqualTo("0023117444");
        assertions.assertThat(lotIdDetailId.getLotIdLineNumber()).isEqualTo("00037");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/before-select.xml", assertionMode = NON_STRICT)
    public void findIdByLotIdKeyAndSerialNumberIfNotExists() {
        Optional<LotIdDetailId> maybeLotIdDetailId =
                lotIdDetailDao.findIdByLotIdKeyAndSerialNumber("0023117444", "6269914268");
        assertions.assertThat(maybeLotIdDetailId).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-clear.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/after-clear.xml", assertionMode = NON_STRICT)
    public void clearSerialNumber() {
        lotIdDetailDao.clearSerialNumber("6269914266", "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/after-add-quantity.xml", assertionMode = NON_STRICT)
    public void addToQuantity() {
        LotIdDetailId lotIdDetailId = LotIdDetailId.builder()
                .lotIdKey("0023117444")
                .lotIdLineNumber("00037")
                .build();
        lotIdDetailDao.addToQuantity(lotIdDetailId, 3, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/before-select.xml", assertionMode = NON_STRICT)
    public void getNextLotIdLineNumberWhenExists() {
        Optional<String> maybeNextLotIdLineNumber = lotIdDetailDao.getNextLotIdLineNumber("0023117444");
        assertions.assertThat(maybeNextLotIdLineNumber).isPresent();
        assertions.assertThat(maybeNextLotIdLineNumber.get()).isEqualTo("00038");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-detail/before-select.xml", assertionMode = NON_STRICT)
    public void getNextLotIdLineNumberWhenNotExists() {
        Optional<String> maybeNextLotIdLineNumber = lotIdDetailDao.getNextLotIdLineNumber("0023117445");
        assertions.assertThat(maybeNextLotIdLineNumber).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-detail/before-select.xml")
    public void getQtyPerBoxes() {
        Map<String, Integer> qtyPerBoxes =
                lotIdDetailDao.getQtyPerBoxes(List.of("PLT0244413"), SkuId.of("", "ROV0000000000000223330"));
        assertions.assertThat(qtyPerBoxes.get("PLT0244413")).isEqualTo(2);
    }
}
