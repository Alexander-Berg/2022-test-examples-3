package ru.yandex.direct.core.testing.mock;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.service.KeywordInclusionService;
import ru.yandex.direct.core.entity.keyword.service.KeywordShowsForecastService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

@Component
@ParametersAreNonnullByDefault
public class KeywordShowsForecastServiceMockUtils {

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private KeywordInclusionService keywordInclusionService;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    /**
     * @return мок {@link KeywordShowsForecastService}, который на все фразы отвечает прогнозом {@code defaultForecast}
     */
    public KeywordShowsForecastService mockWithDefaultForecast(long defaultForecast) {
        AdvqClient mockClient = new AdvqClientStub(defaultForecast);

        return new KeywordShowsForecastService(
                mockClient,
                shardHelper,
                campaignRepository,
                adGroupRepository,
                minusKeywordsPackRepository, minusKeywordPreparingTool, keywordInclusionService,
                stopWordService,
                ppcPropertiesSupport);
    }
}
