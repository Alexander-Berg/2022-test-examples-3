package ru.yandex.autotests.innerpochta.rules;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.MailConst.COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.DOMAIN_YANDEXRU;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.*;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.*;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * @author oleshko
 */
public class ClearAccRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;
    private String[] folderNames = new String[]{"inbox", "sent", "draft", "spam", "trash", "outbox", "reply_later"};

    private ClearAccRule(Producer<AllureStepStorage> producer) {
        this.producer = producer;
    }

    public static ClearAccRule clearAcc(Producer<AllureStepStorage> producer) {
        return new ClearAccRule(producer);
    }

    @Override
    @Step("Очищаем аккаунт, если он из TUS")
    protected void before() throws Throwable {
        if (AccLockRule.useTusAccount) {
            Map<String, Boolean> touchSettings = new HashMap<>();
            touchSettings.put(SMART_REPLIES_TOUCH, TRUE);

            Map<String, java.io.Serializable> commonSettings = new HashMap<>();
            commonSettings.put(SETTINGS_PARAM_MESSAGE_AVATARS, TRUE);
            commonSettings.put(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, TRUE);
            commonSettings.put(SETTINGS_ENABLE_AUTOSAVE, STATUS_ON);
            commonSettings.put(SETTINGS_PARAM_ENABLE_FIRSTLINE, TRUE);
            commonSettings.put(SETTINGS_ENABLE_QUOTING, STATUS_ON);
            commonSettings.put(SETTINGS_SHOW_WIDGETS_DECOR, TRUE);
            commonSettings.put(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE);
            commonSettings.put(SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_DONE);
            commonSettings.put(SETTINGS_PARAM_PAGE_AFTER_MOVE, SETTINGS_PARAM_CURRENT_LIST);
            commonSettings.put(SETTINGS_PARAM_ENABLE_IMAP, FALSE);
            commonSettings.put(SETTINGS_PARAM_DISABLE_AUTOEXPUNGE, EMPTY_STR);
            commonSettings.put(SETTINGS_PARAM_TRANSLATE, STATUS_ON);
            commonSettings.put(COLOR_SCHEME, COLOR_SCHEME_COLORFUL);
            commonSettings.put(SETTINGS_PARAM_MESSAGES_PER_PAGE, "30");
            commonSettings.put(SETTINGS_PARAM_DISABLE_YABBLES, EMPTY_STR);
            commonSettings.put(NOTIFY_MESSAGE, STATUS_ON);
            commonSettings.put(SETTINGS_PARAM_NO_REPLY_NOTIFY, FALSE);
            commonSettings.put(SETTINGS_ENABLE_RICHEDIT, TRUE);
            commonSettings.put(SETTINGS_STORED_COMPOSE_STATES, UNDEFINED);
            commonSettings.put(COMPOSE_KUKUTZ_PROMO, TRUE);
            commonSettings.put(SETTINGS_SIZE_VIEW_APP, DEFAULT_SIZE_VIEW_APP);
            commonSettings.put(SETTINGS_SIZE_VIEW_APP2, 0);
            commonSettings.put(SIZE_LAYOUT_LEFT, DEFAULT_SIZE_LAYOUT_LEFT);
            commonSettings.put(SETTINGS_OPEN_MSG_LIST, EMPTY_STR);
            commonSettings.put(HIDE_EMPTY_FOLDERS, FALSE);
            commonSettings.put(REACT_COMPOSE_DISABLE, FALSE);
            commonSettings.put(LIZA_MINIFIED_HEADER, EMPTY_STR);
            commonSettings.put(LIZA_MINIFIED, EMPTY_STR);
            commonSettings.put(SETTINGS_HEAD_FULL_EDITION, FALSE);
            commonSettings.put(DONT_SAVE_HISTORY, FALSE);
            commonSettings.put(SETTINGS_RIGHT_COLUMN_EXPANDED, STATUS_ON);
            commonSettings.put(TIMELINE_COLLAPSE, TRUE);
            commonSettings.put(SETTINGS_CONTEXTMENU_DISABLE, FALSE);
            commonSettings.put(BROWSER_NOTIFY_MESSAGE, STATUS_ON);
            commonSettings.put(WITH_BIGGER_TEXT, EMPTY_STR);
            commonSettings.put(SETTINGS_DND, TRUE);
            commonSettings.put(SHOW_FILTER_NOTIFICATION, EMPTY_STR);
            commonSettings.put(CUSTOM_BUTTONS, EMPTY_STR);
            commonSettings.put(FOLDERS_OPEN, EMPTY_STR);
            commonSettings.put(FORCE_REPLY, TRUE);
            commonSettings.put(SETTINGS_SENDER_CHANGE_SIGN_IN_COMPOSE, EMPTY_STR);
            commonSettings.put(SETTINGS_GEO_ID, EMPTY_STR);
            commonSettings.put(SETTINGS_PARAM_COLLECT_ADRESSES, TRUE);
            commonSettings.put(SETTINGS_PARAM_DISABLE_EVENTS, FALSE);
            commonSettings.put(SETTINGS_COLORFUL_SKIN, "blue");
            commonSettings.put(ENABLE_POP, FALSE);
            commonSettings.put(SETTINGS_USER_NAME, "Default-Имя Default Фамилия");
            commonSettings.put(PROMO_UNSUBSCRIBE_POPUP, "1592307778306");
            commonSettings.put(SHOW_ADVERTISEMENT, DISABLED_ADV);
            commonSettings.put(DISABLE_PROMO, STATUS_TRUE);
            commonSettings.put(OLD_COMPOSE_BETA_PROMO, TRUE);
            commonSettings.put(COMPOSE_BETA_PROMO, STATUS_ON);
            commonSettings.put(COMPOSE_AUTOCOMPLETE_PROMO, STATUS_ON);
            commonSettings.put(SETTINGS_FOLDER_THREAD_VIEW, TRUE);
            commonSettings.put(SETTINGS_DISABLE_INBOXATTACHS, FALSE);
            commonSettings.put(FOLDER_TABS, FALSE);
            commonSettings.put(SETTINGS_SAVE_SENT, TRUE);
            commonSettings.put(SETTINGS_PARAM_ENABLE_HOTKEYS, TRUE);
            commonSettings.put(LAST_USED_COMPOSE_SIZE, COMPOSE_LARGE);
            commonSettings.put(NO_POPUP_MARK_READ, FALSE);
            commonSettings.put(MAIL_360_ONBOARDING, "off");
            commonSettings.put(PROMO_NEWYEAR, "1639526400000");
            commonSettings.put(SIGNATURE_TOP, FALSE);
            commonSettings.put(TOUCH_ONBOARDING, STATUS_ON);
            commonSettings.put(HIDDEN_TRASH_ENABLED, FALSE);
            commonSettings.put(IS_SIDEBAR_EXPANDED, STATUS_NO);
            commonSettings.put(OPT_IN, EMPTY_STR);
            commonSettings.put(SHOW_WIDGETS, TRUE);
            AllureStepStorage user = producer.call();
            if (!urlProps().getProject().equals("cal")) {
                user.apiFoldersSteps().deleteAllDefaultFoldersExceptInitial()
                    .deleteAllCustomFolders();
                for (String folderName : folderNames) {
                    Folder folder = user.apiFoldersSteps().getFolderBySymbol(folderName);
                    if (folder != null) {
                        user.apiFoldersSteps().purgeFolder(folder);
                    }
                }
                user.apiLabelsSteps().deleteAllCustomLabels();
                user.apiAbookSteps().removeAllAbookGroups()
                    .removeAllAbookContacts();
                if (AccLockRule.tags == null || !Arrays.asList(AccLockRule.tags).contains(COLLECTOR)) {
                    user.apiCollectorSteps().removeAllUserCollectors();
                }
                user.apiFiltersSteps().removeAllEmailsFromBlackList()
                    .removeAllEmailsFromWhiteList()
                    .deleteAllUserFilters();
                user.apiTodoSteps().hideTodoList();
                user.apiBackupSteps().deleteBackup()
                    .purgeHiddenTrash();
                user.apiSettingsSteps().changeSignsAmountTo(CLEAR_SIGNS_AMOUNT)
                    .callWithListAndParams(commonSettings);
                if (AccLockRule.tags == null || !Arrays.asList(AccLockRule.tags).contains(PDD_USER_TAG)) {
                    user.apiSettingsSteps().callWith(
                        of(
                            SETTINGS_PARAM_DEFAULT_EMAIL,
                            user.apiSettingsSteps().auth.getLogin() + DOMAIN_YANDEXRU
                        )
                    );
                }

                if (urlProps().getProject().equals("touch")) {
                    user.apiSettingsSteps().callWithListAndParams(touchSettings);
                }
            } else {
                user.apiCalSettingsSteps().deleteLayers()
                    .deleteAllTodoLists()
                    .createNewLayer(user.settingsCalSteps().formDefaultLayer())
                    .updateUserSettings(
                        "Выставляем дефолтные настройки",
                        new Params()
                            .withLastOfferedGeoTz("Europe/Moscow")
                            .withTz("Europe/Moscow")
                            .withDefaultView("week")
                            .withIsAlldayExpanded(true)
                            .withIsCalendarsListExpanded(true)
                            .withIsSubscriptionsListExpanded(true)
                            .withWeekStartDay(1L)
                            .withShowTodosInGrid(true)
                            .withDayStartHour(11L)
                            .withShowWeekNumber(true)
                            .withShowAvailabilityToAnyone(true)
                            .withShowWeekends(true)
                            .withIsAsideExpanded(true)
                            .withTouchViewMode("GRID")
                            .withAutoAcceptEventInvitations(false)
                    );
            }
        }
    }
}
