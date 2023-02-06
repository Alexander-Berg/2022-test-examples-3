package ru.yandex.direct.core.testing.steps;

import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.repository.TestSitelinkSetRepository;
import ru.yandex.direct.utils.Counter;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelinkSet;

public class SitelinkSetSteps {

    private final ClientSteps clientSteps;
    private final SitelinkRepository sitelinkRepository;
    private final SitelinkSetRepository sitelinkSetRepository;
    private final TestSitelinkSetRepository testSitelinkSetRepository;

    public SitelinkSetSteps(ClientSteps clientSteps,
                            SitelinkRepository sitelinkRepository,
                            SitelinkSetRepository sitelinkSetRepository,
                            TestSitelinkSetRepository testSitelinkSetRepository) {
        this.clientSteps = clientSteps;
        this.sitelinkRepository = sitelinkRepository;
        this.sitelinkSetRepository = sitelinkSetRepository;
        this.testSitelinkSetRepository = testSitelinkSetRepository;
    }

    public SitelinkSetInfo createDefaultSitelinkSet() {
        return createSitelinkSet(new SitelinkSetInfo());
    }

    public SitelinkSetInfo createDefaultSitelinkSet(ClientInfo clientInfo) {
        return createSitelinkSet(new SitelinkSetInfo().withClientInfo(clientInfo));
    }

    public SitelinkSetInfo createSitelinkSet(SitelinkSet sitelinkSet, ClientInfo clientInfo) {
        SitelinkSetInfo sitelinkSetInfo = new SitelinkSetInfo()
                .withSitelinkSet(sitelinkSet)
                .withClientInfo(clientInfo);
        return createSitelinkSet(sitelinkSetInfo);
    }

    public SitelinkSetInfo createSitelinkSet(SitelinkSetInfo sitelinkSetInfo) {
        if (sitelinkSetInfo.getSitelinkSet() == null) {
            sitelinkSetInfo.withSitelinkSet(defaultSitelinkSet());
        }
        if (sitelinkSetInfo.getSitelinkSetId() == null) {
            Counter counter = new Counter();
            sitelinkSetInfo.getSitelinkSet().getSitelinks().forEach(s -> s.setOrderNum((long) counter.next()));

            if (sitelinkSetInfo.getClientId() == null) {
                clientSteps.createClient(sitelinkSetInfo.getClientInfo());
            }
            sitelinkSetInfo.getSitelinkSet()
                    .withClientId(sitelinkSetInfo.getClientId().asLong());
            sitelinkRepository.add(sitelinkSetInfo.getShard(),
                    sitelinkSetInfo.getSitelinkSet().getSitelinks());
            sitelinkSetRepository.add(sitelinkSetInfo.getShard(),
                    singletonList(sitelinkSetInfo.getSitelinkSet()));
        }
        return sitelinkSetInfo;
    }

    public void linkBannerWithSitelinkSet(
            int shard,
            Long campaignId, Long bannerId, Long sitelinkSetId) {
        testSitelinkSetRepository.linkBannerWithSitelinkSet(shard, campaignId, bannerId, sitelinkSetId);
    }
}
