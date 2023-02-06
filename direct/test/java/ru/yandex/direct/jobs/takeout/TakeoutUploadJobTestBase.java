package ru.yandex.direct.jobs.takeout;

import java.time.LocalDateTime;
import java.util.Collections;

import javax.annotation.Nonnull;

import io.leangen.graphql.annotations.GraphQLNonNull;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.core.entity.timetarget.service.GeoTimezoneMappingService;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.geobasehelper.GeoBaseHelperStub;
import ru.yandex.direct.mail.MailSender;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.takeout.client.TakeoutClient;
import ru.yandex.direct.takeout.client.TakeoutResponse;
import ru.yandex.direct.useractionlog.ChangeSource;
import ru.yandex.direct.useractionlog.reader.FilterLogRecordsByCampaignTypeBuilder;
import ru.yandex.direct.useractionlog.reader.UserActionLogReader;
import ru.yandex.direct.useractionlog.reader.model.LogEvent;
import ru.yandex.direct.useractionlog.reader.model.LogRecord;
import ru.yandex.direct.useractionlog.reader.model.OutputCategory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TakeoutUploadJobTestBase {
    @Autowired
    private ClientService clientService;
    @Autowired
    private UserService userService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private GeoBaseHelper geoBaseHttpApiHelper;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private PerformanceFilterService performanceFilterService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private VcardService vcardService;
    @Autowired
    private SitelinkSetService sitelinkSetService;
    @Autowired
    private CalloutService calloutService;
    @Autowired
    private TurboLandingService turboLandingService;
    @Autowired
    private CreativeService creativeService;
    @Autowired
    private BannerImageFormatRepository bannerImageFormatRepository;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private AgencyClientRelationService agencyClientRelationService;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private EnvironmentType environmentType;
    @Autowired
    private FilterLogRecordsByCampaignTypeBuilder filterLogRecordsByCampaignTypeBuilder;
    @Autowired
    private GeoTimezoneMappingService geoTimezoneMappingService;
    @Autowired
    private FeedService feedService;
    @Autowired
    private ClientGeoService clientGeoService;

    private UserActionLogReader userActionLogReader = mock(UserActionLogReader.class);

    protected TakeoutClient takeoutClient;
    private BalanceClient balanceClient;
    protected TakeoutRules takeoutRules;

    protected TakeoutJobService initJobService() {
        takeoutClient = takeoutClient();
        balanceClient = balanceClient();
        takeoutRules = new TakeoutRules();
        ((GeoBaseHelperStub) geoBaseHttpApiHelper).addRegionWithParent(Region.TURKEY_REGION_ID,
                Collections.singletonList(Long.valueOf(Region.EUROPE_REGION_ID).intValue()));

        LogRecord logRecord = new LogRecord(LocalDateTime.now(), null, "", new LogEvent() {
            @Nonnull
            @Override
            public @GraphQLNonNull
            OutputCategory getCategory() {
                return OutputCategory.CAMPAIGN_SHOW;
            }
        }, ChangeSource.WEB, false);
        UserActionLogReader.FilterResult filterResult = mock(UserActionLogReader.FilterResult.class);
        when(filterResult.getOffset()).thenReturn(null);
        when(filterResult.getRecords()).thenReturn(Collections.singletonList(logRecord));
        doReturn(filterResult).when(userActionLogReader).filterActionLog(any(), anyInt(), any(), any(), any());

        return new TakeoutJobService(
                takeoutRules,
                takeoutClient,
                clientService,
                userService,
                campaignService,
                bidModifierService,
                adGroupService,
                geoTreeFactory,
                geoBaseHttpApiHelper, keywordService,
                relevanceMatchRepository,
                retargetingService,
                performanceFilterService,
                bannerService,
                vcardService,
                sitelinkSetService,
                calloutService,
                turboLandingService,
                creativeService,
                bannerImageFormatRepository, mailSender, balanceClient, agencyClientRelationService,
                userActionLogReader, dbQueueRepository, environmentType, filterLogRecordsByCampaignTypeBuilder,
                geoTimezoneMappingService, feedService, clientGeoService);
    }

    protected TakeoutClient takeoutClient() {
        TakeoutClient c = mock(TakeoutClient.class);
        TakeoutResponse response = new TakeoutResponse();
        response.setStatus("ok");
        doReturn(response).when(c).done(anySet(), anyString());
        doReturn(response).when(c).uploadFile(any(), anyString());
        return c;
    }

    protected BalanceClient balanceClient() {
        return mock(BalanceClient.class);
    }
}
