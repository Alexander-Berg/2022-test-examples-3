package ru.yandex.market.clab.tms.kpi;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepositoryStub;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepositoryStub;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;
import ru.yandex.market.clab.tms.kpi.stats.GoodProcessStats;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class GoodProcessKpiServiceTest extends KpiServiceTestBase {

    private RawPhotoRepositoryStub rawPhotoRepository = new RawPhotoRepositoryStub();

    private EditedPhotoRepositoryStub editedPhotoRepository = new EditedPhotoRepositoryStub();

    private GoodProcessKpiService goodProcessKpiService;

    @Before
    public void setUp() {
        super.setUp();

        goodProcessKpiService = new GoodProcessKpiService(
            auditRepository,
            goodRepository,
            rawPhotoRepository,
            editedPhotoRepository,
            healthLog
        );
    }

    @Test
    public void testProcessingTimingsAndReturnsStats() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.VERIFIED);

        createAndSaveGoodStateAction(start.plusSeconds(10), newGood.getId(),
            GoodState.NEW, GoodState.ACCEPTED);
        createAndSaveGoodStateAction(start.plusSeconds(20), newGood.getId(),
            GoodState.ACCEPTED, GoodState.PHOTO);
        createAndSaveGoodStateAction(start.plusSeconds(30), newGood.getId(),
            GoodState.PHOTO, GoodState.PHOTOGRAPHED);
        createAndSaveGoodStateAction(start.plusSeconds(40), newGood.getId(),
            GoodState.PHOTOGRAPHED, GoodState.PHOTO_EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(50), newGood.getId(),
            GoodState.PHOTO_EDITING, GoodState.PHOTO_EDITED);
        createAndSaveGoodStateAction(start.plusSeconds(60), newGood.getId(),
            GoodState.PHOTO_EDITED, GoodState.EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(70), newGood.getId(),
            GoodState.EDITING, GoodState.EDITED);
        createAndSaveGoodStateAction(start.plusSeconds(80), newGood.getId(),
            GoodState.EDITED, GoodState.VERIFYING);
        createAndSaveGoodStateAction(start.plusSeconds(90), newGood.getId(),
            GoodState.VERIFYING, GoodState.EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(100), newGood.getId(),
            GoodState.EDITING, GoodState.PHOTO_EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(110), newGood.getId(),
            GoodState.PHOTO_EDITING, GoodState.PHOTO);
        createAndSaveGoodStateAction(start.plusSeconds(120), newGood.getId(),
            GoodState.PHOTO, GoodState.PHOTO_EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(130), newGood.getId(),
            GoodState.PHOTO_EDITING, GoodState.EDITING);
        createAndSaveGoodStateAction(start.plusSeconds(140), newGood.getId(),
            GoodState.EDITING, GoodState.EDITED);
        createAndSaveGoodStateAction(start.plusSeconds(150), newGood.getId(),
            GoodState.EDITED, GoodState.VERIFYING);
        createAndSaveGoodStateAction(start.plusSeconds(160), newGood.getId(),
            GoodState.VERIFYING, GoodState.VERIFIED);

        RawPhoto rawPhoto1 = RandomTestUtils.randomObject(RawPhoto.class)
            .setGoodId(newGood.getId());
        RawPhoto rawPhoto2 = RandomTestUtils.randomObject(RawPhoto.class)
            .setGoodId(newGood.getId());
        rawPhotoRepository.createProcessedPhotos(ImmutableList.of(rawPhoto1, rawPhoto2));

        EditedPhoto editedPhoto1 = RandomTestUtils.randomObject(EditedPhoto.class)
            .setGoodId(newGood.getId());
        EditedPhoto editedPhoto2 = RandomTestUtils.randomObject(EditedPhoto.class)
            .setGoodId(newGood.getId());
        EditedPhoto editedPhoto3 = RandomTestUtils.randomObject(EditedPhoto.class)
            .setGoodId(newGood.getId());
        editedPhotoRepository.createProcessedPhotos(ImmutableList.of(editedPhoto1, editedPhoto2, editedPhoto3));

        goodProcessKpiService.countAndWriteStats(start, start.plusHours(1));

        GoodProcessStats stats = (GoodProcessStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getAcceptedToPublishedTimeSec()).isEqualTo(150);
        assertThat(stats.getPhotoTimeSec()).isEqualTo(20);
        assertThat(stats.getPhotoEditTimeSec()).isEqualTo(40);
        assertThat(stats.getEditTimeSec()).isEqualTo(40);
        assertThat(stats.getVerifyTimeSec()).isEqualTo(40);
        assertThat(stats.getReturnToPhotoCount()).isEqualTo(1);
        assertThat(stats.getReturnToPhotoEditCount()).isEqualTo(1);
        assertThat(stats.getReturnToEditCount()).isEqualTo(1);
        assertThat(stats.getRawPhotoCount()).isEqualTo(2);
        assertThat(stats.getEditedPhotoCount()).isEqualTo(3);
    }

    @Test
    public void testMskuChanges() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.VERIFIED);

        createAndSaveGoodStateAction(start, newGood.getId(),
            GoodState.VERIFYING, GoodState.VERIFIED);

        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(1),
            "param1", "qwerty", "qwerty1");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(2),
            "param1", "qwerty1", "qwerty2");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(3),
            "param1", "qwerty2", "qwerty3");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(4),
            "param2", "qwerty", "qwerty1");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(5),
            "param2", "qwerty1", "qwerty");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(6),
            "param3", null, "qwerty");
        createAndSaveAuditAction(EntityType.MSKU_PARAMETER, newGood.getId(), start.plusSeconds(7),
            "param4", "qwerty", null);

        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(1),
            "1", "qwerty", "qwerty1");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(2),
            "1", "qwerty1", "qwerty2");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(3),
            "1", "qwerty2", "qwerty3");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(4),
            "2", "qwerty4", "qwerty5");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(5),
            "2", "qwerty5", "qwerty4");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(6),
            "3", null, "qwerty6");
        createAndSaveAuditAction(EntityType.MSKU_PICTURE, newGood.getId(), start.plusSeconds(7),
            "XL-Picture", "qwerty7", null);

        createAndSaveAuditAction(EntityType.MSKU_VALUE_LINK, newGood.getId(), start.plusSeconds(1),
            "picker", "qwerty", "qwerty1");
        createAndSaveAuditAction(EntityType.MSKU_VALUE_LINK, newGood.getId(), start.plusSeconds(2),
            "picker", "qwerty1", "qwerty2");

        createAndSaveAuditAction(EntityType.MSKU_ALIAS, newGood.getId(), start.plusSeconds(1),
            "param1", null, "qwerty");
        createAndSaveAuditAction(EntityType.MSKU_ALIAS, newGood.getId(), start.plusSeconds(2),
            "param1", null, "qwerty1");
        createAndSaveAuditAction(EntityType.MSKU_ALIAS, newGood.getId(), start.plusSeconds(3),
            "param2", "qwerty", null);

        goodProcessKpiService.countAndWriteStats(start, start.plusHours(1));

        GoodProcessStats stats = (GoodProcessStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getParameterValuesFilledCount()).isEqualTo(2);
        assertThat(stats.getPicturesFilledCount()).isEqualTo(2);
        assertThat(stats.getPickersFilledCount()).isEqualTo(1);
        assertThat(stats.getEnumOptionAliasesFilledCount()).isEqualTo(2);
    }
}
