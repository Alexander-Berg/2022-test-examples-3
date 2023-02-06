package ru.yandex.market.crm.operatorwindow;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.URLUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.mds.MdsLocation;
import ru.yandex.market.jmf.mds.test.impl.MockMdsService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.def.AttachmentMdsCleaner;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

class AttachmentMdsServiceTest extends AbstractModuleOwTest {

    private static final Fqn DUMMY_FQN = Ou.FQN_DEFAULT;
    private static final int ORPHAN_ATTACHMENT_DELETE_TIMEOUT_MINUTES = 1440;

    @Value("${mds.s3.endpoint}")
    String mdsEndpoint;
    @Inject
    TxService txService;
    @Inject
    DbService dbService;
    @Inject
    GidService gidService;
    @Inject
    BcpService bcpService;
    @Inject
    MockMdsService mdsService;
    @Inject
    AttachmentMdsCleaner attachmentMdsCleaner;

    private String gidForDeleteByUrl;
    private String gidForDeleteByKey;
    private MdsLocation mdsLocationForDeleteByUrl;
    private MdsLocation mdsLocationForDeleteByKey;
    private String gidForSafeByEarlyDate;
    private String gidForSafeByExistEntity;
    private String gidForSafeByInvalidUrlPath;

    @BeforeEach
    @Transactional
    public void setUp() {
        mdsService.setMdsHost(URLUtils.getDomain(mdsEndpoint));
        prepareAttachments();
    }

    private void prepareAttachments() {
        mdsLocationForDeleteByUrl = mdsService.uploadStreamAndGetLocation(null);
        mdsLocationForDeleteByKey = mdsService.uploadStreamAndGetLocation(null);

        txService.doInNewTx(() -> {
            gidForDeleteByUrl = createAttachment(true, Maps.of(
                    Attachment.URL, mdsLocationForDeleteByUrl.url()
            ));

            gidForDeleteByKey = createAttachment(true, Maps.of(
                    Attachment.MDS_BUCKET_NAME, mdsLocationForDeleteByKey.location().getBucketName(),
                    Attachment.MDS_KEY, mdsLocationForDeleteByKey.location().getKey()
            ));

            Entity dummy = bcpService.create(DUMMY_FQN, Maps.of(Bo.TITLE, "Dummy"));
            gidForSafeByExistEntity = createAttachment(true, Maps.of(
                    Attachment.ENTITY, dummy
            ));

            gidForSafeByEarlyDate = createAttachment(false, Maps.of());

            gidForSafeByInvalidUrlPath = createAttachment(true, Maps.of(
                    Attachment.URL, Randoms.url() + "/111/222"
            ));

            return null;
        });
    }

    private String createAttachment(boolean needEditCreationTime, Map<String, Object> newProperties) {
        Map<String, Object> properties = Maps.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, "ContentType",
                Attachment.URL, Randoms.url() + "/" + UUID.randomUUID()
        );

        properties.putAll(newProperties);
        String gid = bcpService.create(Attachment.FQN_DEFAULT, properties).getGid();

        if (needEditCreationTime) {
            dbService.createQuery(
                            "UPDATE " + Attachment.FQN_DEFAULT +
                                    " SET " + Attachment.CREATION_TIME + " = :time" +
                                    " WHERE id = :id"
                    )
                    .setParameter("time",
                            OffsetDateTime.now().minusMinutes(ORPHAN_ATTACHMENT_DELETE_TIMEOUT_MINUTES + 1)
                    )
                    .setParameter("id", gidService.parse(gid).getId())
                    .executeUpdate();
        }

        return gid;
    }

    @Test
    void deleteOrphanAttachmentsMds() {
        Assertions.assertEquals(mdsService.getDeletedCount(), 0);

        txService.doInNewReadOnlyTx(() -> {
            Assertions.assertNotNull(dbService.get(gidForDeleteByKey), "Вложение для удаленя по ключу не существует");
            Assertions.assertNotNull(dbService.get(gidForDeleteByUrl), "Вложение для удаленя по URL'у не существует");
            return null;
        });

        attachmentMdsCleaner.deleteOrphanedAttachmentsFromMds();

        Assertions.assertTrue(
                mdsService.isDeletedBucketKey(null, mdsLocationForDeleteByUrl.location().getKey()),
                "Вложение не удалено из MDS по URL'у"
        );
        Assertions.assertTrue(
                mdsService.isDeletedBucketKey(
                        mdsLocationForDeleteByKey.location().getBucketName(),
                        mdsLocationForDeleteByKey.location().getKey()
                ),
                "Вложение не удалено из MDS по ключу"
        );

        txService.doInNewReadOnlyTx(() -> {
            Assertions.assertNull(dbService.get(gidForDeleteByKey), "Вложение не удалено по ключу");
            Assertions.assertNull(dbService.get(gidForDeleteByUrl), "Вложение не удалено по URL'у");

            Assertions.assertNotNull(
                    dbService.get(gidForSafeByEarlyDate),
                    "Вложение ошибочно удалено: дата не истекла"
            );
            Assertions.assertNotNull(
                    dbService.get(gidForSafeByExistEntity),
                    "Вложение ошибочно удалено: есть ссылка на сущность"
            );
            Assertions.assertNotNull(
                    dbService.get(gidForSafeByInvalidUrlPath),
                    "Вложение ошибочно удалено: URL не валидный"
            );

            return null;
        });
    }
}
