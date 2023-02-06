package ru.yandex.market.mbi.partner.registration;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.db.DbUnitTruncatePolicy;
import ru.yandex.market.common.test.db.TruncateType;
import ru.yandex.market.mbi.partner.registration.config.PartnerRegistrationApplicationConfig;
import ru.yandex.market.mbi.partner.registration.placement.type.yt.AvailablePlacementTypesYtDao;
import ru.yandex.market.mbi.partner.registration.services.MbiBpmnRetrofitService;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.BusinessRegistrationApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.PartnerRegistrationApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.PartnerRegistrationProcessApiClient;
import ru.yandex.mj.generated.client.mbi_partner_registration.api.PlacementRecommendationApiClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {
                ru.yandex.market.javaframework.main.config.SpringApplicationConfig.class,
                PartnerRegistrationApplicationConfig.class,
                FunctionalTestConfig.class,
        }
)
@ActiveProfiles(profiles = {"functionalTest"})
@TestPropertySource({"classpath:functional-test.properties", "classpath:postgres_test.properties"})
@DbUnitTruncatePolicy(schema = "public", truncateType = TruncateType.NOT_TRUNCATE)
@TestExecutionListeners(
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
        listeners = DbUnitTestExecutionListener.class
)
@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractFunctionalTest {

    @Autowired
    protected PartnerRegistrationApiClient partnerRegistrationApiClient;

    @Autowired
    protected BusinessRegistrationApiClient businessRegistrationApiClient;

    @Autowired
    protected PartnerRegistrationProcessApiClient partnerRegistrationProcessApiClient;

    @Autowired
    protected PlacementRecommendationApiClient placementRecommendationApiClient;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ProcessEngine processEngine;

    @MockBean
    protected AvailablePlacementTypesYtDao availablePlacementTypesYtDao;

    @Autowired
    protected MbiBpmnRetrofitService mbiBpmnRetrofitService;

}
