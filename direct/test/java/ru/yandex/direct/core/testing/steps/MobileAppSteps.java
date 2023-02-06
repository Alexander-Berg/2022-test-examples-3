package ru.yandex.direct.core.testing.steps;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppService;
import ru.yandex.direct.core.testing.data.TestMobileApps;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestMobileApps.defaultMobileApp;
import static ru.yandex.direct.core.testing.data.TestMobileApps.getUrlHost;

@ParametersAreNonnullByDefault
@Component
public class MobileAppSteps {
    private ClientSteps clientSteps;
    private MobileAppService mobileAppService;
    private MobileContentSteps mobileContentSteps;

    @Autowired
    public MobileAppSteps(ClientSteps clientSteps, MobileAppService mobileAppService,
                          MobileContentSteps mobileContentSteps) {
        this.clientSteps = clientSteps;
        this.mobileAppService = mobileAppService;
        this.mobileContentSteps = mobileContentSteps;
    }

    public MobileAppInfo createDefaultMobileApp(ClientInfo clientInfo) {
        return createMobileApp(clientInfo, TestMobileApps.DEFAULT_STORE_URL);
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, String storeUrl, String trackerUrl,
                                         String impressionUrl) {
        return createMobileApp(clientInfo, storeUrl, singletonList(trackerUrl), singletonList(impressionUrl));
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, String storeUrl, List<String> trackerUrls,
                                         List<String> impressionUrls) {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(clientInfo, storeUrl);
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl, trackerUrls, impressionUrls);
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, String storeUrl) {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(clientInfo, storeUrl);
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl);
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, MobileContentInfo mobileContentInfo, String storeUrl) {
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl, emptyList(), emptyList());
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, MobileContentInfo mobileContentInfo,
                                         String storeUrl, String trackerUrl, String impressionUrl) {
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl, singletonList(trackerUrl),
                singletonList(impressionUrl));
    }

    public MobileAppInfo createMobileApp(ClientInfo clientInfo, MobileContentInfo mobileContentInfo,
                                         String storeUrl, MobileAppTracker tracker) {
        tracker
                .withClientId(clientInfo.getClientId())
                .withUserParams(emptyList());
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl, List.of(tracker));
    }

    private MobileAppInfo createMobileApp(ClientInfo clientInfo, MobileContentInfo mobileContentInfo,
                                          String storeUrl, List<String> trackerUrls, List<String> impressionUrls) {
        List<MobileAppTracker> trackers = EntryStream.zip(trackerUrls, impressionUrls)
                .mapKeyValue((trackerUrl, impressionUrl) -> new MobileAppTracker()
                        .withClientId(clientInfo.getClientId())
                        .withTrackerId(null)
                        .withUrl(trackerUrl)
                        .withImpressionUrl(impressionUrl)
                        .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                        .withUserParams(emptyList()))
                .toList();
        return createMobileApp(clientInfo, mobileContentInfo, storeUrl, trackers);
    }

    private MobileAppInfo createMobileApp(ClientInfo clientInfo, MobileContentInfo mobileContentInfo,
                                          String storeUrl, List<MobileAppTracker> trackers) {
        var mobileApp = new MobileApp()
                .withName(mobileContentInfo.getMobileContent().getStoreContentId())
                .withStoreHref(storeUrl)
                .withDisplayedAttributes(emptySet())
                .withTrackers(trackers)
                .withDomain(getUrlHost(storeUrl));
        return createMobileApp(
                new MobileAppInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContentInfo(mobileContentInfo)
                        .withMobileApp(mobileApp));

    }

    public MobileAppInfo createMobileApp(MobileAppInfo mobileAppInfo) {

        if (mobileAppInfo.getClientId() == null) {
            clientSteps.createClient(mobileAppInfo.getClientInfo());
        }

        if (mobileAppInfo.getMobileContentId() == null) {
            mobileContentSteps.createMobileContent(mobileAppInfo.getMobileContentInfo());
        }

        var mobileApp = mobileAppInfo.getMobileApp();
        if (mobileApp == null) {
            mobileApp = defaultMobileApp();
            mobileAppInfo.setMobileApp(mobileApp);
        }

        if (mobileApp.getName() == null) {
            mobileApp.setName(mobileAppInfo.getMobileContentInfo().getStoreContentId());
        }

        if (mobileApp.getMobileContentId() == null) {
            mobileApp.setMobileContentId(mobileAppInfo.getMobileContentInfo().getMobileContentId());
        }

        MassResult<Long> result =
                mobileAppService.createAddPartialOperation(null, mobileAppInfo.getClientId(), singletonList(mobileApp))
                        .prepareAndApply();

        if (result.getValidationResult().hasAnyErrors()) {
            throw new IllegalArgumentException(result.getValidationResult().flattenErrors().toString());
        }

        return mobileAppInfo;
    }
}
