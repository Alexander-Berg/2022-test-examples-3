package ru.yandex.search.msal;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.yandex.search.msal.mock.DataType;
import ru.yandex.test.util.TestBase;

public class MsalTestBase extends TestBase {
    protected static Map<String, String> generateData(
        final String data,
        final Map<String, DataType> meta)
        throws Exception
    {
        String[] split = data.split("\\|");
        assert split.length == meta.size();
        Map<String, String> result = new LinkedHashMap<>();

        int index = 0;
        for (String name: meta.keySet()) {
            result.put(name, split[index].trim());
            index += 1;
        }

        return result;
    }
}
