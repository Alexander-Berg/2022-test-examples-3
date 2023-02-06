package ru.yandex.market.abo.util.db.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.bigDecimalSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.booleanSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.dateSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.doubleSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.enumNameSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.intSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.localDateSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.localDateTimeSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.longSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.shortSetter;
import static ru.yandex.market.abo.util.db.batch.EntityPreparedStatementSetter.stringSetter;

/**
 * Проверка корректности сеттеров из {@link EntityPreparedStatementSetter}
 *
 * @author komarovns
 */
class EntityPreparedStatementSetterTest extends EmptyTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testSetters() {
        jdbcTemplate.update("" +
                "CREATE TABLE setters_test (" +
                "    longNonNull          bigint PRIMARY KEY, " +
                "    longNull             bigint, " +

                "    intNonNull           integer, " +
                "    intNull              integer, " +

                "    doubleNonNull        double precision, " +
                "    doubleNull           double precision, " +

                "    shortNonNull         smallint, " +
                "    shortNull            smallint, " +

                "    boolNonNull          boolean, " +
                "    boolNull             boolean, " +

                "    stringNonNull        text, " +
                "    stringNull           text, " +

                "    bigDecimalNonNull    numeric, " +
                "    bigDecimalNull       numeric, " +

                "    localDateTimeNonNull timestamptz, " +
                "    localDateTimeNull    timestamptz, " +

                "    localDateNonNull     date, " +
                "    localDateNull        date, " +

                "    dateNonNull          timestamptz, " +
                "    dateNull             timestamptz, " +

                "    enumNonNull          text, " +
                "    enumNull             text" +
                ")");
        var updater = new PgBatchUpdater<>(jdbcTemplate, PgBatchUpdaterConfig.<Entity>builder()
                .tableName("setters_test").keyColumns("longNonNull")
                .column("longNonNull", longSetter(e -> e.longNonNull))
                .column("longNull", longSetter(e -> e.longNull))

                .column("intNonNull", intSetter(e -> e.intNonNull))
                .column("intNull", intSetter(e -> e.intNull))

                .column("doubleNonNull", doubleSetter(e -> e.doubleNonNull))
                .column("doubleNull", doubleSetter(e -> e.doubleNull))

                .column("shortNonNull", shortSetter(e -> e.shortNonNull))
                .column("shortNull", shortSetter(e -> e.shortNull))

                .column("boolNonNull", booleanSetter(e -> e.boolNonNull))
                .column("boolNull", booleanSetter(e -> e.boolNull))

                .column("stringNonNull", stringSetter(e -> e.stringNonNull))
                .column("stringNull", stringSetter(e -> e.stringNull))

                .column("bigDecimalNonNull", bigDecimalSetter(e -> e.bigDecimalNonNull))
                .column("bigDecimalNull", bigDecimalSetter(e -> e.bigDecimalNull))

                .column("localDateTimeNonNull", localDateTimeSetter(e -> e.localDateTimeNonNull))
                .column("localDateTimeNull", localDateTimeSetter(e -> e.localDateTimeNull))

                .column("localDateNonNull", localDateSetter(e -> e.localDateNonNull))
                .column("localDateNull", localDateSetter(e -> e.localDateNull))

                .column("dateNonNull", dateSetter(e -> e.dateNonNull))
                .column("dateNull", dateSetter(e -> e.dateNull))

                .column("enumNonNull", enumNameSetter(e -> e.enumNonNull))
                .column("enumNull", enumNameSetter(e -> e.enumNull))
                .build()
        );

        updater.insertOrUpdate(List.of(new Entity()));
        var dbEntity = jdbcTemplate.queryForObject(
                "SELECT * FROM setters_test",
                BeanPropertyRowMapper.newInstance(Entity.class)
        );
        assertEquals(new Entity(), dbEntity);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Entity {
        Long longNonNull = 1L;
        Long longNull = null;

        Integer intNonNull = 2;
        Integer intNull = null;

        Double doubleNonNull = 2.5;
        Double doubleNull = null;

        Short shortNonNull = 3;
        Short shortNull = null;

        Boolean boolNonNull = true;
        Boolean boolNull = null;

        String stringNonNull = "4";
        String stringNull = null;

        BigDecimal bigDecimalNonNull = BigDecimal.valueOf(5);
        BigDecimal bigDecimalNull = null;

        LocalDateTime localDateTimeNonNull = LocalDateTime.of(2021, 3, 1, 6, 20);
        LocalDateTime localDateTimeNull = null;

        LocalDate localDateNonNull = LocalDate.of(2021, 3, 1);
        LocalDate localDateNull = null;

        Date dateNonNull = DateUtil.asDate(LocalDateTime.of(2021, 3, 1, 18, 22));
        Date dateNull = null;

        EntityEnum enumNonNull = EntityEnum.VALUE;
        EntityEnum enumNull = null;
    }

    private enum EntityEnum {
        VALUE
    }
}
