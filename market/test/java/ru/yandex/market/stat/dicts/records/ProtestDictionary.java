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
 * Created by kateleb on 03.05.17.
 */
@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "testDir/protest_dictionary", description = "Словарь для интеграционных тестов 2")
public class ProtestDictionary implements DictionaryRecord {

    protected String test_id;
    protected Long test_number;
    protected BigDecimal experimental_field;

    public static ProtestDictionary item(String id1) {
        return ProtestDictionary.builder().test_id(id1)
            .test_number(RandomUtils.nextLong())
            .experimental_field(new BigDecimal(RandomUtils.nextInt()))
            .build();
    }

    public static List<ProtestDictionary> makeDataWithIds(List<String> ids) {
        return ids.stream().map(ProtestDictionary::item).collect(toList());
    }

    public static List<ProtestDictionary> makeDataWithIds(String... ids) {
        return makeDataWithIds(Arrays.asList(ids));
    }
}
