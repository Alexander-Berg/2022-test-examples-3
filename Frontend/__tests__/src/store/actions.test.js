import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import * as actions from 'store/actions';
import rpc from '../lib/rpc';
import * as datasync from '../lib/datasync.js';

let originFetch;

const mockStore = configureStore([thunk]);

describe('plain actions', () => {
    let store;

    const plainActions = new Map();

    plainActions.set(actions.updateConfig, [{ visible: false }]);
    plainActions.set(actions.setFilter, [{ filter: 'f' }]);

    plainActions.set(actions.toggleSettings, [
        { notificationsSettingsEnabled: true },
    ]);

    plainActions.set(actions.updateNotifications, [
        [{ name: 'Notification', recordId: '1' }],
    ]);

    plainActions.set(actions.updateNotification, [
        { recordId: '1' },
        { name: 'Test' },
    ]);

    plainActions.set(actions.updateSettings, [{ setting: true }]);

    plainActions.set(actions.updateServices, [[{ service: { name: 'Test' } }]]);

    beforeEach(() => {
        store = mockStore();
    });

    plainActions.forEach((args, action) => {
        it(`should dispatch ${action.name}`, () => {
            store.dispatch(action(...args));
            expect(store.getActions()).toMatchSnapshot();
        });
    });
});

describe('async actions', () => {
    const state = {
        config: {
            notificationsDatabaseId: 'test_notifications_db',
            serviceDatabaseId: 'test_service_db',
            settingsDatabaseId: 'test_settings_db',
            avatarUrlTemplate: 'avatar/%{login}.jpg',
            domain: 'co.uk',
        },
        filter: null,
        notifications: {
            notifications: [
                {
                    recordId: 'abc01',
                    collectionId: 'test_blocks',
                    is_read: false,
                    mtime: 1,
                    meta:
                        '{"entity":{"type":"resource","resource_type":"dir","preview":"downloader.yandex.com.tr"}}',
                    preview: 'entity',
                },
            ],
            countOfServicesWithNotifications: 1,
        },
        notificationsSettingsEnabled: true,
        rpc: rpc,
        services: [
            {
                collectionId: 'services',
                recordId: 'test',
                block_collection: 'test_blocks',
                message_collection: 'test_message',
                icon_src: '',
                settings_collection: 'test_common_settings',
                unviewed_collection: 'meta',
                unviewed_record: 'id',
                avatar_url_template: '',
                icon_type: 'svg',
                name: 'Стартрек',
                settings: {
                    call_to_ticket: {
                        text: 'Приглашение в тикет',
                        enabled: true,
                    },
                },
            },
        ],
        unviewed: 13,
    };

    const states = {
        original: state,
        filtering: Object.assign({}, state, {
            notifications: {
                notifications: [],
                countOfServicesWithNotifications: 1,
            },
            services: [
                {
                    collectionId: 'services',
                    recordId: 'test',
                    block_collection: 'test_blocks',
                    message_collection: 'test_message',
                    icon_src: '',
                    settings_collection: 'common_settings',
                    avatar_url_template: '',
                    icon_type: 'svg',
                    name: 'Стартрек',
                    settings: {
                        call_to_ticket: {
                            text: 'Приглашение в тикет',
                            enabled: true,
                        },
                    },
                },
                {
                    collectionId: 'services',
                    recordId: 'match',
                    block_collection: 'match_blocks',
                    message_collection: 'match_message',
                    icon_src: '',
                    settings_collection: 'common_settings',
                    icon_type: 'svg',
                    avatar_url_template: '',
                    name: 'Мэтч',
                    settings: {},
                },
            ],
            filter: null,
        }),
        no_unread: Object.assign({}, state, {
            unviewed: 0,
        }),
        no_unread_service: Object.assign({}, state, {
            services: [
                {
                    collectionId: 'services',
                    recordId: 'radio',
                    block_collection: 'radio_blocks',
                    message_collection: 'radio_message',
                    icon_src: '',
                    settings_collection: 'radio_settings',
                    icon_type: 'svg',
                    avatar_url_template: '',
                    name: 'Радио',
                    settings: {},
                    unviewed_collection: 'meta',
                    unviewed_record: 'radio',
                },
            ],
        }),
    };

    let stateType = 'original';

    const store = mockStore(() => {
        return states[stateType];
    });

    beforeEach(() => {
        store.clearActions();
        stateType = 'original';
        global.fetch = () => Promise.resolve();
    });

    afterEach(() => {
        global.fetch = originFetch;
    });

    // https://st.yandex-team.ru/ISL-5520
    it('should fetch services', done => {
        store.dispatch(actions.fetchServices()).then(
            () => {
                expect(rpc.getLastCall()).toMatchSnapshot();
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });

    it('should send settings and update record fields', done => {
        store.dispatch(actions.sendSettings('test', 'dummy', 5)).then(
            () => {
                expect(datasync.getLastTransaction()).toMatchSnapshot();
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });

    it('should send settings and insert record fields', done => {
        datasync.deleteSettingsField();
        store.dispatch(actions.sendSettings('test', 'dummy', 5)).then(
            () => {
                expect(datasync.getLastTransaction()).toMatchSnapshot();
                expect(store.getActions()).toMatchSnapshot();
                datasync.restoreSettingsField();
                done();
            },
            error => {
                datasync.restoreSettingsField();
                done(error);
            }
        );
    });

    it('should dispatch update unviewed counter', () => {
        store.dispatch(actions.updateUnviewed(0));
        expect(rpc.getLastCall()).toMatchSnapshot();
        expect(store.getActions()).toMatchSnapshot();
    });

    it('should reset unviewed counter', done => {
        store.dispatch(actions.resetUnviewed()).then(
            () => {
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });

    // https://st.yandex-team.ru/ISL-5520
    it('should not reset unviewed counter because unread counter is zeroed', done => {
        stateType = 'no_unread';

        store.dispatch(actions.resetUnviewed()).then(
            () => {
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });

    it('should not reset unviewed counter because service has no counter record', done => {
        stateType = 'no_unread_service';

        store.dispatch(actions.resetUnviewed()).then(
            () => {
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });

    it('should change filter to `test` and show all `test` notifications', () => {
        store.dispatch(actions.changeFilter('test'));
        expect(store.getActions()).toMatchSnapshot();
    });

    it('should change filter and show all `match` notifications', () => {
        stateType = 'filtering';
        store.dispatch(actions.changeFilter('match'));
        expect(store.getActions()).toMatchSnapshot();
    });

    it('should set block as read', done => {
        store
            .dispatch(
                actions.setAsRead({
                    recordId: 'abc01',
                    collectionId: 'test_blocks',
                })
            )
            .then(
                () => {
                    expect(datasync.getLastTransaction()).toMatchSnapshot();
                    expect(store.getActions()).toMatchSnapshot();
                    done();
                },
                error => {
                    done(error);
                }
            );
    });

    it('should set all blocks as read', done => {
        store.dispatch(actions.setAllAsRead()).then(
            () => {
                expect(store.getActions()).toMatchSnapshot();
                done();
            },
            error => {
                done(error);
            }
        );
    });
});
