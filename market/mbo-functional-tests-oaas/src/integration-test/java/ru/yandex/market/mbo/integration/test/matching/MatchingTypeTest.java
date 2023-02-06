package ru.yandex.market.mbo.integration.test.matching;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kravchenko-aa
 * @date 2019-12-19
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MatchingTypeTest extends BaseIntegrationTest {

    @Autowired
    JdbcOperations siteCatalogJdbcTemplate;

    @Test
    public void allMatchingTypeAreDefined() {
        List<TypeRecord> types = new ArrayList<>();
        siteCatalogJdbcTemplate.query(
            "select id, descr, sort_order from NG_MATCHING_TYPE order by sort_order", (row) -> {
                types.add(new TypeRecord(
                    row.getInt("id"), row.getString("descr"), row.getInt("sort_order")));
            });

        assertThat(types).containsExactly(
            new TypeRecord(2, "не опознана", 0),
            new TypeRecord(7, "вендор", 1),
            new TypeRecord(6, "отложено", 2),
            new TypeRecord(0, "модель", 3),
            new TypeRecord(3, "таск-модель", 4),
            new TypeRecord(15, "модификация", 5),
            new TypeRecord(16, "таск-модификация", 6),
            new TypeRecord(17, "матчинг по супер-параметру", 7),
            new TypeRecord(18, "матчинг до модели в груп. категории", 8),
            new TypeRecord(21, "матчинг по формализованному параметру", 9),
            new TypeRecord(22, "матчинг по good id", 10),
            new TypeRecord(23, "матчинг по штрихкоду", 11),
            new TypeRecord(24, "матчинг дип-матчером", 12),
            new TypeRecord(25, "отсечено по cutoff good id", 13),
            new TypeRecord(4, "огр. цены", 14),
            new TypeRecord(1, "с отс. словом", 15),
            new TypeRecord(20, "не прошло проверку \"коридора цен\"", 16),
            new TypeRecord(9, "не может быть улучшено", 17),
            new TypeRecord(8, "невозможно распознать до модификации", 18),
            new TypeRecord(5, "мусор", 19),
            new TypeRecord(10, "не может быть улучшено до sku", 20),
            new TypeRecord(11, "ручная привязка к SKU", 21)
        );
    }

    private static class TypeRecord {
        int typeId;
        String description;
        int order;

        TypeRecord(int typeId, String description, int order) {
            this.typeId = typeId;
            this.description = description;
            this.order = order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TypeRecord that = (TypeRecord) o;
            return typeId == that.typeId &&
                order == that.order &&
                Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeId, description, order);
        }

        @Override
        public String toString() {
            return "TypeRecord{" +
                "typeId=" + typeId +
                ", description='" + description + '\'' +
                ", order=" + order +
                '}';
        }
    }
}
