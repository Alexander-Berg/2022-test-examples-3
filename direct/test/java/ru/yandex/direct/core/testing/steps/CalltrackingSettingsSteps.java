package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingSettings;
import ru.yandex.direct.core.entity.calltracking.model.SettingsPhone;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.testing.repository.TestCalltrackingSettingsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class CalltrackingSettingsSteps {

    @Autowired
    private CalltrackingSettingsRepository calltrackingSettingsRepository;

    @Autowired
    private TestCalltrackingSettingsRepository testCalltrackingSettingsRepository;

    public Long add(ClientId clientId, Long domainId, Long counterId, List<String> phonesToTrack) {
        return add(clientId, domainId, counterId, phonesToTrack, true, LocalDateTime.now());
    }

    public Long add(
            ClientId clientId,
            Long domainId,
            Long counterId,
            List<SettingsPhone> phonesToTrack,
            boolean isAvailableCounter
    ) {
        var calltrackingSettings = new CalltrackingSettings()
                .withClientId(clientId)
                .withDomainId(domainId)
                .withCounterId(counterId)
                .withIsAvailableCounter(isAvailableCounter)
                .withPhonesToTrack(phonesToTrack);
        return calltrackingSettingsRepository.add(clientId, List.of(calltrackingSettings)).get(0);
    }

    public Long add(
            ClientId clientId,
            Long domainId,
            Long counterId,
            List<String> phones,
            boolean isAvailableCounter,
            LocalDateTime lastUpdate
    ) {
        List<SettingsPhone> phonesToTrack = mapList(
                phones,
                p -> new SettingsPhone().withPhone(p).withCreateTime(lastUpdate)
        );
        return add(clientId, domainId, counterId, phonesToTrack, isAvailableCounter);
    }

    public Long add(
            ClientId clientId,
            Long domainId,
            Long counterId,
            Map<String, LocalDateTime> phoneByCreateTime,
            boolean isAvailableCounter
    ) {
        List<SettingsPhone> phonesToTrack = phoneByCreateTime
                .entrySet()
                .stream()
                .map(entry -> new SettingsPhone().withPhone(entry.getKey()).withCreateTime(entry.getValue()))
                .collect(Collectors.toList());
        return add(clientId, domainId, counterId, phonesToTrack, isAvailableCounter);
    }

    public Long add(ClientId clientId, Long domainId) {
        return add(clientId, domainId, RandomNumberUtils.nextPositiveLong(), List.of(getUniqPhone()));
    }

    public void updateIsCounterAvailable(int shard, long calltrackingSettingsId, boolean isAvailable) {
        testCalltrackingSettingsRepository.updateIsCounterAvailable(shard, calltrackingSettingsId, isAvailable);
    }

    public void deleteAll(int shard) {
        testCalltrackingSettingsRepository.deleteAll(shard);
    }
}
