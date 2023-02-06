package ru.yandex.market.api.cpa.yam.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.api.cpa.yam.dto.ContactInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestForm;
import ru.yandex.market.api.cpa.yam.dto.SignatoryInfoDTO;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.geocoder.RegionIdFetcher;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link FormToPrepayRequestConverter}.
 *
 * @author avetokhin 13/04/17.
 */
public class FormToPrepayRequestConverterTest {

    private static final long ID = 110L;
    private static final List<Long> DATASOURCES = Arrays.asList(1L, 2L);
    private static final LocalDate LOCAL_DATE = LocalDate.of(2017, 1, 1);

    private final GeoClient geoClient = Mockito.mock(GeoClient.class);
    private final RegionIdFetcher regionIdFetcher = new RegionIdFetcher(geoClient);

    @Test
    public void testEmpty() {
        final List<PrepayRequest> requests = FormToPrepayRequestConverter.convert(ID,
                getForm(null, null, null), regionIdFetcher::fetch);

        for (int i = 0; i < requests.size(); i++) {
            final PrepayRequest request = requests.get(i);
            checkCommon(request, DATASOURCES.get(i));
            assertThat(request.getOrganizationName(), nullValue());
            assertThat(request.getOrganizationType(), nullValue());
            assertThat(request.getOgrn(), nullValue());
            assertThat(request.getInn(), nullValue());
            assertThat(request.getKpp(), nullValue());
            assertThat(request.getPostcode(), nullValue());
            assertThat(request.getFactAddress(), nullValue());
            assertThat(request.getJurAddress(), nullValue());
            assertThat(request.getLicenseNum(), nullValue());
            assertThat(request.getLicenseDate(), nullValue());
            assertThat(request.getBik(), nullValue());
            assertThat(request.getBankName(), nullValue());
            assertThat(request.getAccountNumber(), nullValue());
            assertThat(request.getCorrAccountNumber(), nullValue());
            assertThat(request.getContactPerson(), nullValue());
            assertThat(request.getPhoneNumber(), nullValue());
            assertThat(request.getEmail(), nullValue());
            assertThat(request.getSellerClientId(), nullValue());
            assertThat(request.getCreatedAt(), nullValue());
            assertThat(request.getUpdatedAt(), nullValue());
            assertThat(request.getComment(), nullValue());
            assertThat(request.getSignatory(), nullValue());
            assertThat(request.getSignatoryDocType(), nullValue());
            assertThat(request.getSignatoryDocInfo(), nullValue());
            assertThat(request.getStartDate(), nullValue());
            assertThat(request.getDocuments(), nullValue());
        }
    }

    private Optional<GeoObject> createGeoObject(String geoId) {
        return Optional.of(SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder().withGeoid(geoId).build())
                .withBoundary(Boundary.newBuilder().build())
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .build())
                .build()
        );
    }

    @Test
    public void testFilled() {
        Mockito.when(geoClient.findFirst("fact addr")).thenReturn(createGeoObject("42"));
        final OrganizationInfoDTO orgInfo = getOrganizationInfo();
        final ContactInfoDTO contactInfo = getContactInfo();
        final SignatoryInfoDTO signatoryInfo = getSignatoryInfo();
        final List<PrepayRequest> requests = FormToPrepayRequestConverter.convert(ID,
                getForm(orgInfo, contactInfo, signatoryInfo), regionIdFetcher::fetch);

        for (int i = 0; i < requests.size(); i++) {
            final PrepayRequest request = requests.get(i);
            checkCommon(request, DATASOURCES.get(i));
            assertThat(request.getOrganizationName(), equalTo(orgInfo.getName()));
            assertThat(request.getOrganizationType(), equalTo(orgInfo.getType()));
            assertThat(request.getOgrn(), equalTo(orgInfo.getOgrn()));
            assertThat(request.getInn(), equalTo(orgInfo.getInn()));
            assertThat(request.getKpp(), equalTo(orgInfo.getKpp()));
            assertThat(request.getPostcode(), equalTo(orgInfo.getPostcode()));
            assertThat(request.getFactAddress(), equalTo(orgInfo.getFactAddress()));
            assertThat(request.getFactAddressRegionId(), equalTo(42L));
            assertThat(request.getJurAddress(), equalTo(orgInfo.getJuridicalAddress()));
            assertThat(request.getLicenseNum(), equalTo(orgInfo.getLicenseNumber()));
            assertThat(request.getLicenseDate(), equalTo(LOCAL_DATE.atStartOfDay().toInstant(
                    ZoneId.systemDefault().getRules().getOffset(Instant.now()))));
            assertThat(request.getBik(), equalTo(orgInfo.getBik()));
            assertThat(request.getBankName(), equalTo(orgInfo.getBankName()));
            assertThat(request.getAccountNumber(), equalTo(orgInfo.getAccountNumber()));
            assertThat(request.getCorrAccountNumber(), equalTo(orgInfo.getCorrAccountNumber()));
            assertThat(request.getContactPerson(), equalTo(contactInfo.getName()));
            assertThat(request.getPhoneNumber(), equalTo(contactInfo.getPhoneNumber()));
            assertThat(request.getEmail(), equalTo(contactInfo.getEmail()));
            assertThat(request.getSellerClientId(), nullValue());
            assertThat(request.getCreatedAt(), nullValue());
            assertThat(request.getUpdatedAt(), nullValue());
            assertThat(request.getComment(), nullValue());
            assertThat(request.getSignatory(), equalTo(signatoryInfo.getName()));
            assertThat(request.getSignatoryDocType(), equalTo(signatoryInfo.getDocType()));
            assertThat(request.getSignatoryDocInfo(), equalTo(signatoryInfo.getDocInfo()));
            assertThat(request.getStartDate(), nullValue());
            assertThat(request.getDocuments(), nullValue());
            assertThat(request.getIsAutoFilled(), equalTo(true));
        }
        Mockito.verify(geoClient, Mockito.times(2)).findFirst("fact addr");
    }

    private void checkCommon(final PrepayRequest request, final long datasourceId) {
        assertThat(request.getId(), equalTo(ID));
        assertThat(request.getDatasourceId(), equalTo(datasourceId));
        assertThat(request.getPrepayType(), equalTo(PrepayType.YANDEX_MARKET));
        assertThat(request.getStatus(), equalTo(PartnerApplicationStatus.NEW));
    }

    private PrepayRequestForm getForm(final OrganizationInfoDTO orgInfo, final ContactInfoDTO contactInfo,
                                      final SignatoryInfoDTO signatoryInfo) {
        return new PrepayRequestForm(DATASOURCES, orgInfo, contactInfo, signatoryInfo, 1L, null);
    }

    private SignatoryInfoDTO getSignatoryInfo() {
        return new SignatoryInfoDTO("name", SignatoryDocType.AOA_OR_ENTREPRENEUR, "info", "position");
    }

    private ContactInfoDTO getContactInfo() {
        return new ContactInfoDTO("name", "email", "phone number");
    }

    private OrganizationInfoDTO getOrganizationInfo() {
        return OrganizationInfoDTO.builder()
                .name("name")
                .ogrn("ogrn")
                .type(OrganizationType.ZAO)
                .inn("inn")
                .kpp("kpp")
                .postcode("postcode")
                .factAddress("fact addr")
                .juridicalAddress("jur addr")
                .accountNumber("acc number")
                .corrAccountNumber("corr acc number")
                .bik("bik")
                .bankName("bank name")
                .licenseNumber("license")
                .licenseDate(LOCAL_DATE)
                .isAutoFilled(true)
                .build();
    }
}
