package ru.yandex.market.api.cpa.yam.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.api.cpa.yam.dto.ContactInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayTypeDTO;
import ru.yandex.market.api.cpa.yam.dto.SignatoryInfoDTO;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.api.cpa.yam.exception.InvalidPrepayRequestOperationException;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link PrepayRequestToDTOConverter}.
 *
 * @author avetokhin 13/04/17.
 */
public class PrepayRequestToDTOConverterTest {

    private static final LocalDate LOCAL_DATE = LocalDate.of(2017, 1, 1);
    private static final Instant INSTANT = Instant.ofEpochMilli(1483267119000L);

    private final PrepayRequestToDTOConverter converter =
            new PrepayRequestToDTOConverter(
                    Mockito.mock(PrepayRequestValidatorService.class),
                    new PrepayRequestDocumentToDTOConverter(),
                    new PartnerApplicationConverter()
            );

    /**
     * Отдаем null.
     */
    @Test
    public void testEmpty() {
        assertThat(converter.convert(null), nullValue());
    }

    /**
     * Заявки с разными статусами.
     */
    @Test(expected = InvalidPrepayRequestOperationException.class)
    public void testInconsistentStatuses() {
        final PrepayRequest request1 = getRequest(10L);
        final PrepayRequest request2 = getRequest(20L);
        request2.setStatus(PartnerApplicationStatus.NEW);

        converter.convert(Arrays.asList(request1, request2));
    }

    /**
     * Заявки с разными ID.
     */
    @Test(expected = InvalidPrepayRequestOperationException.class)
    public void testInconsistentIds() {
        final PrepayRequest request1 = new PrepayRequest(1L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.FROZEN, 10L);
        final PrepayRequest request2 = new PrepayRequest(2L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.FROZEN, 20L);

        converter.convert(Arrays.asList(request1, request2));
    }

    /**
     * Корректные данные.
     */
    @Test
    public void testFilled() {
        final Collection<PrepayRequest> requests = getRequests();
        final PrepayRequestDTO dto = converter.convert(requests);

        final PrepayRequest request = requests.iterator().next();

        assertThat(dto, notNullValue());
        assertThat(dto.getRequestId(), equalTo(request.getId()));
        assertThat(dto.getStatus(), equalTo(request.getStatus()));
        assertThat(dto.getPrepayType(), equalTo(HasId.getById(PrepayTypeDTO.class, request.getPrepayType().getId())));
        assertThat(dto.getRequestType(), equalTo(request.getRequestType()));
        assertThat(dto.getDatasourceIds(), equalTo(requests.stream().map(PrepayRequest::getDatasourceId).collect(Collectors.toList())));
        assertThat(dto.getStartDate(), equalTo(request.getStartDate()));
        assertThat(dto.getUpdatedAt(), equalTo(request.getUpdatedAt()));
        assertThat(dto.getComment(), equalTo(request.getComment()));
        assertThat(dto.isFilled(), equalTo(false));
        assertThat(dto.getSellerClientId(), equalTo(request.getSellerClientId()));
        assertThat(dto.getContractId(), equalTo(request.getContractId()));
        assertThat(dto.getPersonId(), equalTo(request.getPersonId()));

        final ContactInfoDTO contactInfo = dto.getContactInfo();
        assertThat(contactInfo, notNullValue());
        assertThat(contactInfo.getName(), equalTo(request.getContactPerson()));
        assertThat(contactInfo.getEmail(), equalTo(request.getEmail()));
        assertThat(contactInfo.getPhoneNumber(), equalTo(request.getPhoneNumber()));

        final OrganizationInfoDTO orgInfo = dto.getOrganizationInfo();
        assertThat(orgInfo, notNullValue());
        assertThat(orgInfo.getName(), equalTo(request.getOrganizationName()));
        assertThat(orgInfo.getType(), equalTo(request.getOrganizationType()));
        assertThat(orgInfo.getOgrn(), equalTo(request.getOgrn()));
        assertThat(orgInfo.getInn(), equalTo(request.getInn()));
        assertThat(orgInfo.getKpp(), equalTo(request.getKpp()));
        assertThat(orgInfo.getPostcode(), equalTo(request.getPostcode()));
        assertThat(orgInfo.getFactAddress(), equalTo(request.getFactAddress()));
        assertThat(orgInfo.getJuridicalAddress(), equalTo(request.getJurAddress()));
        assertThat(orgInfo.getAccountNumber(), equalTo(request.getAccountNumber()));
        assertThat(orgInfo.getCorrAccountNumber(), equalTo(request.getCorrAccountNumber()));
        assertThat(orgInfo.getBik(), equalTo(request.getBik()));
        assertThat(orgInfo.getBankName(), equalTo(request.getBankName()));
        assertThat(orgInfo.getLicenseNumber(), equalTo(request.getLicenseNum()));
        assertThat(orgInfo.getLicenseDate(), equalTo(LOCAL_DATE));

        final SignatoryInfoDTO signatoryInfo = dto.getSignatoryInfo();
        assertThat(signatoryInfo, notNullValue());
        assertThat(signatoryInfo.getName(), equalTo(request.getSignatory()));
        assertThat(signatoryInfo.getDocType(), equalTo(request.getSignatoryDocType()));
        assertThat(signatoryInfo.getDocInfo(), equalTo(request.getSignatoryDocInfo()));
    }


    private Collection<PrepayRequest> getRequests() {
        return Arrays.asList(getRequest(10L), getRequest(20L));
    }

    private PrepayRequest getRequest(final long datasourceId) {
        final PrepayRequest request =
                new PrepayRequest(1L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.FROZEN, datasourceId);
        request.setCreatedAt(Instant.now());
        request.setStartDate(Instant.now());
        request.setUpdatedAt(Instant.now());
        request.setComment("comment");

        request.setOrganizationName("orgName");
        request.setOrganizationType(OrganizationType.ZAO);
        request.setOgrn("ogrn");
        request.setInn("inn");
        request.setKpp("kpp");
        request.setPostcode("postcode");
        request.setFactAddress("fact");
        request.setJurAddress("jur");
        request.setLicenseNum("license");
        request.setLicenseDate(INSTANT);
        request.setBankName("bank");
        request.setBik("bik");
        request.setAccountNumber("acc");
        request.setCorrAccountNumber("corr");
        request.setSellerClientId(10L);
        request.setContractId(11L);
        request.setPersonId(12L);

        request.setContactPerson("contact");
        request.setEmail("email");
        request.setPhoneNumber("phone");

        request.setSignatory("signatory");
        request.setSignatoryDocType(SignatoryDocType.POA);
        request.setSignatoryDocInfo("info");
        request.setSignatoryPosition("position");

        request.setRequestType(RequestType.MARKETPLACE);
        return request;
    }

}
