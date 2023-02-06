package ru.yandex.market.stat.dicts.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.stat.dicts.parsers.BaseLineDictionaryParser;
import ru.yandex.market.stat.parsers.ParseException;

import java.io.IOException;

/**
 * Created by kateleb on 05.05.17.
 */
public class TestDictionaryParser extends BaseLineDictionaryParser<TestDictionary> {
    private final ObjectMapper mapper = new ObjectMapper();

    public TestDictionaryParser() {
        super(TestDictionary.class);
    }

    @Override
    public TestDictionary parseRecord(String line) throws ParseException {
        //WTF? why first file line has prefix ��w�
        if (line.contains("��\u0000\u0005w�")) {
            line = line.replace("��\u0000\u0005w�","");
        }
        try {
            return mapper.readValue(line, TestDictionary.class);
        } catch (IOException e) {
            throw new RuntimeException("Can't parse data from json!\n" + e.getMessage());
        }
    }
}
