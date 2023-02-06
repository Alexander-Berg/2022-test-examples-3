package ru.yandex.market.gutgin.tms.service.goodcontent;

import Market.DataCamp.DataCampOffer;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.gutgin.tms.service.SskuLockService;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;
import ru.yandex.market.partner.content.common.db.dao.SskuLockDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DataBucket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.SskuLock;
import ru.yandex.market.partner.content.common.entity.goodcontent.SkuTicket;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SskuLockServiceTest extends BaseDbGutGinTest {

    private static final Long CATEGORY_ID = 999L;
    private static final Integer PARTNER_SHOP_ID_DEFAULT = 123;
    private static final Integer SOURCE_ID = 456;

    @Autowired
    private GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    private SskuLockService sskuLockService;

    @Autowired
    private SskuLockDao sskuLockDao;

    @Test
    public void test() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID_DEFAULT);
        List<GcSkuTicket> gcSkuTickets1 = createTickets("1", "2");
        List<GcSkuTicket> gcSkuTickets2 = createTickets("2", "3");

        sskuLockService.insert(getSskuLocks(gcSkuTickets1));
        sskuLockService.insert(getSskuLocks(gcSkuTickets2));

        Assert.assertTrue(sskuLockService.lock(gcSkuTickets1));
        Assert.assertFalse(sskuLockService.lock(gcSkuTickets2));

        sskuLockService.unlock(gcSkuTickets1);

        Assert.assertTrue(sskuLockService.lock(gcSkuTickets2));
    }

    @Test
    public void whenInsertingAndLockIsFreeClearsGcSkuTicketLink() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID_DEFAULT);
        List<GcSkuTicket> skuTickets = createTickets("1");

        List<GcSkuTicket> otherSskuTickets = createTickets("1");

        sskuLockService.insert(getSskuLocks(skuTickets));

        assertThat(sskuLockService.lock(skuTickets)).isTrue();

        sskuLockService.insert(getSskuLocks(otherSskuTickets));
        assertThat(sskuLockService.lock(otherSskuTickets)).isFalse();

        List<SskuLock> sskuLocks = sskuLockDao.findAll();
        assertThat(sskuLocks.size()).isEqualTo(1);
        assertThat(sskuLocks.get(0).getGcSkuTicket()).isEqualTo(skuTickets.get(0).getId());

        sskuLockService.unlock(skuTickets);

        sskuLockService.insert(getSskuLocks(otherSskuTickets));

        List<SskuLock> sskuLocks2 = sskuLockDao.findAll();
        assertThat(sskuLocks2.size()).isEqualTo(1);
        assertThat(sskuLocks2.get(0).getGcSkuTicket()).isNull();
    }

    @Test
    public void whenTicketTryAcquireLockShouldReenter() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID_DEFAULT);
        List<GcSkuTicket> skuTickets = createTickets("1");

        sskuLockService.insert(getSskuLocks(skuTickets));

        assertThat(sskuLockService.lock(skuTickets)).isTrue();
        assertThat(sskuLockService.lock(skuTickets)).isTrue();
    }

    @Test
    public void whenOneTicketFromBatchCannotAcquireLockShouldNotGetLock() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID_DEFAULT);
        List<GcSkuTicket> skuTickets1 = createTickets("1", "2", "3");
        List<GcSkuTicket> skuTickets2 = createTickets("2");
        sskuLockService.insert(getSskuLocks(skuTickets1));
        sskuLockService.insert(getSskuLocks(skuTickets2));
        assertThat(sskuLockService.lock(skuTickets1)).isTrue();
        assertThat(sskuLockService.lock(skuTickets2)).isFalse();
    }

    @Test
    public void whenNoSskuLockRecordsShouldThrowException() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID_DEFAULT);
        List<GcSkuTicket> skuTickets = createTickets("1");
        Assertions.assertThatThrownBy(() -> sskuLockService.lock(skuTickets))
            .isInstanceOf(IllegalStateException.class)
            .hasStackTraceContaining("SSKU_LOCK table doesn't match tickets trying lock");
    }

    private List<SkuTicket> getSskuLocks(List<GcSkuTicket> skuTickets) {
        return skuTickets.stream()
            .map(gcSkuTicket -> {
                SkuTicket skuTicket = new SkuTicket();
                skuTicket.setShopSku(gcSkuTicket.getShopSku());
                skuTicket.setPartnerShopId(gcSkuTicket.getPartnerShopId());
                return skuTicket;
            }).collect(Collectors.toList());
    }

    private List<GcSkuTicket> createTickets(String... shopSkus) {
        List<DatacampOffer> offers = Arrays.stream(shopSkus)
            .map(this::createOffer)
            .collect(Collectors.toList());
        DataBucket dataBucket = createNewDatabucket();
        List<Long> gcSkuTicketIds = gcSkuTicketDao.saveDatacampTickets(dataBucket, offers, GcSkuTicketType.DATA_CAMP);
        return gcSkuTicketDao.fetchByIds(gcSkuTicketIds);
    }

    private DataBucket createNewDatabucket() {
        long dataBucketId = createDataBucketId(CATEGORY_ID, SOURCE_ID);
        return dataBucketDao.fetchOneById(dataBucketId);
    }

    private DatacampOffer createOffer(String shopSku) {
        DataCampOffer.Offer offer = new DcpOfferBuilder(PARTNER_SHOP_ID_DEFAULT, shopSku)
            .withName("name")
            .build();
        DatacampOffer dbOffer = new DatacampOffer();
        dbOffer.setBusinessId(PARTNER_SHOP_ID_DEFAULT);
        dbOffer.setOfferId(shopSku);
        dbOffer.setData(offer);
        dbOffer.setSourceId(SOURCE_ID);
        return dbOffer;
    }
}
