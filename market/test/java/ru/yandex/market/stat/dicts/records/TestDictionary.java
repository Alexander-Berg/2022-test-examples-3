package ru.yandex.market.stat.dicts.records;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.math.RandomUtils;
import ru.yandex.market.stat.parsers.annotations.Table;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by kateleb on 27.04.17.
 */
@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "test_dictionary", description = "Словарь для интеграционных тестов")
public class TestDictionary implements DictionaryRecord {

    protected String test_id;
    protected Long test_number;
    protected BigDecimal experimental_field;

    public static TestDictionary item(String id1) {
        return TestDictionary.builder().test_id(id1)
            .test_number(RandomUtils.nextLong())
            .experimental_field(new BigDecimal(RandomUtils.nextInt()))
            .build();
    }

    public static List<TestDictionary> makeDataWithIds(List<String> ids) {
        return ids.stream().map(TestDictionary::item).collect(toList());
    }

    public static List<TestDictionary> makeDataWithIds(String... ids) {
        return makeDataWithIds(Arrays.asList(ids));
    }
}
