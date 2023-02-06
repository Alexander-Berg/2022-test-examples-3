package ru.yandex.travel.testing.mockito;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.testing.mockito.ThreadSafeMockBuilder.newThreadSafeMockBuilder;

public class ThreadSafeMockAnswerTest {
    @Test
    public <T extends TestComponent & ThreadSafeMock<TestComponent>> void testMiscMocks() {
        T mock = newThreadSafeMockBuilder(TestComponent.class)
                .withDefaultInitializer(testComponent ->
                        when(testComponent.method1()).thenReturn("A"))
                .build();
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(0);
        TestComponent impl1 = mock.getCurrentMocksHolder();

        mock.initNewMocks(newImpl -> when(newImpl.method2()).thenReturn(1));
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(1);
        TestComponent impl2 = mock.getCurrentMocksHolder();
        assertThat(impl2).isNotSameAs(impl1);

        mock.resetToDefaultMocks();
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(0);
        TestComponent impl3 = mock.getCurrentMocksHolder();
        assertThat(impl3).isSameAs(impl1);

        mock.initNewMocks(newImpl -> {
            when(newImpl.method1()).thenReturn("B");
            when(newImpl.method2()).thenReturn(2);
        });
        assertThat(mock.method1()).isEqualTo("B");
        assertThat(mock.method2()).isEqualTo(2);
        TestComponent impl4 = mock.getCurrentMocksHolder();
        assertThat(impl4).isNotSameAs(impl1);
        assertThat(impl4).isNotSameAs(impl2);
    }

    @Test
    public <T extends TestComponent & ThreadSafeMock<TestComponent>> void testExtendedMocks() {
        T mock = newThreadSafeMockBuilder(TestComponent.class)
                .withDefaultInitializer(testComponent ->
                        when(testComponent.method1()).thenReturn("A"))
                .build();
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(0);
        TestComponent impl1 = mock.getCurrentMocksHolder();

        mock.extendCurrentMocks(newImpl -> when(newImpl.method2()).thenReturn(1));
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(1);
        TestComponent impl2 = mock.getCurrentMocksHolder();
        assertThat(impl2).isNotSameAs(impl1);

        mock.extendCurrentMocks(newImpl -> when(newImpl.method1()).thenReturn("B"));
        assertThat(mock.method1()).isEqualTo("B");
        assertThat(mock.method2()).isEqualTo(1);
        TestComponent impl3 = mock.getCurrentMocksHolder();
        assertThat(impl3).isNotSameAs(impl1).isNotSameAs(impl2);

        mock.resetToDefaultMocks();
        assertThat(mock.method1()).isEqualTo("A");
        assertThat(mock.method2()).isEqualTo(0);
        TestComponent impl4 = mock.getCurrentMocksHolder();
        assertThat(impl4).isSameAs(impl1);
    }

    @Test
    public <T extends TestComponent & ThreadSafeMock<TestComponent>> void testCornerCases() {
        T noDefaultInitializer = newThreadSafeMockBuilder(TestComponent.class).build();
        assertThat(noDefaultInitializer.method1()).isEqualTo(null);
        assertThat(noDefaultInitializer.method2()).isEqualTo(0);

        noDefaultInitializer.extendCurrentMocks(newImpl -> when(newImpl.method2()).thenReturn(1));
        assertThat(noDefaultInitializer.method1()).isEqualTo(null);
        assertThat(noDefaultInitializer.method2()).isEqualTo(1);
    }

    interface TestComponent {
        String method1();

        int method2();
    }
}
