package ru.yandex.direct.api.v5.entity.dictionaries;

import org.mockito.Answers;
import org.springframework.context.ApplicationContext;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesRepository;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Locale.ENGLISH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DictionariesServiceBuilder {

    private final ApplicationContext context;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private CryptaSegmentRepository cryptaSegmentRepository;
    private TargetingCategoriesRepository targetingCategoriesRepository;
    private GeoTimezoneRepository geoTimezoneRepository;

    private ApiAuthenticationSource apiAuthenticationSource;
    private ApiContextHolder apiContextHolder;
    private final ApiUnitsService apiUnitsService;
    private final AccelInfoHeaderSetter accelInfoHeaderSetter;
    private TranslationService translationService;


    DictionariesServiceBuilder(ApplicationContext context) {
        this.context = context;

        this.ppcPropertiesSupport = context.getBean(PpcPropertiesSupport.class);
        this.cryptaSegmentRepository = context.getBean(CryptaSegmentRepository.class);
        this.targetingCategoriesRepository = context.getBean(TargetingCategoriesRepository.class);
        this.geoTimezoneRepository = context.getBean(GeoTimezoneRepository.class);

        this.apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        this.apiContextHolder = mock(ApiContextHolder.class, Answers.RETURNS_DEEP_STUBS);
        this.apiUnitsService = mock(ApiUnitsService.class);
        this.accelInfoHeaderSetter = mock(AccelInfoHeaderSetter.class);
        this.translationService = mock(TranslationService.class);
        when(translationService.getLocale()).thenReturn(ENGLISH);
    }

    public DictionariesServiceBuilder withPpcPropertiesSupport(PpcPropertiesSupport ppcPropertiesSupport) {
        this.ppcPropertiesSupport = ppcPropertiesSupport;
        return this;
    }

    public DictionariesServiceBuilder withCryptaSegmentRepository(CryptaSegmentRepository cryptaSegmentRepository) {
        this.cryptaSegmentRepository = cryptaSegmentRepository;
        return this;
    }

    public DictionariesServiceBuilder withClientAuth(ClientInfo clientInfo) {
        when(apiAuthenticationSource.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        return this;
    }

    public DictionariesServiceBuilder withApiContextHolder(ApiContextHolder apiContextHolder) {
        this.apiContextHolder = apiContextHolder;
        return this;
    }

    public DictionariesServiceBuilder withTargetingCategoriesRepository(TargetingCategoriesRepository targetingCategoriesRepository) {
        this.targetingCategoriesRepository = targetingCategoriesRepository;
        return this;
    }

    public DictionariesServiceBuilder withGeoTimezoneRepository(GeoTimezoneRepository geoTimezoneRepository) {
        this.geoTimezoneRepository = geoTimezoneRepository;
        return this;
    }

    public DictionariesService build() {
        return new DictionariesService(
                apiUnitsService,
                accelInfoHeaderSetter,
                context.getBean(TranslationService.class),
                context.getBean(GeoTreeFactory.class),
                geoTimezoneRepository,
                context.getBean(SspPlatformsRepository.class),
                targetingCategoriesRepository,
                cryptaSegmentRepository,
                ppcPropertiesSupport,
                apiContextHolder,
                apiAuthenticationSource,
                context.getBean(FeatureService.class),
                context.getBean(BannerConstantsService.class),
                context.getBean(PerformanceFilterStorage.class));
    }
}
