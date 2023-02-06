package ru.yandex.market.crm.campaign.test.loggers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.loggers.ExecutedActionsLogger;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

/**
 * @author apershukov
 */
@Component
public class TestExecutedActionsLogger implements ExecutedActionsLogger {

    private final List<LogEntry> entries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void log(String actionId, String segmentId, String idValue, UidType uidType, String variant) {
        LogEntry entry = new LogEntry(actionId, segmentId, idValue, uidType, variant);
        entries.add(entry);
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void reset() {
        entries.clear();
    }

    public static class LogEntry {

        private final String actionId;
        private final String segmentId;
        private final String idValue;
        private final UidType uidType;
        private final String variant;

        LogEntry(String actionId, String segmentId, String idValue, UidType uidType, String variant) {
            this.actionId = actionId;
            this.segmentId = segmentId;
            this.idValue = idValue;
            this.uidType = uidType;
            this.variant = variant;
        }

        public String getSegmentId() {
            return segmentId;
        }

        public String getActionId() {
            return actionId;
        }

        public String getIdValue() {
            return idValue;
        }

        public String getVariant() {
            return variant;
        }

        public UidType getUidType() {
            return uidType;
        }

        public Uid getUid() {
            return Uid.of(uidType, idValue);
        }
    }
}
