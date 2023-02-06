package ru.yandex.market.communication.proxy.tms.command;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.telephony.client.model.PlaybackUploadResponse;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.service.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.communication.proxy.service.environment.memoizers.PlaybackIdMemoizers.BEFORE_CONVERSATION_PLAYBACK_ID;
import static ru.yandex.market.communication.proxy.service.environment.memoizers.PlaybackIdMemoizers.BUYER_PLAYBACK_ID;
import static ru.yandex.market.communication.proxy.service.environment.memoizers.PlaybackIdMemoizers.SHOP_PLAYBACK_ID;

public class LoadTelephonyPlaybackCommandTest extends AbstractCommunicationProxyTest {

    @Autowired
    private LoadTelephonyPlaybackCommand tested;
    @Mock
    private Terminal terminal;
    @Mock
    private PrintWriter printWriter;
    @Autowired
    private TelephonyClient telephonyClient;
    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void testUploadPlaybackForBuyer() {
        String playback = "test-playback\n";

        CommandInvocation commandInvocation = new CommandInvocation("load-telephony-playback",
                new String[]{"buyer", this.getClass().getResource("testUploadPlayback.txt").toString()},
                Collections.emptyMap());

        when(telephonyClient.uploadPlayback(aryEq(playback.getBytes())))
                .thenReturn(new PlaybackUploadResponse().withPlaybackId("123"));

        tested.executeCommand(commandInvocation, terminal);

        assertEquals("123", environmentService.getValue(BUYER_PLAYBACK_ID, ""));
    }


    @Test
    public void testUploadPlaybackForShop() {
        String playback = "test-playback\n";

        CommandInvocation commandInvocation = new CommandInvocation("load-telephony-playback",
                new String[]{"shop", this.getClass().getResource("testUploadPlayback.txt").toString()},
                Collections.emptyMap());

        when(telephonyClient.uploadPlayback(aryEq(playback.getBytes())))
                .thenReturn(new PlaybackUploadResponse().withPlaybackId("123"));

        tested.executeCommand(commandInvocation, terminal);

        assertEquals("123", environmentService.getValue(SHOP_PLAYBACK_ID, ""));
    }

    @Test
    public void testUploadBeforeConversationPlayback() {
        String playback = "test-playback\n";

        CommandInvocation commandInvocation = new CommandInvocation("load-telephony-playback",
                new String[]{"BEFORE_CONVERSATION", this.getClass().getResource("testUploadPlayback.txt").toString()},
                Collections.emptyMap());

        when(telephonyClient.uploadPlayback(aryEq(playback.getBytes())))
                .thenReturn(new PlaybackUploadResponse().withPlaybackId("123"));

        tested.executeCommand(commandInvocation, terminal);

        assertEquals("123", environmentService.getValue(BEFORE_CONVERSATION_PLAYBACK_ID, ""));
    }
}
