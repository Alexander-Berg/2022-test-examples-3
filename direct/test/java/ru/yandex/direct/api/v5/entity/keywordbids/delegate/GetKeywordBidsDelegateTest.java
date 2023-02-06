package ru.yandex.direct.api.v5.entity.keywordbids.delegate;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.keywordbids.KeywordBidAnyFieldEnum;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.testing.configuration.Api5TestingConfiguration;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsShowsForecastService;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.auction.service.BsAuctionService;
import ru.yandex.direct.core.entity.bids.container.CompleteBidData;
import ru.yandex.direct.core.entity.bids.container.KeywordBidPokazometerData;
import ru.yandex.direct.core.entity.bids.container.ShowConditionSelectionCriteria;
import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.core.entity.bids.service.PokazometerService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.pokazometer.PhraseResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@ContextHierarchy({
        @ContextConfiguration(classes = Api5TestingConfiguration.class),
        @ContextConfiguration(classes = GetKeywordBidsDelegateTest.OverridingConfiguration.class)
})
@Api5Test
@RunWith(SpringRunner.class)
public class GetKeywordBidsDelegateTest {
    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.RUB;

    @Autowired
    Steps steps;
    @Autowired
    ApiUserRepository apiUserRepository;

    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;
    @Autowired
    BsAuctionService bsAuctionServiceMock;

    @Autowired
    GetKeywordBidsDelegate getKeywordBidsDelegate;

    private KeywordInfo keywordInfo;

    @Before
    public void setUp() {
        keywordInfo = steps.keywordSteps().createDefaultKeyword();
        ClientInfo clientInfo = keywordInfo.getAdGroupInfo().getClientInfo();

        ApiUser operator = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        new ApiAuthenticationSourceMockBuilder()
                .withOperator(operator)
                .tuneAuthSourceMock(apiAuthenticationSourceMock);
    }

    @Test
    public void get_filterRequiredTrafficVolumes() {
        when(bsAuctionServiceMock.getBsTrafaretResults(any(), anyList()))
                .thenAnswer(invocation -> {
                    List<AdGroupForAuction> keywords = invocation.getArgument(1);
                    return keywords.stream()
                            .map(AdGroupForAuction::getKeywords)
                            .flatMap(Collection::stream)
                            .map(keyword -> new KeywordTrafaretData()
                                    .withKeyword(keyword)
                                    // эмулируем ответ со значниями trafficVolume от 0 до 120
                                    .withBidItems(IntStreamEx.rangeClosed(0, 120)
                                            .mapToObj(tv -> bidItem(tv * 10_000L, tv))
                                            .toList()
                                    ))
                            .collect(Collectors.toList());
                });
        GenericGetRequest<KeywordBidAnyFieldEnum, ShowConditionSelectionCriteria> getRequest = new GenericGetRequest<>(
                EnumSet.of(KeywordBidAnyFieldEnum.SEARCH_AUCTION_BIDS),
                new ShowConditionSelectionCriteria().withShowConditionIds(singleton(keywordInfo.getId())),
                maxLimited());
        List<CompleteBidData<KeywordTrafaretData>> completeBidData = getKeywordBidsDelegate.get(getRequest);
        assertThat(completeBidData).hasSize(1);
        CompleteBidData<KeywordTrafaretData> first = completeBidData.iterator().next();
        assertThat(first.getDynamicData()).isNotNull();
        KeywordTrafaretData bsAuctionData = first.getDynamicData().getBsAuctionData();
        List<TrafaretBidItem> bidItems = bsAuctionData.getBidItems();

        List<Long> expectedTrafficVolumes =
                StreamEx.of(
                        5_0000L,
                        10_0000L,
                        15_0000L,
                        65_0000L,
                        70_0000L,
                        75_0000L,
                        80_0000L)
                        // с 85 ожидаем все до 120
                        .append(LongStreamEx.rangeClosed(85_0000L, 120_0000L, 1_0000L).boxed())
                        .reverseSorted()
                        .toList();
        assertThat(bidItems)
                .extracting(TrafaretBidItem::getPositionCtrCorrection)
                .isEqualTo(expectedTrafficVolumes);
    }

    @Test
    public void get_returnMax_whenMaxNotInExpectedValues() {
        long maxTrafficVolume = 67_0000L;
        when(bsAuctionServiceMock.getBsTrafaretResults(any(), anyList()))
                .thenAnswer(invocation -> {
                    List<AdGroupForAuction> keywords = invocation.getArgument(1);
                    return keywords.stream()
                            .map(AdGroupForAuction::getKeywords)
                            .flatMap(Collection::stream)
                            .map(keyword -> new KeywordTrafaretData()
                                    .withKeyword(keyword)
                                    .withBidItems(asList(
                                            bidItem(maxTrafficVolume, 67),
                                            bidItem(10_0000L, 10),
                                            bidItem(7_5000L, 7.5),
                                            bidItem(6_2000L, 6.2),
                                            bidItem(5_5000L, 5.5)
                                    )))
                            .collect(Collectors.toList());
                });
        GenericGetRequest<KeywordBidAnyFieldEnum, ShowConditionSelectionCriteria> getRequest = new GenericGetRequest<>(
                EnumSet.of(KeywordBidAnyFieldEnum.SEARCH_AUCTION_BIDS),
                new ShowConditionSelectionCriteria().withShowConditionIds(singleton(keywordInfo.getId())),
                maxLimited());
        List<CompleteBidData<KeywordTrafaretData>> completeBidData = getKeywordBidsDelegate.get(getRequest);
        assertThat(completeBidData).hasSize(1);
        CompleteBidData<KeywordTrafaretData> first = completeBidData.iterator().next();
        KeywordTrafaretData bsAuctionData = first.getDynamicData().getBsAuctionData();
        List<TrafaretBidItem> bidItems = bsAuctionData.getBidItems();
        assertThat(bidItems)
                .describedAs("Ответ должен содержать элемент с максимальным объёмом трафика %s", maxTrafficVolume)
                .anySatisfy(item -> assertThat(item.getPositionCtrCorrection()).isEqualTo(maxTrafficVolume));
    }

    /**
     * В тестах на get необходимо подменять ответ Торгов и Показометра.
     * Делать это на уровне клиентов неудобно, поскольку на уровне сервисов данные сильно меняются.
     * Для того, чтобы подменить ответ, делаем mock'и для сервисов.
     * Чтобы mock'и не приходилось вручную передвать в тестируемый {@link GetKeywordBidsDelegate}, создаём конфигурацию,
     * в которой {@code getKeywordBidsDelegate} будет подменяться на тот, что с mock'ами.
     *
     * @see ru.yandex.direct.core.entity.bids.service.BidServiceSetAutoTest.OverridingConfiguration
     */
    @Configuration
    @ComponentScan(basePackageClasses = BidService.class)
    @ComponentScan(basePackageClasses = GetKeywordBidsDelegate.class,
            // Чтобы не подтягивались конфигурации с соседних тестов
            excludeFilters = {
                    @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION)
            })
    static class OverridingConfiguration {

        /**
         * Подменить {@link AdGroupsShowsForecastService} на mock чуть проще,
         * чем настраивать ответ {@link ru.yandex.direct.advq.AdvqClient}
         */
        @Bean
        @Primary
        public AdGroupsShowsForecastService adGroupsShowsForecastService() {
            AdGroupsShowsForecastService mock = mock(AdGroupsShowsForecastService.class);
            doNothing().when(mock).updateShowsForecastIfNeeded(any(), anyList(), any());
            return mock;
        }

        @Bean
        @Primary
        public PokazometerService pokazometerService() {
            PokazometerService mock = mock(PokazometerService.class);
            Answer<Object> stubAnswer = invocation -> {
                List<AdGroupForAuction> adGroupsForAuction = invocation.getArgument(0);
                return adGroupsForAuction
                        .stream()
                        .map(AdGroupForAuction::getKeywords)
                        .flatMap(Collection::stream)
                        .map(Keyword::getId)
                        .map(id -> new KeywordBidPokazometerData(id, ImmutableMap.of(
                                PhraseResponse.Coverage.LOW, Money.valueOf(20, CURRENCY_CODE),
                                PhraseResponse.Coverage.MEDIUM, Money.valueOf(50, CURRENCY_CODE),
                                PhraseResponse.Coverage.HIGH, Money.valueOf(100, CURRENCY_CODE)
                        )))
                        .collect(Collectors.toList());
            };
            when(mock.getPokazometerResults(anyList()))
                    .thenAnswer(stubAnswer);
            when(mock.safeGetPokazometerResults(anyList()))
                    .thenAnswer(stubAnswer);
            return mock;
        }

        @Bean
        @Primary
        public BsAuctionService bsAuctionService() {
            return mock(BsAuctionService.class);
        }
    }

    private static Money money(double value) {
        return Money.valueOf(value, CURRENCY_CODE);
    }

    private static TrafaretBidItem bidItem(long ctrCorrection, double money) {
        return new TrafaretBidItem()
                .withPositionCtrCorrection(ctrCorrection)
                .withBid(money(money))
                .withPrice(money(money));
    }
}
