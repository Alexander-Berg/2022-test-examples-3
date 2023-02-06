package ru.yandex.market.mbo.tms.health.model.on.market;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.core.audit.OracleAuditServiceStub;
import ru.yandex.market.mbo.db.GuruVendorsReaderStub;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.history.EntityType;
import ru.yandex.market.mbo.history.model.EntityHistoryEntry;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.history.model.ValueType;

public class ModelOnMarketEvaluatorTest {

    private static final Long CATEGORY_ID = 1000L;
    private static final Long GLOBAL_VENDOR_ID = 100L;
    private static final Long UNPUBLISHED_GLOBAL_VENDOR_ID = 110L;
    private static final Long LOCAL_VENDOR_ID = 10L;
    private static final Long MODEL_ID = 10000L;

    private static final String PUBLISHED = "1";
    private static final String UNPUBLISHED = "2";

    @Ignore("should rewrite it MBO-14995")
    @Test
    public void testPublishedChanging() {
        AuditServiceMock audit = new AuditServiceMock();
        audit.writeActions(getAuditActions());
        GuruVendorsReaderStub guruVendorsReaderStub = new GuruVendorsReaderStub();
        generateVendors(guruVendorsReaderStub);
        OracleAuditServiceStub oracleAuditServiceStub = generateOracleAudit();

        ModelsOnMarketEvaluator modelsEvaluator = new ModelsOnMarketEvaluator(audit, guruVendorsReaderStub,
            oracleAuditServiceStub, null);

        ModelsOnMarketCounter counter = modelsEvaluator.getModelsOnMarketCounter();

        Assert.assertEquals(1, counter.getPublishedCategories().size());
        Assert.assertEquals(1, counter.getUnpublishedCategories().size());

        Assert.assertEquals(1, counter.getPublishedVendorsByHid(CATEGORY_ID).size());
        Assert.assertEquals(1, counter.getUnpublishedVendorsByHid(CATEGORY_ID).size());

        Assert.assertEquals(1, counter.getPublishedModels().size());
        Assert.assertEquals(1, counter.getUnpublishedModels().size());

    }

    public Collection<AuditAction> getAuditActions() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.HOUR, -1);

        List<AuditAction> actions = new ArrayList<>();
        actions.add(getAction(date.getTime(), "Показывать карточки",
            "Гуру-карточка", "Кластер", CATEGORY_ID));
        actions.add(getAction(date.getTime(), "Показывать карточки",
            "Кластер", "Гуру-карточка", CATEGORY_ID + 1));
        return actions;
    }

    private void generateVendors(GuruVendorsReaderStub guruService) {
        OptionImpl publishedOption = new OptionImpl(Option.OptionType.VENDOR);
        publishedOption.setId(LOCAL_VENDOR_ID);
        publishedOption.setPublished(true);
        guruService.addVendor(GLOBAL_VENDOR_ID, CATEGORY_ID, publishedOption);

        OptionImpl unpublishedOption = new OptionImpl(Option.OptionType.VENDOR);
        unpublishedOption.setId(LOCAL_VENDOR_ID + 1);
        unpublishedOption.setPublished(false);
        guruService.addVendor(UNPUBLISHED_GLOBAL_VENDOR_ID, CATEGORY_ID, unpublishedOption);
    }

    private OracleAuditServiceStub generateOracleAudit() {
        OracleAuditServiceStub oracleAuditServiceStub = new OracleAuditServiceStub();

        Calendar date = Calendar.getInstance();
        date.add(Calendar.HOUR, -1);

        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.LOCAL_VENDOR, LOCAL_VENDOR_ID,
            new Snapshot("publish_level", ValueType.INTEGER, PUBLISHED),
            new Snapshot("publish_level", ValueType.INTEGER, UNPUBLISHED),
            "", "", ""));

        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.LOCAL_VENDOR, LOCAL_VENDOR_ID + 1,
            new Snapshot("publish_level", ValueType.INTEGER, UNPUBLISHED),
            new Snapshot("publish_level", ValueType.INTEGER, PUBLISHED),
            "", "", ""));

        long modelId = MODEL_ID;
        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.MODEL, modelId,
            new Snapshot("publish_level", ValueType.INTEGER, UNPUBLISHED),
            new Snapshot("publish_level", ValueType.INTEGER, PUBLISHED),
            "", "", ""));
        ++modelId;

        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.MODEL, modelId,
            new Snapshot("publish_level", ValueType.INTEGER, PUBLISHED),
            new Snapshot("publish_level", ValueType.INTEGER, UNPUBLISHED),
            "", "", ""));
        ++modelId;

        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.MODEL, modelId,
            new Snapshot("name", ValueType.STRING, null),
            new Snapshot("name", ValueType.STRING, "test name"),
            "", "", ""));
        ++modelId;

        oracleAuditServiceStub.addEntity(new EntityHistoryEntry(0, 0, date,
            0, 0, EntityType.MODEL, modelId,
            new Snapshot("name", ValueType.STRING, "test name"),
            new Snapshot("name", ValueType.STRING, null),
            "", "", ""));

        return oracleAuditServiceStub;
    }

    private AuditAction getAction(Date date, String property, String oldVal, String newVal, Long hid) {
        AuditAction action = new AuditAction();
        action.setCategoryId(hid);
        action.setEntityType(AuditAction.EntityType.CATEGORY);
        action.setPropertyName(property);
        action.setOldValue(oldVal);
        action.setNewValue(newVal);
        action.setDate(date);
        return action;
    }
}
