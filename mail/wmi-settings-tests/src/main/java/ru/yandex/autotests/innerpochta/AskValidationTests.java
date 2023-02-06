package ru.yandex.autotests.innerpochta;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.oper.GetAll;
import ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile;
import ru.yandex.autotests.innerpochta.utils.oper.GetProfile;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.getProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateOneProfileSetting;
import static ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule.profile;
import static ru.yandex.autotests.innerpochta.wmi.core.filter.log.LoggerFilterBuilder.log;
import static ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.getSettingValue;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.profileCount;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.validatedEmails;
/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Тесты с включением ask_validator")
@Description("Проверки с валидацией и без")
@Features("Запросы с валидацией в ББ")
@Stories("Параметр ask_validation")
public class AskValidationTests {

    public static final String DEFAULT_EMAIL_SETTING = "default_email";
    public static final String VALIDATED_EMAIL_SETTING = "emails";
    public static final String DONE = "Done";

    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule())
            .around(profile(accInfo.uid()).backup(DEFAULT_EMAIL_SETTING));


    @Test
    public void withAskValidatorShouldSeeValidatedEmails() throws IOException {
        GetAll all = getAll(settings(accInfo.uid()).askValidator())
                .log(log().onlyIfError())
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200);

        assertThat("Список провалидированных email не должен быть пуст",
                validatedEmails(all.toString()), hasSize(greaterThan(0)));
        assertThat(new DefaultHttpClient(), hasSetting(DEFAULT_EMAIL_SETTING, isIn(validatedEmails(all.toString())))
                .with(all.log(log())));
    }


    @Test
    public void validatedEmailsShouldBeEqualInAllAndAllProfile() throws IOException {
        GetAll all = getAll(settings(accInfo.uid()).askValidator())
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200);

        GetAllProfile profile = GetAllProfile.getAllProfile(settings(accInfo.uid())
                .askValidator())
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200);

        List<String> emails = validatedEmails(profile.toString());

        assertThat("Список провалидированных email должен соответствовать в разных запросах",
                validatedEmails(all.toString()), hasItems(emails.toArray(new String[emails.size()])));
    }

    @Test
    public void defaultEmailCanBeOnlyOneOfValidated() throws IOException {
        GetAll all = getAll(settings(accInfo.uid()).askValidator())
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200);

        List<String> emails = validatedEmails(all.toString());
        String current = getSettingValue(all.toString(), DEFAULT_EMAIL_SETTING);
        emails.remove(current);

        updateOneProfileSetting(accInfo.uid(), DEFAULT_EMAIL_SETTING,
                emails.get(RandomUtils.nextInt(emails.size() - 1)))
                .post().via(new DefaultHttpClient()).assertResponse(equalTo(DONE));

        assertThat(new DefaultHttpClient(), hasSetting(DEFAULT_EMAIL_SETTING, not(is(current))).
                with(getProfile(settings(accInfo.uid())
                        .askValidator().settingsList(DEFAULT_EMAIL_SETTING))));
    }


    @Test
    public void getProfileReturnEmptyIfValidationTurnOff() throws IOException {
        Integer count = profileCount(
                getProfile(settings(accInfo.uid())
                        .settingsList(VALIDATED_EMAIL_SETTING))
                        .get().via(new DefaultHttpClient())
                        .statusCodeShouldBe(HttpStatus.OK_200).toString()
        );
        assertThat("Неверное количество узлов с настройками", count, equalTo(0));
    }


    @Test
    public void getProfileReturnAllValidatedEmailsIfValidationOn() throws IOException {
        GetProfile getProfile = getProfile(settings(accInfo.uid())
                .askValidator()
                .settingsList(VALIDATED_EMAIL_SETTING, DEFAULT_EMAIL_SETTING))
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200);

        List<String> validatedEmails = validatedEmails(getProfile.toString());
        assertThat("Список провалидированных email с запросом валидатора, не должен быть пуст",
                validatedEmails, hasSize(greaterThan(0)));

        assertThat("Дефолтный email должен быть одним из списка провалидированных",
                getSettingValue(getProfile.toString(), DEFAULT_EMAIL_SETTING),
                isIn(validatedEmails));
    }
}
