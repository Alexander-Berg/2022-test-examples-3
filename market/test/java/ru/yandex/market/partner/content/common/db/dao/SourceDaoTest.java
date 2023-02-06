package ru.yandex.market.partner.content.common.db.dao;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.SourceCheckPartDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.SourceCheckPart;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import static java.lang.Math.abs;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SOURCE_CHECK_PART;
import static ru.yandex.market.partner.content.common.db.jooq.enums.FileType.ONE_CATEGORY_SIMPLE_EXCEL;

public class SourceDaoTest extends BaseDbCommonTest {
    private static final Random RANDOM = new Random(13294345);

    @Autowired
    private SourceDao sourceDao;

    @Autowired
    private SourceCheckPartDao sourceCheckPartDao;

    @Test
    public void mergeSource() throws Exception {
        final int partnerShopId = abs(RANDOM.nextInt());
        final int sourceId = abs(RANDOM.nextInt());
        final String sourceName = "test";
        final String sourceUrl = "http://test";
        final float checkPart = 0.5f;

        DSLContext dsl = DSL.using(configuration);
        dsl.insertInto(SOURCE_CHECK_PART)
            .set(SOURCE_CHECK_PART.PARTNER_SHOP_ID, partnerShopId)
            .set(SOURCE_CHECK_PART.CHECK_PART, checkPart)
            .execute();

        sourceDao.mergeSource(sourceId, sourceName, sourceUrl, partnerShopId);

        final SourceCheckPart sourceCheckPart = sourceCheckPartDao.fetchOne(
            ru.yandex.market.partner.content.common.db.jooq.tables.SourceCheckPart.SOURCE_CHECK_PART.SOURCE_ID,
            sourceId
        );
        final float part = sourceCheckPart == null ? 1.0f : sourceCheckPart.getCheckPart();

        Assert.assertEquals(checkPart, part, 0f);
    }

    @Test
    public void getSourceByDataBucketId() throws Exception {
        final int partnerShopId = abs(RANDOM.nextInt());
        final int sourceId = abs(RANDOM.nextInt());
        final String sourceName = "test2";
        final String sourceUrl = "http://test2";
        final long categoryId = abs(RANDOM.nextLong());

        sourceDao.mergeSource(sourceId, sourceName, sourceUrl, partnerShopId);

        long fileDataProcessRequestId = partnerContentDao.createFileDataProcessRequest(
            sourceId,
            null,
            "http://file",
            false,
            ONE_CATEGORY_SIMPLE_EXCEL,
            partnerShopId,
            false, false, false);

        long fileProcessId = fileProcessDao.insertNewFileProcess(fileDataProcessRequestId, ProcessType.BETTER_FILE_PROCESS);

        final long dataBucketId = dataBucketDao.getOrCreateDataBucket(categoryId, fileProcessId, sourceId, Timestamp.from(Instant.now()));

        Optional<Source> sourceOptional = sourceDao.getSourceByDataBucketId(dataBucketId);
        Assert.assertTrue(sourceOptional.isPresent());
        Source source = sourceOptional.get();
        Assert.assertEquals(partnerShopId, (int) source.getPartnerShopId());
    }

}
