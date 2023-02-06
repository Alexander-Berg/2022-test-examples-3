package ru.yandex.market.partner.content.common;

import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nullable;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.config.CommonTestConfig;
import ru.yandex.market.partner.content.common.db.dao.AbstractProcessDao;
import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerContentDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcTicketProcessDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.TicketProcessRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;
import ru.yandex.market.partner.content.common.service.mappings.PartnerShopService;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.SOURCE;

/**
 * @author s-ermakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class,
    classes = CommonTestConfig.class)
@Transactional
@TestPropertySource("classpath:test-application.properties")
public abstract class BaseDbCommonTest {

    @Autowired
    @Qualifier("jooq.config.configuration")
    protected Configuration configuration;
    @Autowired
    @Qualifier("jooq.config.configuration.ro")
    protected Configuration slaveConfiguration;
    @Autowired
    protected PartnerContentDao partnerContentDao;
    @Autowired
    protected FileProcessDao fileProcessDao;
    @Autowired
    protected DataBucketDao dataBucketDao;
    @Autowired
    protected SourceDao sourceDao;
    @Autowired
    protected PartnerShopService partnerShopService;
    @Autowired
    protected GcTicketProcessDao gcTicketProcessDao;
    @Autowired
    protected GcSkuTicketDao gcSkuTicketDao;
    @Autowired
    protected GcSkuValidationDao gcSkuValidationDao;
    @Autowired
    protected TicketProcessRequestDao ticketProcessRequestDao;
    @Autowired
    protected AbstractProcessDao abstractProcessDao;
    @Autowired
    protected DatacampOfferDao datacampOfferDao;

    protected DSLContext dsl() {
        return DSL.using(configuration);
    }

    protected void createSource(int sourceId, @Nullable Integer partnerShopId) {
        sourceDao.insert(new Source(sourceId, "Source " + sourceId, null, partnerShopId, false, null));
    }

    protected long createFileProcessId(long fileDataProcessRequestId) {
        return fileProcessDao.insertNewFileProcess(fileDataProcessRequestId, ProcessType.GOOD_FILE_PROCESS);
    }

    protected long createDataBucketId(long categoryId, int sourceId) {
        long fileDataProcessRequest = createFileDataProcessRequest(sourceId);
        long fileProcessId = createFileProcessId(fileDataProcessRequest);
        dsl().insertInto(SOURCE)
            .set(SOURCE.SOURCE_ID, sourceId)
            .set(SOURCE.SOURCE_NAME, "testSource")
            .onConflictDoNothing()
            .execute();

        return dataBucketDao.getOrCreateDataBucket(categoryId, fileProcessId, sourceId,
            Timestamp.from(Instant.now()));
    }

    protected long createDataBucketId(long categoryId, int sourceId, long fileDataProcessRequest) {
        long fileProcessId = createFileProcessId(fileDataProcessRequest);
        return createDataBucketIdByProcessId(categoryId, sourceId, fileProcessId);
    }

    protected long createDataBucketIdByProcessId(long categoryId, int sourceId, long fileProcessId) {
        return dataBucketDao.getOrCreateDataBucket(categoryId, fileProcessId, sourceId,
            Timestamp.from(Instant.now()));
    }

    protected long createFileDataProcessRequest(int sourceId) {
        return createFileDataProcessRequest(sourceId, null);
    }

    protected long createFileDataProcessRequest(int sourceId, Boolean ignoreWhiteBackgroundCheck) {
        return partnerContentDao.createFileDataProcessRequest(
            sourceId, null, "http://some.url", false, FileType.DCP_SINGLE_EXCEL,
            null, ignoreWhiteBackgroundCheck, false, false);
    }

}
