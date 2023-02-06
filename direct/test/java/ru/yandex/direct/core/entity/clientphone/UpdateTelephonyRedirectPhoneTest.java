package ru.yandex.direct.core.entity.clientphone;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.telephony.client.model.TelephonyPhoneRequest;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.core.entity.clientphone.validation.ClientPhoneDefects.phoneCountryNotAllowed;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.fillTextDefaultSystemFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class UpdateTelephonyRedirectPhoneTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private ClientPhoneLocker locker;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private TelephonyPhoneService telephonyPhoneService;

    private int shard;
    private ClientId clientId;
    private Long bannerId;

    private ClientPhoneService clientPhoneService;
    private TelephonyClient telephonyClient;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        bannerId = createBanner(clientInfo);

        telephonyClient = mock(TelephonyClient.class);
        clientPhoneService = new ClientPhoneService(shardHelper, clientPhoneRepository, telephonyClient,
                bannerCommonRepository, null, null, null, null, null, null, locker,
                telephonyPhoneService, null);
    }

    private Long createBanner(ClientInfo clientInfo) {
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var banner = activeTextBanner(null, null).withStatusBsSynced(StatusBsSynced.YES);
        fillTextDefaultSystemFields(banner);
        var bannerInfo = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo().withBanner(banner).withAdGroupInfo(adGroupInfo)
        );
        return bannerInfo.getBannerId();
    }

    public static Object[][] parametersForUpdateTelephonyRedirectPhone_success() {
        return new Object[][]{
                {"ABC-номер", "+74951112233"},
                {"DEF-номер", "+79121112233"},
                {"бесплатный номер", "+78001112233"},
        };
    }

    @Test
    @Parameters(method = "parametersForUpdateTelephonyRedirectPhone_success")
    @TestCaseName("{0}")
    public void updateTelephonyRedirectPhone_success(
            @SuppressWarnings("unused") String testName,
            String newRedirectPhone
    ) {
        String telephonyPhone = getUniqPhone();
        String telephonyServiceId = RandomStringUtils.randomAlphabetic(15);
        String oldRedirectPhone = getUniqPhone();
        Long metrikaCounter = RandomUtils.nextLong();
        Long permalinkId = RandomUtils.nextLong();

        ClientPhone phone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withCounterId(metrikaCounter)
                .withPermalinkId(permalinkId)
                .withTelephonyPhone(new PhoneNumber().withPhone(telephonyPhone))
                .withPhoneNumber(new PhoneNumber().withPhone(oldRedirectPhone))
                .withTelephonyServiceId(telephonyServiceId)
                .withComment("")
                .withIsDeleted(false);

        Long phoneId = clientPhoneRepository.add(clientId, List.of(phone)).get(0);

        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerId, phoneId);

        ClientPhone phoneToUpdate = new ClientPhone()
                .withId(phoneId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(newRedirectPhone))
                .withIsDeleted(false);

        Result<Long> result = clientPhoneService.updateTelephonyRedirectPhone(phoneToUpdate);

        assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoErrors()));

        var updatedBanner = bannerService.getBannersByIds(List.of(bannerId)).get(0);
        assertThat(updatedBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);

        verify(telephonyClient, times(0)).unlinkServiceNumber(telephonyServiceId, true);
        verify(telephonyClient, times(1)).linkServiceNumber(clientId.asLong(),
                new TelephonyPhoneRequest()
                        .withPermalinkId(permalinkId)
                        .withCounterId(metrikaCounter)
                        .withTelephonyServiceId(telephonyServiceId)
                        .withRedirectPhone(newRedirectPhone)
        );

        ClientPhone phoneFromDb = clientPhoneRepository.getByPhoneIds(clientId, List.of(phoneId)).get(0);

        ClientPhone updatedTelephonyPhone = phone.withPhoneNumber(new PhoneNumber().withPhone(newRedirectPhone));
        assertThat(phoneFromDb).isEqualToIgnoringGivenFields(updatedTelephonyPhone, "lastShowTime");
    }

    public static Object[][] params() {
        return new Object[][]{
                {"+723", invalidValue()}, // слишком короткий
                {"+711111111111111111", invalidValue()}, // сликом длинный
                {"", invalidValue()}, // пустой
                {"+77003332211", phoneCountryNotAllowed()}, // не русский DIRECT-120863
                {"+7a003332211", invalidValue()}, // номер с буквой (пример с ревью)
                {"79003332211", invalidValue()}, // неправильный формат (не E164)
        };
    }

    @Test
    @Parameters(method = "params")
    public void updateTelephonyRedirectPhone_invalidPhone(String newRedirectPhone, Defect<?> expectedDefect) {

        ClientPhone phone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withCounterId(RandomUtils.nextLong())
                .withPermalinkId(RandomUtils.nextLong())
                .withTelephonyPhone(new PhoneNumber().withPhone(getUniqPhone()))
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                .withTelephonyServiceId(RandomStringUtils.randomAlphabetic(15))
                .withComment("")
                .withIsDeleted(false);

        Long phoneId = clientPhoneRepository.add(clientId, List.of(phone)).get(0);

        ClientPhone phoneToUpdate = new ClientPhone()
                .withId(phoneId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(newRedirectPhone));

        Result<Long> result = clientPhoneService.updateTelephonyRedirectPhone(phoneToUpdate);
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(field(ClientPhone.PHONE_NUMBER)), expectedDefect))));
        // проверим, что нет других ошибок
        assertThat(result.getValidationResult().flattenErrors()).hasSize(1);

        assertThat(result.getSuccessfulObjectsCount()).isZero();

        verify(telephonyClient, times(0)).unlinkServiceNumber(anyString(), anyBoolean());
        verify(telephonyClient, times(0)).linkServiceNumber(anyLong(), any());

        ClientPhone phoneFromDb = clientPhoneRepository.getByPhoneIds(clientId, List.of(phoneId)).get(0);

        assertThat(phoneFromDb).isEqualToIgnoringGivenFields(phone, "lastShowTime");
    }

    @Test
    public void updateTelephonyRedirectPhone_noAction_whenTelephonyServiceNumberIdIsNull() {
        ClientPhone phone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withCounterId(RandomUtils.nextLong())
                .withPermalinkId(RandomUtils.nextLong())
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                .withTelephonyPhone(null)
                .withTelephonyServiceId(null)
                .withComment("")
                .withIsDeleted(false);

        Long phoneId = clientPhoneRepository.add(clientId, List.of(phone)).get(0);

        String newRedirectPhone = getUniqPhone();
        ClientPhone phoneToUpdate = new ClientPhone()
                .withId(phoneId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(newRedirectPhone));

        Result<Long> result = clientPhoneService.updateTelephonyRedirectPhone(phoneToUpdate);
        assertThat(result.getValidationResult())
                .is(matchedBy(Matchers.hasNoErrors()));

        verify(telephonyClient, times(0)).unlinkServiceNumber(anyString(), anyBoolean());
        verify(telephonyClient, times(0)).linkServiceNumber(anyLong(), any());

        ClientPhone phoneFromDb = clientPhoneRepository.getByPhoneIds(clientId, List.of(phoneId)).get(0);

        ClientPhone expected = phone.withPhoneNumber(new PhoneNumber().withPhone(newRedirectPhone));
        assertThat(phoneFromDb).isEqualToIgnoringGivenFields(expected, "lastShowTime");
    }
}
