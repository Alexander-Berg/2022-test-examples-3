package ru.yandex.calendar.frontend.caldav.impl;

import java.util.Optional;

import lombok.val;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.frontend.caldav.proto.webdav.DavSyncToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LayerSyncTokenTest {
    private static Instant parseToken(String token) {
        return LayerSyncToken.getInstantFromSyncToken(new DavSyncToken(token));
    }

    @Test
    public void checkWeirdPrefixRaisesAnException() {
        assertThatThrownBy(() -> parseToken("corrupted:file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The token provided does not match any of the prefixes");
    }

    @Test
    public void checkBadEndingCausesAnException() {
        assertThatThrownBy(() -> parseToken("sync-token:1559305502874a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The token provided does not match any of the prefixes");
    }

    @Test
    public void checkOldPrefixIsOk() {
        assertThat(parseToken("data:,1559305502874").getMillis())
                .isEqualTo(1559305502874L);
    }

    @Test
    public void checkNewPrefixIsOk() {
        val info = LayerSyncToken.parseSyncToken(new DavSyncToken("sync-token:42 1559305502874"));
        assertThat(info.getMillis()).isEqualTo(1559305502874L);
        assertThat(info.getExternalId()).isEmpty();
    }

    @Test
    public void checkLongNumberCausesAnException() {
        assertThatThrownBy(() -> parseToken("sync-token:42 9999999999999999999999999999999999999999999999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An error has occurred while parsing the token");
    }

    @Test
    public void checkNewPrefixIsOkWithAdditionalInfo() {
        val info = LayerSyncToken.parseSyncToken(new DavSyncToken("sync-token:42 1559305502874 RobbStark"));
        assertThat(info.getMillis()).isEqualTo(1559305502874L);
        assertThat(info.getExternalId()).hasValue("RobbStark");
    }

    @Test
    public void checkSyncTokenWithoutVersionCausesAnException() {
        assertThatThrownBy(() -> parseToken("sync-token:1559305502874"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The token provided does not match any of the prefixes");
    }

    @Test
    public void checkSyncTokenGenerationInNewFormat() {
        val instant = Optional.of(new Instant(1559305502874L));
        assertThat(LayerSyncToken.lastUpdateTsToSyncToken(instant).getValue())
                .isEqualTo("sync-token:1 1559305502874");
    }
}
