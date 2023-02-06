package ru.yandex.market.abo.core.outlet.license;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.market.abo.core.outlet.model.OutletLicenseCheck;
import ru.yandex.market.abo.core.outlet.model.OutletLicenseInfo;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.AddressDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletLegalInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 02.07.19
 */
class AlcoLicenseAutoApproverTest {
    @Mock
    GeoClient geoClient;
    @Mock
    ExecutorService pool;
    @InjectMocks
    @Spy
    AlcoLicenseAutoApprover alcoLicenseAutoApprover;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TestHelper.mockExecutorService(pool);
    }

    @ParameterizedTest(name = "canAutoCloseTest_{index}")
    @MethodSource("canAutoCloseTestArgumentProvider")
    void canAutoCloseTest(String mbiJurAddress, String fsrarJurAddress,
                          String mbiFactAddress, String fsrarFactAddress,
                          LocalDate mbiIssueDate, LocalDate fsrarIssueDate, LocalDate fsrarUpdateDate,
                          LocalDate mbiExpireDate, LocalDate fsrarExpireDate,
                          boolean expected
    ) {
        var check = mock(OutletLicenseCheck.class);
        when(check.getIssueDate()).thenReturn(mbiIssueDate);
        when(check.getExpiryDate()).thenReturn(mbiExpireDate);

        var licenseInfo = mock(OutletLicenseInfo.class);
        when(licenseInfo.getJuridicalAddress()).thenReturn(fsrarJurAddress);
        when(licenseInfo.getFactAddress()).thenReturn(fsrarFactAddress);
        when(licenseInfo.getIssueDate()).thenReturn(fsrarIssueDate);
        when(licenseInfo.getUpdateDate()).thenReturn(fsrarUpdateDate);
        when(licenseInfo.getExpiryDate()).thenReturn(fsrarExpireDate);

        var mbiLegalInfo = mock(OutletLegalInfoDTO.class);
        var mbiOutlet = mock(OutletInfoDTO.class);
        when(mbiOutlet.getLegalInfo()).thenReturn(mbiLegalInfo);
        when(mbiLegalInfo.getJuridicalAddress()).thenReturn(mbiJurAddress);

        doReturn(Set.of(mbiFactAddress)).when(alcoLicenseAutoApprover).buildMbiAddressCandidates(any());
        doAnswer(TestHelper.identityAnswer()).when(alcoLicenseAutoApprover).normalizeAddressWithGeoClient(anyString());
        doAnswer(TestHelper.identityAnswer()).when(alcoLicenseAutoApprover).formatFsrarAddress(anyString());
        doAnswer(TestHelper.identityAnswer()).when(alcoLicenseAutoApprover).formatMbiJurAddress(anyString());

        assertEquals(expected, alcoLicenseAutoApprover.canAutoClose(check, licenseInfo, mbiOutlet));
    }

    private static Stream<? extends Arguments> canAutoCloseTestArgumentProvider() {
        var now = LocalDate.now();
        return Stream.of(
                AutoCloseTestData.initializedBuilder()
                        .withExpected(true)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withMbiIssueDate(now.minusDays(1))
                        .withFsrarIssueDate(now.minusDays(2))
                        .withFsrarUpdateDate(now.minusDays(1))
                        .withExpected(true)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withFsrarJurAddress("a")
                        .withMbiJurAddress("b")
                        .withExpected(false)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withFsrarFactAddress("a")
                        .withMbiFactAddress("b")
                        .withExpected(false)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withFsrarIssueDate(now.minusYears(1))
                        .withMbiIssueDate(now.minusYears(2))
                        .withExpected(false)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withFsrarExpireDate(now.plusYears(1))
                        .withMbiIssueDate(now.plusYears(2))
                        .withExpected(false)
                        .build(),
                AutoCloseTestData.initializedBuilder()
                        .withFsrarExpireDate(now.minusMonths(1))
                        .withMbiIssueDate(now.minusMonths(1))
                        .withExpected(false)
                        .build()

        );
    }

    @ParameterizedTest
    @CsvSource({
            "addr_resp,building_resp,addr_resp",
            ",,",
            "addr_resp,,"
    })
    void normalizeAddressWithGeoClientTest(String addressResponse, String buildingNumberResponse, String expected
    ) {
        var geoObject = mock(GeoObject.class);
        when(geoObject.getAddressLine()).thenReturn(addressResponse);
        when(geoObject.getPremiseNumber()).thenReturn(buildingNumberResponse);
        when(geoClient.findFirst(anyString())).thenReturn(Optional.of(geoObject));
        assertEquals(expected, alcoLicenseAutoApprover.normalizeAddressWithGeoClient(""));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "" +
                    "Россия, , Санкт-Петербург г, , , , 11-я В.О. линия, 32/44, , литера А, помещение 1Н;" +
                    "Россия, Санкт-Петербург г, 11-я В.О. линия, 32/44, литера А",
            "" +
                    "Россия,,Самарская Область,Шигонский район,,с. Байдеряково,ул. Центральная,д.160,,,, магазин;" +
                    "Россия,Самарская Область,Шигонский район,с. Байдеряково,ул. Центральная,д.160",
    }, delimiter = ';')
    void formatFsrarAddressTest(String input, String expected) {
        assertEquals(expected, alcoLicenseAutoApprover.formatFsrarAddress(input));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "" +
                    "199178, г. Санкт-Петербург, 11-я линия В.О., д. 32/44, лит. А, пом. 1-Н  тел. (812) 321-60-60;" +
                    "199178, г. Санкт-Петербург, 11-я линия В.О., д. 32/44, лит. А",
            "" +
                    "129337, г.Москва, шоссе Ярославское, д.124, этаж 1,  помещение XI, комната 10, офис 7;" +
                    "129337, г.Москва, шоссе Ярославское, д.124"
    }, delimiter = ';')
    void formatMbiJurAddressTest(String input, String expected) {
        assertEquals(expected, alcoLicenseAutoApprover.formatMbiJurAddress(input));
    }


    @Test
    void buildMbiAddressCandidatesTest() {
        var mbiAddress = new AddressDTO("city", "street", "1", "3", null, "2", null, null);
        assertEquals(Set.of(
                "city, street, 1/2/3",
                "city, street, 1/2,3",
                "city, street, 1,2/3",
                "city, street, 1,2,3",
                "city, street, 1/2c3",
                "city, street, 1,2c3",
                "city, street, 1к2/3",
                "city, street, 1к2,3",
                "city, street, 1к2c3"
        ), alcoLicenseAutoApprover.buildMbiAddressCandidates(mbiAddress));
    }
}
