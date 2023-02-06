package ru.yandex.market.wrap.infor.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Defaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.api.DefaultApi;
import ru.yandex.market.wrap.infor.configuration.property.InforClientProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Тест проверяет работу оборачивания исключений в InforClientImpl и WrappedInforClientImpl.
 * <p>Параметризуем каждый запуск теста следующим методом интерфейса WrappedInforClient,
 * подставляя параметрами рефлективно сгенерированные объекты, и проверяем, что исключение от DefaultApi корректно
 * прокидывается до WrappedInforClient.</p>
 */
class WrappedInforClientTest extends SoftAssertionSupport {

    private static final Logger log = LoggerFactory.getLogger(WrappedInforClient.class);

    private WrappedInforClient wrappedInforClient;
    private InforClient inforClient;
    private DefaultApi defaultApi;

    private static final String WAREHOUSE_KEY = "TestWarehouseKey";
    private static final String ENTERPRISE_KEY = "TestEnterpriseKey";
    private static final String USERNAME = "TesUsername";
    private static final String PASSWORD = "TestPassword";


    /**
     * Метод генерирует массив со всеми методами интерфеса WrappedInforClient.
     */
    static Stream<Arguments> parameters() {
        return Arrays.stream(WrappedInforClient.class.getMethods()).map(Arguments::of);
    }

    @BeforeEach
    void setUp() throws InvocationTargetException, IllegalAccessException {
        InforCredentials inforCredentials = new InforCredentials(USERNAME, PASSWORD);
        InforClientProperties properties = InforClientProperties.builder()
            .url(null)
            .username(USERNAME)
            .password(PASSWORD)
            .enterpriseKey(ENTERPRISE_KEY)
            .warehouseKey(WAREHOUSE_KEY)
            .connectTimeoutMillis(0)
            .readTimeoutMillis(0)
            .build();

        defaultApi = mock(DefaultApi.class);
        inforClient = new InforClientImpl(WAREHOUSE_KEY, ENTERPRISE_KEY, defaultApi, inforCredentials);
        wrappedInforClient = new WrappedInforClientImpl(inforClient, properties);

        // общий ответ с исключением, которым DefaultApi будет отвечать на любой вызов
        Answer<Object> exceptionalAnswer = invocation -> {
            throw (new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                String.format("{\"localizedMessage\":\"Error has been thrown for user: %s with password: %s\"}",
                    USERNAME, PASSWORD).getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));
        };
        // заставляем мок DefaultApi на любой публичный вызов отвечать исключением
        for (Method m : DefaultApi.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                // invoke() заставляет ловить исключения IllegalAccessException | InvocationTargetException,
                // но, если все хорошо, их не должно быть:
                // IllegalAccessException - мы берем только паблик методы именно этого класса
                // InvocationTargetException - мок не должен так поступать
                m.invoke(doAnswer(exceptionalAnswer).when(defaultApi),
                    IntStream.range(0, m.getParameterTypes().length).mapToObj(i -> ArgumentMatchers.any()).toArray());
            }
        }
    }

    /**
     * Тест запускает цепочку вызовов от текущего метода (currentMethod) из WrappedInforClient.
     */
    @MethodSource("parameters")
    @ParameterizedTest(name = "{index} : {0}.")
    void callSingleInterfaceMethod(Method currentMethod) {
        // проверяем, что исключене действительно случилось
        softly.assertThatThrownBy(() -> {
            try {
                log.info("Trying to call: " + currentMethod.getName());
                currentMethod.invoke(wrappedInforClient, createMethodArgs(currentMethod));
                // рефлективный вызов оборачивает наше исключение в InvocationTargetException, проверяем, что все как надо
            } catch (IllegalAccessException | InvocationTargetException ex) {
                assertThat(ex.getCause()).matches(cause -> cause instanceof FulfillmentApiException);
                FulfillmentApiException cause = (FulfillmentApiException) ex.getCause();
                assertThat(cause.getErrorsArray()).isNotNull();
                softly.assertThat(Arrays.stream(cause.getErrorsArray()).map(ErrorPair::getMessage))
                    .containsExactly("Infor client exception. " +
                            "Error code: 400 - Error has been thrown for user: ***** with password: *****");
                log.info("Exception had been thrown, assertions passed");
                throw cause;
            }
        }).isInstanceOf(FulfillmentApiException.class);
    }

    /**
     * Создаем массив аргументов для вызова реального метода.
     */
    private Object[] createMethodArgs(Method method) {
        return Arrays.stream(method.getParameterTypes())
            .map(this::getInstanceWithSomewhatInitedFields)
            .toArray();
    }

    /**
     * Создаем инстанс класса для передачи в аргумент: подставляем дефолтные значения для примитивов
     * и пробуем создать новый инстанс для объектов, либо null, если совсем не получается.
     * На текущий момент newInstance() будет вызываться для различных DTO в силу интерфейса InforClient:
     * там есть методы, у которых аргументы - это либо примитивы, либо DTO, либо String.
     */
    @SuppressWarnings("unchecked") // ругается на каст стринги, но кастуем только после ифа
    private <T> T getInstanceWithSomewhatInitedFields(Class<T> type) {
        if (String.class.equals(type)) {
            return (T) "";
        }
        T instance = Defaults.defaultValue(type);
        if (instance != null) {
            return instance;
        }
        try {
            return type.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            // can't construct, hope null will work
            return null;
        }
    }
}
