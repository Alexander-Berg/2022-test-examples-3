package ru.yandex.market.sqb.test.db;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;

/**
 * Билдер для создания касомных реплейсментов в для dbUnit dataset-ов.
 *
 * @author otedikova
 */
public class ReplacementDataSetBuilder {
    private static final String SYSTIMESTAMP = "[SYSTIMESTAMP]";
    private ReplacementDataSet replacementDataSet;
    private static final String TODAY = "[TODAY]";
    private static final String YESTERDAY = "[YESTERDAY]";

    ReplacementDataSetBuilder(IDataSet dataSet) {
        this.replacementDataSet = new ReplacementDataSet(dataSet);
    }

    /**
     * Плейсхолдер [SYSTIMESTAMP] для использования текущего времени в dataset-е.
     *
     * @return билдер с поддержкой плейсхолдера [SYSTIMESTAMP].
     */
    public ReplacementDataSetBuilder withSystimestampReplacement() {
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
        final LocalDateTime today = now.truncatedTo(ChronoUnit.DAYS);
        final LocalDateTime yesterday = today.minusDays(1);
        replacementDataSet.addReplacementObject(SYSTIMESTAMP, Timestamp.valueOf(now));
        replacementDataSet.addReplacementObject(TODAY, Timestamp.valueOf(today));
        replacementDataSet.addReplacementObject(YESTERDAY, Timestamp.valueOf(yesterday));
        return this;
    }

    public ReplacementDataSet build() {
        return replacementDataSet;
    }
}
