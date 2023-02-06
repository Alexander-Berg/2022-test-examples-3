package ru.yandex.market.partner.mvc.controller.campaign.model.registration;

import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.mvc.controller.campaign.model.NotificationContact;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopRegistrationDTO;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopSubtype;
import ru.yandex.market.partner.mvc.exception.BadRequestException;
import ru.yandex.market.partner.mvc.exception.ErrorSubcode;
import ru.yandex.market.partner.mvc.exception.PartnerErrorInfo;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CPC;

class ShopRegistrationDataTest {
    private static final String CONTACT_EMAIL = "email";

    /**
     * Тест проверяет, что домен обязателен для белых магазинов.
     */
    @Test
    void validateDomainForWhiteShopTest() {
        ShopRegistrationDTO dto = createDto();
        dto.setDomain(null);

        final BadRequestException error = assertThrows(BadRequestException.class, () -> {
            final WhiteShopRegistrationData whiteData = new WhiteShopRegistrationData(ShopSubtype.REGULAR, CPC);
            whiteData.validate(dto);

        });
        checkErrorField(error, "domain", ErrorSubcode.MISSING);
    }

    /**
     * Тест проверяет, что домен обязателен для желтых онлайн магазинов.
     */
    @Test
    void validateDomainForFmcgShopTest() {
        ShopRegistrationDTO dto = createDto();
        dto.setDomain(null);
        dto.setOnline(true);

        final BadRequestException error = assertThrows(BadRequestException.class, () -> {
            final FmcgShopRegistrationData fmcgData = new FmcgShopRegistrationData();
            fmcgData.validate(dto);

        });
        checkErrorField(error, "domain", ErrorSubcode.MISSING);
    }

    /**
     * Тест проверяет, что контакт обязателен (на примере валидации данных для белого магазина).
     */
    @Test
    void validateNullContactTest() {
        ShopRegistrationDTO dto = createDto();
        dto.setNotificationContact(null);

        final BadRequestException error = assertThrows(BadRequestException.class, () -> {
            final WhiteShopRegistrationData whiteData = new WhiteShopRegistrationData(ShopSubtype.REGULAR, CPC);
            whiteData.validate(dto);
        });
        checkErrorField(error, "notificationContact", ErrorSubcode.MISSING);
    }

    /**
     * Тест проверяет, что имя магазина обязательно (на примере валидации данных для белого магазина).
     */
    @Test
    void validateEmptyNameTest() {
        ShopRegistrationDTO dto = createDto();
        dto.setShopName("");
        final BadRequestException error = assertThrows(BadRequestException.class, () -> {
            final WhiteShopRegistrationData whiteData = new WhiteShopRegistrationData(ShopSubtype.SMB, CPC);
            whiteData.validate(dto);
        });
        checkErrorField(error, "shopName", ErrorSubcode.MISSING);
    }

    /**
     * Тест проверяет конвертацию dto в {@code WhiteShopRegistrationData}.
     */
    @Test
    void conversionTest() {
        final ShopRegistrationDTO dto = createRandomDto();
        final PartnerDefaultRequestHandler.PartnerHttpServRequest request = createRequest();

        WhiteShopRegistrationData data = new WhiteShopRegistrationData(ShopSubtype.REGULAR, CPC);
        data.fill(dto, request);

        Assert.assertEquals(request.getUid(), data.getUid());
        Assert.assertEquals(request.getEffectiveUid(), data.getEuid());
        Assert.assertEquals(dto.getDomain(), data.getDomain());
        Assert.assertEquals(dto.getInternalShopName(), data.getInternalShopName());
        Assert.assertEquals(dto.getLocalRegionId(), data.getLocalRegionId());
        Assert.assertEquals(dto.getRegionId(), data.getRegionId());
        Assert.assertEquals(dto.getOwnerLogin(), data.getOwnerLogin());
        Assert.assertEquals(dto.getShopName(), data.getShopName());

        final NotificationContact expectedContact = dto.getNotificationContact();
        final ContactWithEmail contact = data.getNotificationContact();
        Assert.assertEquals(expectedContact.getFirstName(), contact.getFirstName());
        Assert.assertEquals(expectedContact.getLastName(), contact.getLastName());
        Assert.assertEquals(expectedContact.getPhone(), contact.getPhone());
        Assert.assertEquals(CommonShopRegistrationData.DEFAULT_POSITION, contact.getPosition());

        final ContactEmail email = contact.getEmails().iterator().next();
        Assert.assertEquals(CONTACT_EMAIL, email.getEmail());
        Assert.assertFalse(email.isValid());
        Assert.assertTrue(email.isActive());
    }

    @Test
    void testEmptyDomain() {
        final ShopRegistrationDTO dto = createRandomDto();
        dto.setDomain(StringUtils.EMPTY);
        final PartnerDefaultRequestHandler.PartnerHttpServRequest request = createRequest();

        DeliveryShopRegistrationData data = new DeliveryShopRegistrationData();
        data.fill(dto, request);

        assertThat(data.getDomain(), nullValue());
    }

    private void checkErrorField(BadRequestException e, String field, String errorSubcode) {
        final List<PartnerErrorInfo> errors = e.getErrors();
        final PartnerErrorInfo error = errors.get(0);
        final Map<String, Object> details = error.getDetails();

        assertEquals(field, details.get("field"));
        assertEquals(errorSubcode, details.get("subcode"));
    }

    private ShopRegistrationDTO createDto() {
        final NotificationContact contact = new NotificationContact();
        contact.setFirstName("first");
        contact.setLastName("last");
        contact.setEmail("mail@mail.ru");
        contact.setPhone("phone");

        final ShopRegistrationDTO dto = new ShopRegistrationDTO();
        dto.setRegionId(1L);
        dto.setLocalRegionId(2L);
        dto.setDomain("domain");
        dto.setShopName("shopName");
        dto.setInternalShopName("internalShopName");
        dto.setNotificationContact(contact);
        return dto;
    }

    private ShopRegistrationDTO createRandomDto() {
        final ShopRegistrationDTO dto = EnhancedRandom.random(ShopRegistrationDTO.class);
        final NotificationContact notificationContact = dto.getNotificationContact();
        notificationContact.setEmail(CONTACT_EMAIL);
        return dto;
    }

    private PartnerDefaultRequestHandler.PartnerHttpServRequest createRequest() {
        return new MockPartnerRequest(1L, 2L, 3L, 4L);
    }
}
