package ru.yandex.direct.core.testing.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.banner.model.AdditionType;
import ru.yandex.direct.core.entity.banner.model.BannerAddition;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerAdditionsRepository;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCallouts.defaultCallout;

public class CalloutSteps {
    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private OldBannerAdditionsRepository bannerAdditionsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CalloutService calloutService;

    public Callout createDefaultCallout(ClientInfo clientInfo) {
        Callout defaultCallout = defaultCallout(clientInfo.getClientId());
        calloutRepository.add(clientInfo.getShard(), singletonList(defaultCallout));
        return defaultCallout;
    }

    public Callout createCalloutWithText(ClientInfo clientInfo, String text) {
        Callout callout = defaultCallout(clientInfo.getClientId())
                .withText(text);
        calloutRepository.add(clientInfo.getShard(), singletonList(callout));
        return callout;
    }

    public <B extends OldBanner> void linkCalloutToBanner(Callout callout, AbstractBannerInfo<B> banner) {
        checkState(callout.getClientId().equals(banner.getClientId().asLong()));

        var addition = new BannerAddition().withAdditionType(AdditionType.CALLOUT)
                .withBannerId(banner.getBannerId())
                .withId(callout.getId())
                .withSequenceNum(0L);
        bannerAdditionsRepository.addOrUpdateBannerAdditions(dslContextProvider.ppc(banner.getShard()),
                List.of(addition));
    }

    public Callout getCallout(int shard, Long calloutId) {
        return getCallouts(shard, Collections.singleton(calloutId)).get(0);
    }

    public List<Callout> getCallouts(int shard, Collection<Long> calloutIds) {
        return calloutRepository.get(shard, calloutIds);
    }

    public void deleteCallouts(ClientId clientId, Collection<Long> calloutIds) {
        calloutService.detachAndDeleteCallouts(new ArrayList<>(calloutIds), clientId);
    }

}
