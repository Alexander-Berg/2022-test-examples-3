package ru.yandex.market.tpl.core.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public abstract class AbstractDbDictTest extends TplAbstractTest {

    private static final String QUERY = "SELECT :columnName FROM :tableName";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Class<? extends Enum<?>> enumClass;
    private final String tableName;
    private final String columnName;

    @Test
    @DisplayName("Все элементы в enum-е должны совпадать с элементами в справочнике в базе данных")
    void testDbDictContainsAllEnumValues() {
        List<String> allQueuesFromDb = jdbcTemplate.query(
                QUERY.replace(":columnName", columnName)
                        .replace(":tableName", tableName),
                (rs, rowNum) -> rs.getString(columnName)
        );

        List<String> allQueuesFromEnum = streamEnumValues()
                .map(Enum::name)
                .collect(Collectors.toList());

        assertThat(allQueuesFromEnum)
                .containsExactlyInAnyOrderElementsOf(allQueuesFromDb);
    }

    private Stream<? extends Enum<?>> streamEnumValues() {
        return Arrays.stream(enumClass.getEnumConstants());
    }

}
