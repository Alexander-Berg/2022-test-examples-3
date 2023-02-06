package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.innerpochta.utils.rules.BackupSettingWithApiRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.utils.oper.Get.get;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAllProfile.getAllProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.GetProfile.getProfile;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateOneProfileSetting;
import static ru.yandex.autotests.innerpochta.utils.matchers.SettingsJsonValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.03.13
 * Time: 16:27
 */
@Aqua.Test
@Title("Общие тесты на установку настроек профиля")
@Description("Установка по-одному")
@RunWith(Parameterized.class)
@Features("Изменение профиля")
@Stories("Изменение в обе стороны")
public class ProfileChangingSettings {

    public static final String ON = "on";
    public static final String OFF = "off";


    @ClassRule
    public static AccountRule accInfo = new AccountRule();

    @Parameterized.Parameters(name = "{index}-{0}")
    public static Collection<Object[]> data() {
        return asList(

                        /*
                        Полный список /etc/ymsettings/mail_profile.conf
                         */

                new Object[]{"yandex_sign_enable", ON, OFF},
                new Object[]{"duplicate_menu", ON, OFF},
                new Object[]{"jump_to_next_message", ON, OFF},
                new Object[]{"save_sent", ON, OFF},
                new Object[]{"ml_to_inbox", ON, OFF},
                new Object[]{"collect_addresses", ON, OFF},
                new Object[]{"suggest_addresses", ON, OFF},
                new Object[]{"pop_spam_enable", ON, OFF},
                new Object[]{"pop_spam_subject_mark_enable", ON, OFF},
                new Object[]{"close_quoting", ON, OFF},
                new Object[]{"folder_thread_view", ON, OFF},
                new Object[]{"new_interface_by_default", ON, OFF},
                new Object[]{"broad_view", ON, OFF},
                new Object[]{"enable_autosave", ON, OFF},
                new Object[]{"enable_welcome_page", ON, OFF},
                new Object[]{"daria_welcome_page", ON, OFF},
                new Object[]{"first_login", ON, OFF},
                new Object[]{"pop3_makes_read", ON, OFF},
                new Object[]{"show_avatars", ON, OFF},
                new Object[]{"enable_hotkeys", ON, OFF},
                new Object[]{"enable_richedit", ON, OFF},
                new Object[]{"enable_quoting", ON, OFF},
                new Object[]{"enable_pop3_max_download", ON, OFF},
                new Object[]{"hide_tip_for_video_letter", ON, OFF},
                new Object[]{"show_advertisement", ON, OFF},
                new Object[]{"show_news", ON, OFF},
                new Object[]{"enable_firstline", ON, OFF},
                new Object[]{"enable_social_notification", ON, OFF},
                new Object[]{"enable_mailbox_selection", ON, OFF},
                new Object[]{"enable_pop", ON, OFF},
                new Object[]{"enable_imap", ON, OFF},
                new Object[]{"show_weather", ON, OFF},
                new Object[]{"show_stocks", ON, OFF},
                new Object[]{"show_chat", ON, OFF},
                new Object[]{"subs_show_unread", ON, OFF},
                new Object[]{"subs_show_item", ON, OFF},
                new Object[]{"dont_delete_msg_from_imap", ON, OFF},
                new Object[]{"copy_smtp_messages_to_sent", ON, OFF},
                new Object[]{"subs_show_informer", ON, OFF},
                new Object[]{"subs_show_line", ON, OFF},
                new Object[]{"enable_images", ON, OFF},
                new Object[]{"enable_images_in_spam", ON, OFF},
                new Object[]{"hide_daria_header", ON, OFF},
                new Object[]{"use_small_fonts", ON, OFF},
                new Object[]{"have_seen_stamp", ON, OFF},
                new Object[]{"have_seen_daria", ON, OFF},
                new Object[]{"use_monospace_in_text", ON, OFF},
                new Object[]{"signature_top", ON, OFF},
                new Object[]{"alert_on_empty_subject", ON, OFF},
                new Object[]{"dnd_enabled", ON, OFF},
                new Object[]{"https_enabled", ON, OFF},
                new Object[]{"translate", ON, OFF},
                new Object[]{"show_todo", ON, OFF},
                new Object[]{"show_socnet_avatars", ON, OFF},
                new Object[]{"imap_rename_enabled", ON, OFF},
                new Object[]{"webchat_turned_off", ON, OFF},
                new Object[]{"show_unread", ON, OFF},
                //int
                new Object[]{"subs_messages_per_page", "1", "50"},
                new Object[]{"subs_messages_per_page", "50", "100"},
                new Object[]{"messages_per_page", "1", "100"},
                new Object[]{"messages_per_page", "100", "200"},
                new Object[]{"mobile_messages_per_page", "1", "100"},
                new Object[]{"mobile_messages_per_page", "100", "200"},
                new Object[]{"suggest_addr_maxnum", "1", "2500"},
                new Object[]{"suggest_addr_maxnum", "2500", "5000"},
                new Object[]{"abook_page_size", "1", "2500"},
                new Object[]{"abook_page_size", "2500", "5000"},
                new Object[]{"pop3_max_download", "50", "75"},
                new Object[]{"pop3_max_download", "75", "100"},
                //char
                new Object[]{"quotation_char", "!", "+"},
                new Object[]{"quotation_char", "-", "~"},
                //select
                new Object[]{"skin_name", "neo", "neo2"},
                new Object[]{"skin_name", "classic", "modern"},
                new Object[]{"page_after_send", "done", "current_list"},
                new Object[]{"page_after_send", "done", "sent_list"},
                new Object[]{"page_after_delete", "current_list", "next_message"},
                new Object[]{"page_after_delete", "deleted_list", "inbox"},
                new Object[]{"page_after_move", "source_folder", "dest_folder"},
                new Object[]{"page_after_move", "current_list", "next_message"},
                new Object[]{"label_sort", "by_count", "by_abc"},
                //fields
                new Object[]{"color_scheme", "blue", "orange"},
                new Object[]{"color_scheme", randomAlphanumeric(1000), randomAlphanumeric(1000)},
                new Object[]{"signature", "", randomAlphanumeric(2046)},
                new Object[]{"signature_eng", "1", randomAlphanumeric(2046)},
                new Object[]{"mobile_sign", "1", randomAlphanumeric(2046)},
                new Object[]{"from_name", "1", randomAlphanumeric(510)},
                new Object[]{"from_name_eng", "1", randomAlphanumeric(510)},
                new Object[]{"interface_settings", "1", randomAlphanumeric(2048)}
        );
    }


    public static final String SUID_RO = "302832431";
    public static final String MDB_RO = "mdb000.yandex.ru";

    @Parameterized.Parameter(0)
    public String settingName;
    @Parameterized.Parameter(1)
    public String firstChange;
    @Parameterized.Parameter(2)
    public String secondChange;


    public BackupSettingWithApiRule backup = BackupSettingWithApiRule.profile(accInfo.uid());

    @Rule
    public TestRule chain = new LogConfigRule().around(new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            backup.backup(settingName);
        }
    }).around(backup);


    @Test
    public void profileShouldShowChanging() throws IOException {
        shouldSeeChangeAfterUpdate(firstChange);
        shouldSeeChangeAfterUpdate(secondChange);
        shouldSeeChangeAfterUpdate(firstChange);
    }

    private void shouldSeeChangeAfterUpdate(String changeTo) throws IOException {
        updateOneProfileSetting(accInfo.uid(), settingName, changeTo)
                .post().via(client()).assertResponse(equalTo("Done"));

        assertThat(client(), withWaitFor(
                hasSetting(settingName, changeTo).with(
                        getProfile(settings(accInfo.uid())
                                .settingsList(settingName))
                ),
                SECONDS.toMillis(5)));

        assertThat(client(), withWaitFor(
                hasSetting(settingName, changeTo).with(
                        get(settings(accInfo.uid()).settingsList(settingName))),
                SECONDS.toMillis(2)));

        assertThat(client(), withWaitFor(
                hasSetting(settingName, changeTo).with(
                        getAll(settings(accInfo.uid()).askValidator())),
                SECONDS.toMillis(1)));

        assertThat(client(), withWaitFor(
                hasSetting(settingName, changeTo).with(
                        getAllProfile(settings(accInfo.uid()).askValidator())),
                SECONDS.toMillis(1)));
    }

    private DefaultHttpClient client() {
        return new DefaultHttpClient();
    }
}
