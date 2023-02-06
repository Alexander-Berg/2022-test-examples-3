package ru.yandex.direct.telephony.client;

import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.telephony.client.model.TelephonyPhoneRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.telephony.client.ProtobufMapper.CALL_SOURCE;
import static ru.yandex.direct.telephony.client.ProtobufMapper.CLIENT_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.COUNTER_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.ORG_ID_META_KEY;
import static ru.yandex.telephony.backend.lib.proto.telephony_platform.CallRecordingScope.ON_CONVERSATION_STARTED;

public class ProtobufMapperTest {

    @Test
    public void composePhoneMeta_withAllParams() {
        long clientId = 1;
        TelephonyPhoneRequest telephonyPhone =
                new TelephonyPhoneRequest()
                        .withCounterId(100L)
                        .withPermalinkId(123L)
                        .withCallRecordingScope(ON_CONVERSATION_STARTED);

        Map<String, String> actual = ProtobufMapper.composePhoneMeta(clientId, telephonyPhone);

        assertThat(actual)
                .containsAllEntriesOf(Map.of(
                        CLIENT_ID_META_KEY, "1",
                        COUNTER_ID_META_KEY, "100",
                        ORG_ID_META_KEY, "123",
                        CALL_SOURCE, "adv"
                        )
                );
    }

    @Test
    public void composePhoneMeta_withoutPermalinkId() {
        long clientId = 1;
        TelephonyPhoneRequest telephonyPhone =
                new TelephonyPhoneRequest()
                        .withCounterId(100L)
                        .withPermalinkId(null)
                        .withCallRecordingScope(ON_CONVERSATION_STARTED);

        Map<String, String> actual = ProtobufMapper.composePhoneMeta(clientId, telephonyPhone);

        assertThat(actual)
                .containsAllEntriesOf(Map.of(
                        CLIENT_ID_META_KEY, "1",
                        COUNTER_ID_META_KEY, "100",
                        CALL_SOURCE, "site"
                        )
                );
    }

    @Test
    public void composePhoneMeta_withoutZeroPermalinkId() {
        long clientId = 1;
        TelephonyPhoneRequest telephonyPhone =
                new TelephonyPhoneRequest()
                        .withCounterId(100L)
                        .withPermalinkId(0L)
                        .withCallRecordingScope(ON_CONVERSATION_STARTED);

        Map<String, String> actual = ProtobufMapper.composePhoneMeta(clientId, telephonyPhone);

        assertThat(actual)
                .containsAllEntriesOf(Map.of(
                        CLIENT_ID_META_KEY, "1",
                        COUNTER_ID_META_KEY, "100",
                        CALL_SOURCE, "site"
                        )
                );
    }
}
