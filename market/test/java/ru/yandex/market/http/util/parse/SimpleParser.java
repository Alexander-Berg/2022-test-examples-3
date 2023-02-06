package ru.yandex.market.http.util.parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.yandex.market.common.Parser;

/**
 * @author dimkarp93
 */
public class SimpleParser implements Parser<String[]> {
    private String[] value;

    @Override
    public String[] getParsed() {
        return value;
    }

    @Override
    public String[] parse(InputStream in)  {
        try {
            int size = in.available();
            byte[] bytes = new byte[size];
            in.read(bytes);
            return new String(bytes).split(";", 2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] parse(byte[] bytes) {
        return parse(new ByteArrayInputStream(bytes));
    }
}
