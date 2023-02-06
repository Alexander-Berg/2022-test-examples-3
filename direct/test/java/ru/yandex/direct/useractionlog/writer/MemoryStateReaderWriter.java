package ru.yandex.direct.useractionlog.writer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.useractionlog.db.StateReader;
import ru.yandex.direct.useractionlog.db.StateReaderWriter;
import ru.yandex.direct.useractionlog.db.StateWriter;
import ru.yandex.direct.useractionlog.db.UserActionLogStates;

@ParametersAreNonnullByDefault
public class MemoryStateReaderWriter implements StateReader, StateWriter {
    private Map<String, UserActionLogStates> stateMap = new HashMap<>();
    private Set<Integer> breakBeforeLogSavingAttempts = new HashSet<>();
    private Set<Integer> breakAfterLogSavingAttempts = new HashSet<>();
    private Set<Integer> breakBeforeDictSavingAttempts = new HashSet<>();
    private int logStatesSaved = 1;
    private int dictStatesSaved = 1;
    private boolean broken;

    public StateReaderWriter asStateReaderWriter() {
        return new StateReaderWriter(this, this);
    }

    public void fix() {
        broken = false;
    }

    private void throwIfBroken() {
        if (broken) {
            throw new ScaryTerribleException();
        }
    }

    @Override
    public UserActionLogStates getActualStates(String source) {
        return stateMap.getOrDefault(source, UserActionLogStates.EMPTY);
    }

    @Override
    public Map<String, MySQLBinlogState> getAllActualLogStates() {
        return stateMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getLog()));
    }

    @Override
    public void saveLogState(String source, MySQLBinlogState state) {
        throwIfBroken();
        try {
            broken = breakBeforeLogSavingAttempts.contains(logStatesSaved);
            throwIfBroken();
            stateMap.put(source, UserActionLogStates.builder()
                    .fromOldStates(stateMap.getOrDefault(source, UserActionLogStates.EMPTY))
                    .withLog(state)
                    .build());
            broken = breakAfterLogSavingAttempts.contains(logStatesSaved);
            throwIfBroken();
        } finally {
            ++logStatesSaved;
        }
    }

    @Override
    public void saveDictState(String source, MySQLBinlogState state) {
        throwIfBroken();
        broken = breakBeforeDictSavingAttempts.contains(dictStatesSaved++);
        throwIfBroken();
        stateMap.put(source, UserActionLogStates.builder()
                .fromOldStates(stateMap.getOrDefault(source, UserActionLogStates.EMPTY))
                .withDict(state)
                .build());

    }

    @Override
    public void saveBothStates(String source, MySQLBinlogState state) {
        stateMap.put(source, UserActionLogStates.builder()
                .withLog(state)
                .withDict(state)
                .build());
    }

    public MemoryStateReaderWriter breakBeforeLogSavingAttempts(Integer... attempts) {
        breakBeforeLogSavingAttempts.addAll(Arrays.asList(attempts));
        return this;
    }

    public MemoryStateReaderWriter breakAfterLogSavingAttempts(Integer... attempts) {
        breakAfterLogSavingAttempts.addAll(Arrays.asList(attempts));
        return this;
    }

    public MemoryStateReaderWriter breakBeforeDictSavingAttempts(Integer... attempts) {
        breakBeforeDictSavingAttempts.addAll(Arrays.asList(attempts));
        return this;
    }

    public int errorsShouldBeThrown() {
        return breakBeforeDictSavingAttempts.size()
                + breakAfterLogSavingAttempts.size()
                + breakBeforeLogSavingAttempts.size();
    }

    public static class ScaryTerribleException extends RuntimeException {
    }
}
