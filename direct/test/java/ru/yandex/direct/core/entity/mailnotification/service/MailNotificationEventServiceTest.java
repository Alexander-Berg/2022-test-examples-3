package ru.yandex.direct.core.entity.mailnotification.service;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.Result;
import org.jooq.TableField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mailnotification.model.GenericEvent;
import ru.yandex.direct.core.entity.mailnotification.model.KeywordEvent;
import ru.yandex.direct.core.entity.mailnotification.model.ObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.EventsEventobject;
import ru.yandex.direct.dbschema.ppc.enums.EventsEventtype;
import ru.yandex.direct.dbschema.ppc.tables.records.EventsRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.mailnotification.model.KeywordEvent.addedKeywordEvent;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.dbschema.ppc.tables.Events.EVENTS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MailNotificationEventServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private MailNotificationEventService service;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo managerClientInfo;
    private ClientInfo clientInfo;
    private AdGroupInfo adGroupInfo;
    private int shard;

    private List<TableField<?, ?>> fieldsToRead = asList(EVENTS.CID, EVENTS.OBJECTUID, EVENTS.OBJECTID,
            EVENTS.UID, EVENTS.EVENTOBJECT, EVENTS.EVENTTYPE, EVENTS.JSON_DATA);

    @Before
    public void setUp() {
        managerClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(managerClientInfo);

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(managerClientInfo.getUid())));
        shard = adGroupInfo.getShard();
    }

    @Test
    public void addAddedKeywordEvent_addedSuccessfully() {
        String phrase = "sometimes nothing is the hardest thing to do";
        KeywordEvent addedKeywordEvent = addedKeywordEvent(managerClientInfo.getUid(), clientInfo.getUid(),
                adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), adGroupInfo.getAdGroup().getName(), phrase);
        service.queueEvents(managerClientInfo.getUid(), clientInfo.getClientId(), singletonList(addedKeywordEvent));

        Result<EventsRecord> actual = getActualRecords(adGroupInfo, ObjectType.ADGROUP);
        EventsRecord expectedRecord = getExpectedRecord(adGroupInfo,
                EventsEventobject.adgroup, EventsEventtype.adgr_word, "", phrase);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingOnlyGivenFields(expectedRecord,
                "cid", "objectuid", "objectid", "uid", "eventobject", "eventtype", "jsonData");
    }

    @Test
    public void addPhraseSearchPriceChangeEvent_addedSuccessfully() {
        GenericEvent searchPriceEvent = KeywordEvent
                .changedSearchPriceEvent(managerClientInfo.getUid(), clientInfo.getUid(), adGroupInfo.getAdGroup(),
                        BigDecimal.ZERO, BigDecimal.ONE);
        service.queueEvents(managerClientInfo.getUid(), clientInfo.getClientId(), singletonList(searchPriceEvent));

        Result<EventsRecord> actual = getActualRecords(adGroupInfo, ObjectType.PHRASE);
        EventsRecord expectedRecord = getExpectedRecord(adGroupInfo,
                EventsEventobject.phrase, EventsEventtype.ph_price, 0, 1);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingOnlyGivenFields(expectedRecord,
                "cid", "objectuid", "objectid", "uid", "eventobject", "eventtype", "jsonData");
    }

    @Test
    public void addPhraseContextPriceChangeEvent_addedSuccessfully() {
        GenericEvent contextPriceEvent = KeywordEvent
                .changedContextPriceEvent(managerClientInfo.getUid(), clientInfo.getUid(), adGroupInfo.getAdGroup(),
                        BigDecimal.ZERO, BigDecimal.ONE);
        service.queueEvents(managerClientInfo.getUid(), clientInfo.getClientId(), singletonList(contextPriceEvent));

        Result<EventsRecord> actual = getActualRecords(adGroupInfo, ObjectType.PHRASE);
        EventsRecord expectedRecord = getExpectedRecord(adGroupInfo,
                EventsEventobject.phrase, EventsEventtype.ph_price_ctx, 0, 1);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingOnlyGivenFields(expectedRecord,
                "cid", "objectuid", "objectid", "uid", "eventobject", "eventtype", "jsonData");
    }

    @Test
    public void addFourEvents_allAddedSuccessfully() {
        GenericEvent event = KeywordEvent
                .changedContextPriceEvent(managerClientInfo.getUid(), clientInfo.getUid(), adGroupInfo.getAdGroup(),
                        BigDecimal.ZERO, BigDecimal.ONE);

        service.queueEvents(managerClientInfo.getUid(), clientInfo.getClientId(), asList(event, event, event, event));

        Result<EventsRecord> actual = getActualRecords(adGroupInfo, ObjectType.PHRASE);

        assertThat(actual).hasSize(4);
    }

    @Test
    public void addEventByClient_noAddedEvent() {
        var anotherAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        GenericEvent event = KeywordEvent
                .changedContextPriceEvent(anotherAdGroupInfo.getUid(), anotherAdGroupInfo.getUid(),
                        anotherAdGroupInfo.getAdGroup(),
                        BigDecimal.ZERO, BigDecimal.ONE);

        service.queueEvents(anotherAdGroupInfo.getUid(), anotherAdGroupInfo.getClientId(), singletonList(event));

        Result<EventsRecord> actual = getActualRecords(anotherAdGroupInfo, ObjectType.PHRASE);

        assertThat(actual).isEmpty();
    }

    @QueryWithoutIndex("Используется только в тестах")
    private Result<EventsRecord> getActualRecords(AdGroupInfo adGroupInfo, ObjectType objectType) {
        return dslContextProvider.ppc(shard)
                .select(fieldsToRead)
                .from(EVENTS)
                .where(EVENTS.OBJECTID.eq(adGroupInfo.getAdGroupId())
                        .and(EVENTS.EVENTOBJECT.eq(ObjectType.toSource(objectType))))
                .fetch()
                .into(EVENTS);
    }

    private EventsRecord getExpectedRecord(AdGroupInfo adGroupInfo,
                                           EventsEventobject objType, EventsEventtype eventType, Object oldValue,
                                           Object newValue) {
        if (oldValue instanceof String || newValue instanceof String) {
            oldValue = "\"" + oldValue + "\"";
            newValue = "\"" + newValue + "\"";
        }
        EventsRecord expectedRecord = new EventsRecord();
        expectedRecord.setCid(adGroupInfo.getCampaignId());
        expectedRecord.setObjectuid(clientInfo.getUid());
        expectedRecord.setUid(managerClientInfo.getUid());
        expectedRecord.setObjectid(adGroupInfo.getAdGroupId());
        expectedRecord.setEventobject(objType);
        expectedRecord.setEventtype(eventType);
        expectedRecord.setJsonData(String.format(
                "{\"pid\":%d,\"group_name\":\"%s\",\"old_text\":%s,\"new_text\":%s}",
                adGroupInfo.getAdGroupId(), adGroupInfo.getAdGroup().getName(), oldValue, newValue));
        return expectedRecord;
    }

}
