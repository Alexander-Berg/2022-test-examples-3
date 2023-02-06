package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingPhone;
import ru.yandex.direct.core.testing.repository.TestCalltrackingPhonesRepository;

public class CalltrackingPhoneSteps {
    @Autowired
    private TestCalltrackingPhonesRepository testCalltrackingPhonesRepository;

    public void add(String phone, LocalDateTime lastUpdate) {
        testCalltrackingPhonesRepository.add(phone, lastUpdate);
    }

    public List<CalltrackingPhone> getAllPhones() {
        return testCalltrackingPhonesRepository.getAllPhones();
    }

    public void deleteAll() {
        testCalltrackingPhonesRepository.deleteAll();
    }
}
