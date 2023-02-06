package ru.yandex.travel.testing.mockito;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.WrongTypeOfReturnValue;
import org.mockito.stubbing.OngoingStubbing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class MockitoConcurrencyFailureTest {
    @Test
    public void testBrokenOngoingStubbing() throws InterruptedException {
        TestInterface mock = Mockito.mock(TestInterface.class);

        // imitating simultaneous concurrent access to the mock
        // T1: when(mock.methodA()).thenReturn("ABC");
        // T2: mock.methodB();

        OngoingStubbing<String> stubbing = when(mock.methodA());

        Thread t = new Thread(() -> {
            //noinspection Convert2MethodRef
            mock.methodB();
        });
        t.start();
        t.join();

        // methodA stubbing has been replaced with methodB
        assertThatThrownBy(() -> stubbing.thenReturn("ABC"))
                .isExactlyInstanceOf(WrongTypeOfReturnValue.class)
                .hasMessageContaining("" +
                        "String cannot be returned by methodB()\n" +
                        "methodB() should return Number");
    }

    interface TestInterface {
        String methodA();

        @SuppressWarnings("UnusedReturnValue")
        Number methodB();
    }
}
