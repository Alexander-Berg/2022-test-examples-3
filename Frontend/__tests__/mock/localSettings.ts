import { EXTERNAL_ORGANIZATION_ID } from '../../../../shared/lib/constants';
import { LocalSettingsState } from '../../localSettings';

export function localSettingsMockFactory() {
    return {
        createState: (state: Partial<LocalSettingsState> = {}) => ({
            sendByEnter: true,
            enableBrowserPush: 0,
            enableWebPush: false,
            enableNotifications: false,
            enableNotificationSound: false,
            enableNotificationText: false,
            enableAlicePin: false,
            notificationPopupShown: false,
            muteNotificationSound: false,
            recommendedChats: true,
            recommendedContacts: true,
            videoInput: undefined,
            audioInput: undefined,
            audioOutput: undefined,
            enableMobileAppSuggest: true,
            startrekSidebar: false,
            theme: 'light' as Client.Themes,
            orgId: EXTERNAL_ORGANIZATION_ID,
            ...state,
        }),
    };
}
