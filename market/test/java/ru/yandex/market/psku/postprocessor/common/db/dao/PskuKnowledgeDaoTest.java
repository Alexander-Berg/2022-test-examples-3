package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PskuKnowledgeDaoTest extends BaseDBTest {
        @Autowired
        PskuKnowledgeDao pskuKnowledgeDao;

        @Test
        public void upsertInsertsEntries() {
            PskuKnowledge entry = createBasicEntry(1);
            entry.setShopSku("shopSku");

            pskuKnowledgeDao.upsert(Arrays.asList(entry));

            List<PskuKnowledge> allEntries = pskuKnowledgeDao.findAll();
            assertThat(allEntries).hasSize(1);
            assertThat(allEntries).allSatisfy(
                e -> assertThat(e).hasNoNullFieldsOrPropertiesExcept("approvedCategoryId"));
        }

        @Test
        public void upsertDoesNotUpdateShopSkuIfNotNull() {
            Timestamp timestamp = Timestamp.from(Instant.now().minusMillis(10));
            PskuKnowledge entry = createBasicEntry(1L, timestamp, timestamp);
            String oldShopSku = "Old shop sku";
            entry.setShopSku(oldShopSku);

            PskuKnowledge newEntry = createBasicEntry(1L);
            newEntry.setShopSku("New shop sku");

            pskuKnowledgeDao.insert(entry);
            pskuKnowledgeDao.upsert(Arrays.asList(newEntry));

            List<PskuKnowledge> allEntries = pskuKnowledgeDao.findAll();

            assertThat(allEntries).extracting(PskuKnowledge::getShopSku)
                .containsExactlyInAnyOrder(oldShopSku);
        }
        @Test
        public void upsertSetsShopSkuIfNull() {
            Timestamp timestamp = Timestamp.from(Instant.now().minusMillis(10));
            PskuKnowledge entry = createBasicEntry(1L, timestamp, timestamp);
            entry.setShopSku(null);

            PskuKnowledge newEntry = createBasicEntry(1L);
            String newShopSku = "New shop sku";
            newEntry.setShopSku(newShopSku);

            pskuKnowledgeDao.insert(entry);
            pskuKnowledgeDao.upsert(Arrays.asList(newEntry));

            List<PskuKnowledge> allEntries = pskuKnowledgeDao.findAll();

            assertThat(allEntries).extracting(PskuKnowledge::getShopSku)
                .containsExactlyInAnyOrder(newShopSku);
        }

        @Test
        public void upsertOverridesPskuTitle() {
            Timestamp timestamp = Timestamp.from(Instant.now().minusMillis(10));
            PskuKnowledge entry = createBasicEntry(1L, timestamp, timestamp);
            entry.setPskuTitle("Old Title");
            PskuKnowledge newEntry = createBasicEntry(1L);
            String newTitle = "New title";
            newEntry.setPskuTitle(newTitle);

            pskuKnowledgeDao.insert(entry);
            pskuKnowledgeDao.upsert(Arrays.asList(newEntry));

            List<PskuKnowledge> allEntries = pskuKnowledgeDao.findAll();
            assertThat(allEntries).hasSize(1);

            PskuKnowledge resultEntry = allEntries.get(0);
            assertThat(resultEntry.getPskuTitle()).isEqualTo(newTitle);
            assertThat(resultEntry.getCreationTs()).isNotEqualTo(resultEntry.getLastUpdateTs());
        }

        @Test
        public void detectsDuplicatesFromDifferentSuppliersCorrectly() {
            PskuKnowledge duplicate1 = createBasicEntry(1L);
            PskuKnowledge duplicate2 = createBasicEntry(2L);
            duplicate2.setSupplierId(11L);

            pskuKnowledgeDao.insert(duplicate1, duplicate2);

            assertThat(pskuKnowledgeDao.areDuplicatesFromDifferentSuppliers(
                Arrays.asList(duplicate1.getId(), duplicate2.getId())))
                .isTrue();
        }

        @Test
        public void doesNotMarkNonDuplicatesAsDuplicates() {
            PskuKnowledge diffTitle1 = createBasicEntry(1L);
            PskuKnowledge diffTitle2 = createBasicEntry(2L);
            diffTitle2.setSupplierId(11L);
            diffTitle2.setPskuTitle("Other");

            PskuKnowledge sameSupplier1 = createBasicEntry(3L);
            PskuKnowledge sameSupplier2 = createBasicEntry(4L);

            pskuKnowledgeDao.insert(diffTitle1, diffTitle2, sameSupplier1, sameSupplier2);

            assertThat(pskuKnowledgeDao.areDuplicatesFromDifferentSuppliers(
                Arrays.asList(diffTitle1.getId(), diffTitle2.getId())))
                .isFalse();

            assertThat(pskuKnowledgeDao.areDuplicatesFromDifferentSuppliers(
                Arrays.asList(sameSupplier1.getId(), sameSupplier2.getId())))
                .isFalse();
        }

        private PskuKnowledge createBasicEntry(long id) {
            PskuKnowledge entry = new PskuKnowledge();
            entry.setId(id);
            entry.setPskuTitle("Title");
            entry.setSupplierId(10L);
            entry.setShopSku("shop sku");
            entry.setMainPictureUrl("main picture url");
            entry.setVendorName("vendor name");
            entry.setPmodelSkuCount(1L);
            return entry;
        }

        private PskuKnowledge createBasicEntry(long id, Timestamp createionTs, Timestamp lastUpdateTs) {
            PskuKnowledge entry = createBasicEntry(id);
            entry.setCreationTs(createionTs);
            entry.setLastUpdateTs(lastUpdateTs);
            return entry;
        }
}