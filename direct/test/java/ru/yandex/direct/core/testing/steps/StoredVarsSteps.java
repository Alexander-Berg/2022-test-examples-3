package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.repository.TestStoredVarsRepository;
import ru.yandex.direct.dbschema.monitor.enums.StoredVarsStatuslocked;

/**
 * Степы для работы с хранилищем сессионных переменных (таблицей monitor.stored_vars)
 */
@ParametersAreNonnullByDefault
public class StoredVarsSteps {
    private static final long min = 1;
    private static final long max = Integer.MAX_VALUE;

    private final long uid = RandomUtils.nextLong(min, max);
    private final ULong name = ULong.valueOf(RandomUtils.nextLong(min, max));
    private final byte[] yamlData = {};
    private final StoredVarsStatuslocked statusLockedYes = StoredVarsStatuslocked.Yes;

    @Autowired
    private TestStoredVarsRepository repository;

    public Long createStoredVar(long cid, LocalDateTime logdate) {
        return createStoredVar(cid, uid, name, yamlData, logdate, statusLockedYes);
    }

    private Long createStoredVar(long cid, long uid, ULong name, byte[] yamldata, LocalDateTime logdate,
                                 StoredVarsStatuslocked statuslocked) {
        return repository.createStoredVar(cid, uid, name, yamldata, logdate, statuslocked);
    }

    public List<Long> getAllStoredVars() {
        return repository.getAllStoredVarsIds();
    }

    public void clearAllStoredVars() {
        repository.clearAllStoredVars();
    }

}
