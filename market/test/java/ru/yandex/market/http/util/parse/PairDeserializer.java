package ru.yandex.market.http.util.parse;

import ru.yandex.market.common.Deserializer;

/**
 * @author dimkarp93
 */
public class PairDeserializer implements Deserializer {
    @Override
    public <T> T readObject(Class<T> clazz, String value) {
        String[] result = value.split(":");
        return (T) new Pair<>(result[0], result[1]);
    }

    @Override
    public <T> T readObject(Class<T> clazz, byte[] bytes) {
        return readObject(clazz, new String(bytes));
    }

    @Override
    public <T> T readObject(byte[] bytes, Class<T> clazz, Class... genericClasses) {
        return readObject(clazz, bytes);
    }
}
