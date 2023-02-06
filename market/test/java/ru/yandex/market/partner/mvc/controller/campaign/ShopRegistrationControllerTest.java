package ru.yandex.market.partner.mvc.controller.campaign;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.agency.AgencyNotRegisteredException;
import ru.yandex.market.core.balance.model.ClientNotFoundException;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.contact.ContactAlreadyLinkedException;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.ds.model.InvalidInternalPartnerNameException;
import ru.yandex.market.core.geobase.InvalidRegionException;
import ru.yandex.market.core.param.InvalidParamValueException;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.param.validator.DomainParamValidator;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.partner.campaign.PartnerReplicationService;
import ru.yandex.market.partner.campaign.ShopRegistrationService;
import ru.yandex.market.partner.campaign.exception.DuplicateInternalNameException;
import ru.yandex.market.partner.campaign.exception.DuplicateShopNameException;
import ru.yandex.market.partner.campaign.exception.NotAnOwnerException;
import ru.yandex.market.partner.campaign.exception.TooManyCampaignsException;
import ru.yandex.market.partner.campaign.exception.UnknownOwnerException;
import ru.yandex.market.partner.campaign.model.ShopRegistrationResult;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.mvc.controller.campaign.model.NotificationContact;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopRegistrationDTO;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopRegistrationResponse;
import ru.yandex.market.partner.mvc.exception.BadRequestException;
import ru.yandex.market.partner.mvc.exception.ForbiddenException;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

/**
 * Проверяет работу контроллера для успешного кейса и проверяет конвертацию бизнес-исключений во фронтовые.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@RunWith(MockitoJUnitRunner.class)
public class ShopRegistrationControllerTest {
    private static final long UID = 1;
    private static final long EUID = 2;
    private static final long DS_ID = 3;
    private static final long CN_ID = 4;
    private static final long MANAGER_ID = 5;
    private static final long AGENCY_ID = 6;
    private static final long OWNER_ID = 7;

    private ShopRegistrationController instance;

    @Mock
    private ShopRegistrationService service;

    @Mock
    private PartnerLinkService partnerLinkService;

    @Mock
    private DomainParamValidator domainParamValidator;

    @Mock
    private PartnerReplicationService partnerReplicationService;

    private ShopRegistrationDTO dto;
    private PartnerDefaultRequestHandler.PartnerHttpServRequest request;

    @Before
    public void setUp() {
        instance = new ShopRegistrationController(service, domainParamValidator,
                partnerLinkService, partnerReplicationService, null);
        dto = new ShopRegistrationDTO();
        fillShopRegistrationDTORequiredFields(dto);
        request = new MockPartnerRequest(UID, EUID, DS_ID, CN_ID);
    }

    /**
     * Тест проверяет корректную ветвь работы контроллера - контроллер должен вернуть заполненный ответ.
     */
    @Test
    public void registerOK() {
        CampaignInfo campaignInfo = new CampaignInfo(CN_ID, DS_ID, 0, 0);
        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setManagerId(MANAGER_ID);
        ShopRegistrationResult result = new ShopRegistrationResult(
                campaignInfo, datasourceInfo, AGENCY_ID, OWNER_ID, null);

        Mockito.when(service.registerShop(Mockito.any())).thenReturn(result);

        ShopRegistrationResponse resp = instance.register(request, dto);

        Assert.assertEquals(DS_ID, resp.getDatasourceId());
        Assert.assertEquals(CN_ID, resp.getCampaignId());
        Assert.assertEquals(MANAGER_ID, resp.getManagerId());
        Assert.assertEquals(AGENCY_ID, resp.getAgencyId());
        Assert.assertEquals(OWNER_ID, resp.getOwnerId());
    }

    /**
     * Тест проверяет, что, если сервис выкинет InvalidInternalShopNameException, контроллер выбросит
     * BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerInvalidShopName() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new InvalidInternalPartnerNameException(""));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет DuplicateShopNameException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerDuplicateShopDomain() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new DuplicateShopNameException(0, ""));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет DuplicateShopNameException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerDuplicateInternalName() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new DuplicateInternalNameException());

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет TooManyCampaignsException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerTooManyCampaigns() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new TooManyCampaignsException(1, 10));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет UnknownOwnerException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerUnknownOwner() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new UnknownOwnerException(""));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет NotAnOwnerException, контроллер выбросит ForbiddenException.
     */
    @Test(expected = ForbiddenException.class)
    public void registerNotAnOwner() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new NotAnOwnerException(AGENCY_ID,
                OWNER_ID));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет InvalidRegionException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerInvalidRegion() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new InvalidRegionException(0));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет InvalidParamValueException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerInvalidParamValue() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(
                new InvalidParamValueException(new StringParamValue(0, 0, 0, "")));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет AgencyNotRegisteredException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void registerAgencyNotRegistered() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new AgencyNotRegisteredException(0));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет ContactAlreadyLinkedException, контроллер выбросит BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void contactAlreadyLinkedToAnotherClient() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new ContactAlreadyLinkedException(0L,
                0L, null));

        instance.register(request, dto);
    }

    /**
     * Тест проверяет, что, если сервис выкинет ClientNotFoundException, контроллер выбросит ForbiddenException.
     */
    @Test(expected = ForbiddenException.class)
    public void contactClientNotRegistered() {
        Mockito.when(service.registerShop(Mockito.any())).thenThrow(new ClientNotFoundException(0L));

        instance.register(request, dto);
    }

    private void fillShopRegistrationDTORequiredFields(ShopRegistrationDTO dto) {
        dto.setRegionId(1L);
        dto.setLocalRegionId(2L);
        dto.setShopName("name");
        dto.setDomain("domain");

        final NotificationContact contact = new NotificationContact();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        dto.setNotificationContact(contact);
    }
}
