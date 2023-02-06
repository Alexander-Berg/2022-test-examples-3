package ru.yandex.direct.core.entity.promocodes.service;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.promocodes.repository.PromocodeDomainsRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;

import static org.mockito.Mockito.mock;

public class PromocodesAntiFraudServiceBuilder {
    private BalanceClient balanceClient;
    private ShardHelper shardHelper;
    private HostingsHandler hostingsHandler;
    private CampaignRepository campaignRepository;
    private BannerDomainRepository bannerDomainRepository;
    private DomainRepository domainRepository;
    private PpcPropertiesSupport propertiesSupport;
    private PromocodeDomainsRepository promocodeDomainsRepository;
    private PromocodesTearOffMailSenderService mailSenderService;

    public PromocodesAntiFraudServiceBuilder withPropertiesSupport(PpcPropertiesSupport propertiesSupport) {
        this.propertiesSupport = propertiesSupport;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withBalanceClient(BalanceClient balanceClient) {
        this.balanceClient = balanceClient;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withShardHelper(ShardHelper shardHelper) {
        this.shardHelper = shardHelper;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withHostingsHandler(HostingsHandler hostingsHandler) {
        this.hostingsHandler = hostingsHandler;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withCampaignRepository(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withBannerRepository(BannerDomainRepository bannerRepository) {
        this.bannerDomainRepository = bannerRepository;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withDomainRepository(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withPromocodeDomainsRepository(
            PromocodeDomainsRepository promocodeDomainsRepository) {
        this.promocodeDomainsRepository = promocodeDomainsRepository;
        return this;
    }

    public PromocodesAntiFraudServiceBuilder withMailSenderService(
            PromocodesTearOffMailSenderService mailSenderService) {
        this.mailSenderService = mailSenderService;
        return this;
    }

    public PromocodesAntiFraudService build() {
        if (propertiesSupport == null) {
            propertiesSupport = mock(PpcPropertiesSupport.class);
        }
        return new PromocodesAntiFraudService(balanceClient,
             campaignRepository,
                bannerDomainRepository,
             domainRepository,
             shardHelper,
             hostingsHandler,
             propertiesSupport,
             promocodeDomainsRepository,
             mailSenderService);
    }
}
