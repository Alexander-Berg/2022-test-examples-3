package ru.yandex.market.pers.grade.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.cs.billing.api.hold.CsBillingHoldingApi;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.cataloger.model.CatalogerResponseWrapper;
import ru.yandex.market.cataloger.model.VersionInfoWrapper;
import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.grade.core.config.GradeCoreConfig;
import ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory;
import ru.yandex.market.pers.grade.core.util.AvatarnicaClient;
import ru.yandex.market.pers.grade.core.util.MbiApiClient;
import ru.yandex.market.pers.grade.core.util.StaffClient;
import ru.yandex.market.pers.grade.core.util.bell.BellClient;
import ru.yandex.market.pers.grade.core.util.datasync.DataSyncClient;
import ru.yandex.market.pers.grade.core.util.reactor.ReactorClient;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.pay.client.PersPayClient;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.tvm.TvmChecker;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.domain.Options;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;
import ru.yandex.market.sdk.userinfo.util.Result;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollectionOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory.generateModel;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@Import({
    GradeMockedDbConfig.class,
    GradeCoreConfig.class,
    GradeCreator.class,
    FactorCreator.class,
})
@Configuration
public class PersCoreMockConfig {
    public static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    @Bean
    public TvmClient tvmClient() {
        return PersTestMocksHolder.registerMock(TvmClient.class);
    }

    @Bean
    public TvmChecker tvmChecker() {
        return PersTestMocksHolder.registerMock(TvmChecker.class);
    }

    @Bean
    public MbiApiClient mbiApiClient() {
        return PersTestMocksHolder.registerMock(MbiApiClient.class);
    }

    @Bean
    public QaClient qaClient() {
        return PersTestMocksHolder.registerMock(QaClient.class);
    }

    @Bean
    public PersAuthorClient authorClient() {
        return PersTestMocksHolder.registerMock(PersAuthorClient.class);
    }

    @Bean
    public BellClient bellClient() {
        return PersTestMocksHolder.registerMock(BellClient.class);
    }

    @Bean
    public ReactorClient reactorClient() {
        return PersTestMocksHolder.registerMock(ReactorClient.class);
    }

    @Bean
    public AvatarnicaClient avatarnicaClient() {
        return PersTestMocksHolder.registerMock(AvatarnicaClient.class);
    }

    @Bean
    public DataSyncClient dataSyncClient() {
        return PersTestMocksHolder.registerMock(DataSyncClient.class);
    }

    @Bean
    public StaffClient staffClient() {
        return PersTestMocksHolder.registerMock(StaffClient.class);
    }

    @Bean
    public CsBillingHoldingApi holcClient() {
        return PersTestMocksHolder.registerMock(CsBillingHoldingApi.class);
    }

    @Bean
    public PersPayClient persPayClient() {
        return PersTestMocksHolder.registerMock(PersPayClient.class);
    }

    @Bean
    public StartrekService startrekService() {
        return PersTestMocksHolder.registerMock(StartrekService.class);
    }

    @Bean
    public CleanWebClient cleanWebClient() {
        return PersTestMocksHolder.registerMock(CleanWebClient.class);
    }

    @Bean
    public RestTemplate persNotifyRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class);
    }

    @Bean
    public RestTemplate mboCardRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class, PersCoreMockFactory::okRestTemplate);
    }

    @Bean
    public RestTemplate previewPublishRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class,
            PersCoreMockFactory::goodPreviewPublishRestTemplate);
    }

    @Bean
    public RestTemplate rateAndGoodsRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class);
    }

    @Bean
    public BlackBoxService blackBoxService() {
        return PersTestMocksHolder.registerMock(BlackBoxService.class);
    }

    @Bean
    public UserInfoService blackBoxUserService() {
        return PersTestMocksHolder.registerMock(UserInfoService.class, result -> {
            when(result.getUserInfo(anyLong())).thenReturn(generateUserInfo());
            when(result.getUserInfo(anyString())).thenReturn(generateUserInfo());
            when(result.getUserInfo(anyLong(), anyVararg())).thenReturn(generateUserInfo());
            when(result.getUserInfo(anyString(), anyVararg())).thenReturn(generateUserInfo());
        });
    }

    private long generateUid() {
        return Math.abs(RND.nextLong());
    }

    private UserInfo generateUserInfo() {
        final long uid = generateUid();
        final BlackBoxUserInfo boxUserInfo = new BlackBoxUserInfo(uid);
        boxUserInfo.addField(UserInfoField.LOGIN, "login" + uid);
        return boxUserInfo;
    }

    @Bean
    public ru.yandex.market.sdk.userinfo.service.UserInfoService userInfoService() {
        return PersTestMocksHolder.registerMock(ru.yandex.market.sdk.userinfo.service.UserInfoService.class, result ->
            when(result.getUserInfoRaw(anyCollectionOf(Long.class), any(Options.class)))
                .thenReturn(Result.ofValue(Collections.singletonList(new AggregateUserInfo(new SberlogInfo())))));
    }

    @Bean
    public PersNotifyClient persNotifyClient() {
        return PersTestMocksHolder.registerMock(PersNotifyClient.class, result -> {
            try {
                Email activeEmail = new Email("active@email.ru", true);
                Email anotherEmail = new Email("not_active@email.ru", false);
                when(result.getEmails(anyLong())).thenReturn(ImmutableSet.of(activeEmail, anotherEmail));
            } catch (PersNotifyClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Bean
    public ShopInfoService shopInfoExternalService() {
        return PersTestMocksHolder.registerMock(ShopInfoService.class, result -> {
            when(result.getShopName(anyLong())).thenReturn(Optional.of("www.774.com"));
            when(result.getShopNames(anyVararg())).thenAnswer(new Answer<List<String>>() {
                @Override
                public List<String> answer(InvocationOnMock invocation) throws Throwable {
                    int cnt = ((long[]) invocation.getArguments()[0]).length;
                    return Stream.generate(() -> "www.774.com")
                        .limit(cnt)
                        .collect(Collectors.toList());
                }
            });
        });
    }

    @Bean
    public CatalogerClient catalogerClient() {
        return PersTestMocksHolder.registerMock(CatalogerClient.class, result -> {
            when(result.getNavigationTreeFromDepartment()).thenReturn(generateGetNavigationTreeFromDepartment());
            when(result.getCatalogerVersion()).thenReturn(generateGetCatalogerVersion());
        });
    }

    private Optional<VersionInfoWrapper> generateGetCatalogerVersion() {
        try {
            String content = StreamUtils.copyToString(getClass().getClassLoader().getResourceAsStream(
                "data/cataloger_stat.json"), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
            return Optional.of(objectMapper.readValue(content, VersionInfoWrapper.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<CatalogerResponseWrapper> generateGetNavigationTreeFromDepartment() {
        try {
            String content = StreamUtils.copyToString(getClass().getClassLoader().getResourceAsStream(
                "data/cataloger_navigation_tree.json"), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
            return Optional.of(objectMapper.readValue(content, CatalogerResponseWrapper.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }


    @Bean
    public ReportService report() {
        return PersTestMocksHolder.registerMock(ReportService.class, result -> {
            when(result.getModelById(anyLong())).thenReturn(Optional.of(generateModel()));
            when(result.getModelsForCategoryIdOrderedByPrice(anyLong()))
                .thenReturn(Arrays.asList(generateModel(), generateModel()));
        });
    }

    @Bean
    public Supplier<ExecutorService> threadPoolSupplier() {
        return MoreExecutors::newDirectExecutorService;
    }

    @Bean
    public Supplier<ExecutorService> threadPoolSupplierCached() {
        return MoreExecutors::newDirectExecutorService;
    }

    @Bean
    public Supplier<ExecutorService> previewCreatorExecutorService() {
        return MoreExecutors::newDirectExecutorService;
    }

}
