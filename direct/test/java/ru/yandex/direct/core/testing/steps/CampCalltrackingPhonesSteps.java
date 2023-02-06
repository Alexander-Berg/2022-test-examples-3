package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.calltracking.model.CampCalltrackingPhones;
import ru.yandex.direct.core.entity.campcalltrackingphones.repository.CampCalltrackingPhonesRepository;
import ru.yandex.direct.core.testing.repository.TestCampCalltrackingPhonesRepository;

public class CampCalltrackingPhonesSteps {
    @Autowired
    private CampCalltrackingPhonesRepository campCalltrackingPhonesRepository;
    @Autowired
    private TestCampCalltrackingPhonesRepository testCampCalltrackingPhonesRepository;

    public void add(int shard, long clientPhoneId, long campaignId) {
        campCalltrackingPhonesRepository.add(shard, clientPhoneId, campaignId);
    }

    public Map<Long, List<CampCalltrackingPhones>> getByCampaignId(int shard) {
        return campCalltrackingPhonesRepository.getByCampaignId(shard);
    }

    public void deleteAll(int shard) {
        testCampCalltrackingPhonesRepository.deleteAll(shard);
    }
}
