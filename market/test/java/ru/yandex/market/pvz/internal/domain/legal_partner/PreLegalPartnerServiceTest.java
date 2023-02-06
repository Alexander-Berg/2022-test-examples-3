package ru.yandex.market.pvz.internal.domain.legal_partner;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.NotificationContactDTO;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationRequest;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.legal_partner.dto.PreLegalPartnerDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.util.logging.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.CABINET_CREATION_ERROR_MESSAGE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.CONTACT_HAS_BUSINESS_PARTNERS_ERROR_CODE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.CONTACT_HAS_BUSINESS_PARTNERS_ERROR_MSG;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.EMPTY_LOGIN_ERROR_CODE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.EMPTY_LOGIN_ERROR_MESSAGE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.TOO_MANY_CAMPAIGNS_ERROR_CODE;
import static ru.yandex.market.mbi.api.client.RestMbiApiClient.TOO_MANY_CAMPAIGNS_ERROR_MSG;
import static ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration.TRACER_UID;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.generateRandomINN;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_CLIENT_AREA;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_COLLABORATION_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_FLOOR;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_SEPARATE_ENTRANCE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_STREET_ENTRANCE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_WINDOWS;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_OGRN;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_ORGANISATION_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_ADDRESS;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_CEILING_HEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_COMMENT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_COUNT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LON;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_PHOTO_URL;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_REGION;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_SQUARE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_POLYGON_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_WANT_BRAND;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_WAREHOUSE_AREA;
import static ru.yandex.market.pvz.internal.domain.legal_partner.PreLegalPartnerService.NON_UNIQUE_CLIENT_MESSAGE;
import static ru.yandex.market.pvz.internal.domain.legal_partner.PreLegalPartnerService.NON_UNIQUE_INN_MESSAGE;
import static ru.yandex.market.pvz.internal.domain.legal_partner.PreLegalPartnerService.NON_UNIQUE_UID_MESSAGE;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PreLegalPartnerServiceTest {

    public static final String MBI_CABINET_CREATE_ERROR_RESPONSE =
            "<validation-error>" +
            "   <message>" +
            "       Can't add contact link for contact with id 103, there are partners with " +
            "       different business type on it" +
            "   </message>" +
            "   <codes>" +
            "      <code>%s</code>" +
            "   </codes>" +
            "</validation-error>";

    public static final String MBI_CABINET_CREATE_SUCCESS_RESPONSE =
            "<register-shop-and-campaign-response>" +
            "   <datasource-id>1</datasource-id>" +
            "   <campaign-id>2</campaign-id>" +
            "   <manager-id>3</manager-id>" +
            "   <owner-id>4</owner-id>" +
            "   <client-id>5</client-id>" +
            "</register-shop-and-campaign-response>";

    @MockBean
    private MbiApiClient mbiApiClient;

    @MockBean
    private RestOperations restTemplate;

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final PreLegalPartnerQueryService preLegalPartnerQueryService;

    private final PreLegalPartnerService preLegalPartnerService;

    private MbiApiClient mbiApiClient2;

    @BeforeEach
    void init() {
        mbiApiClient2 = new RestMbiApiClient(restTemplate, "");
    }

    @Test
    void createNewPreLegalPartner() {
        long partnerId = 2000L;
        var mbiResponse = buildMbiResponse(partnerId, 4000L, 10000L);
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(mbiResponse);

        String taxpayerNumber = generateRandomINN();
        var requestDto = PreLegalPartnerDto.builder()
                .delegateName(DEFAULT_DELEGATE_NAME)
                .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                .delegatePhone(DEFAULT_DELEGATE_PHONE)
                .legalType(DEFAULT_LEGAL_TYPE)
                .legalForm(DEFAULT_LEGAL_FORM)
                .organisationName(DEFAULT_ORGANISATION_NAME)
                .taxpayerNumber(taxpayerNumber)
                .ogrn(DEFAULT_OGRN)
                .collaborationForm(DEFAULT_COLLABORATION_FORM)
                .wantBrand(DEFAULT_WANT_BRAND)
                .pickupPointCount(DEFAULT_PICKUP_POINT_COUNT)
                .pickupPointRegion(DEFAULT_PICKUP_POINT_REGION)
                .pickupPointLocality(DEFAULT_PICKUP_POINT_LOCALITY)
                .pickupPointAddress(DEFAULT_PICKUP_POINT_ADDRESS)
                .pickupPointLatitude(DEFAULT_PICKUP_POINT_LAT)
                .pickupPointLongitude(DEFAULT_PICKUP_POINT_LON)
                .pickupPointSquare(DEFAULT_PICKUP_POINT_SQUARE)
                .pickupPointCeilingHeight(DEFAULT_PICKUP_POINT_CEILING_HEIGHT)
                .pickupPointPhotoUrl(DEFAULT_PICKUP_POINT_PHOTO_URL)
                .pickupPointComment(DEFAULT_PICKUP_POINT_COMMENT)
                .pickupPointFloor(DEFAULT_FLOOR)
                .pickupPointPolygonId(DEFAULT_POLYGON_ID)
                .clientArea(DEFAULT_CLIENT_AREA)
                .warehouseArea(DEFAULT_WAREHOUSE_AREA)
                .hasStreetEntrance(DEFAULT_HAS_STREET_ENTRANCE)
                .hasSeparateEntrance(DEFAULT_HAS_SEPARATE_ENTRANCE)
                .hasWindows(DEFAULT_HAS_WINDOWS)
                .build();
        var actual = preLegalPartnerService.create(requestDto);

        var created = preLegalPartnerQueryService.getByPartnerId(partnerId);

        assertThat(created.getId()).isNotNull();
        var expected = buildResponseDto(created.getId(), partnerId, taxpayerNumber, true);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createNewPreLegalPartnerWithoutPickupPoint() {
        long partnerId = 2000L;
        var mbiResponse = buildMbiResponse(partnerId, 4000L, 10000L);
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(mbiResponse);

        String taxpayerNumber = generateRandomINN();
        var requestDto = PreLegalPartnerDto.builder()
                .delegateName(DEFAULT_DELEGATE_NAME)
                .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                .delegatePhone(DEFAULT_DELEGATE_PHONE)
                .legalType(DEFAULT_LEGAL_TYPE)
                .legalForm(DEFAULT_LEGAL_FORM)
                .organisationName(DEFAULT_ORGANISATION_NAME)
                .taxpayerNumber(taxpayerNumber)
                .ogrn(DEFAULT_OGRN)
                .collaborationForm(DEFAULT_COLLABORATION_FORM)
                .wantBrand(DEFAULT_WANT_BRAND)
                .build();
        var actual = preLegalPartnerService.create(requestDto);

        var created = preLegalPartnerQueryService.getByPartnerId(partnerId);

        assertThat(created.getId()).isNotNull();
        var expected = buildResponseDto(created.getId(), partnerId, taxpayerNumber, false);

        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource
    void testCreatePartnerWithAnError(String errorCode, String errorMessage) {
        var requestDto = mockRestTemplateAndBuildPrePartner(
                HttpStatus.BAD_REQUEST,
                String.format(MBI_CABINET_CREATE_ERROR_RESPONSE, errorCode)
        );
        SimpleShopRegistrationRequest request = buildSimpleShopRequest(requestDto);

        assertThatThrownBy(() -> mbiApiClient2.simpleRegisterShop(1, 1, request))
                .isExactlyInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining(errorMessage);
    }

    private static Stream<Arguments> testCreatePartnerWithAnError() {
        return Stream.of(
                Arguments.of(TOO_MANY_CAMPAIGNS_ERROR_CODE, TOO_MANY_CAMPAIGNS_ERROR_MSG),
                Arguments.of(CONTACT_HAS_BUSINESS_PARTNERS_ERROR_CODE, CONTACT_HAS_BUSINESS_PARTNERS_ERROR_MSG),
                Arguments.of(EMPTY_LOGIN_ERROR_CODE, EMPTY_LOGIN_ERROR_MESSAGE),
                Arguments.of(null, CABINET_CREATION_ERROR_MESSAGE)
        );
    }

    private PreLegalPartnerParams mockRestTemplateAndBuildPrePartner(HttpStatus status, String requestBody) {
        Mockito.doReturn(
                ResponseEntity
                        .status(status)
                        .contentType(MediaType.TEXT_XML)
                        .body(requestBody)
        ).when(restTemplate).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));

        String taxpayerNumber = generateRandomINN();
        return PreLegalPartnerParams.builder()
                .delegateName(DEFAULT_DELEGATE_NAME)
                .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                .delegatePhone(DEFAULT_DELEGATE_PHONE)
                .legalType(DEFAULT_LEGAL_TYPE)
                .legalForm(DEFAULT_LEGAL_FORM)
                .organisationName(DEFAULT_ORGANISATION_NAME)
                .taxpayerNumber(taxpayerNumber)
                .ogrn(DEFAULT_OGRN)
                .collaborationForm(DEFAULT_COLLABORATION_FORM)
                .wantBrand(DEFAULT_WANT_BRAND)
                .build();
    }

    private SimpleShopRegistrationRequest buildSimpleShopRequest(PreLegalPartnerParams dto) {
        SimpleShopRegistrationRequest request = new SimpleShopRegistrationRequest();
        request.setShopName(DEFAULT_DELEGATE_NAME);
        request.setNotificationContact(buildNotificationContact(dto));
        request.setCampaignType(CampaignType.TPL_PARTNER);
        return request;
    }

    private NotificationContactDTO buildNotificationContact(PreLegalPartnerParams params) {
        var notificationContact = new NotificationContactDTO();
        String[] nameParts = params.getDelegateName().split("\\s+");
        String lastName = nameParts.length > 0 ? nameParts[0] : "Партнерский";
        String firstName = nameParts.length > 1 ? nameParts[1] : "Пвз";

        notificationContact.setFirstName(firstName);
        notificationContact.setLastName(lastName);
        notificationContact.setEmail(params.getDelegateEmail());
        notificationContact.setPhone(params.getDelegatePhone());
        return notificationContact;
    }

    @Test
    void tryToCreatePartnerThenSuccess() {
        var requestDto = mockRestTemplateAndBuildPrePartner(
                HttpStatus.OK, MBI_CABINET_CREATE_SUCCESS_RESPONSE
        );
        SimpleShopRegistrationRequest request = buildSimpleShopRequest(requestDto);

        SimpleShopRegistrationResponse result = mbiApiClient2.simpleRegisterShop(1, 1, request);

        assertThat(result.getDatasourceId()).isEqualTo(1);
        assertThat(result.getCampaignId()).isEqualTo(2);
        assertThat(result.getManagerId()).isEqualTo(3);
        assertThat(result.getOwnerId()).isEqualTo(4);
        assertThat(result.getClientId()).isEqualTo(5);
    }

    @Test
    void tryToCreatePreLegalPartnerWithoutTracerUid() {
        Tracer.resetContext();

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplIllegalArgumentException.class)
                .hasMessage("UID is not provided");

        Tracer.global().put(TRACER_UID, String.valueOf(DEFAULT_UID));
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentUidInPreLegalPartner() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        long uid = preLegalPartner.getOwnerUid();
        Tracer.global().put(TRACER_UID, String.valueOf(uid));

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_UID_MESSAGE);

        Tracer.global().put(TRACER_UID, DEFAULT_UID);
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentUidInLegalPartner() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        long uid = legalPartner.getOwnerUid();
        Tracer.global().put(TRACER_UID, String.valueOf(uid));

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_UID_MESSAGE);

        Tracer.global().put(TRACER_UID, DEFAULT_UID);
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentINNInPreLegalPartner() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        String taxpayerNumber = preLegalPartner.getTaxpayerNumber();

        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_INN_MESSAGE);
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentINNInLegalPartner() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        String taxpayerNumber = legalPartner.getOrganization().getTaxpayerNumber();

        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_INN_MESSAGE);
    }

    @Test
    void tryToCreatePreLegalPartnerWithMbiInternalError() {
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).
                thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Error",
                        HttpHeaders.EMPTY, new byte[0], Charset.defaultCharset()));

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplExternalException.class)
                .hasMessage(CABINET_CREATION_ERROR_MESSAGE);
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentClientInPreLegalPartner() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        long partnerId = 2000L;
        long clientId = preLegalPartner.getBalanceClientId();
        var mbiResponse = buildMbiResponse(partnerId, clientId, 10000L);
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(mbiResponse);

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_CLIENT_MESSAGE);
    }

    @Test
    void tryToCreatePreLegalPartnerWithExistentClientInLegalPartner() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        long partnerId = 2000L;
        long clientId = legalPartner.getBalanceClientId();
        var mbiResponse = buildMbiResponse(partnerId, clientId, 10000L);
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(mbiResponse);

        String taxpayerNumber = generateRandomINN();
        var requestDto = buildRequestDto(taxpayerNumber);
        assertThatThrownBy(() -> preLegalPartnerService.create(requestDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage(NON_UNIQUE_CLIENT_MESSAGE);
    }

    @Test
    void getPreLegalPartner() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        var actual = preLegalPartnerService.get(preLegalPartner.getPartnerId());

        assertThat(preLegalPartner.getId()).isNotNull();
        var expected = buildResponseDto(
                preLegalPartner.getId(), preLegalPartner.getPartnerId(), preLegalPartner.getTaxpayerNumber(), true);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void preLegalPartnerNotFound() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        assertThatThrownBy(() -> preLegalPartnerService.get(preLegalPartner.getPartnerId() + 1))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    private PreLegalPartnerDto buildRequestDto(String taxpayerNumber) {
        return defaultPreLegalPartnerDto(true)
                .taxpayerNumber(taxpayerNumber)
                .build();
    }

    private PreLegalPartnerDto.PreLegalPartnerDtoBuilder defaultPreLegalPartnerDto(boolean withPickupPoint) {
        var builder = PreLegalPartnerDto.builder()
                .delegateName(DEFAULT_DELEGATE_NAME)
                .delegateEmail(DEFAULT_DELEGATE_EMAIL)
                .delegatePhone(DEFAULT_DELEGATE_PHONE)
                .legalType(DEFAULT_LEGAL_TYPE)
                .legalForm(DEFAULT_LEGAL_FORM)
                .organisationName(DEFAULT_ORGANISATION_NAME)
                .ogrn(DEFAULT_OGRN)
                .collaborationForm(DEFAULT_COLLABORATION_FORM)
                .wantBrand(DEFAULT_WANT_BRAND)
                .approveStatus(PreLegalPartnerApproveStatus.CHECKING)
                .pickupPointCount(0);

        if (withPickupPoint) {
            builder.pickupPointCount(DEFAULT_PICKUP_POINT_COUNT)
                    .pickupPointRegion(DEFAULT_PICKUP_POINT_REGION)
                    .pickupPointLocality(DEFAULT_PICKUP_POINT_LOCALITY)
                    .pickupPointAddress(DEFAULT_PICKUP_POINT_ADDRESS)
                    .pickupPointLatitude(DEFAULT_PICKUP_POINT_LAT)
                    .pickupPointLongitude(DEFAULT_PICKUP_POINT_LON)
                    .pickupPointSquare(DEFAULT_PICKUP_POINT_SQUARE)
                    .pickupPointCeilingHeight(DEFAULT_PICKUP_POINT_CEILING_HEIGHT)
                    .pickupPointPhotoUrl(DEFAULT_PICKUP_POINT_PHOTO_URL)
                    .pickupPointComment(DEFAULT_PICKUP_POINT_COMMENT)
                    .pickupPointFloor(DEFAULT_FLOOR)
                    .pickupPointPolygonId(DEFAULT_POLYGON_ID)
                    .clientArea(DEFAULT_CLIENT_AREA)
                    .warehouseArea(DEFAULT_WAREHOUSE_AREA)
                    .hasStreetEntrance(DEFAULT_HAS_STREET_ENTRANCE)
                    .hasSeparateEntrance(DEFAULT_HAS_SEPARATE_ENTRANCE)
                    .hasWindows(DEFAULT_HAS_WINDOWS);
        }

        return builder;
    }

    private PreLegalPartnerDto buildResponseDto(
            long id, long partnerId, String taxpayerNumber, boolean withPickupPoint) {
        return defaultPreLegalPartnerDto(withPickupPoint)
                .id(id)
                .partnerId(partnerId)
                .taxpayerNumber(taxpayerNumber)
                .build();
    }

    private SimpleShopRegistrationResponse buildMbiResponse(long partnerId, long clientId, long uid) {
        var mbiResponse = new SimpleShopRegistrationResponse();
        mbiResponse.setCampaignId(partnerId);
        mbiResponse.setDatasourceId(DEFAULT_DATASOURCE_ID);
        mbiResponse.setClientId(clientId);
        mbiResponse.setOwnerId(uid);
        return mbiResponse;
    }
}
