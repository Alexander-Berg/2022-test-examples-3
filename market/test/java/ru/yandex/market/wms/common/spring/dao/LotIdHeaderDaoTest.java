package ru.yandex.market.wms.common.spring.dao;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.LotIdHeader;
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdHeaderDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LotIdHeaderDaoTest extends IntegrationTest {

    @Autowired
    private LotIdHeaderDao lotIdHeaderDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-header/after-insert.xml", assertionMode = NON_STRICT)
    public void simpleInsert() {
        LotIdHeader header = LotIdHeader.builder()
                .lotIdKey("0023117444")
                .storerKey("465872")
                .sku("ROV0000000000000223330")
                .ioFlag("I")
                .lot("0000404946")
                .id("PLT0244413")
                .sourceKey("B002171137")
                .sourceLineNumber("00020")
                .pickDetailKey("0027528416")
                .status("9")
                .addWho("TEST1")
                .editWho("TEST2")
                .build();
        lotIdHeaderDao.insert(header);
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-header/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-header/before-select.xml", assertionMode = NON_STRICT)
    public void findWithoutPickDetailWhenExists() {
        Optional<LotIdHeader> maybeLotIdHeader = lotIdHeaderDao.findWithoutPickDetailAndLock("0000404947",
                "PLT0244415");
        assertions.assertThat(maybeLotIdHeader).isPresent();
        LotIdHeader lotIdHeader = maybeLotIdHeader.get();
        assertions.assertThat(lotIdHeader.getLotIdKey()).isEqualTo("0023117445");
        assertions.assertThat(lotIdHeader.getStorerKey()).isEqualTo("465872");
        assertions.assertThat(lotIdHeader.getSku()).isEqualTo("ROV0000000000000223330");
        assertions.assertThat(lotIdHeader.getIoFlag()).isEqualTo("I");
        assertions.assertThat(lotIdHeader.getLot()).isEqualTo("0000404947");
        assertions.assertThat(lotIdHeader.getId()).isEqualTo("PLT0244415");
        assertions.assertThat(lotIdHeader.getSourceKey()).isEqualTo("B002171138");
        assertions.assertThat(lotIdHeader.getSourceLineNumber()).isEqualTo("00025");
        assertions.assertThat(lotIdHeader.getPickDetailKey()).isNull();
        assertions.assertThat(lotIdHeader.getStatus()).isEqualTo("5");
        assertions.assertThat(lotIdHeader.getAddWho()).isEqualTo("TEST1");
        assertions.assertThat(lotIdHeader.getEditWho()).isEqualTo("TEST2");
    }

    @Test
    @DatabaseSetup("/db/dao/lot-id-header/before-select.xml")
    @ExpectedDatabase(value = "/db/dao/lot-id-header/before-select.xml", assertionMode = NON_STRICT)
    public void findWithoutPickDetailWhenNotExists() {
        Optional<LotIdHeader> maybeLotIdHeader = lotIdHeaderDao.findWithoutPickDetailAndLock("0000404946",
                "PLT0244413");
        assertions.assertThat(maybeLotIdHeader).isNotPresent();
    }
}
