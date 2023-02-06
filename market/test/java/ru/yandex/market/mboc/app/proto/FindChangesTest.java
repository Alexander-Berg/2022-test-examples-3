package ru.yandex.market.mboc.app.proto;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.FindChangesRequest;
import ru.yandex.market.mboc.http.SupplierOffer;

public class FindChangesTest extends BaseOfferChangesServiceTest {

    @Test
    public void processFirstBatch() {
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L).build());
        var offersList = changes.getOffersList();

        // ручка будет возвращать только строки с маппингами
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60),
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852),
                offer(100, "sku100", 10, 101, 102, 103),
                offer(200, "sku200", 20, 201, 202)
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processEmptyBatch() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(seqId);
    }

    @Test
    public void processFarFarAwayBatch() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();
        var lastModifiedSeqId = Short.MAX_VALUE + seqId;

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(lastModifiedSeqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(lastModifiedSeqId);
    }

    @Test
    public void processNewChanges() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer1 = offerRepository.findOfferByBusinessSkuKey(42, "sku1");
        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer1.setTitle("offer1"), offer4.setTitle("offer4"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // ручка будет возвращать только строки с маппингами
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60).toBuilder().setTitle("offer4").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processNewChangesButChangesAreWithoutOffer() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer1 = offerRepository.findOfferByBusinessSkuKey(42, "sku1");
        offerRepository.updateOffers(offer1.setTitle("offer1"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // ручка ничего не вернет, так как обновился оффер без маппинга. Но last_modified_seq_id обновит
        Assertions.assertThat(offersList).isEmpty();
        Assertions.assertThat(changes.getLastModifiedSeqId()).isGreaterThan(seqId);
    }

    @Test
    public void processNewChangesWithOfferDeletion() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.deleteInTest(60, "sku4");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // ручка будет возвращать только строки с маппингами
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                deletedOffer(60, "sku4", 404040)
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processNewChangesWithOfferDeletion2() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.deleteInTest(60, "sku4");
        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(77, "sku5").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // ручка будет возвращать только строки с маппингами
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "title")
            .containsExactlyInAnyOrder(
                deletedOffer(60, "sku4", 404040),
                offer(465852, "000042.sku5", 505050, 465852).toBuilder().setTitle("t").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processNewChangesWithOfferDeletion3() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.deleteInTest(100, "sku100");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "title")
            .containsExactlyInAnyOrder(
                deletedOffer(100, "sku100", 10)
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processNewChangesWithOfferWithoutMappingDeletion() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.deleteInTest(42, "sku1");

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setTitle("offer4"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // ручка будет возвращать только строки с маппингами
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60).toBuilder().setTitle("offer4").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    @Test
    public void processNewChangesWithOfferDeletionThenRestore() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offer4.setId(Offer.EMPTY_ID);
        offer4.storeOfferContent(OfferContent.initEmptyContent()); // без этой строки вставка оффера не работает

        offerRepository.deleteInTest(60, "sku4");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.insertOffers(offer4);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        // В ответе будет только одна строка, но уже нового оффера
        // Удаленный оффер возвращаться не должен
        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "modifiedSeqId")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60).toBuilder().setModifiedSeqId(seqId + 2).build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 2);
    }

    @Test
    public void processChangePrimaryKey() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setBusinessId(61));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "modifiedSeqId")
            .containsExactlyInAnyOrder(
                // по-хорошему в ответе должно быть сначала удаление оффера, потом его создание
                // (оба события с уникальным modif_id)
                // но в текущих реализациях на стороне сервера такое сделать не получится
                // а на стороне клиента реализовывать такую логику не хочется
                // к счастью, такой сценарий не должен быть на практике
//                deletedOffer(60, "sku4").toBuilder().setModifiedSeqId(seqId + 1).build(),
                offer(61, "sku4", 404040, 60).toBuilder().setModifiedSeqId(seqId + 1).build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 1);
    }

    /**
     * Процесс change, then delete.
     * https://a.yandex-team.ru/review/2167096/details#comment-2945142
     */
    @Test
    public void processNewChangesFirstChangeThenDelete() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setBusinessId(61));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.deleteInTest(61, "sku4");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "isDeleted", "modifiedSeqId")
            .containsExactlyInAnyOrder(
//                 по хорошему в ответе должно быть 2 события:
//                Удалили оффер по старому ключу
//                deletedOffer(60, "sku4").toBuilder().setModifiedSeqId(seqId + 1).build(),
                // удалили оффер по новому ключу
                deletedOffer(61, "sku4", 404040)
                    .toBuilder().setModifiedSeqId(seqId + 2).build()
                // но в реальности будет только одно событие
                // Считаем, что этот случай не будет воссоздан на практике
            );
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 2);
    }

    @Test
    public void processDeletedMapping() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4
            .setApprovedSkuMappingInternal(new Offer.Mapping(0, LocalDateTime.now()))
        );
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 0, 60).toBuilder().setTitle("offer4").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId())
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());
    }

    /**
     * Тест проверяет, что если за время батча оффер успел несколько раз измениться,
     * то ручка вернет только одно изменение.
     */
    @Test
    public void processSeveralChanges() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setTitle("new title"));
        offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setTitle("new title 2"));

        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("modifiedSeqId", "businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
                    .toBuilder().setModifiedSeqId(seqId + 2).setTitle("new title 2").build()
            );
    }

    /**
     * Тест проверяет, что если за время батча оффер успел несколько раз измениться,
     * а между изменениями произошло генерация версии,
     * то ручка вернет только одно изменение.
     */
    @Test
    public void processSeveralChanges2() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setTitle("new title"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offer4 = offerRepository.findOfferByBusinessSkuKey(60, "sku4");
        offerRepository.updateOffers(offer4.setTitle("new title 2"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("modifiedSeqId", "businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
                    .toBuilder().setModifiedSeqId(seqId + 2).setTitle("new title 2").build()
            );
    }

    /**
     * Тест проверяет, что если за время батча оффер успел несколько раз измениться,
     * в результате чего дубликаты отфильтровались, то lastModifId должен быть корректным
     * <p>
     * Представим изменения в виде последовательности:
     * (оффер) -> порядковый номер метки
     * 60, sku4 -> 1
     * 42, sku1 -> 2
     * 60, sku4 -> 3
     * 42, sku1 -> 4
     * 77, sku5 -> 5
     * <p>
     * Если мы запросим c limit = 2, process_limit = 10000, то система должна вернуть
     * offers: 60, sku4 -> 3
     * last_modif_id: 3
     */
    @Test
    public void processSeveralChanges3() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(60, "sku4").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(42, "sku1").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(60, "sku4").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(42, "sku1").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(77, "sku5").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(FindChangesRequest.newBuilder()
            .setLastModifiedSeqId(seqId)
            .setLimit(2)
            .setProcessLimit(10000)
            .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("modifiedSeqId", "businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
                    .toBuilder().setModifiedSeqId(seqId + 3).setTitle("t").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 3);
    }

    /**
     * Тест проверяет, что если несколько уникальных изменений,
     * то lastModifId должен указывать на максимальный modif_id
     * <p>
     * Представим изменения в виде последовательности:
     * (оффер) -> порядковый номер метки
     * 60, sku4 -> 1
     * 42, sku1 -> 2
     * 79, sku7 -> 3
     * 79, sku8 -> 4
     * 77, sku5 -> 5
     * <p>
     * Если мы запросим c limit = 2, process_limit = 4 то система должна вернуть
     * offers: 60, sku4 -> 1
     * last_modif_id: 4
     */
    @Test
    public void processSeveralChanges4() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(60, "sku4").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(42, "sku1").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(79, "sku7").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(79, "sku8").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(77, "sku5").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(FindChangesRequest.newBuilder()
            .setLastModifiedSeqId(seqId)
            .setLimit(2)
            .setProcessLimit(4)
            .build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields("modifiedSeqId", "businessId", "shopSku", "approvedMappingMskuId",
                "serviceOffersList", "title")
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
                    .toBuilder().setModifiedSeqId(seqId + 1).setTitle("t").build()
            );
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 4);
    }

    /**
     * Тест проверяет, что если есть изменения, но не одно из них не подходит
     * то lastModifId должен указывать на максимальный modif_id
     * <p>
     * Представим изменения в виде последовательности:
     * (оффер) -> порядковый номер метки
     * 42, sku1 -> 1
     * 79, sku7 -> 2
     * 79, sku8 -> 3
     * 77, sku5 -> 4
     * <p>
     * Если мы запросим c limit = 2, process_limit = 3 то система должна вернуть
     * offers: empty
     * last_modif_id: 3
     */
    @Test
    public void processSeveralChanges5() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(42, "sku1").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(79, "sku7").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(79, "sku8").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offerRepository.updateOffers(offerRepository.findOfferByBusinessSkuKey(77, "sku5").setTitle("t"));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(FindChangesRequest.newBuilder()
            .setLastModifiedSeqId(seqId)
            .setLimit(2)
            .setProcessLimit(3)
            .build());
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId + 3);
    }

    @Test
    public void filterByRealSupplier() {
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852)
            );
    }

    @Test
    public void filterByBlueOffers() {
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60),
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852),
                offer(100, "sku100", 10, 101, 102, 103),
                offer(200, "sku200", 20, 201)
            );
    }

    @Test
    public void filterByFby() {
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60),
                // при FBY real_supplier всегда должны возвращаться, внезависимости от того, если у них флаг или нет
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852),
                offer(100, "sku100", 10, 101, 103)
            );
    }

    @Test
    public void filterByFbyPlusAndFBS() {
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY_PLUS)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBS)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60),
                offer(200, "sku200", 20, 201)
            );
    }

    @Test
    public void filterByFbsAndRealSupplier() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBS)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
        Assertions.assertThat(changes.getLastModifiedSeqId()).isEqualTo(seqId);
    }

    @Test
    public void filterByFbyDeletedServiceOffer() {
        // проверяем, что ручка вернет 101 FBY поставщика
        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .contains(
                offer(100, "sku100", 10, 101, 103)
            );

        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();
        // удаляем сервисный оффер
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        offerRepository.updateOffers(sku100.removeServiceOfferIfExistsForTests(101));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // снова запрашиваем по ручке и по фильтру FBY
        var changes2 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        Assertions.assertThat(changes2.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 103)
            );

        // снова запрашиваем по ручке, но уже без фильтра
        var changes3 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build()
        );
        Assertions.assertThat(changes3.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 102, 103)
            );
    }

    @Test
    public void processDeleteAndRestoreServiceOffer() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        // удаляем сервисный оффер
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        offerRepository.updateOffers(sku100.removeServiceOfferIfExistsForTests(101));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // снова его воскрешаем
        sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        sku100.addNewServiceOfferIfNotExistsForTests(supplierRepository.findById(101));
        offerRepository.updateOffers(sku100);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // запрашиваем по ручке и по фильтру FBY
        var changes2 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        Assertions.assertThat(changes2.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 101, 103)
            );

        // снова запрашиваем по ручке, но уже без фильтра
        var changes3 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build()
        );
        Assertions.assertThat(changes3.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 101, 102, 103)
            );
    }

    @Test
    public void processDeleteTwoServiceOffers() {
        var seqId = offerUpdateSequenceService.getLastModifiedSequenceId();

        // удаляем сервисный оффер
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        offerRepository.updateOffers(sku100.removeServiceOfferIfExistsForTests(101));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // запрашиваем по ручке и по фильтру FBY
        var changes2 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        Assertions.assertThat(changes2.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 103)
            );

        // удаляем еще один сервисный оффер
        sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        offerRepository.updateOffers(sku100.removeServiceOfferIfExistsForTests(103));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // снова запрашиваем по ручке и по фильтру FBY
        // оффер будет удаленным, так как не осталось сервисных офферов, подходящих под условие
        var changes3 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId)
                .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
                .build()
        );
        Assertions.assertThat(changes3.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                deletedOffer(100, "sku100", 10)
            );

        // снова запрашиваем по ручке, но уже без фильтра
        // оффер будет существовать, так как под условие попадает один оффер
        var changes4 = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(seqId).build()
        );
        Assertions.assertThat(changes4.getOffersList())
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 102)
            );
    }

    @Test
    public void processDeletedOfferWontReturnByFilter() {
        offerRepository.deleteInTest(60, "sku4");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var changes = mboCategoryOfferChangesService.findChanges(
            FindChangesRequest.newBuilder().setLastModifiedSeqId(0L)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                .build()
        );
        var offersList = changes.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852)
            );
    }

    @Test
    public void testGetMaxAndMinModifiedSeqId() {
        var changes = mboCategoryOfferChangesService.getMaxAndMinModifiedSeqId(
            MboCategoryOfferChanges.VoidRequest.newBuilder().build());
        var maxModifiedSeqId = changes.getMaxModifiedSeqId();
        var minModifiedSeqId = changes.getMinModifiedSeqId();

        Assertions.assertThat(maxModifiedSeqId)
            .isGreaterThan(0L)
            .isEqualTo(offerUpdateSequenceService.getLastModifiedSequenceId());

        Assertions.assertThat(minModifiedSeqId)
            .isLessThan(offerUpdateSequenceService.getLastModifiedSequenceId());
        // В наших тестах seq между перезапусками не сбрасывается, поэтому мы не можем сравнивать с 0L
        // достаточно убедиться что min < max
//            .isEqualTo(0L);
    }
}
