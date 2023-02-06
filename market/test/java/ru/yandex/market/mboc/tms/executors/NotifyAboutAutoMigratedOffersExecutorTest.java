package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jooq.JSONB;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationRemovedOffer;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.model.data.NotificationData;
import ru.yandex.market.mboc.common.notifications.model.data.StringNotificationData;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class NotifyAboutAutoMigratedOffersExecutorTest extends BaseDbTestClass {
    private static final int BIZ_ID = OfferTestUtils.businessSupplier().getId();

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private NotifyAboutAutoMigratedOffersExecutor notifyAboutAutoMigratedOffersExecutor;

    @Before
    public void setUp() throws Exception {
        notifyAboutAutoMigratedOffersExecutor = new NotifyAboutAutoMigratedOffersExecutor(
            notificationRepository, migrationRemovedOfferRepository, offerRepository, storageKeyValueService
        );
        supplierRepository.insertBatch(OfferTestUtils.businessSupplier(), OfferTestUtils.simpleSupplier());

    }

    @Test
    public void notifyAboutAutoMigratedOffersDifferentTitle() {
        String tsString = "2020-05-28T12:21:00.167389";
        LocalDateTime modifiedDate = LocalDateTime.parse(tsString);

        String shopSku = "196112";
        String title = "Интерактивная игрушка роботизированный шар Sphero Mini (Красный)";
        String str = "{\"id\": 13273054, \"title\": \"Интерактивная игрушка роботизированный шар Sphero Mini " +
            "(Красный)\", \"vendor\": \"Sphero\", \"created\": \"2020-05-28T12:20:59.631057\", \"updated\": " +
            "\"2020-12-29T19:50:32.870886\", \"bar_code\": \"\", \"group_id\": null, \"model_id\": 44779490, " +
            "\"shop_sku\": \"196112\", \"is_golden\": null, \"vendor_id\": 10963455, \"category_id\": 10682618, " +
            "\"realization\": null, \"supplier_id\": 137028, \"vendor_code\": \"\", \"availability\": \"ACTIVE\", " +
            "\"binding_kind\": \"APPROVED\", \"last_version\": 920399869, \"reclassified\": null, \"upload_to_yt\": " +
            "null, \"manual_vendor\": false, \"markup_status\": \"OPEN\", \"service_offers\": [{\"supplier_id\": " +
            "137028, \"availability\": \"ACTIVE\", \"supplier_type\": \"MARKET_SHOP\", \"service_acceptance\": " +
            "\"NEW\"}], \"tracker_ticket\": null, \"content_comment\": null, " +
            "\"mapped_model_id\": null, \"ticket_critical\": null, \"ticket_deadline\": null, \"created_by_login\": " +
            "\"sigalaeva\", \"hide_from_toloka\": null, \"acceptance_status\": \"NEW\", \"content_lab_state\": " +
            "\"CL_NONE\", \"is_datacamp_offer\": false, \"market_model_name\": \"Интерактивная игрушка робот Sphero " +
            "Mini\", \"modified_by_login\": null, \"offer_destination\": \"WHITE\", \"processing_status\": \"OPEN\", " +
            "\"additional_tickets\": {}, \"content_changed_ts\": \"2020-05-28T12:20:59.631058\", " +
            "\"mapped_category_id\": null, \"market_vendor_name\": null, \"shop_category_name\": \"Детские " +
            "товары\\\\Игрушки и игровые комплексы\\\\Роботы и трансформеры\", " +
            "\"upload_to_yt_stamp\": 9059888, \"comment_modified_by\": null, \"content_lab_message\": null, " +
            "\"datacamp_content_ts\": null, \"mapping_destination\": \"WHITE\", \"mapping_modified_by\": " +
            "\"sigalaeva\", \"through_content_lab\": false, \"processing_ticket_id\": null, \"supplier_category_id\":" +
            " null, \"classifier_confidence\": null, " +
            "\"upload_to_erp_done_ts\": \"2020-06-05T21:36:17.354897\", \"classifier_category_id\": null, " +
            "\"content_sku_mapping_id\": 100929052981, \"content_sku_mapping_ts\": \"2020-05-28T12:21:00.167389\", " +
            "\"is_reprocess_requested\": false, \"suggest_sku_mapping_id\": null, \"suggest_sku_mapping_ts\": null, " +
            "\"approved_sku_mapping_id\": 100929052981, \"approved_sku_mapping_ts\": \"2020-05-28T12:21:00.167389\", " +
            "\"content_comment_details\": [], \"content_lab_ticket_type\": null, \"mapped_model_confidence\": null, " +
            "\"supplier_sku_mapping_id\": null, \"supplier_sku_mapping_ts\": null, " +
            "\"automatic_classification\": false, \"datacamp_content_version\": null, \"suggest_sku_mapping_type\": " +
            "null, \"tracker_ticket_from_clab\": null, \"content_comment_from_clab\": null, " +
            "\"honest_mark_depratment_id\": null, \"psku_has_content_mappings\": false, " +
            "\"supplier_model_mapping_id\": null, \"acceptance_status_modified\": \"2020-05-28T12:20:59.631054\", " +
            "\"content_processing_task_id\": null, \"mapped_category_confidence\": null, " +
            "\"processing_status_modified\": \"2020-05-28T12:20:59.631057\", \"supplier_mapping_timestamp\": null, " +
            "\"content_status_active_error\": null, \"supplier_sku_mapping_status\": \"NONE\", " +
            "\"content_sku_mapping_sku_type\": \"MARKET\", \"market_specific_content_hash\": null, " +
            "\"suggest_sku_mapping_sku_type\": null, \"approved_sku_mapping_sku_type\": \"MARKET\", " +
            "\"comment_from_clab_modified_by\": null, \"content_processed_with_errors\": null, " +
            "\"supplier_model_mapping_status\": null, \"supplier_sku_mapping_check_ts\": null, " +
            "\"supplier_sku_mapping_sku_type\": null, \"last_primary_processing_status\": null, " +
            "\"approved_sku_mapping_confidence\": \"CONTENT\", \"deleted_approved_sku_mapping_id\": null, " +
            "\"deleted_approved_sku_mapping_ts\": null, \"supplier_category_mapping_status\": null, " +
            "\"supplier_sku_mapping_category_id\": null, \"supplier_sku_mapping_check_login\": null, " +
            "\"content_comment_from_clab_details\": [], \"market_specific_content_hash_sent\": null, " +
            "\"honest_mark_department_probability\": null, \"supplier_sku_mapping_change_reason\": null, " +
            "\"deleted_approved_sku_mapping_sku_type\": null, " +
            "\"deleted_approved_sku_mapping_confidence\": null}";

        var offer = OfferTestUtils.simpleOffer()
            .setBusinessId(BIZ_ID)
            .setShopSku(shopSku)
            .updateApprovedSkuMapping(new Offer.Mapping(1234, modifiedDate, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setCategoryIdForTests(123L, Offer.BindingKind.APPROVED);
        offerRepository.insertOffers(offer);

        JSONB json = JSONB.valueOf(str);

        MigrationRemovedOffer migrationRemovedOffer = new MigrationRemovedOffer(1L, json,
            BIZ_ID, LocalDateTime.now().minusDays(1), null);
        migrationRemovedOfferRepository.save(migrationRemovedOffer);

        LocalDate overriddenToday = LocalDate.now().plusDays(1);
        notifyAboutAutoMigratedOffersExecutor.overrideToday(overriddenToday);

        notifyAboutAutoMigratedOffersExecutor.execute();

        List<Notification> notifications = notificationRepository.findAll();
        Assertions.assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        Assert.assertEquals(Notification.NotificationType.AUTO_MIGRATED_WHITE_REPORT,
            notification.getNotificationType());
        NotificationData data = notification.getData();
        Assert.assertEquals(NotificationData.DataType.STRING_DATA_TYPE, data.getDataType());
        var stringNotificationData = (StringNotificationData) data;
        Assertions.assertThat(stringNotificationData.getData())
            .contains("Офферу id " + offer.getId() + " был проставлен маппинг 100929052981. " +
                "Есть различия в тайтле (" + offer.getTitle() + " != " + title + ").");

        Assert.assertEquals(overriddenToday.toString(),
            storageKeyValueService.getString(NotifyAboutAutoMigratedOffersExecutor.KV_KEY, null));
    }

    @Test
    public void notifyAboutAutoMigratedOffersNoSuggest() {
        String tsString = "2020-05-28T12:21:00.167389";
        LocalDateTime modifiedDate = LocalDateTime.parse(tsString);

        String shopSku = "196112";
        String title = "Интерактивная игрушка роботизированный шар Sphero Mini (Красный)";
        String str = "{\"id\": 13273054, \"title\": \"Интерактивная игрушка роботизированный шар Sphero Mini " +
            "(Красный)\", \"shop_sku\": \"196112\", \"approved_sku_mapping_id\": 100929052981, " +
            "\"approved_sku_mapping_ts\": \"2020-05-28T12:21:00.167389\"}";

        var offer = OfferTestUtils.simpleOffer()
            .setBusinessId(BIZ_ID)
            .setShopSku(shopSku)
            .setTitle(title)
            .updateApprovedSkuMapping(new Offer.Mapping(1234, modifiedDate, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setCategoryIdForTests(123L, Offer.BindingKind.APPROVED);
        offerRepository.insertOffers(offer);

        JSONB json = JSONB.valueOf(str);

        MigrationRemovedOffer migrationRemovedOffer = new MigrationRemovedOffer(1L, json,
            BIZ_ID, LocalDateTime.now().minusDays(1), null);
        migrationRemovedOfferRepository.save(migrationRemovedOffer);

        LocalDate overriddenToday = LocalDate.now().plusDays(1);
        notifyAboutAutoMigratedOffersExecutor.overrideToday(overriddenToday);

        notifyAboutAutoMigratedOffersExecutor.execute();

        List<Notification> notifications = notificationRepository.findAll();
        Assertions.assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        Assert.assertEquals(Notification.NotificationType.AUTO_MIGRATED_WHITE_REPORT,
            notification.getNotificationType());
        NotificationData data = notification.getData();
        Assert.assertEquals(NotificationData.DataType.STRING_DATA_TYPE, data.getDataType());
        var stringNotificationData = (StringNotificationData) data;
        Assertions.assertThat(stringNotificationData.getData())
            .contains("Офферу id " + offer.getId() + " был проставлен маппинг 100929052981, " +
                "который отличается от suggest маппинга 0.");
    }
}
