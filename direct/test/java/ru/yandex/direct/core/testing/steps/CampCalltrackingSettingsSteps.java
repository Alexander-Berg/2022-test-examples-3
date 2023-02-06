package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.repository.TestCampCalltrackingSettingsRepository;

public class CampCalltrackingSettingsSteps {
    @Autowired
    private TestCampCalltrackingSettingsRepository testCampCalltrackingSettingsRepository;

    public void link(int shard, Long cid, Long calltrackingSettingsId) {
        testCampCalltrackingSettingsRepository.link(shard, cid, calltrackingSettingsId);
    }

    public void deleteAll(int shard) {
        testCampCalltrackingSettingsRepository.deleteAll(shard);
    }
}
