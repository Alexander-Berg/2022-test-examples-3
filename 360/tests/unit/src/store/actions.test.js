import getStore from './index';
import { setUrl, syncUrl, openFileInDisk, openFileInDiskApp } from '../../../../src/store/actions';
import { OPEN_APP_DIALOG } from '../../../../src/store/action-types';

describe('actions', () => {
    it('syncUrl должен менять url', () => {
        const store = getStore({});
        store.dispatch(setUrl({
            host: 'yadi.sk',
            pathname: '/d/3wMPH8ob3KKMBa/000',
            query: {}
        }));
        store.dispatch(syncUrl());
        expect(window.location.href).toBe('https://yadi.sk/d/3wMPH8ob3KKMBa/000');
    });

    describe('openFileInDisk', () => {
        const originalWindowOpen = window.open;
        const originalWindowHistoryPushState = window.history.pushState;
        beforeEach(() => {
            window.open = jest.fn();
            window.history.pushState = jest.fn();
        });
        afterEach(() => {
            window.open = originalWindowOpen;
            window.history.pushState = originalWindowHistoryPushState;
        });

        it('should open disk client in browser', () => {
            const filePath = '/disk/Загрузки/file.ext';
            const diskOrigin = 'https://disk.yandex.ru';
            const store = getStore({
                ua: {},
                environment: {
                    experiments: {
                        flags: {}
                    }
                },
                services: {
                    disk: diskOrigin
                },
                operations: {
                    save: {
                        [filePath]: {
                            path: '/disk/Загрузки/file.ext'
                        }
                    }
                }
            });
            store.dispatch(openFileInDisk(filePath));
            expect(window.open).toBeCalledWith(
                `${diskOrigin}/client/recent?dialog=select&idDialog=${encodeURIComponent(filePath)}&skip-promo=1`,
                '_blank'
            );
        });

        it('should open open-app pane on Android', () => {
            const getState = () => ({
                ua: {
                    OSFamily: 'Android'
                },
                environment: {
                    experiments: {
                        flags: {}
                    }
                },
                url: {},
                overlays: {}
            });
            const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch, getState) : arg);
            openFileInDisk('resource-id')(dispatch, getState);
            expect(dispatch.mock.calls[0]).toEqual([{
                resourceId: 'resource-id',
                type: OPEN_APP_DIALOG
            }]);
        });

        it('should open open-app pane on iOS', () => {
            const getState = () => ({
                ua: {
                    OSFamily: 'iOS'
                },
                url: {},
                overlays: {}
            });
            const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch, getState) : arg);
            openFileInDisk('resource-id')(dispatch, getState);
            expect(dispatch.mock.calls[0]).toEqual([{
                resourceId: 'resource-id',
                type: OPEN_APP_DIALOG
            }]);
        });
    });

    describe('openFileInDiskApp', () => {
        const originalWindowOpen = window.open;
        beforeEach(() => {
            window.open = jest.fn();
        });
        afterEach(() => {
            window.open = originalWindowOpen;
        });

        const resourceId = '/disk/Загрузки/file.ext';
        const getState = (OSFamily) => () => ({
            ua: {
                OSFamily
            },
            user: {
                loginMd5: 'login-md5',
                id: '123'
            }, operations: {
                save: {
                    [resourceId]: {
                        path: resourceId,
                        name: resourceId.split('/').pop()
                    }
                }
            },
            environment: {
                iosAppMetrikaParams: {
                    appId: '18895',
                    trackId: '170707850325161498'
                }
            }
        });
        const dispatch = jest.fn();

        it('should not do anything on desktop', () => {
            ['Windows', 'MacOS', 'Linux'].forEach((osFamily) => {
                openFileInDiskApp(resourceId)(dispatch, getState(osFamily));
                expect(dispatch).not.toBeCalled();
                expect(window.open).not.toBeCalled();
            });
        });

        it('should open Android appmetrika on Android', () => {
            openFileInDiskApp(resourceId)(dispatch, getState('Android'));
            expect(window.open).toBeCalledWith(
                // eslint-disable-next-line max-len
                'https://redirect.appmetrica.yandex.com/serve/96639634120940843?user=login-md5&uid=123&path=%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8&file=file.ext',
                '_blank'
            );
        });

        it('should open iOS appmetrika on iOS', () => {
            openFileInDiskApp(resourceId)(dispatch, getState('iOS'));
            expect(window.open).toBeCalledWith(
                // eslint-disable-next-line max-len
                'https://18895.redirect.appmetrica.yandex.com/open_dir?path=%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8&file=file.ext&uid=123&appmetrica_tracking_id=170707850325161498',
                '_blank'
            );
        });
    });
});
