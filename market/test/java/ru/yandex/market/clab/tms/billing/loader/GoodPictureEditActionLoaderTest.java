package ru.yandex.market.clab.tms.billing.loader;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.common.service.audit.AuditActionFilter;
import ru.yandex.market.clab.common.service.audit.AuditRepository;
import ru.yandex.market.clab.common.service.audit.AuditRepositoryStub;
import ru.yandex.market.clab.common.service.audit.AuditService;
import ru.yandex.market.clab.common.service.audit.AuditedEntity;
import ru.yandex.market.clab.common.service.audit.AuditedEntityId;
import ru.yandex.market.clab.common.service.audit.AuditedProperty;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.good.GoodServiceImpl;
import ru.yandex.market.clab.common.service.photo.DirectUploadedPhotoRepositoryStub;
import ru.yandex.market.clab.common.service.photo.UploadedPhotoRepository;
import ru.yandex.market.clab.common.test.asset.Dates;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.UploadedPhoto;
import ru.yandex.market.clab.tms.billing.BillingTarifProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.asset.Names.DYLAN;
import static ru.yandex.market.clab.common.test.asset.Names.LUCAS;
import static ru.yandex.market.clab.common.test.asset.Names.MABEL;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 26.04.2019
 */
public class GoodPictureEditActionLoaderTest {

    private static final String SKU_ID = "sku-id";
    private static final long CATEGORY_ID = 82086972;
    private static final int PRICE = 780;
    private static final int PRICE_DELETE = 330;
    private static final String OCEAN = "https://jing.yandex-team.ru/files/pochemuto/ocean.jpg";
    private static final String CAT = "https://jing.yandex-team.ru/files/pochemuto/cat.jpg";
    private static final String GIRL = "https://jing.yandex-team.ru/files/pochemuto/girl.jpeg.html";

    private AuditRepository auditRepository;
    private GoodService goodService;
    private AuditService auditService;
    private GoodPictureEditActionLoader loader;
    private ControlledClock clock;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private BillingTarifProvider tarifProvider;
    private UploadedPhotoRepository uploadedPhotoRepository;

    @Before
    public void setUp() {
        GoodRepository goodRepository = new GoodRepositoryStub();
        uploadedPhotoRepository = new DirectUploadedPhotoRepositoryStub();

        auditRepository = new AuditRepositoryStub();
        goodService = new GoodServiceImpl(goodRepository, mock(SsBarcodeRepository.class));
        auditService = new AuditService(auditRepository, "mbo-robot-unittest");
        loader = new GoodPictureEditActionLoader(auditRepository, goodRepository, uploadedPhotoRepository);

        clock = new ControlledClock(Clock.fixed(Dates.SUMMER_DAY.toInstant(), Dates.MSK_ZONE));
        periodStart = LocalDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
        periodEnd = periodStart.plusDays(1);

        tarifProvider = mock(BillingTarifProvider.class);
        when(tarifProvider.getTarif(PaidAction.MSKU_PICTURE_CHANGE, CATEGORY_ID))
            .thenReturn(createTarif(PaidAction.MSKU_PICTURE_CHANGE, PRICE));
        when(tarifProvider.getTarif(PaidAction.MSKU_PICTURE_DELETE, CATEGORY_ID))
            .thenReturn(createTarif(PaidAction.MSKU_PICTURE_DELETE, PRICE_DELETE));
    }

    private BillingTarif createTarif(PaidAction action, int price) {
        return new BillingTarif(2L, CATEGORY_ID, Dates.SUMMER_DAY.minusMonths(2)
            .toLocalDateTime(), action, price);
    }

    @Test
    public void countOneChange() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(changeAction(verifiedTime, DYLAN));

        BillingAction action = actions.get(0);
        assertThat(action.getAuditActionId()).isNotNull();

        AuditAction linkedAction = getLinkedAuditAction(action);
        assertThat(linkedAction.getEntityType()).isEqualTo(EntityType.MSKU_PICTURE);
        assertThat(linkedAction.getNewValue()).isEqualTo(CAT);
    }

    @Test
    public void collapsing() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        saveChanges(DYLAN, msku.withPic1(CAT), msku.withPic1(GIRL));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);

        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(changeAction(verifiedTime, DYLAN));

        BillingAction action = actions.get(0);
        assertThat(action.getAuditActionId()).isNotNull();

        AuditAction auditAction = getLinkedAuditAction(action);
        assertThat(auditAction.getNewValue()).isEqualTo(GIRL);
    }

    @Test
    public void collapsingToNothing() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        saveChanges(DYLAN, msku.withPic1(CAT), msku.withPic1(OCEAN));
        clock.tickHour();
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).isEmpty();
    }

    @Test
    public void countRemoving() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(null));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(deleteAction(verifiedTime, DYLAN));
    }

    @Test
    public void dontCollapseDifferentUsers() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        saveChanges(MABEL, msku.withPic1(CAT), msku.withPic1(GIRL));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(
                changeAction(verifiedTime, DYLAN),
                changeAction(verifiedTime, MABEL)
            );
    }

    @Test
    public void countNotByPropertyName() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        // swap pic1 with pic2
        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        saveChanges(DYLAN, msku.withPic2(CAT), msku.withPic2(OCEAN));
        clock.tickHour();
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .isEmpty(); // dont count change order
    }

    @Test
    public void shouldCountMakeSimplePicture() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        uploadedPhotoRepository.save(new UploadedPhoto().setUrl(GIRL).setGoodId(0L));
        // swap pic1 with pic2
        saveChanges(DYLAN, msku.withPic1(OCEAN), msku.withPic1(CAT));
        clock.tickHour();
        saveChanges(DYLAN, msku.withPic2(CAT), msku.withPic2(GIRL));
        clock.tickHour();
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));

        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);

        assertThat(actions).extracting("paidAction")
            .containsOnlyOnce(PaidAction.ADD_PICTURE_TO_MODEL);
    }

    private static BillingAction changeAction(LocalDateTime verifiedTime, String login) {
        return new BillingAction()
            .setBillingDate(verifiedTime)
            .setStaffLogin(login)
            .setCategoryId(CATEGORY_ID)
            .setPaidAction(PaidAction.MSKU_PICTURE_CHANGE)
            .setPriceKopeck(PRICE);
    }

    private static BillingAction deleteAction(LocalDateTime verifiedTime, String login) {
        return new BillingAction()
            .setBillingDate(verifiedTime)
            .setStaffLogin(login)
            .setCategoryId(CATEGORY_ID)
            .setPaidAction(PaidAction.MSKU_PICTURE_DELETE)
            .setPriceKopeck(PRICE_DELETE);
    }

    private AuditAction getLinkedAuditAction(BillingAction billingAction) {
        AuditActionFilter filter = new AuditActionFilter().addId(billingAction.getAuditActionId());
        List<AuditAction> linkedActions = auditRepository.findActions(filter);
        assertThat(linkedActions).hasSize(1);
        return linkedActions.get(0);
    }

    private void saveChanges(String user, Object before, Object after) {
        before = wrapIfNeeded(before);
        after = wrapIfNeeded(after);

        List<AuditAction> actions = auditService.createAuditActions(before, after);
        actions.forEach(a -> {
            a.setStaffLogin(user);
            a.setActionDate(LocalDateTime.now(clock));
        });
        if (before instanceof Msku) {
            // we avoid using ModelAuditService for simplicity
            // fix behavior difference here
            actions.forEach(a -> {
                // model audit
                a.setActionType(AuditService.getActionType(a.getOldValue(), a.getNewValue()));
            });
        }
        auditService.writeAuditActions(actions);
    }

    private Object wrapIfNeeded(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Good) {
            return new GoodWrapper((Good) object);
        }
        return object;
    }

    private Good createGood() {
        Good good = new Good();
        good.setCategoryId(CATEGORY_ID);
        return goodService.createGood(good);
    }

    @AuditedEntity(EntityType.MSKU_PICTURE)
    public static class Msku {
        public static final String XL_PICTURE_1 = "XlPicture_1";
        public static final String XL_PICTURE_2 = "XlPicture_2";

        private final long goodId;

        private final String mskuId;

        private final String xlPicture1;
        private final String xlPicture2;

        private Msku(long goodId, String mskuId, String xlPicture1, String xlPicture2) {
            this.goodId = goodId;
            this.mskuId = mskuId;
            this.xlPicture1 = xlPicture1;
            this.xlPicture2 = xlPicture2;
        }

        private Msku(long goodId, String mskuId) {
            this.goodId = goodId;
            this.mskuId = mskuId;
            this.xlPicture1 = null;
            this.xlPicture2 = null;
        }

        @AuditedEntityId
        public long getGoodId() {
            return goodId;
        }

        @AuditedEntityId(internal = false)
        public String getMskuId() {
            return mskuId;
        }

        @AuditedProperty(XL_PICTURE_1)
        public String getXlPicture1() {
            return xlPicture1;
        }

        @AuditedProperty(XL_PICTURE_2)
        public String getXlPicture2() {
            return xlPicture2;
        }

        public Msku withPic1(String url) {
            return new Msku(goodId, mskuId, url, xlPicture2);
        }

        public Msku withPic2(String url) {
            return new Msku(goodId, mskuId, xlPicture1, url);
        }
    }
}

