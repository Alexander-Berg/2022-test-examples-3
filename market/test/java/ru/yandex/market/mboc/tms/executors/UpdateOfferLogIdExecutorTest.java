package ru.yandex.market.mboc.tms.executors;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferLogIdService;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author apluhin
 * @created 2/16/21
 */
public class UpdateOfferLogIdExecutorTest extends BaseDbTestClass {

    private UpdateOfferLogIdExecutor executor;
    private OfferLogIdService offerLogIdService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    TransactionHelper transactionHelper;

    @Before
    public void setUp() throws Exception {
        offerLogIdService = new OfferLogIdService(jdbcTemplate);
        executor = new UpdateOfferLogIdExecutor(offerLogIdService);
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
    }

    @Test
    public void whenNoRecordsShouldPass() {
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
        executor.execute();
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
    }

    @Test
    public void whenHasNewRecordsShouldUpdateAll() {
        int count = 512;
        List<Offer> offers = OfferTestUtils.generateTestOffers(count);

        offerRepository.insertOffers(offers);

        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(count);

        executor.execute();
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
    }

}
