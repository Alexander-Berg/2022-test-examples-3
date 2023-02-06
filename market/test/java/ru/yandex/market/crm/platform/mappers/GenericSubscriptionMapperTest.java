package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.GenericSubscription.Channel;
import ru.yandex.market.crm.platform.models.GenericSubscription.Status;
import ru.yandex.market.request.trace.TskvRecordBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author vtarasoff
 * @since 09.08.2021
 */
public class GenericSubscriptionMapperTest {
    private static final long ID_PUID = 111;
    private static final String ID_UUID = "abc";

    private static final int TYPE_PUID = 0;
    private static final int TYPE_UUID = 1;

    private static final int CHANNEL_EMAIL = 1;
    private static final int CHANNEL_PUSH = 2;

    private static final int TYPE = 1;

    private static final String KEY_PARAMS = "params";
    private static final String NO_KEY_PARAMS = "no_params";

    private static final int STATUS_UNSUBSCRIBED = 0;
    private static final int STATUS_SUBSCRIBED = 1;
    private static final int STATUS_CONFIRMATION = 2;

    private static final long CREATED_AT = Instant.now().toEpochMilli();
    private static final long MODIFIED_AT = Instant.now().toEpochMilli();

    private static final String ID_DELIM = "$";

    private static final GenericSubscription SUBSCRIPTION_TEMPLATE = GenericSubscription.newBuilder()
            .setUid(Uids.create(UidType.UUID, ID_UUID))
            .setId(Channel.PUSH_VALUE + ID_DELIM + TYPE + ID_DELIM + KEY_PARAMS)
            .setChannel(Channel.PUSH)
            .setType(TYPE)
            .setStatus(Status.SUBSCRIBED)
            .setCreatedAt(CREATED_AT)
            .setModifiedAt(MODIFIED_AT)
            .build();

    private final GenericSubscriptionMapper mapper = new GenericSubscriptionMapper();

    @Test
    void shouldMapSubscriptionWithUuidCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(List.of(SUBSCRIPTION_TEMPLATE), result);
    }

    @Test
    void shouldMapSubscriptionWithPuidCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_PUID, TYPE_PUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(
                List.of(
                        SUBSCRIPTION_TEMPLATE
                                .toBuilder()
                                .setUid(Uids.create(UidType.PUID, ID_PUID))
                                .build()
                ),
                result
        );
    }

    @Test
    void shouldFailMapSubscriptionWithWrongPuidValueType() {
        assertThrows(
                NumberFormatException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_PUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithEmptyUuidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        "", TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullUuidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        null, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithWrongIdType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, 2, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullIdType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, null, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldMapSubscriptionWithEmailChannelCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_PUID, TYPE_PUID, CHANNEL_EMAIL, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(
                List.of(
                        SUBSCRIPTION_TEMPLATE
                                .toBuilder()
                                .setUid(Uids.create(UidType.PUID, ID_PUID))
                                .setChannel(Channel.MAIL)
                                .setId(Channel.MAIL_VALUE + ID_DELIM + TYPE + ID_DELIM + KEY_PARAMS)
                                .build()
                ),
                result
        );
    }

    @Test
    void shouldFailMapSubscriptionWithEmailChannelForUuid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_EMAIL, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithWrongChannel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_PUID, TYPE_PUID, 3, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullChannel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_PUID, TYPE_PUID, null, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldMapSubscriptionWithNoKeyParamsCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, NO_KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(
                List.of(
                        SUBSCRIPTION_TEMPLATE
                                .toBuilder()
                                .setId(Channel.PUSH_VALUE + ID_DELIM + TYPE)
                                .build()
                ),
                result
        );
    }

    @Test
    void shouldFailMapSubscriptionWithEmptyKeyParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, "", STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullKeyParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, null, STATUS_SUBSCRIBED, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldMapSubscriptionWithUnsubscribedStatusCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_UNSUBSCRIBED, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(
                List.of(
                        SUBSCRIPTION_TEMPLATE
                                .toBuilder()
                                .setStatus(Status.UNSUBSCRIBED)
                                .build()
                ),
                result
        );
    }

    @Test
    void shouldMapSubscriptionWithConfirmationStatusCorrectly() {
        List<GenericSubscription> result = mapper.apply(tskv(
                ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_CONFIRMATION, CREATED_AT, MODIFIED_AT
        ));

        assertEquals(
                List.of(
                        SUBSCRIPTION_TEMPLATE
                                .toBuilder()
                                .setStatus(Status.CONFIRMATION)
                                .build()
                ),
                result
        );
    }

    @Test
    void shouldFailMapSubscriptionWithWrongStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, 3, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, null, CREATED_AT, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullCreatedAt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, null, MODIFIED_AT
                ))
        );
    }

    @Test
    void shouldFailMapSubscriptionWithNullModifiedAt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.apply(tskv(
                        ID_UUID, TYPE_UUID, CHANNEL_PUSH, TYPE, KEY_PARAMS, STATUS_SUBSCRIBED, CREATED_AT, null
                ))
        );
    }

    private byte[] tskv(Object idValue,
                        Integer idType,
                        Integer channel,
                        Integer type,
                        String keyParams,
                        Integer status,
                        Long createdAt,
                        Long modifiedAt) {
        return new TskvRecordBuilder()
                .add("ID_VALUE", idValue)
                .add("ID_TYPE", idType)
                .add("CHANNEL", channel)
                .add("TYPE", type)
                .add("KEY_PARAMS", keyParams)
                .add("STATUS", status)
                .add("CREATED_AT", createdAt)
                .add("MODIFIED_AT", modifiedAt)
                .build()
                .getBytes(StandardCharsets.UTF_8);
    }
}
