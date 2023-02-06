package ru.yandex.market.javaframework.clients.calladapters.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LoggingUnsuccessfulCallTest {

    @Mock
    private Call next;

    @InjectMocks
    private LoggingUnsuccessfulCall loggingUnsuccessfulCall;

    @Test
    public void timeout_callsNext() {
        loggingUnsuccessfulCall.timeout();
        verify(next).timeout();
    }

    @Test
    public void clone_callsClone() {
        loggingUnsuccessfulCall.clone();
        verify(next).clone();
    }
}
