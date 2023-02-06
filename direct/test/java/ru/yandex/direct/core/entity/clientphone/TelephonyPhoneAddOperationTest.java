package ru.yandex.direct.core.entity.clientphone;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestOrganizations;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestClientPhoneRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.metrika.client.model.response.TurnOnCallTrackingResponse;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService.MSK_495_REGION_CODE;
import static ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService.MSK_499_REGION_CODE;
import static ru.yandex.direct.core.entity.clientphone.validation.ClientPhoneDefects.phoneCountryNotAllowed;
import static ru.yandex.direct.telephony.client.ProtobufMapper.CLIENT_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.COUNTER_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.ORG_ID_META_KEY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TelephonyPhoneAddOperationTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    private TestClientPhoneRepository testClientPhoneRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationsClientStub organizationsClientStub;

    @Mock
    private TelephonyClient telephonyClient;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private MetrikaClient metrikaClient;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private BannerCommonRepository newBannerCommonRepository;

    private TelephonyPhoneService telephonyPhoneService;

    private ClientId clientId;
    private Long uid;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();

        testClientPhoneRepository.delete(clientInfo.getShard(), clientId);

        String number = getUniqPhone();
        var serviceNumber = ServiceNumber.newBuilder().setNum(number).build();
        when(telephonyClient.getServiceNumber()).thenReturn(serviceNumber);
        when(telephonyClient.getServiceNumber(not(eq(MSK_495_REGION_CODE)))).thenReturn(serviceNumber);
        when(telephonyClient.tryToGetServiceNumber(any())).thenCallRealMethod();
        when(metrikaClient.turnOnCallTracking(anyLong()))
                .thenReturn(new TurnOnCallTrackingResponse()
                        .withGoal(new CounterGoal()
                                .withId(1)
                                .withType(CounterGoal.Type.CALL)
                                .withName("Звонок")));

        telephonyPhoneService = new TelephonyPhoneService(telephonyClient, clientPhoneRepository, featureService,
                shardHelper, newBannerCommonRepository, metrikaClient, null, null);
    }

    @Test
    public void prepareAndApply_success() {

        String telephonyNum = getUniqPhone();
        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();

        when(telephonyClient.getServiceNumber()).thenReturn(
                ServiceNumber.newBuilder()
                        .setNum(telephonyNum)
                        .setVersion(1)
                        .build()
        );

        Map<String, String> meta = Map.of(
                ORG_ID_META_KEY, permalinkId.toString(),
                CLIENT_ID_META_KEY, clientId.toString(),
                COUNTER_ID_META_KEY, counterId.toString());
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(
                List.of(
                        ServiceNumber.newBuilder()
                                .setNum(telephonyNum)
                                .setVersion(1)
                                .putAllMeta(meta)
                                .build()
                )
        );

        organizationsClientStub.addUidsAndCounterIdsByPermalinkId(permalinkId, List.of(uid), counterId);

        TelephonyPhoneAddOperation operation = createOperation(clientId, List.of(getClientPhone(permalinkId)));

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        List<ClientPhone> clientPhones = clientPhoneRepository.getByClientId(clientId);

        assertThat(clientPhones).isNotEmpty();
        ClientPhone clientPhone = clientPhones.get(0);
        assertThat(clientPhone.getPhoneType()).isEqualTo(ClientPhoneType.TELEPHONY);
        assertThat(clientPhone.getTelephonyPhone().getPhone()).isEqualTo(telephonyNum);
        assertThat(clientPhone.getPermalinkId()).isEqualTo(permalinkId);
        assertThat(clientPhone.getCounterId()).isEqualTo(counterId);
    }

    @Test
    public void prepareAndApply_skipOrgWithWrongPhone() {
        String telephonyNum = getUniqPhone();
        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();

        when(telephonyClient.getServiceNumber()).thenReturn(
                ServiceNumber.newBuilder()
                        .setVersion(1)
                        .setNum(telephonyNum)
                        .build()
        );

        organizationsClientStub.addUidsAndCounterIdsByPermalinkId(permalinkId, List.of(uid), counterId);
        String foreignCountryPhone = "+77003332211";
        organizationsClientStub.changeCompanyPhones(permalinkId, singletonList(foreignCountryPhone));

        TelephonyPhoneAddOperation operation = createOperation(clientId, List.of(getClientPhone(permalinkId)));

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(ClientPhone.PHONE_NUMBER)), phoneCountryNotAllowed()
                ))
        ));
    }

    @Test
    public void prepareAndApply_cantGetTelephony() {
        when(telephonyClient.getServiceNumber()).thenReturn(null);

        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();

        TelephonyPhoneAddOperation operation = createOperation(clientId, List.of(getClientPhone(permalinkId)));

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getErrorCount()).isNotZero();
    }

    @Test
    public void prepareAndApply_notMskOrgPhone_successful() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.ABC_NUMBER_TELEPHONY_ALLOWED, true);
        var permalink = TestOrganizations.defaultOrganization(clientId).getPermalinkId();
        // Телефон организации не московский
        var orgPhone = "+79121234567";
        organizationsClientStub.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(uid),
                List.of(orgPhone)
        );

        createOperation(clientId, List.of(getClientPhone(permalink))).prepareAndApply();
        verify(telephonyClient, times(1)).getServiceNumber();
    }

    @Test
    public void prepareAndApply_get499Telephony_successful() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.ABC_NUMBER_TELEPHONY_ALLOWED, true);
        var permalink = TestOrganizations.defaultOrganization(clientId).getPermalinkId();
        // Телефон организации московский
        var orgPhone = "+74991234567";
        organizationsClientStub.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(uid),
                List.of(orgPhone)
        );

        createOperation(clientId, List.of(getClientPhone(permalink))).prepareAndApply();
        verify(telephonyClient, times(1)).getServiceNumber(eq(MSK_499_REGION_CODE));
        verify(telephonyClient, times(0)).getServiceNumber();
    }

    @Test
    public void prepareAndApply_get495TelephonyButOnlyDefExists_successful() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.ABC_NUMBER_TELEPHONY_ALLOWED, true);
        var permalink = TestOrganizations.defaultOrganization(clientId).getPermalinkId();
        // Телефон организации московский
        var orgPhone = "+74951234567";
        organizationsClientStub.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(uid),
                List.of(orgPhone)
        );

        createOperation(clientId, List.of(getClientPhone(permalink))).prepareAndApply();
        verify(telephonyClient, times(1)).getServiceNumber(eq(MSK_495_REGION_CODE));
        verify(telephonyClient, times(1)).getServiceNumber();
    }

    private ClientPhone getClientPhone(Long permalinkId) {
        return new ClientPhone()
                .withClientId(clientId)
                .withPermalinkId(permalinkId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withIsDeleted(false);
    }

    private TelephonyPhoneAddOperation createOperation(ClientId clientId, List<ClientPhone> clientPhones) {
        return new TelephonyPhoneAddOperation(
                clientId,
                clientPhones,
                clientPhoneRepository,
                organizationService,
                telephonyPhoneService
        );
    }

}
