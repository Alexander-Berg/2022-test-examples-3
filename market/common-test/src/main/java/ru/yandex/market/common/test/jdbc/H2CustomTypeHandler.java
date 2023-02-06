package ru.yandex.market.common.test.jdbc;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.h2.api.CustomDataTypesHandler;
import org.h2.store.DataHandler;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueJavaObject;

/**
 * -Dh2.customDataTypesHandler=ru.yandex.market.common.test.jdbc.H2CustomTypeHandler
 *
 * @see https://h2database.com/html/advanced.html#custom_data_types_handler_api
 */
public class H2CustomTypeHandler implements CustomDataTypesHandler {
    private static RuntimeException notImplemented() {
        return new IllegalStateException("not implemented");
    }

    @Override
    public DataType getDataTypeByName(String name) {
        throw notImplemented();
    }

    @Override
    public DataType getDataTypeById(int type) {
        throw notImplemented();
    }

    @Override
    public int getDataTypeOrder(int type) {
        throw notImplemented();
    }

    @Override
    public Value convert(Value source, int targetType) {
        throw notImplemented();
    }

    @Override
    public String getDataTypeClassName(int type) {
        throw notImplemented();
    }

    @Override
    public int getTypeIdFromClass(Class<?> cls) {
        throw notImplemented();
    }

    @Override
    public Value getValue(int type, Object data, DataHandler dataHandler) {
        if (data instanceof long[]) {
            final Object[] boxed = LongStream.of((long[]) data).boxed().toArray();
            return DataType.convertToValue(null, boxed, Value.LONG);
        } else if (data instanceof int[]) {
            final Object[] boxed = IntStream.of((int[]) data).boxed().toArray();
            return DataType.convertToValue(null, boxed, Value.INT);
        }

        // fallback to default
        return ValueJavaObject.getNoCopy(data, null, dataHandler);
    }

    @Override
    public Object getObject(Value value, Class<?> cls) {
        throw notImplemented();
    }

    @Override
    public boolean supportsAdd(int type) {
        throw notImplemented();
    }

    @Override
    public int getAddProofType(int type) {
        throw notImplemented();
    }
}
