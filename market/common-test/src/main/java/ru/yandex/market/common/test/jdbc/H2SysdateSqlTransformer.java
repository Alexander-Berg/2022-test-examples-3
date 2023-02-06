package ru.yandex.market.common.test.jdbc;

import org.intellij.lang.annotations.Language;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import ru.yandex.market.common.test.transformer.PatternStringTransformer;

/**
 * Транформаер sysdate.
 * Можно использовать для установки даты в тесте.
 * {@see InstrumentedDataSource#withExtraTransformer}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class H2SysdateSqlTransformer extends PatternStringTransformer {

    private static final DateTimeFormatter SQL_TIMESTAMP_LITERAL_FORMAT =
            DateTimeFormatter.ofPattern("'TIMESTAMP '''uuuu-MM-dd HH:mm:ss.SSS''");

    @Language("RegExp")
    private static final String SYSDATE_PATTERN = "(?<prefix>\\W)sysdate(?<postfix>\\W)";

    public H2SysdateSqlTransformer(final Instant instant) {
        super(SYSDATE_PATTERN, getReplacement(instant));
    }

    private static String getReplacement(final Instant instant) {
        return String.format("${prefix}%s${postfix}", formatInstant(instant));
    }

    private static String formatInstant(final Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(SQL_TIMESTAMP_LITERAL_FORMAT);
    }
}
