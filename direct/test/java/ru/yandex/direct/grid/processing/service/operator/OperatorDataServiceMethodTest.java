package ru.yandex.direct.grid.processing.service.operator;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.client.service.ClientOfficeService;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.util.HttpUtil.DETECTED_LOCALE_HEADER_NAME;
import static ru.yandex.direct.common.util.HttpUtil.YANDEX_GID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;

@ParametersAreNonnullByDefault
public class OperatorDataServiceMethodTest {

    private static final Locale LOCALE = Locale.ENGLISH;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ClientOfficeService clientOfficeService;

    @Mock
    private TranslationService translationService;

    @Mock
    private GeoBaseHelper geoBaseHelper;

    @InjectMocks
    private OperatorDataService operatorDataService;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

        doReturn(LOCALE).when(translationService).getLocale();
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    public void checkGetOfficeContact() {
        Long countryRegionId = RandomNumberUtils.nextPositiveLong();
        operatorDataService.getOfficeContact(countryRegionId);

        verify(clientOfficeService)
                .getOfficeContactForFooter(GLOBAL_REGION_ID, Language.EN, null, countryRegionId);
    }

    @Test
    public void checkGetOfficeContact_WithYandexGidCookie() {
        Long region = Region.KAZAKHSTAN_REGION_ID;
        Cookie yandexGidCookie = new Cookie(YANDEX_GID, region.toString());
        doReturn(new Cookie[]{yandexGidCookie})
                .when(httpServletRequest).getCookies();
        doReturn(Optional.of(region))
                .when(geoBaseHelper).convertToDirectRegionId(region);

        operatorDataService.getOfficeContact(null);

        verify(clientOfficeService).getOfficeContactForFooter(region, Language.EN, null, null);
    }

    @Test
    public void checkGetOfficeContact_WithInvalidValueFromYandexGidCookie() {
        Cookie yandexGidCookieWithInvalidValue = new Cookie(YANDEX_GID, "");
        doReturn(new Cookie[]{yandexGidCookieWithInvalidValue})
                .when(httpServletRequest).getCookies();

        operatorDataService.getOfficeContact(null);

        verify(clientOfficeService).getOfficeContactForFooter(GLOBAL_REGION_ID, Language.EN, null, null);
    }

    @Test
    public void checkGetOfficeContact_WithLocaleFromHttpRequestAttribute() {
        Locale locale = new Locale("ru");
        doReturn(locale)
                .when(httpServletRequest).getAttribute(DETECTED_LOCALE_HEADER_NAME);

        operatorDataService.getOfficeContact(null);

        Language expectedLanguage = Language.fromLocale(locale);
        verify(clientOfficeService).getOfficeContactForFooter(GLOBAL_REGION_ID, expectedLanguage, null, null);
    }

    @Test
    public void checkGetOfficeContact_WithInvalidLocaleFromHttpRequestAttribute() {
        String invalidLocale = "bla bla";
        doReturn(invalidLocale)
                .when(httpServletRequest).getAttribute(DETECTED_LOCALE_HEADER_NAME);

        operatorDataService.getOfficeContact(null);

        verify(clientOfficeService).getOfficeContactForFooter(GLOBAL_REGION_ID, Language.EN, null, null);
    }

}
