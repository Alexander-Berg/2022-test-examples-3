package ru.yandex.market.javaframework.clients.calladapters.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Retrofit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class LoggingUnsuccessfulCallAdapterTest {

    @Mock
    private CallAdapter<Object, Object> nextAdapter;

    private Retrofit retrofit;

    @BeforeEach
    public void beforeEach() {
        CallAdapter.Factory factoryMock = Mockito.mock(CallAdapter.Factory.class);
        doReturn(nextAdapter).when(factoryMock).get(any(), any(), any());
        retrofit = new Retrofit.Builder()
            .baseUrl("http://example.com")
            .addCallAdapterFactory(factoryMock)
            .build();
    }

    @Test
    public void adapterReturnsTypeOfNextAdapter() {
        LoggingUnsuccessfulCallAdapter callAdapter = LoggingUnsuccessfulCallAdapter.of(this::noAdditionalBehavior);
        Type type = type("Type");
        doReturn(type).when(nextAdapter).responseType();

        CallAdapter<?, ?> adapter = callAdapter.get(type, new Annotation[]{}, retrofit);

        Type actualType = adapter.responseType();
        assertEquals(type.getTypeName(), actualType.getTypeName());
    }

    private Type type(String name) {
        return new Type() {
            @Override
            public String getTypeName() {
                return name;
            }
        };
    }

    @Test
    public void adapterReturnsResultOfNextAdapter() {
        LoggingUnsuccessfulCallAdapter callAdapter = LoggingUnsuccessfulCallAdapter.of(this::noAdditionalBehavior);
        Object result = new Object();
        doReturn(result).when(nextAdapter).adapt(any());

        CallAdapter<?, ?> adapter = callAdapter.get(type("Type"), new Annotation[]{}, retrofit);

        Object actualResult = adapter.adapt(mock(Call.class));
        assertEquals(result, actualResult);
    }

    private Callback<Object> noAdditionalBehavior(Callback<Object> initial) {
        return initial;
    }
}
