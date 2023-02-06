package ru.yandex.direct.jobs.urlmonitoring;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.urlmonitoring.service.UrlMonitoringService;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.solomon.SolomonPushClient;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.URL_MONITORING_ON_LB_AND_YT_ENABLED;

@JobsTest
@ExtendWith(SpringExtension.class)
class ImportUrlMonitoringStatesFromYtJobTest {

    private static final String INVALID_DOMAIN =
            "xn-------43dloebjba1abcedwrrphnuaac5dva5b0a5f4bxs.xn-----6kcabbab0bf5acbvlegzdtom6ajegiilq0hf3qtdm.xn--p1ai";
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    DomainService domainService;
    @Autowired
    DomainRepository domainRepository;
    @Mock
    private UrlMonitoringService urlMonitoringService;
    @Mock
    private SolomonPushClient solomonPushClient;

    private ImportUrlMonitoringStatesFromYtJob importUrlMonitoringStatesFromYtJob;

    @BeforeEach
    void setUp() {
        initMocks(this);
        ppcPropertiesSupport.set(URL_MONITORING_ON_LB_AND_YT_ENABLED.getName(), Boolean.toString(true));
        importUrlMonitoringStatesFromYtJob = new ImportUrlMonitoringStatesFromYtJob(ppcPropertiesSupport, domainService,
                urlMonitoringService, solomonPushClient);
        when(urlMonitoringService.importDeadDomains())
                .thenReturn(Arrays.asList(
                        "https://AbCdE.fG",
                        "http://антикортехснаб-дв.рф",
                        "http://ABC.xn--90afbtqidn8gf.xn--p1ai",
                        "https://" + INVALID_DOMAIN));
    }

    @Test
    void execute() {
        importUrlMonitoringStatesFromYtJob.execute();
        List<String> expectedDomains = Arrays.asList("AbCdE.fG", "антикортехснаб-дв.рф", "ABC.xn--90afbtqidn8gf.xn--p1ai",
                "ABC.добрыйдомъ.рф", "xn----7sbaegldxs2adk0apje6c.xn--p1ai");
        Map<String, Long> domainsToIdsFromPpcDict = domainRepository
                .getDomainsToIdsFromPpcDict(StreamEx.of(expectedDomains).append(INVALID_DOMAIN).toList());
        assertThat(domainsToIdsFromPpcDict.keySet(), hasItems(expectedDomains.toArray(new String[]{})));
        assertEquals(domainsToIdsFromPpcDict.size(), expectedDomains.size());
    }
}
