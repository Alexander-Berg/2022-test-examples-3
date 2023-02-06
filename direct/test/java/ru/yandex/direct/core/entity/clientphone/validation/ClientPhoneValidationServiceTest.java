package ru.yandex.direct.core.entity.clientphone.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@ParametersAreNonnullByDefault
public class ClientPhoneValidationServiceTest {

    @Mock
    private ShardHelper shardHelper;
    @Mock
    private ClientPhoneRepository clientPhoneRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private BannerCommonRepository bannerCommonRepository;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;
    @Mock
    private CampaignOperationService campaignOperationService;

    private ClientPhoneService clientPhoneService;

    private ClientId clientId;

    @Before
    public void setUp() {
        initMocks(this);
        clientId = ClientId.fromLong(1L);
        clientPhoneService = new ClientPhoneService(shardHelper, clientPhoneRepository, null,
                bannerCommonRepository, organizationService, bannersUpdateOperationFactory,
                null, null, null, null, null, null, null);
    }

    @Test
    public void addManualClientPhone_smallPhone() {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone("+71")
                        .withExtension(9L))
                .withComment("comment");

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Path errPath = path(field(ClientPhone.PHONE_NUMBER.name()), field(PhoneNumber.PHONE.name()));

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(errPath, CollectionDefects.minStringLength(8))
        )));
    }

    @Test
    public void addManualClientPhone_tooLongPhoneWithExtension() {
        String phone = "+" + "1".repeat(13);
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone(phone)
                        .withExtension(123456L))
                .withComment("comment");

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Path errPath = path(field(ClientPhone.PHONE_NUMBER.name()));

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(errPath, PhoneValidator.StringLengthDefectIds.ENTIRE_PHONE_WITH_EXTENSION_IS_TOO_LONG)
        )));
    }

    @Test
    public void addManualClientPhone_tooLongPhone() {
        String phone = "+" + "1".repeat(15);
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone(phone))
                .withComment("comment");

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Path errPath = path(field(ClientPhone.PHONE_NUMBER.name()), field(PhoneNumber.PHONE.name()));

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(errPath, CollectionDefects.maxStringLength(14))
        )));
    }

    @Test
    public void addManualClientPhone_tooLongExtension() {
        String phone = "+" + "1".repeat(8);
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone(phone)
                        .withExtension(1234567L));

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Path errPath = path(field(ClientPhone.PHONE_NUMBER.name()), field(PhoneNumber.EXTENSION.name()));

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(errPath, NumberDefects.lessThanOrEqualTo(999999L))
        )));
    }

    @Test
    public void addManualClientPhone_tooLongComment() {
        String comment = "a".repeat(256);
        String phone = "+" + "2".repeat(8);
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone(phone))
                .withComment(comment);

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Path errPath = path(field(ClientPhone.COMMENT.name()));

        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(errPath, CollectionDefects.maxStringLength(255))
        )));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void preValidateTelephonyPhones_noError_whenNullPhoneInPhoneNumber() {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withCounterId(1L)
                .withPermalinkId(1L)
                .withPhoneNumber(new PhoneNumber().withPhone(null));

        ValidationResult<List<ClientPhone>, Defect> result =
                ClientPhoneValidationService.preValidateTelephonyPhones(List.of(clientPhone));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(ClientPhone.PHONE_NUMBER)),
                        CommonDefects.notNull()
                )
        )));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void preValidateTelephonyPhones_noError_whenNullPhoneNumber() {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withCounterId(1L)
                .withPermalinkId(1L)
                .withPhoneNumber(null);

        ValidationResult<List<ClientPhone>, Defect> result =
                ClientPhoneValidationService.preValidateTelephonyPhones(List.of(clientPhone));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(ClientPhone.PHONE_NUMBER)),
                        CommonDefects.notNull()
                )
        )));
    }
}
