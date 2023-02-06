package ru.yandex.market.logistics.management.service.combinator;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/service/combinator/db/before/korobyte_restrictions_to_jsonb.xml")
public class KorobyteRestrictionsToJsonbTest extends AbstractContextualAspectValidationTest {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("testData")
    @SneakyThrows
    void test(
        @SuppressWarnings("unused") String displayName,
        long korobyteRestrictionId,
        String pathToExpectedJson
    ) {
        assertEquals(
            pathToJson(pathToExpectedJson),
            convertToJsonb(korobyteRestrictionId),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Nonnull
    public static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of("Все поля не пустые", 1L, "data/service/combinator/json/korobyte_restrictions_1.json"),
            Arguments.of("Все поля пустые", 2L, "data/service/combinator/json/korobyte_restrictions_2.json"),
            Arguments.of("Смешанный вариант", 3L, "data/service/combinator/json/korobyte_restrictions_3.json")
        );
    }

    @SneakyThrows
    @Nullable
    private String convertToJsonb(long korobyteRestrictionId) {
        return jdbcTemplate.queryForObject(
            "SELECT korobyte_restrictions_to_jsonb(:korobyteRestrictionId)::TEXT",
            Map.of("korobyteRestrictionId", korobyteRestrictionId),
            String.class
        );
    }

}
