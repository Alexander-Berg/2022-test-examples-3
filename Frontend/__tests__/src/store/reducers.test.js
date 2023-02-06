import { initStore } from 'store/index.js';
import * as actions from 'store/actions.js';

describe('reducers', () => {
    let store;

    beforeEach(() => {
        store = initStore({});
        store.dispatch({ type: '' });
    });

    describe('rpc', () => {
        it('should update rpc', () => {
            store.dispatch({ type: 'ANY_ACTION', rpc: 5 });
            expect(store.getState()).toMatchSnapshot();
        });
        it('should not update rpc', () => {
            store.dispatch({ type: 'ANY_ACTION' });
            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('config', () => {
        it('should update config', () => {
            store.dispatch(
                actions.updateConfig({
                    notificationsDatabaseId: 'notifications_database',
                    settingsDatabaseId: 'settings_database',
                    avatarUrlTemplate: 'avatar',
                })
            );
            store.dispatch(
                actions.updateConfig({
                    source: 'disk',
                })
            );

            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('filter', () => {
        it('should update filter', () => {
            store.dispatch(actions.setFilter('cpu'));
            store.dispatch(actions.setFilter('ppid'));

            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('notificationsSettingsEnable', () => {
        it('should update notificationsSettingsEnabled', () => {
            store.dispatch(actions.toggleSettings(false));

            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('services', () => {
        const services = [
            {
                name: 'Диск',
                recordId: 'disk',
                settings: {
                    fine: {
                        enabled: true,
                    },
                },
            },
            {
                name: 'Драйв',
                recordId: 'drive',
                settings: {
                    fine: {
                        enabled: false,
                    },
                },
            },
        ];

        it('should update services', () => {
            store.dispatch(actions.updateServices(services));

            expect(store.getState()).toMatchSnapshot();
        });
        it('should rewrite services', () => {
            store.dispatch(actions.updateServices(services));
            store.dispatch(actions.updateServices([{ name: 'YandexDrive' }]));

            expect(store.getState()).toMatchSnapshot();
        });
        it('should update service settings', () => {
            store.dispatch(actions.updateServices(services));

            store.dispatch(
                actions.updateSettings({
                    serviceId: 'drive',
                    serviceSettings: {
                        fine: true,
                    },
                })
            );

            store.dispatch(
                actions.updateSettings({
                    serviceId: 'drive',
                    serviceSettings: {
                        dummy: false,
                    },
                })
            );

            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('notifications', () => {
        const notifications = {
            notifications: [{ recordId: '1', mtime: 1700 }],
        };

        it('should set notifications', () => {
            store.dispatch(actions.updateNotifications(notifications));

            expect(store.getState()).toMatchSnapshot();
        });
        it('should update notification', () => {
            store.dispatch(actions.updateNotifications(notifications));
            store.dispatch(
                actions.updateNotification({ recordId: '1' }, { mtime: 1701 })
            );
            store.dispatch(
                actions.updateNotification({ recordId: '2' }, { mtime: 0 })
            );

            expect(store.getState()).toMatchSnapshot();
        });
    });
    describe('unviewed', () => {
        it('should update unviewed', () => {
            store.dispatch(actions.updateUnviewed(1));

            expect(store.getState()).toMatchSnapshot();
        });
    });
});
