package ru.yandex.autotests.directintapi.tests.officecontact;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.officecontact.YandexOfficeRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.OFFICE_CONTACT)
@Title("Проверка получения информации об офисах")
@Issue("https://st.yandex-team.ru/DIRECT-50325")
@RunWith(Parameterized.class)
public class OfficeContactTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);


    @Parameterized.Parameter(value = 0)
    public String lang;

    @Parameterized.Parameter(value = 1)
    public Long geoId;

    @Parameterized.Parameter(value = 2)
    public String domain;

    @Parameterized.Parameter(value = 3)
    public String expectedAnswer;

    @Parameterized.Parameters(name = "Язык: {0}, гео id:{1}, домен: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"ru", 2l, "ru", "+7 (812) 633-36-00 доб. 2480 (отдел клиентского сервиса, с 5:00 до 24:00) 8 800 234-24-80 (отдел клиентского сервиса, звонок из регионов России бесплатный)"},
                {"tr", 2l, "", "+7 (495) 739-37-77 (Moskova'da telefon) 8 800 234-24-80 (Rusya bölgesinden gelen çağrılar ücretsiz)"},
                {"en", 122l, "com", "+7 (495) 780-65-20 (support service, call on weekdays from 10:00 to 20:00 Moscow time) 8 800 250-96-39 ext. 2482 (support service, toll-free from Russian regions)"},
                {"", null, "", "+7 (495) 739-37-77 (отдел клиентского сервиса, с 5:00 до 24:00) 8 800 234-24-80 (отдел клиентского сервиса, звонок из регионов России бесплатный)"},
        });
    }

    @Test
    @Title("Получение данных офисов")
    public void getOfficeContacts() {
        String actualResponse = api.userSteps.getDarkSideSteps().getOfficeContactsSteps()
                .getOfficeContacts(new YandexOfficeRequest()
                        .withDomain(domain).withGeoId(geoId).withLang(lang)).replaceAll(String.valueOf((char) 10), " ");
        assertThat("Контакты соотвествуют ожиданями", actualResponse, equalTo(expectedAnswer));
    }
}
