package ru.yandex.market.core.cutoff.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.testing.ShopProgram;

class CutoffTypeDatabaseMirrorTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testThatReflectionIsPerfect() {
        List<CutoffTypeDescription> databaseCutoffTypes = getAllCutoffTypeDescriptions();
        List<CutoffTypeDescription> sourceCodeCutoffTypes = Stream.of(CutoffType.values())
                .filter(t -> t != CutoffType.NULL)
                .map(t -> new CutoffTypeDescription(t.getId(), t.name(), t.program()))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(databaseCutoffTypes, Matchers.containsInAnyOrder(
                sourceCodeCutoffTypes.stream().map(Matchers::equalTo).collect(Collectors.toList())));

        MatcherAssert.assertThat(sourceCodeCutoffTypes, Matchers.containsInAnyOrder(
                databaseCutoffTypes.stream().map(Matchers::equalTo).collect(Collectors.toList())));
    }

    private List<CutoffTypeDescription> getAllCutoffTypeDescriptions() {
        return jdbcTemplate.query(
                "SELECT id, name, shop_program FROM shops_web.cutoff_types",
                (rs, rowNum) -> {
                    int id1 = rs.getInt("id");
                    String name = rs.getString("name");
                    ShopProgram program = ShopProgram.valueOf(rs.getString("shop_program"));
                    return new CutoffTypeDescription(id1, name, program);
                });
    }

    private static class CutoffTypeDescription {
        private final int id;
        private final String name;
        private final ShopProgram program;

        CutoffTypeDescription(int id, String name, ShopProgram program) {
            this.id = id;
            this.name = name;
            this.program = program;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CutoffTypeDescription)) {
                return false;
            }
            CutoffTypeDescription that = (CutoffTypeDescription) o;
            return id == that.id &&
                    Objects.equals(name, that.name) &&
                    program == that.program;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, program);
        }

        @Override
        public String toString() {
            return "CutoffTypeDescription{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", program=" + program +
                    '}';
        }
    }

}
