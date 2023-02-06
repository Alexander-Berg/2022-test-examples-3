package ru.yandex.market.loyalty.admin.yt.service;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.TableDescriptions.Archivation;
import ru.yandex.market.loyalty.admin.yt.service.TableDescriptions.TableDefinition;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class TableDescriptionsTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldAllArchivationTableDescriptionsContainsAllColumns() throws IllegalAccessException {
        boolean hasArchivationAnnotation = false;
        for (Field field : TableDescriptions.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Archivation.class)) {
                continue;
            }
            TableDefinition tableDefinition = (TableDefinition) field.get(null);
            String tableName = field.getAnnotation(Archivation.class).value();
            List<Pair<String, YtTableDescription.Column.Type>> descriptionColumns = tableDefinition.all().stream()
                    .map(column -> Pair.of(column.getPgName().toLowerCase(), column.getType()))
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toUnmodifiableList());
            List<Pair<String, String>> dbColumns = jdbcTemplate.query("" +
                            "SELECT column_name, data_type" +
                            "  FROM information_schema.columns" +
                            " WHERE table_schema = 'public'" +
                            "   AND table_name   = ?",
                    (rs, i) -> Pair.of(rs.getString("column_name"), rs.getString("data_type")), tableName.toLowerCase()
            )
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toUnmodifiableList());
            assertEquals(
                    "Inconsistent column names for table = " + tableName,
                    dbColumns.stream()
                            .map(Pair::getKey)
                            .collect(Collectors.toUnmodifiableList()),
                    descriptionColumns.stream()
                            .map(Pair::getKey)
                            .collect(Collectors.toUnmodifiableList())
            );
            for (int i = 0; i < dbColumns.size(); ++i) {
                YtTableDescription.Column.Type descriptionType = descriptionColumns.get(i).getValue();
                Pair<String, String> dbColumn = dbColumns.get(i);
                String columnName = dbColumn.getKey();
                String pgType = dbColumn.getValue();
                assertTrue(
                        pgType + " is not mapped to " + descriptionType + " for " + tableName + '.' + columnName,
                        descriptionType.getPgMappedTypes().contains(pgType)
                );
            }
            hasArchivationAnnotation = true;
        }
        assertTrue("There are no fields with the @Archivation", hasArchivationAnnotation);
    }
}
