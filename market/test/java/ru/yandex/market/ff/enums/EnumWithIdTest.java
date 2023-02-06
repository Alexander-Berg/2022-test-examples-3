package ru.yandex.market.ff.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.EnumWithId;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.VatRate;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;


/**
 * @author kotovdv 07/08/2017.
 */
@DatabaseSetup("classpath:empty.xml")
class EnumWithIdTest extends IntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFindById() {
        EnumWithId value = EnumWithId.findById(0, RequestStatus.class);

        assertThat(value, equalTo(RequestStatus.CREATED));
    }

    /**
     * Проверяет, что для всех записей из енумов есть запись в базе.
     */
    @ParameterizedTest
    @MethodSource("dbEnumsParams")
    <T extends Enum & EnumWithId> void testEnumsInDatabase(String table, Class<T> clazz) {
        Set<Integer> dbIds = new HashSet<>(
                jdbcTemplate.queryForList(String.format("select id from %s", table), Integer.class));
        Set<Integer> enumIds = Arrays.stream(clazz.getEnumConstants())
                .map(HasId::getId)
                .collect(toSet());

        assertThat(enumIds, hasSize(dbIds.size()));
        assertThat(enumIds, containsInAnyOrder(dbIds.toArray()));
    }

    private static Stream<Arguments> dbEnumsParams() {
        return Stream.of(
                Arguments.of("request_type", RequestType.class),
                Arguments.of("request_status", RequestStatus.class),
                Arguments.of("vat_rate", VatRate.class)
        );
    }
}
