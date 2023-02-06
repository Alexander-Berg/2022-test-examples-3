package ru.yandex.direct.core.entity.client.service;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.core.entity.client.model.office.GeoCity;
import ru.yandex.direct.core.entity.client.model.office.OfficeContact;
import ru.yandex.direct.core.entity.yandexoffice.YandexOfficesRepository;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetOfficeContactForFooterTest {

    private static final Long DEFAULT_REGION_ID = Region.GLOBAL_REGION_ID;
    private static final Language DEFAULT_LANGUAGE = Language.RU;

    @Mock
    private ClientGeoService clientGeoService;

    @Mock
    private YandexOfficesRepository yandexOfficesRepository;

    private ClientOfficeService clientOfficeService;

    @Parameterized.Parameters(name = "language={0}, yandexDomain={1}, geoCity={2}, countryRegionId={3}, "
            + "expectedOfficeContact={4}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {DEFAULT_LANGUAGE, YandexDomain.BY, null, null, OfficeContact.BLR},
                {DEFAULT_LANGUAGE, YandexDomain.BY, GeoCity.KZ, null, OfficeContact.BLR},
                {DEFAULT_LANGUAGE, null, GeoCity.BY, null, OfficeContact.BLR},

                {DEFAULT_LANGUAGE, YandexDomain.UA, null, null, OfficeContact.UKR},
                {DEFAULT_LANGUAGE, YandexDomain.UA, GeoCity.KZ, null, OfficeContact.UKR},

                {DEFAULT_LANGUAGE, YandexDomain.TR, null, null, OfficeContact.TR},
                {DEFAULT_LANGUAGE, YandexDomain.TR, GeoCity.KZ, null, OfficeContact.TR},

                {DEFAULT_LANGUAGE, null, null, Region.KAZAKHSTAN_REGION_ID, OfficeContact.KAZ},
                {DEFAULT_LANGUAGE, YandexDomain.KZ, null, null, OfficeContact.KAZ},
                {DEFAULT_LANGUAGE, null, GeoCity.KZ, null, OfficeContact.KAZ},
                {DEFAULT_LANGUAGE, YandexDomain.KZ, GeoCity.KZ, Region.KAZAKHSTAN_REGION_ID, OfficeContact.KAZ},

                {Language.EN, null, null, null, OfficeContact.EN},
                {Language.UK, null, GeoCity.KIEV, null, OfficeContact.KIEV},
                {Language.UK, null, null, null, OfficeContact.UKR},

                {Language.RU, null, GeoCity.UA, null, OfficeContact.UKR},
                {Language.RU, null, GeoCity.ODESSA, null, OfficeContact.UKR},
                {Language.RU, null, GeoCity.KIEV, null, OfficeContact.KIEV},

                {Language.RU, null, GeoCity.MOSCOW, null, OfficeContact.MSK},
                {Language.RU, null, GeoCity.CENTER, null, OfficeContact.MSK},
                {Language.RU, null, GeoCity.VOLGA, null, OfficeContact.MSK},
                {Language.RU, null, GeoCity.RU, null, OfficeContact.MSK},

                {Language.RU, null, GeoCity.SPB, null, OfficeContact.SPB},
                {Language.RU, null, GeoCity.NORTH, null, OfficeContact.SPB},

                {Language.RU, null, GeoCity.EBURG, null, OfficeContact.EKB},
                {Language.RU, null, GeoCity.URAL, null, OfficeContact.EKB},

                {Language.RU, null, GeoCity.NOVOSIB, null, OfficeContact.NSK},
                {Language.RU, null, GeoCity.SIBERIA, null, OfficeContact.NSK},
                {Language.RU, null, GeoCity.N_NOVGOROD, null, OfficeContact.N_NOVGOROD},
                {Language.RU, null, GeoCity.SOUTH, null, OfficeContact.ROSTOV},
                {Language.RU, null, GeoCity.TATARSTAN, null, OfficeContact.KAZAN},

                {Language.RU, null, null, null, OfficeContact.OTHER},
                {Language.TR, null, null, null, OfficeContact.OTHER},
        });
    }

    @Parameterized.Parameter
    public Language language;
    @Parameterized.Parameter(1)
    public YandexDomain yandexDomain;
    @Parameterized.Parameter(2)
    public GeoCity geoCity;
    @Parameterized.Parameter(3)
    public Long countryRegionId;
    @Parameterized.Parameter(4)
    public OfficeContact expectedOfficeContact;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        clientOfficeService = spy(new ClientOfficeService(clientGeoService, yandexOfficesRepository));
        doReturn(geoCity).when(clientOfficeService).getGeoCity(eq(DEFAULT_REGION_ID), any());
    }

    @Test
    public void checkGetOfficeContactForFooter() {
        OfficeContact officeContact = clientOfficeService
                .getOfficeContactForFooter(DEFAULT_REGION_ID, language, yandexDomain, countryRegionId);

        verify(clientGeoService).getClientTranslocalGeoTree(yandexDomain);
        assertThat(officeContact).isEqualTo(expectedOfficeContact);
    }

}
