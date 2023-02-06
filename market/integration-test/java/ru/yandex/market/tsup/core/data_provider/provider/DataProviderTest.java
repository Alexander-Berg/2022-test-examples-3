package ru.yandex.market.tsup.core.data_provider.provider;

import java.lang.reflect.Method;
import java.util.Optional;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

class DataProviderTest extends AbstractContextualTest {
    @Autowired
    private TestDataProvider dataProvider;

    @Test
    void getProvideMethod() {
        Optional<Method> provideMethod = dataProvider.getProvideMethod();
        Assertions.assertThat(provideMethod).isNotEmpty();
        provideMethod.ifPresent(this::validateTestMethod);
    }

    @SneakyThrows
    private void validateTestMethod(Method m) {
        String value = (String) m.invoke(dataProvider, new TestProviderFilter().setP(1L), null);
        Assertions.assertThat(value).isEqualTo(TestDataProvider.RESULT);
    }

    @Test
    void getProviderClass() {
        // Специальный метод getProviderClass возвращает класс провайдера, а не прокси
        Assertions.assertThat(dataProvider.getProviderClass()).isEqualTo(TestDataProvider.class);
    }
}
