package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateVar;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.messages.TemplateVarAlgorithm;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AndroidPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AppMetricaPushMessage;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.IOSPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessages;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.platform.PlatformUserIdType;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.services.trigger.ProcessErrorCodes;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.core.test.loggers.TestSentPushesLogWriter;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper;
import ru.yandex.market.crm.core.test.utils.AppMetricaHelper.SendPushesRequest;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.MobileTablesHelper;
import ru.yandex.market.crm.core.test.utils.PlatformHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.MobileAppInfoUtil;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.external.personal.PersonalFullname;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.mapreduce.domain.push.ActionType;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.platform.api.Edge;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.control.IdSplitter;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.crm.triggers.test.BpmnErrorMatcher;
import ru.yandex.market.crm.triggers.test.helpers.BigBTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper.ProcessInstance;
import ru.yandex.market.crm.triggers.test.helpers.builders.SendPushTaskBuilder;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.MobileTablesHelper.tokenStatus;
import static ru.yandex.market.crm.triggers.test.helpers.BigBTestHelper.profile;

/**
 * @author apershukov
 */
public class SendPushTriggerTaskTest extends AbstractServiceTest {
    private static final DateTimeFormatter ACTIVITY_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String MOBILE_APP_INFO = "MobileAppInfo";
    private static final String METRIKA_MOBILE_APP = "MetrikaMobileApp";
    private static final String PUSH_TOKEN_STATUSES = "PushTokenStatuses";
    private static final String GENERIC_SUBSCRIPTION = "GenericSubscription";
    private static final String TRIGGER_ID = "test_trigger";
    private static final String PUSH_TASK = "push_task";

    private static final String DEVICE_ID_1 = "device_id_1";
    private static final String DEVICE_ID_2 = "device_id_2";

    private static final String DEVICE_ID_HASH_1 = "device_id_hash_1";
    private static final String DEVICE_ID_HASH_2 = "device_id_hash_2";

    private static final String UUID_1 = "uuid-111";
    private static final String UUID_2 = "uuid-222";

    private static final long CRYPTA_ID = 111;

    private static final long PUID = 111;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Inject
    private SendPushTriggerTask task;
    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;
    @Inject
    private TriggerService triggerService;
    @Inject
    private TriggersHelper triggersHelper;
    @Inject
    private PlatformHelper platformHelper;
    @Inject
    private AppMetricaHelper appMetricaHelper;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private TestSentPushesLogWriter sentPushesLogWriter;
    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;
    @Inject
    private BigBTestHelper bigBTestHelper;
    @Inject
    private TestEnvironmentResolver environmentResolver;
    @Inject
    private IdSplitter idSplitter;
    @Inject
    private PersonalService personalService;

    @Value("${global.control.percent}")
    private int controlPercent;

    private MessageTemplate<PushMessageConf> template;

    private static MobileAppInfo.Builder mobileAppInfoBuilder(String uuid, boolean registered) {
        ru.yandex.market.crm.platform.commons.Uid platformUuid = toUuid(uuid);

        return MobileAppInfo.newBuilder()
                .addUid(platformUuid)
                .setUuid(platformUuid.getStringValue())
                .setAppName(MobileAppInfoUtil.BLUE_AND_APPNAME)
                .setPlatform(MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID)
                .setRegistered(registered)
                .setDisabledBySystem(false)
                .setTriggersRegistered(registered)
                .setModificationTime("2019-02-01 14:13:05.0");
    }


    private static MetrikaMobileApp metrikaMobileApp(String uuid,
                                                     String deviceId,
                                                     String deviceIdHash) {
        return MobileTablesHelper.metrikaMobileApp(Color.BLUE, uuid, deviceId, deviceIdHash)
                .toBuilder()
                .setAppInfo(MetrikaMobileApp.AppInfo.newBuilder().setPlatform(MobilePlatform.ANDROID))
                .build();
    }

    private static MetrikaMobileApp metrikaMobileApp(String uuid,
                                                     String deviceId,
                                                     String deviceIdHash,
                                                     Locale locale) {
        return metrikaMobileApp(uuid, deviceId, deviceIdHash).toBuilder().setLocale(locale.toString()).build();
    }

    private static MetrikaMobileApp metrikaMobileApp(String uuid,
                                                     String deviceId,
                                                     String deviceIdHash,
                                                     Locale locale,
                                                     String activityTime) {
        return metrikaMobileApp(uuid, deviceId, deviceIdHash, locale).toBuilder().setActivityTime(activityTime).build();
    }

    private static GenericSubscription genericSubscription(String uuid, long subscriptionType, boolean subscribed) {
        ru.yandex.market.crm.platform.commons.Uid platformUuid = toUuid(uuid);

        GenericSubscription.Channel channel = GenericSubscription.Channel.PUSH;
        GenericSubscription.Status status = subscribed
                ? GenericSubscription.Status.SUBSCRIBED
                : GenericSubscription.Status.UNSUBSCRIBED;
        long now = Instant.now().toEpochMilli();

        return GenericSubscription.newBuilder()
                .setUid(platformUuid)
                .setId(channel.getNumber() + "$" + subscriptionType)
                .setChannel(channel)
                .setType(subscriptionType)
                .setStatus(status)
                .setCreatedAt(now)
                .setModifiedAt(now)
                .build();
    }

    private static ru.yandex.market.crm.platform.commons.Uid toUuid(String uuid) {
        return Uids.create(UidType.UUID, uuid);
    }

    private static ru.yandex.market.crm.platform.commons.Uid toPuid(long puid) {
        return Uids.create(UidType.PUID, puid);
    }

    private static ru.yandex.market.crm.platform.commons.Uid toDeviceIdHash(String deviceIdHash) {
        return Uids.create(UidType.MM_DEVICE_ID_HASH, deviceIdHash);
    }

    @Before
    public void setUp() {
        environmentResolver.setEnvironment(Environment.PRODUCTION);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        template = prepareMessageTemplate();
    }

    @Test
    public void testThrowBpmnErrorIfCommunicationIdAttributeReturnsNull() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("null")
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, Locale.US))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        try {
            runTask(Uid.asUuid(UUID_1), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testSendToUuidFromCommunicationIdAttribute() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid '" + UUID_2 + "'")
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_2),
                        mobileAppInfoBuilder(UUID_2, true)
                                .setKeyUid(toUuid(UUID_2))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, Locale.US))
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_2),
                        metrikaMobileApp(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2, Locale.US))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_2),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_2, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        runTask(Uid.asUuid(UUID_1), process);

        appMetricaHelper.verify();

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(1, records.size());

        Map<String, String> record = records.get(0);
        assertEquals(UUID_2, record.get("uuid"));
    }

    /**
     * Если в Идентификаторе для коммуникаций указано 2 варианта, причём первый (uuid) равен null,
     * тогда отправляем на uuid для указанного puid
     */
    @Test
    public void testSendToPuidFromCommunicationIdAttributeIfFirstIdIsNull() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid(null) ?: puid(" + PUID + ")")
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1,  DEVICE_ID_HASH_1, Locale.US,
                                LocalDateTime.now().format(ACTIVITY_DT_FORMATTER)))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_2), process);

        appMetricaHelper.verify();

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(1, records.size());

        Map<String, String> record = records.get(0);
        assertEquals(UUID_1, record.get("uuid"));
    }

    /**
     * Если в поле Идентификатор коммуникаций указан идентификатор типа puid, и в фактах MetrikaMobileApp для puid'а
     * время последней активности больше, чем 10 дней назад, то push не отправляем,
     * при этом бросаем исключение типа NO_COMMUNICATION_IDS
     */
    @Test
    public void testThrowBpmnErrorIfCommunicationIdIsPuidAndActivityTimeWasLongTimeAgo() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid(null) ?: puid(" + PUID + ")")
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1,  DEVICE_ID_HASH_1, Locale.US,
                                LocalDateTime.now().minusDays(15).format(ACTIVITY_DT_FORMATTER)))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );
        try {
            runTask(Uid.asUuid(UUID_2), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testThrowBpmnErrorIfNullSubscriptionTypeAndMobileAppInfoNotRegisteredForUuidFromCommunicationIdAttribute()
            throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid '" + UUID_1 + "'")
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, false)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        try {
            runTask(Uid.asUuid(UUID_1), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testThrowBpmnErrorIfNoSubscriptionForUuidFromCommunicationIdAttribute() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid '" + UUID_1 + "'").setSubscriptionType(64)
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(GENERIC_SUBSCRIPTION, toUuid(UUID_1), genericSubscription(UUID_1, 63, true))
                .putFact(GENERIC_SUBSCRIPTION, toUuid(UUID_1), genericSubscription(UUID_1, 64, false));

        try {
            runTask(Uid.asUuid(UUID_1), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testSendToUuidFromCommunicationIdAttributeIfSubscriptionNotExistOnTesting() throws Exception {
        environmentResolver.setEnvironment(Environment.TESTING);
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setCommunicationId("uuid '" + UUID_1 + "'").setSubscriptionType(63)
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process);

        appMetricaHelper.verify();
    }

    @Test
    public void testThrowBpmnErrorIfNullSubscriptionTypeAndMobileAppInfoNotRegisteredForNoCommunicationIdAttribute()
            throws Exception {
        ProcessDefinition process = prepareTrigger();

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, false)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        platformHelper
                .prepareUser(
                        UidType.UUID,
                        UUID_1,
                        createUser(
                                Uid.asUuid(UUID_1),
                                Uid.asUuid(UUID_1)
                        )
                );

        try {
            runTask(Uid.asUuid(UUID_1), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testThrowBpmnErrorIfNoSubscriptionNotFromCommunicationIdAttributeOnProd() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setSubscriptionType(63)
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(GENERIC_SUBSCRIPTION, toUuid(UUID_1), genericSubscription(UUID_1, 63, false))
                .putFact(GENERIC_SUBSCRIPTION, toUuid(UUID_1), genericSubscription(UUID_1, 64, true));

        platformHelper
                .prepareUser(
                        UidType.UUID,
                        UUID_1,
                        createUser(
                                Uid.asUuid(UUID_1),
                                Uid.asUuid(UUID_1)
                        )
                );

        try {
            runTask(Uid.asUuid(UUID_1), process);
        } catch (BpmnError error) {
            assertThat(error.getErrorCode(), equalTo(ProcessErrorCodes.NO_COMMUNICATION_IDS));
            return;
        }
        fail("Should throws no communication ids error");
    }

    @Test
    public void testSendIfSubscriptionExistsForNoCommunicationIdAttribute() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setSubscriptionType(63)
        );

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(GENERIC_SUBSCRIPTION, toUuid(UUID_1), genericSubscription(UUID_1, 63, true));

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process);

        appMetricaHelper.verify();
    }

    /**
     * В случае если процесс запущен с подходящим для отправки uuid'ом
     * отправка происходит по нему даже если такой пользователь нам не известен
     */
    @Test
    public void testSendToSameUuidForWhichProcessWasStarted() throws Exception {
        ProcessDefinition process = prepareTrigger();

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process);

        appMetricaHelper.verify();

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(1, records.size());

        Map<String, String> record = records.get(0);
        assertEquals(UUID_1, record.get("uuid"));
        assertEquals(process.getId(), record.get("triggerId"));
        assertEquals(PUSH_TASK, record.get("blockId"));
        assertEquals(template.getId(), record.get("templateId"));
    }

    /**
     * Отправка push не должна зависеть от локали
     */
    @Test
    public void testSendingDoesntDependOnLocale() throws Exception {
        ProcessDefinition process = prepareTrigger();

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_2),
                        mobileAppInfoBuilder(UUID_2, true)
                                .setKeyUid(toUuid(UUID_2))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1, new Locale("ru", "RU")))
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_2),
                        metrikaMobileApp(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2, Locale.US))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_2),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_2, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        runTask(Uid.asUuid(UUID_1), process);
        runTask(Uid.asUuid(UUID_2), process);

        appMetricaHelper.verify();
    }

    /**
     * В случае если в контексте процесса находится идентификатор
     * отличный от uuid, пытается получить uuid устройства с которым от связан
     * в настоящий момент. Если такой uuid нашелся и он пригоден для отправки -
     * используем его.
     */
    @Test
    public void testUseLinkedDeviceForPuidIfPossible() throws Exception {
        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toPuid(PUID))
                                .addUid(toPuid(PUID))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        ProcessDefinition process = prepareTrigger();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asPuid(PUID), process);

        appMetricaHelper.verify();
    }

    /**
     * В случае если связанное устройство недоступно для коммуникации и галочка использования только
     * связанного устройства не установлена, для отправки пушей используются склеенные uuid'ы,
     * имеющие непосредственную связь с идентификатором пользователя в контексте процесса
     */
    @Test
    public void testUseNearUuidIfLinkedDeviceIsUnavailable() throws Exception {
        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, false)
                                .setKeyUid(toPuid(PUID))
                                .addUid(toPuid(PUID))
                                .build()
                )
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_2),
                        mobileAppInfoBuilder(UUID_2, true)
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_2), metrikaMobileApp(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_2),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_2, true)
                );

        platformHelper
                .prepareUser(
                        UidType.PUID,
                        String.valueOf(PUID),
                        createUser(
                                Uid.asPuid(PUID),
                                Uid.asUuid(UUID_2)
                        )
                );

        ProcessDefinition process = prepareTrigger();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        runTask(Uid.asPuid(PUID), process);

        appMetricaHelper.verify();
    }

    /**
     * В случае, если связанное устройство недоступно и при этом установлена
     * галочка использования только связанного устройства вместо того чтобы
     * использовать склейку выбрасывается ошибка отсутствия идентификаторов
     * для коммуникации
     */
    @Test
    public void testThrowExceptionIfNoLinkedDeviceIsAvailableWhenRequired() throws Exception {
        thrown.expect(BpmnErrorMatcher.expectCode(ProcessErrorCodes.NO_COMMUNICATION_IDS));

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );

        platformHelper
                .prepareUser(
                        UidType.PUID,
                        String.valueOf(PUID),
                        createUser(
                                Uid.asPuid(PUID),
                                Uid.asUuid(UUID_1)
                        )
                );

        ProcessDefinition process = prepareTrigger(
                SendPushTaskBuilder::useSameDeviceOnly
        );

        runTask(Uid.asPuid(PUID), process);
    }

    @Test
    public void testThrowExceptionIfDeviceIsDisabled() throws Exception {
        thrown.expect(BpmnErrorMatcher.expectCode(ProcessErrorCodes.NO_COMMUNICATION_IDS));

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .setDisabledBySystem(true)
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(DEVICE_ID_HASH_1, true)
                );

        platformHelper
                .prepareUser(
                        UidType.PUID,
                        String.valueOf(PUID),
                        createUser(
                                Uid.asPuid(PUID),
                                Uid.asUuid(UUID_1)
                        )
                );

        ProcessDefinition process = prepareTrigger();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asPuid(PUID), process);
    }

    /**
     * Ссылки в триггерных пушах помечаются параметром clid=622
     */
    @Test
    public void testSetClidToPushUrl() throws Exception {
        ProcessDefinition process = prepareTrigger();

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(DEVICE_ID_HASH_1, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process);

        SendPushesRequest request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull("No push message was sent", request);

        PushMessages messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        AppMetricaPushMessage<AndroidPushMessageContent> message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        String url = message.getOpenUrl();
        assertNotNull(url);

        assertTrue("Url must contain correct clid. Url: " + url, url.contains("clid=622"));
    }

    /**
     * Пуш не должен быть отправлен на неактивный device_id
     */
    @Test
    public void testPushDontSendOnNonActiveDevice() throws Exception {
        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toPuid(PUID))
                                .addUid(toPuid(PUID))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, false)
                );

        ProcessDefinition process = prepareTrigger();

        thrown.expect(BpmnErrorMatcher.expectCode(ProcessErrorCodes.NO_COMMUNICATION_IDS));
        runTask(Uid.asPuid(PUID), process);
    }

    /**
     * При отправке пуша из двух device_id будет выбран тот, который является активным в АппМетрике
     */
    @Test
    public void testFromTwoDevicesPushSendOnActiveDevice() throws Exception {
        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toPuid(PUID))
                                .addUid(toPuid(PUID))
                                .build()
                )
                .putFact(
                        MOBILE_APP_INFO,
                        toPuid(PUID),
                        mobileAppInfoBuilder(UUID_2, true)
                                .setKeyUid(toPuid(PUID))
                                .addUid(toPuid(PUID))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_2), metrikaMobileApp(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(DEVICE_ID_HASH_1, true)
                );

        ProcessDefinition process = prepareTrigger();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asPuid(PUID), process);
        appMetricaHelper.verify();
    }

    /**
     * В случае если в блоке включен учет глобального контроля, на uuid который в процессе первоначального
     * разбиения попал в глобальный контроль отправка не происходит
     */
    @Test
    public void testDoNotSendToDeviceFromGlobalControl() throws Exception {
        bigBTestHelper.prepareProfile(Uid.asUuid(UUID_1), profile(CRYPTA_ID));

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(String.valueOf(CRYPTA_ID), false)
        );

        ProcessDefinition process = prepareTriggerWithGlobalControl();

        prepareDeviceData();

        runAndAssertControlError(Uid.asUuid(UUID_1), process);
    }

    /**
     * В случае если в блоке включен учет глобального контроля, на uuid который в процессе первоначального
     * разбиения попал в глобальную целевую группу, происходит отправка
     */
    @Test
    public void testSendToDeviceFromGlobalTarget() throws Exception {
        bigBTestHelper.prepareProfile(Uid.asUuid(UUID_1), profile(CRYPTA_ID));

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(String.valueOf(CRYPTA_ID), true)
        );

        ProcessDefinition process = prepareTriggerWithGlobalControl();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process);

        appMetricaHelper.verify();
    }

    /**
     * В случае если в блок со включенным вычитанием глобального контроля попадает
     * uuid которого нет в предварительном разбиении, и при этом его crypta-id так же не попал в
     * предварительное разбиение, crypta-id определяется в глобальный сплит. В тот же сплит попадает uuid.
     * <p>
     * При этом сохраняется результат сплитовки его crypta-id
     */
    @Test
    public void testSplitUnknownCryptaId() throws Exception {
        bigBTestHelper.prepareProfile(Uid.asUuid(UUID_1), profile(CRYPTA_ID));

        var process = prepareTriggerWithGlobalControl();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runExpectingControlError(Uid.asUuid(UUID_1), process);

        var isInTarget = idSplitter.isInTarget(String.valueOf(CRYPTA_ID), controlPercent);

        if (isInTarget) {
            appMetricaHelper.verify();
        }

        var cryptaIdRow = globalSplitsTestHelper.getGlobalSplitsRows().get(0);
        assertNotNull("Crypta id table row was not inserted", cryptaIdRow);

        assertEquals(String.valueOf(CRYPTA_ID), cryptaIdRow.getString("crypta_id"));
        assertEquals(
                "Uuid split does not match its crypta id split",
                isInTarget,
                cryptaIdRow.getBool("in_target")
        );
    }

    /**
     * В случае если в блок со включенным вычитанием глобального контроля попадает
     * uuid которого нет в предварительном разбиении, и при этом его crypta-id не удалось
     * найти uuid определяется в глобальный сплит по хешу от него самого
     */
    @Test
    public void testSplitUnkownUuidWithoutCryptaId() throws Exception {
        bigBTestHelper.prepareNotFound(Uid.asUuid(UUID_1));

        var process = prepareTriggerWithGlobalControl();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runExpectingControlError(Uid.asUuid(UUID_1), process);

        if (idSplitter.isInTarget(UUID_1, controlPercent)) {
            appMetricaHelper.verify();
        }
    }

    /**
     * Отправка push должна зависеть от платформы устройства (Android, iOS)
     */
    @Test
    public void testSendingDependsOnMobilePlatform() throws Exception {
        ProcessDefinition process = prepareTrigger();

        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_2),
                        mobileAppInfoBuilder(UUID_2, true)
                                .setKeyUid(toUuid(UUID_2))
                                .setPlatform(MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE)
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1),
                        metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_2),
                        metrikaMobileApp(UUID_2, DEVICE_ID_2, DEVICE_ID_HASH_2)
                                .toBuilder()
                                .setAppInfo(MetrikaMobileApp.AppInfo.newBuilder().setPlatform(MobilePlatform.IOS))
                                .build()
                )
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                )
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_2),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_2, true)
                );

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        appMetricaHelper.expectDevice(DEVICE_ID_HASH_2);

        runTask(Uid.asUuid(UUID_1), process);
        runTask(Uid.asUuid(UUID_2), process);

        appMetricaHelper.verify();

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(2, records.size());

        Map<String, String> record = records.get(0);
        assertFalse(record.containsKey("platform"));

        record = records.get(1);
        assertFalse(record.containsKey("platform"));
    }

    /**
     * Если в шаблоне пуша присутствуют секретные переменные, то при логировании отправки секретные переменные
     * в данных заменяются строкой из *, длина которой равна длине значения секретной переменной
     */
    @Test
    public void testHidingSecretVarsInLog() throws Exception {
        String title = "secretVar: ${secretVar1} and notSecretVar: ${notSecretVar1}";
        String text = "secretVar: ${secretVar2} and notSecretVar: ${notSecretVar2}";
        List<MessageTemplateVar> vars = List.of(
                new MessageTemplateVar("secretVar1", MessageTemplateVar.Type.STRING, true),
                new MessageTemplateVar("secretVar2", MessageTemplateVar.Type.NUMBER, true),
                new MessageTemplateVar("notSecretVar1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("notSecretVar2", MessageTemplateVar.Type.NUMBER, false)
        );

        AndroidPushConf androidPushConf = new AndroidPushConf();
        androidPushConf.setTitle(title);
        androidPushConf.setText(text);

        IosPushConf iosPushConf = new IosPushConf();

        template = prepareMessageTemplate(androidPushConf, iosPushConf, vars);

        ProcessDefinition process = prepareTrigger();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process, processInstance ->
                processInstance
                        .setVariable("secretVar1", "secret_key")
                        .setVariable("secretVar2", 123)
                        .setVariable("notSecretVar1", "public_key")
                        .setVariable("notSecretVar2", 456)
        );

        appMetricaHelper.verify();

        List<Map<String, String>> records = sentPushesLogWriter.getRecordsAsList();
        assertEquals(1, records.size());

        Map<String, String> record = records.get(0);
        assertEquals(UUID_1, record.get("uuid"));
        assertEquals(process.getId(), record.get("triggerId"));
        assertEquals(PUSH_TASK, record.get("blockId"));
        assertEquals(template.getId(), record.get("templateId"));

        String expectedTitle = title
                .replace("${secretVar1}", "*".repeat("secret_key".length()))
                .replace("${notSecretVar1}", "public_key");
        assertEquals(expectedTitle, record.get("title"));

        String expectedText = text
                .replace("${secretVar2}", "***")
                .replace("${notSecretVar2}", "456");
        assertEquals(expectedText, record.get("text"));
    }

    /**
     * Корректные параметры во входных данных должны подставляться в сообщение
     */
    @Test
    public void testSetupValidTimeToLive() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setTimeToLive("300").setTimeToLiveOnDevice("200")
        );
        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        runTask(Uid.asUuid(UUID_1), process);

        SendPushesRequest sendPushesRequest = appMetricaHelper.pollForSendRequest(5);
        List<AppMetricaHelper.Batch> batches = sendPushesRequest.getSendBatchRequest().getBatches();
        AppMetricaPushMessage<AndroidPushMessageContent> androidMessage =
                batches.get(0).getPushMessages().getAndroidAppMetricaPushMessage();
        Long timeToLiveOnDevice = androidMessage.getContent().getTimeToLiveOnDevice();
        assertNotNull(timeToLiveOnDevice);
        assertEquals(200L, timeToLiveOnDevice.longValue());
        Integer timeToLive = androidMessage.getContent().getTimeToLive();
        assertNotNull(timeToLive);
        assertEquals(300, timeToLive.intValue());

        AppMetricaPushMessage<IOSPushMessageContent> iosMessage =
                batches.get(0).getPushMessages().getiOSAppMetricaPushMessage();
        Integer expiration = iosMessage.getContent().getExpiration();
        assertNotNull(expiration);
        assertEquals(300, expiration.intValue());
    }

    /**
     * Некорректный параметр во входных данных <b>не</b> должен подставляться в сообщение
     */
    @Test
    public void testSetupInvalidTimeToLive() throws Exception {
        ProcessDefinition process = prepareTrigger(
                builder -> builder.setTimeToLive("invalid")
        );
        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);
        runTask(Uid.asUuid(UUID_1), process);

        SendPushesRequest sendPushesRequest = appMetricaHelper.pollForSendRequest(5);
        List<AppMetricaHelper.Batch> batches = sendPushesRequest.getSendBatchRequest().getBatches();
        AppMetricaPushMessage<AndroidPushMessageContent> androidMessage =
                batches.get(0).getPushMessages().getAndroidAppMetricaPushMessage();
        assertNull(androidMessage.getContent().getTimeToLiveOnDevice());
        assertNull(androidMessage.getContent().getTimeToLive());

        AppMetricaPushMessage<IOSPushMessageContent> iosMessage =
                batches.get(0).getPushMessages().getiOSAppMetricaPushMessage();
        assertNull(iosMessage.getContent().getExpiration());
    }

    /**
     * Если в конфигурации шаблона сообщения настроены вычислимые с помощью скрипта переменные,
     * то они должны быть конкретно вычислены и подставлены в сообщение
     */
    @Test
    public void testComputeWithScriptComputableVariable() throws Exception {
        var title = "simpleVar: ${simpleVar1} and computableVar: ${computableVar1}";
        var text = "simpleVar: ${simpleVar2} and computableVar: ${computableVar2}";
        var vars = List.of(
                new MessageTemplateVar("simpleVar1", MessageTemplateVar.Type.STRING, false),
                new MessageTemplateVar("simpleVar2", MessageTemplateVar.Type.NUMBER, false),
                new MessageTemplateVar("computableVar1", MessageTemplateVar.Type.STRING, false,
                        true, null, null, "'result=' + processStrVar"),
                new MessageTemplateVar("computableVar2", MessageTemplateVar.Type.NUMBER, false,
                        true, null, null, "100000 + processNumVar")
        );

        var androidPushConf = new AndroidPushConf();
        androidPushConf.setTitle(title);
        androidPushConf.setText(text);

        var iosPushConf = new IosPushConf();

        template = prepareMessageTemplate(androidPushConf, iosPushConf, vars);

        var process = prepareTrigger();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process, processInstance ->
                processInstance
                        .setVariable("simpleVar1", "some_simple_var")
                        .setVariable("simpleVar2", 12345)
                        .setVariable("processStrVar", "some_string")
                        .setVariable("processNumVar", 500)
        );

        var request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull("No push message was sent", request);

        var messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        var message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        var expectedTitle = title
                .replace("${simpleVar1}", "some_simple_var")
                .replace("${computableVar1}", "result=some_string");
        assertEquals(expectedTitle, message.getContent().getTitle());

        var expectedText = text
                .replace("${simpleVar2}", "12345")
                .replace("${computableVar2}", "100500");
        assertEquals(expectedText, message.getContent().getText());
    }

    /**
     * Если в контексте процесса уже существует переменная с тем же именем, что и вычисляемая переменная из сообщения,
     * то отправка падает с ошибкой
     */
    @Test
    public void testFailSendingIfContextHasVariableWithNameSimilarComputableVariable() {
        var vars = List.of(
                new MessageTemplateVar("computableVar1", MessageTemplateVar.Type.STRING, false,
                        true, null, null, "'result=' + processStrVar")
        );

        var androidPushConf = new AndroidPushConf();
        androidPushConf.setTitle("title");
        androidPushConf.setText("text");

        var iosPushConf = new IosPushConf();

        template = prepareMessageTemplate(androidPushConf, iosPushConf, vars);

        var process = prepareTrigger();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        try {
            runTask(Uid.asUuid(UUID_1), process, processInstance ->
                    processInstance
                            .setVariable("computableVar1", "some_simple_var")
                            .setVariable("processStrVar", "some_string")
            );
            fail("Task running didn't throw expected exception");
        } catch (Exception actualException) {
            assertEquals(
                    "Process context already has variable with name of computable variable: computableVar1",
                    actualException.getMessage()
            );
        }
    }

    /**
     * Если для вычисления переменной шаблона сообщения используется и алгоритм, и скрипт, то сперва переменная события
     * должна быть обработана алгоритмом, а полученное значение обработано скриптом преобразования
     * и установлено в сообщении
     */
    @Test
    public void testComputeVarWithAlgorithmAndScript() throws Exception {
        var title = "A. Var: ${computableVar}";
        var text = "B. Var: ${computableVar}";
        var script = """
                String name = "${$algorithmResult.surname} ${$algorithmResult.forename} ${$algorithmResult.patronymic}";
                return name;
                """;
        var eventVarName = "personalFullNameId";
        var eventVarValue = "some_personal_full_name_id";
        var vars = List.of(
                new MessageTemplateVar("computableVar", MessageTemplateVar.Type.STRING, false,
                        true, TemplateVarAlgorithm.PERSONAL_FULL_NAME, eventVarName, script)
        );

        var androidPushConf = new AndroidPushConf();
        androidPushConf.setTitle(title);
        androidPushConf.setText(text);

        var iosPushConf = new IosPushConf();

        template = prepareMessageTemplate(androidPushConf, iosPushConf, vars);

        when(personalService.retrieveFullname(eventVarValue))
                .thenReturn(new PersonalFullname("Иван", "Иванов", "Иванович"));

        var process = prepareTrigger();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process, processInstance ->
                processInstance.setVariable(eventVarName, eventVarValue)
        );

        var request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull("No push message was sent", request);

        var messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        var message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        var expectedTitle = title
                .replace("${computableVar}", "Иванов Иван Иванович");
        assertEquals(expectedTitle, message.getContent().getTitle());

        var expectedText = text
                .replace("${computableVar}", "Иванов Иван Иванович");
        assertEquals(expectedText, message.getContent().getText());
    }

    /**
     * Если для вычисления переменной шаблона сообщения используется алгоритм,
     * а скрипт заполнен символами пробела и переноса, то переменная события обрабатывается только алгоритмом,
     * и полученное значение устанавливается в сообщении
     */
    @Test
    public void testComputeVarWithAlgorithmOnly() throws Exception {
        var title = "Some title!";
        var text = "Var: ${computableVar.forename}";
        var emptyScript = "\n\r\n    \r\n\n   ";
        var eventVarName = "some_object.field_lvl1.field_lvl2";
        var eventVarValue = "some_personal_full_name_id";
        var vars = List.of(
                new MessageTemplateVar("computableVar", MessageTemplateVar.Type.OBJECT, false,
                        true, TemplateVarAlgorithm.PERSONAL_FULL_NAME, eventVarName, emptyScript)
        );

        var androidPushConf = new AndroidPushConf();
        androidPushConf.setTitle(title);
        androidPushConf.setText(text);

        var iosPushConf = new IosPushConf();

        template = prepareMessageTemplate(androidPushConf, iosPushConf, vars);

        when(personalService.retrieveFullname(eventVarValue))
                .thenReturn(new PersonalFullname("Иван", "Иванов", "Иванович"));

        var process = prepareTrigger();

        prepareDeviceData();

        appMetricaHelper.expectDevice(DEVICE_ID_HASH_1);

        runTask(Uid.asUuid(UUID_1), process, processInstance ->
                processInstance.setVariable(
                        "some_object",
                        Map.of("field_lvl1", Map.of("field_lvl2", eventVarValue))
                )
        );

        var request = appMetricaHelper.pollForSendRequest(60);
        assertNotNull("No push message was sent", request);

        var messages = request.getSendBatchRequest().getBatches().get(0).getPushMessages();
        assertNotNull(messages);

        var message = messages.getAndroidAppMetricaPushMessage();
        assertNotNull(message);

        assertEquals("Var: Иван", message.getContent().getText());

    }

    private void prepareDeviceData() {
        platformHelper
                .putFact(
                        MOBILE_APP_INFO,
                        toUuid(UUID_1),
                        mobileAppInfoBuilder(UUID_1, true)
                                .setKeyUid(toUuid(UUID_1))
                                .build()
                )
                .putFact(METRIKA_MOBILE_APP, toUuid(UUID_1), metrikaMobileApp(UUID_1, DEVICE_ID_1, DEVICE_ID_HASH_1))
                .putFact(
                        PUSH_TOKEN_STATUSES,
                        toDeviceIdHash(DEVICE_ID_HASH_1),
                        tokenStatus(Color.BLUE, DEVICE_ID_HASH_1, true)
                );
    }

    private void runTask(Uid uid, ProcessDefinition process) throws Exception {
        runTask(uid, process, processInstance -> {
        });
    }

    private void runTask(Uid uid,
                         ProcessDefinition processDefinition,
                         Consumer<ProcessInstance> processInstanceCustomizer) throws Exception {
        ProcessInstance processInstance = new ProcessInstance(uid);
        processInstanceCustomizer.accept(processInstance);

        triggersHelper.runTask(
                task,
                processDefinition.getId(),
                PUSH_TASK,
                processInstance
        );
    }

    private ProcessDefinition prepareTriggerWithGlobalControl() {
        return prepareTrigger(block -> block.globalControlEnabled(true));
    }

    private ProcessDefinition prepareTrigger() {
        return prepareTrigger(Function.identity());
    }

    private ProcessDefinition prepareTrigger(Function<SendPushTaskBuilder, SendPushTaskBuilder> configurer) {
        SendPushTaskBuilder builder = TriggersHelper.triggerBuilder(TRIGGER_ID)
                .startEvent().message(MessageTypes.COIN_CREATED)
                .sendPushTask(PUSH_TASK)
                .templateId(template.getId());

        BpmnModelInstance instance = configurer.apply(builder)
                .endEvent()
                .done();

        return triggerService.addTrigger(instance, null);
    }

    private MessageTemplate<PushMessageConf> prepareMessageTemplate() {
        AndroidPushConf androidPushConf = new AndroidPushConf();
        androidPushConf.setActionType(ActionType.URL);
        androidPushConf.setAction("https://market.yandex.ru/product/111");

        IosPushConf iosPushConf = new IosPushConf();

        return prepareMessageTemplate(androidPushConf, iosPushConf, Collections.emptyList());
    }

    private MessageTemplate<PushMessageConf> prepareMessageTemplate(AndroidPushConf androidPushConf,
                                                                    IosPushConf iosPushConf,
                                                                    List<MessageTemplateVar> vars) {
        var config = new PushMessageConf();
        config.setPushConfigs(Map.of(
                androidPushConf.getPlatform(), androidPushConf,
                iosPushConf.getPlatform(), iosPushConf
        ));
        config.setVars(vars);

        var template = new MessageTemplate<PushMessageConf>();
        template.setType(MessageTemplateType.PUSH);
        template.setId(UUID.randomUUID().toString());
        template.setName("Test template");
        template.setVersion(1);
        template.setKey(UUID.randomUUID().toString());
        template.setState(MessageTemplateState.PUBLISHED);
        template.setConfig(config);

        messageTemplatesDAO.save(template);
        return template;
    }

    private void runAndAssertControlError(Uid uid, ProcessDefinition process) throws Exception {
        assertTrue(
                "Expected error did not happen",
                runExpectingControlError(uid, process)
        );
    }

    private boolean runExpectingControlError(Uid uid, ProcessDefinition process) throws Exception {
        try {
            runTask(uid, process);
        } catch (Exception e) {
            if (BpmnErrorMatcher.expectCode(ProcessErrorCodes.IN_GLOBAL_CONTROL).matches(e)) {
                return true;
            }
            throw e;
        }
        return false;
    }

    private ru.yandex.market.crm.platform.api.User createUser(Uid... userUids) {
        List<ru.yandex.market.crm.platform.commons.Uid> uids = Arrays.stream(userUids)
                .map(userUid -> {
                    UidType uidType = PlatformUserIdType.select(userUid.getType()).get().getPlatformType();
                    ru.yandex.market.crm.platform.commons.Uid.Builder builder
                            = ru.yandex.market.crm.platform.commons.Uid.newBuilder().setType(uidType);
                    if (uidType != UidType.PUID) {
                        builder.setStringValue(userUid.getValue());
                    } else {
                        builder.setIntValue(Integer.parseInt(userUid.getValue()));
                    }
                    return builder.build();
                })
                .collect(Collectors.toUnmodifiableList());
        return ru.yandex.market.crm.platform.api.User.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIdsGraph(ru.yandex.market.crm.platform.api.IdsGraph.newBuilder()
                        .addAllNode(uids)
                        .addEdge(Edge.newBuilder()
                                .setNode1(0)
                                .setNode2(1)
                        )
                )
                .build();
    }
}
