package ru.yandex.market.core.post;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.core.post.model.BankData;
import ru.yandex.market.core.post.model.ContractOffer;
import ru.yandex.market.core.post.model.OrganizationData;
import ru.yandex.market.core.post.model.PersonalData;
import ru.yandex.market.core.post.model.PostRegionEntity;
import ru.yandex.market.core.post.model.dto.PostContractOfferDto;
import ru.yandex.market.core.post.model.dto.ShippingPointDto;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.core.post.model.dto.PostContractOfferDto.PostalGroup.PARCELS;
import static ru.yandex.market.core.post.model.dto.PostContractOfferDto.PostalServices.INTERNAL;

class ModelToDtoConverterTest {

    @Test
    void testConvertToDto() {
        PostRegionEntity postRegion = new PostRegionEntity(1L, "773", "test");
        PostContractOfferDto dto = ModelToDtoConverter.convertToDto(createTestData(), postRegion);

        assertNotNull(dto);
        assertEquals(1, dto.getOfferCreationSystem());
        assertEquals("Bank name", dto.getBankName());
        assertEquals("Bank city", dto.getBankCity());
        assertEquals("Bank account", dto.getBankAccount());
        assertEquals("Bank corrAccount", dto.getBankCorrAccount());
        assertEquals("Bank bik", dto.getBankBik());
        assertEquals("Legal address", dto.getLegalAddress());
        assertEquals(Integer.valueOf(166), dto.getOrgFormType());
        assertEquals("Org fullName", dto.getOrgFullname());
        assertEquals("Org inn", dto.getOrgInn());
        assertEquals("Org kpp", dto.getOrgKpp());
        assertEquals("Org name", dto.getOrgName());
        assertEquals("Org ogrn", dto.getOrgOgrn());
        assertEquals("Org ogrnip", dto.getOrgOgrnip());
        assertEquals("Org site", dto.getOrgSite());
        assertEquals("Org snils", dto.getSnils());
        assertEquals(PostContractOfferDto.OrganizationType.OOO, dto.getOrgType());
        assertEquals("Accountant Name", dto.getAccountantGivenName());
        assertEquals("Accountant surname", dto.getAccountantSurname());
        assertEquals("Accountant middleName", dto.getAccountantMiddleName());
        assertEquals("1111111", dto.getAccountantPhone());
        assertEquals("Signer Name", dto.getSignerGivenName());
        assertEquals("Signer surname", dto.getSignerSurname());
        assertEquals("Signer middleName", dto.getSignerMiddleName());
        assertEquals("Signer passport", dto.getSignerPassport());
        assertEquals("Signer", dto.getSignerPosition());
        assertEquals(PostContractOfferDto.SignerRight.CHARTER, dto.getSignerRight());
        assertEquals("Contact Name", dto.getContactGivenName());
        assertEquals("Contact surname", dto.getContactSurname());
        assertEquals("Contact middleName", dto.getContactMiddleName());
        assertEquals("contact@mail.ru", dto.getContactEmail());
        assertEquals("3333333", dto.getContactPhone());
        assertEquals("Mailing address", dto.getMailingAddress());
        assertEquals(255, dto.getPlannedMonthlyNumber());
        assertEquals(PostContractOfferDto.MailRank.WO_RANK, dto.getMailRank());
        assertEquals(Sets.newHashSet(PARCELS), dto.getPostalGroups());
        assertEquals(Sets.newHashSet(INTERNAL), dto.getServiceTypes());
        assertEquals("773", dto.getRegionSerialNumber());
        assertEquals("1.0.02", dto.getOfferVersion());
        assertTrue(dto.isOffer());
        MatcherAssert.assertThat(dto.getShippingPoints(),
                new ReflectionAssertMatcher<>(
                        singletonList(
                                ShippingPointDto.builder()
                                        .withOperatorPostcode("220")
                                        .withReturnAddressType(ShippingPointDto.ReturnAddressType.POSTOFFICE_ADDRESS)
                                        .build())));
    }

    private ContractOffer createTestData() {
        return ContractOffer.builder()
                .withOrganizationData(OrganizationData.builder()
                        .withBankData(BankData.builder()
                                .withBankName("Bank name")
                                .withBankCity("Bank city")
                                .withBankAccount("Bank account")
                                .withBankCorrAccount("Bank corrAccount")
                                .withBankBik("Bank bik")
                                .build())
                        .withLegalAddress("Legal address")
                        .withOrgFormType(166)
                        .withOrgFullname("Org fullName")
                        .withOrgInn("Org inn")
                        .withOrgKpp("Org kpp")
                        .withOrgName("Org name")
                        .withOrgOgrn("Org ogrn")
                        .withOrgOgrnip("Org ogrnip")
                        .withOrgSite("Org site")
                        .withOrgType(OrganizationData.OrganizationType.OOO)
                        .withSnils("Org snils")
                        .build())
                .withAccountant(PersonalData.builder()
                        .withName("Accountant Name")
                        .withSurname("Accountant surname")
                        .withMiddleName("Accountant middleName")
                        .withEmail("accountant@mail.ru")
                        .withPassport("Accountant passport")
                        .withPhoneNumber("1111111")
                        .withAdditionalNumber("111")
                        .withPosition("Accountant")
                        .build())
                .withSigner(PersonalData.builder()
                        .withName("Signer Name")
                        .withSurname("Signer surname")
                        .withMiddleName("Signer middleName")
                        .withEmail("signer@mail.ru")
                        .withPassport("Signer passport")
                        .withPhoneNumber("22222222")
                        .withAdditionalNumber("222")
                        .withPosition("Signer")
                        .build())
                .withContact(PersonalData.builder()
                        .withName("Contact Name")
                        .withSurname("Contact surname")
                        .withMiddleName("Contact middleName")
                        .withEmail("contact@mail.ru")
                        .withPassport("Contact passport")
                        .withPhoneNumber("3333333")
                        .withAdditionalNumber("333")
                        .withPosition("Contact")
                        .build())
                .withSignerRight(ContractOffer.SignerRight.CHARTER)
                .withMailingAddress("Mailing address")
                .withPlannedMonthlyNumber(255)
                .withShippingPoint("220")
                .build();
    }


}
