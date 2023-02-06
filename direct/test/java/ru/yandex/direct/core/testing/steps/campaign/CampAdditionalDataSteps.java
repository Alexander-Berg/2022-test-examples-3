package ru.yandex.direct.core.testing.steps.campaign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.testing.repository.TestCampAdditionalDataRepository;

@Lazy
@Component
public class CampAdditionalDataSteps {
    @Autowired
    private TestCampAdditionalDataRepository testCampAdditionalDataRepository;

    public void addHref(int shard, Long cid, String href) {
        testCampAdditionalDataRepository.addHref(shard, cid, href);
    }
}
