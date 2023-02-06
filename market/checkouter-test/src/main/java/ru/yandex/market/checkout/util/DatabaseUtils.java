package ru.yandex.market.checkout.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.Nullable;
import org.jooq.Converter;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.AbstractConverter;
import org.jooq.impl.TableRecordImpl;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkouter.jooq.Tables;

import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_CHECKPOINT;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_CHECKPOINT_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_PROMO_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_EVENT;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_ITEM;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX_ITEM_HISTORY;

public final class DatabaseUtils {

    public static final Converter SIMPLE_CONVERTER = new ToValueConverter();
    private static final List<Table<?>> TABLES = new ArrayList<>();
    private static final EnhancedRandom ENHANCED_RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .randomize(Integer.class, (Randomizer<Integer>) () -> RandomUtils.nextInt(0, 100))
            .randomize(Long.class, (Randomizer<Long>) () -> RandomUtils.nextLong(1000, 100000))
            .randomize(String.class, (Randomizer<String>) () -> RandomStringUtils.randomAlphabetic(10))
            .randomize(BigInteger.class, (Randomizer<BigInteger>) () -> BigInteger.valueOf(RandomUtils.nextLong(1000,
                    100000)))
            .build();

    private static long nextId;

    static {
        for (java.lang.reflect.Field field : Tables.class.getFields()) {
            try {
                TABLES.add((Table) field.get(null));
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
    }

    private DatabaseUtils() {
    }

    @Nonnull
    public static TableRecordImpl<?> getTableRecord(@Nonnull Table<?> table) {
        TableRecordImpl<?> record = (TableRecordImpl<?>) table.newRecord();
        Set<?> pkFields = Optional.ofNullable(table.getPrimaryKey())
                .map(pk -> Set.copyOf(pk.getFields()))
                .orElse(Collections.emptySet());
        record.fieldsRow()
                .fieldStream()
                .forEach(
                        field -> {
                            if (pkFields.contains(field) && field.getDataType().isNumeric()) {
                                record.set(field, ++nextId, new IdConverter(field.getType()));
                                return;
                            }
                            if (table.getName().equals("return_item") &&
                                    field.getName().equals("pictures_urls")) {
                                record.set(field, JSONB.valueOf("[]"), SIMPLE_CONVERTER);
                                return;
                            }
                            if (field.getDataType().getSQLDataType().getCastTypeName().equals("jsonb")) {
                                record.set(field, JSONB.valueOf("{}"), SIMPLE_CONVERTER);
                            } else if (field.getType().equals(BigDecimal.class)) {
                                int maxPrecision = Math.min(field.getDataType().precision(),
                                        field.getDataType().scale() + 5);
                                long maxUnscaled = BigInteger.TEN.pow(maxPrecision).longValue();
                                record.set(field, BigDecimal.valueOf(RandomUtils.nextLong(1, maxUnscaled),
                                        field.getDataType().scale()), SIMPLE_CONVERTER);
                            } else {
                                record.set(field, ENHANCED_RANDOM.nextObject(field.getType()), SIMPLE_CONVERTER);
                            }
                        }
                );

        return record;
    }

    @Nonnull
    public static List<ForeignKeyInfo> getForeignKeys(@Nonnull JdbcTemplate template, List<Table<?>> tables) {
        List<Map<String, Object>> foreignKeys = queryTableForeignKeys(template);
        if (foreignKeys.isEmpty()) {
            return List.of();
        }

        List<ForeignKeyInfo> foreignKeyInfos = foreignKeys.stream()
                .map(DatabaseUtils::mapRowToTableField)
                .filter(fk -> Objects.nonNull(fk)
                        && (tables.contains(fk.getReferencedTable()) || tables.contains(fk.getSourceTable())))
                .collect(Collectors.toList());

        addAdditionalForeignKeys(foreignKeyInfos);

        return foreignKeyInfos;
    }

    private static void addAdditionalForeignKeys(@Nonnull List<ForeignKeyInfo> foreignKeyInfos) {
        foreignKeyInfos.add(new ForeignKeyInfo(
                ORDER_EVENT, ORDER_HISTORY, ORDER_EVENT.HISTORY_ID, ORDER_HISTORY.ID
        ));
        foreignKeyInfos.add(new ForeignKeyInfo(
                PARCEL_BOX_ITEM_HISTORY, PARCEL_BOX, PARCEL_BOX_ITEM_HISTORY.PARCEL_BOX_ID, PARCEL_BOX.ID
        ));
        foreignKeyInfos.add(new ForeignKeyInfo(
                DELIVERY_TRACK_HISTORY, DELIVERY_TRACK, DELIVERY_TRACK_HISTORY.ID, DELIVERY_TRACK.ID
        ));
        foreignKeyInfos.add(new ForeignKeyInfo(
                DELIVERY_TRACK_CHECKPOINT, DELIVERY_TRACK,
                DELIVERY_TRACK_CHECKPOINT.DELIVERY_TRACK_ID, DELIVERY_TRACK.ID
        ));
        foreignKeyInfos.add(new ForeignKeyInfo(
                DELIVERY_TRACK_CHECKPOINT_HISTORY, DELIVERY_TRACK,
                DELIVERY_TRACK_CHECKPOINT_HISTORY.DELIVERY_TRACK_ID, DELIVERY_TRACK.ID
        ));
        foreignKeyInfos.add(new ForeignKeyInfo(
                ITEM_PROMO_HISTORY, ORDER_ITEM, ITEM_PROMO_HISTORY.ITEM_ID, ORDER_ITEM.ID
        ));
    }

    @Nullable
    private static ForeignKeyInfo mapRowToTableField(@Nonnull Map<String, Object> keys) {
        Table<?> sourceTable = findTableByName(keys.get("table_name"));
        Table<?> referencedTable = findTableByName(keys.get("foreign_table_name"));
        if (sourceTable == null || referencedTable == null) {
            return null;
        }

        return new ForeignKeyInfo(
                sourceTable,
                referencedTable,
                findTableFieldByName(keys.get("column_name"), sourceTable.newRecord().fields()),
                findTableFieldByName(keys.get("foreign_column_name"), referencedTable.newRecord().fields())
        );

    }

    @Nullable
    private static Field<?> findTableFieldByName(@Nonnull Object fieldName, @Nonnull Field<?>[] tableFields) {
        return Stream.of(tableFields)
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static Table<?> findTableByName(@Nonnull Object tableName) {
        return TABLES.stream()
                .filter(t -> t.getName().equals(tableName))
                .findFirst()
                .orElse(null);
    }

    @Nonnull
    private static List<Map<String, Object>> queryTableForeignKeys(@Nonnull JdbcTemplate template) {
        return template.queryForList("SELECT\n" +
                "    tc.table_name,\n" +
                "    kcu.column_name,\n" +
                "    ccu.table_name AS foreign_table_name,\n" +
                "    ccu.column_name AS foreign_column_name \n" +
                "FROM \n" +
                "    information_schema.table_constraints AS tc \n" +
                "    JOIN information_schema.key_column_usage AS kcu\n" +
                "      ON tc.constraint_name = kcu.constraint_name\n" +
                "      AND tc.table_schema = kcu.table_schema\n" +
                "    JOIN information_schema.constraint_column_usage AS ccu\n" +
                "      ON ccu.constraint_name = tc.constraint_name\n" +
                "      AND ccu.table_schema = tc.table_schema\n" +
                "WHERE tc.constraint_type = 'FOREIGN KEY'");
    }

    public static class ToValueConverter implements Converter {

        @Override
        public Object from(Object databaseObject) {
            return databaseObject;
        }

        @Override
        public Object to(Object userObject) {
            return userObject;
        }

        @Override
        public Class fromType() {
            return null;
        }

        @Override
        public Class toType() {
            return null;
        }
    }

    public static class IdConverter<T> extends AbstractConverter<T, Long> {

        public IdConverter(Class<T> idType) {
            super(idType, Long.class);
        }

        @Override
        public Long from(T databaseObject) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T to(Long id) {
            Object dbId;
            if (fromType().equals(Integer.class)) {
                dbId = id.intValue();
            } else if (fromType().equals(Long.class)) {
                dbId = id;
            } else if (fromType().equals(BigInteger.class)) {
                dbId = BigInteger.valueOf(id);
            } else {
                throw new UnsupportedOperationException("Unsupported id field type: " + fromType().getName());
            }
            return fromType().cast(dbId);
        }
    }

    public static class ForeignKeyInfo {

        private final Table<?> sourceTable;
        private final Table<?> referencedTable;
        private final Field<?> sourceField;
        private final Field<?> referencedField;

        private ForeignKeyInfo(Table<?> sourceTable,
                               Table<?> referencedTable,
                               Field<?> sourceField,
                               Field<?> referencedField) {
            this.sourceTable = sourceTable;
            this.referencedTable = referencedTable;
            this.sourceField = sourceField;
            this.referencedField = referencedField;
        }

        public Table<?> getSourceTable() {
            return sourceTable;
        }

        public Table<?> getReferencedTable() {
            return referencedTable;
        }

        public Field<?> getSourceField() {
            return sourceField;
        }

        public Field<?> getReferencedField() {
            return referencedField;
        }
    }
}
