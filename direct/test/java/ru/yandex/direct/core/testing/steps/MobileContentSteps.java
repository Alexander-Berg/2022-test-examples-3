package ru.yandex.direct.core.testing.steps;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusNo;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithNoAgeLabel;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithNoReview;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithNoSize;

@ParametersAreNonnullByDefault
@Component
public class MobileContentSteps {
    private MobileContentRepository repository;
    private ShardHelper shardHelper;
    private ClientSteps clientSteps;
    private DomainSteps domainSteps;

    @Autowired
    public MobileContentSteps(MobileContentRepository repository, ClientSteps clientSteps, ShardHelper shardHelper,
                              DomainSteps domainSteps) {
        this.repository = repository;
        this.clientSteps = clientSteps;
        this.shardHelper = shardHelper;
        this.domainSteps = domainSteps;
    }

    public MobileContentInfo createDefaultMobileContent() {
        return createDefaultMobileContent(clientSteps.createDefaultClient());
    }

    public MobileContentInfo createDefaultMobileContent(int shard) {
        return createDefaultMobileContent(clientSteps.createClient(new ClientInfo().withShard(shard)));
    }

    public MobileContentInfo createDefaultIosMobileContent() {
        return createMobileContent(new MobileContentInfo().withMobileContent(iosMobileContent()));
    }

    public MobileContentInfo createDefaultAndroidMobileContent() {
        return createMobileContent(new MobileContentInfo().withMobileContent(androidMobileContent()));
    }

    public MobileContentInfo createMobileContentWithUnmoderatedIcon() {
        return createMobileContent(
                new MobileContentInfo().withMobileContent(mobileContentWithAppIconModerationStatusNo()));
    }

    public MobileContentInfo createMobileContentWithNoSize() {
        return createMobileContent(new MobileContentInfo().withMobileContent(mobileContentWithNoSize()));
    }

    public MobileContentInfo createMobileContentWithNoAgeLabel() {
        return createMobileContent(new MobileContentInfo().withMobileContent(mobileContentWithNoAgeLabel()));
    }

    public MobileContentInfo createMobileContentWithNoReview() {
        return createMobileContent(new MobileContentInfo().withMobileContent(mobileContentWithNoReview()));
    }

    public MobileContentInfo createDefaultMobileContent(ClientInfo clientInfo) {
        return createMobileContent(new MobileContentInfo().withClientInfo(clientInfo));
    }

    public MobileContentInfo createMobileContent(int shard, MobileContentInfo mobileContentInfo) {
        if (mobileContentInfo.getClientInfo() == null) {
            mobileContentInfo.withClientInfo(clientSteps.createClient(new ClientInfo().withShard(shard)));
        }
        assertThat(mobileContentInfo.getClientInfo().getShard())
                .as("шард должен совпадать с заданным")
                .isEqualTo(shard);
        return createMobileContent(mobileContentInfo);
    }

    public MobileContentInfo createMobileContent(MobileContentInfo mobileContentInfo) {
        return createMobileContent(mobileContentInfo, true);
    }

    public MobileContentInfo createMobileContent(MobileContentInfo mobileContentInfo, boolean createWithDomain) {
        if (mobileContentInfo.getClientInfo() == null) {
            mobileContentInfo.withClientInfo(clientSteps.createDefaultClient());
        } else if (mobileContentInfo.getClientId() == null) {
            clientSteps.createClient(mobileContentInfo.getClientInfo());
        }

        if (mobileContentInfo.getMobileContent() == null) {
            mobileContentInfo.withMobileContent(defaultMobileContent());
        }
        mobileContentInfo.getMobileContent().setClientId(mobileContentInfo.getClientId().asLong());

        if (mobileContentInfo.getMobileContent().getPublisherDomainId() == null && createWithDomain) {
            DomainInfo domain = domainSteps.createDomain(mobileContentInfo.getShard());
            mobileContentInfo.getMobileContent().setPublisherDomainId(domain.getDomainId());
        }

        if (mobileContentInfo.getMobileContentId() == null) {
            createMobileContent(mobileContentInfo.getShard(), mobileContentInfo.getClientId(),
                    mobileContentInfo.getMobileContent());
        }

        return mobileContentInfo;
    }

    private MobileContent createMobileContent(int shard, ClientId clientId,
                                              MobileContent mobileContent) {
        repository.getOrCreateMobileContentList(shard, clientId, Collections.singletonList(mobileContent));
        return mobileContent;
    }

    public MobileContentInfo createMobileContent(ClientInfo clientInfo, String storeUrl) {
        MobileContent mobileContent = mobileContentFromStoreUrl(storeUrl)
                .withClientId(clientInfo.getClientId().asLong());

        MobileContentInfo mobileContentInfo = new MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(mobileContent);

        createMobileContent(clientInfo.getShard(), mobileContentInfo);
        return mobileContentInfo;
    }
}
