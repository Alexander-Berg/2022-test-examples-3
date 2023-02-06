package ru.yandex.market.checkout.checkouter.monitoring.db.maintenance;


import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.storage.CheckouterSequence;
import ru.yandex.market.checkout.storage.impl.DatabaseStorage;

public class SequenceMaintenanceTest extends AbstractWebTestBase {

    @Autowired
    private DatabaseStorage databaseStorage;

    private static final EnumSet<CheckouterSequence> MATCH_TABLE_NAME_CHECK_IGNORE_SEQUENCES = EnumSet.of(
            CheckouterSequence.BANK_DETAILS_SEQ,
            CheckouterSequence.ORDER_ADDRESS_SEQ,
            CheckouterSequence.ORDER_SEQ,
            CheckouterSequence.ORDER_DELIVERY_TRACK_SEQ,
            CheckouterSequence.ORDER_DELIVERY_TRACK_CHECKPOINT_SEQ,
            CheckouterSequence.ORDER_DELIVERY_TRACK_HISTORY_SEQ,
            CheckouterSequence.ORDER_DELIVERY_TRACK_CHECKPOINT_HISTORY_SEQ,
            CheckouterSequence.ORDER_CHANGE_REQUEST_SEQ,
            CheckouterSequence.ORDER_REFUND_SEQ,
            CheckouterSequence.ORDER_REFUND_HISTORY_SEQ,
            CheckouterSequence.ORDER_RECEIPT_SEQ
    );
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("^(?<tableName>[a-z0-9_]+)_seq$");
    private static final String SQL_IS_TABLE_EXISTS = "SELECT 1\n" +
            "FROM information_schema.tables\n" +
            "WHERE table_schema = 'public'\n" +
            "  AND table_name = ?";

    @Test
    public void checkSequenceExists() {
        for (CheckouterSequence sequence : CheckouterSequence.values()) {
            databaseStorage.getNextSequenceVal(sequence);
        }
    }

    @Test
    public void checkSequenceNameMatchesRegex() {
        for (CheckouterSequence sequence : CheckouterSequence.values()) {
            Matcher matcher = SEQUENCE_PATTERN.matcher(sequence.getName());
            Assertions.assertTrue(matcher.matches());
        }
    }

    @Test
    public void checkSequenceNameMatchesTableName() {
        for (CheckouterSequence sequence : CheckouterSequence.values()) {
            if (MATCH_TABLE_NAME_CHECK_IGNORE_SEQUENCES.contains(sequence)) {
                continue;
            }

            Matcher matcher = SEQUENCE_PATTERN.matcher(sequence.getName());
            matcher.matches();

            try {
                masterJdbcTemplate.queryForObject(SQL_IS_TABLE_EXISTS, Long.class, matcher.group("tableName"));
            } catch (EmptyResultDataAccessException e) {
                Assertions.fail("Sequence name " + sequence.getName() + " does not match table name");
            }
        }
    }
}
