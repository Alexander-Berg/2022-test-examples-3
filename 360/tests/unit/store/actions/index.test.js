import '../../noscript';
import {
    updateUser, updatePage, checkVersion, maybeBlockActionOverdraft
} from '../../../../components/redux/store/actions';
import { UPDATE_USER, UPDATE_PAGE } from '../../../../components/redux/store/actions/types';
const mockedDispatch = jest.fn();

import SettingsActions from '../../../../components/redux/store/actions/settings';
jest.mock('../../../../components/redux/store/actions/settings', () => ({
    updateSettings: jest.fn(),
    saveSettings: jest.fn()
}));

import DialogsActions from '../../../../components/redux/store/actions/dialogs';
jest.mock('../../../../components/redux/store/actions/dialogs', () => ({
    STATE: {
        OPENED: 'opened'
    },
    openDialog: jest.fn(),
    closeDialog: jest.fn()
}));

describe('index actions', () => {
    describe('updateUser', () => {
        const UID = '123';
        const mockedGetState = ({ uid = UID } = {}) => ({
            user: { uid }
        });

        const originalNsModelGet = ns.Model.get;
        const mockedTrigger = jest.fn();

        beforeAll(() => {
            ns.Model.get = jest.fn((modelName) => {
                switch (modelName) {
                    case 'userCurrent':
                        return { trigger: mockedTrigger };
                }
            });
        });
        beforeEach(() => {
            jest.clearAllMocks();
        });
        afterAll(() => {
            ns.Model.get = originalNsModelGet;
            jest.clearAllMocks();
        });

        it('should update store with user data, trigger userCurrent change', () => {
            const NEW_USER_DATA = { uid: UID, name: 'userName' };

            updateUser(NEW_USER_DATA)(mockedDispatch, mockedGetState);
            expect(mockedDispatch.mock.calls[0]).toEqual([{
                type: UPDATE_USER,
                payload: NEW_USER_DATA
            }]);
            expect(ns.Model.get.mock.calls).toEqual([['userCurrent']]);
            expect(mockedTrigger.mock.calls[0]).toEqual(['ns-model-changed']);
        });

        it('should update invites folder info if user uid has changed', () => {
            const NEW_USER_DATA = { uid: 'newUid', name: 'userName' };

            updateUser(NEW_USER_DATA)(mockedDispatch, mockedGetState);
            expect(ns.Model.get.mock.calls).toEqual([['userCurrent']]);
        });
    });

    describe('updatePage', () => {
        let getState;
        const dispatch = jest.fn((action) => typeof action === 'function' ? action(dispatch, getState) : action);

        const originalDateNow = Date.now;
        const originalLocationReload = window.location.reload;
        beforeEach(() => {
            SettingsActions.saveSettings.mockReset();
            Date.now = () => 1584380691799;
            window.location.reload = jest.fn();
        });
        afterEach(() => {
            jest.clearAllMocks();
            Date.now = originalDateNow;
            window.location.reload = originalLocationReload;
        });

        it('должен сохранять lastContext при переходе в последние файлы', () => {
            getState = () => ({
                page: { idContext: '/disk' },
                settings: { lastContext: '/disk' },
                dialogs: {},
                uploader: {}
            });

            updatePage({ idContext: '/recent' })(dispatch, getState);
            expect(SettingsActions.saveSettings).toBeCalledWith({ key: 'lastContext', value: '/recent' });
        });

        it('не должен сохранять lastContext при переходе из фотосреза в файлы', () => {
            getState = () => ({
                page: { idContext: '/photo' },
                settings: { lastContext: '/disk' },
                dialogs: {},
                uploader: {}
            });

            updatePage({ idContext: '/disk' })(dispatch, getState);
            expect(SettingsActions.saveSettings).toBeCalledTimes(0);
        });

        it('должен сохранять /photo в lastContext при переходе в фотосрез', () => {
            getState = () => ({
                page: { idContext: '/disk' },
                settings: { lastContext: '/disk' },
                dialogs: {},
                uploader: {}
            });

            updatePage({ idContext: '/photo' })(dispatch, getState);
            // expect(settingsModel.save).toBeCalledWith({ key: 'lastContext', value: '/photo' });
        });

        it('должен сохранять /albums в lastContext при переходе из фотосреза в альбом-срез', () => {
            getState = () => ({
                page: { idContext: '/photo' },
                settings: { lastContext: '/photo' },
                dialogs: {},
                uploader: {}
            });

            updatePage({ idContext: '/photo', filter: 'beautiful' })(dispatch, getState);
            expect(SettingsActions.saveSettings).toBeCalledWith({ key: 'lastContext', value: '/albums' });
        });

        it('должен сохранять /albums в lastContext при переходе в альбомы', () => {
            getState = () => ({
                page: { idContext: '/photo' },
                settings: { lastContext: '/photo' },
                dialogs: {},
                uploader: {}
            });

            updatePage({ idContext: '/albums' })(dispatch, getState);
            expect(SettingsActions.saveSettings).toBeCalledWith({
                key: 'lastContext',
                value: '/albums'
            });
        });

        it('не должен скрывать открытый диалог, если контекст не поменялся', () => {
            getState = () => ({
                page: { idContext: '/disk' },
                settings: { lastContext: '/disk' },
                dialogs: { selectFolder: { state: 'opened' } },
                uploader: {}
            });

            updatePage({ idContext: '/disk' })(dispatch, getState);
            expect(DialogsActions.closeDialog).not.toBeCalled();
        });

        it('должен скрыть открытый диалог при смене контекста', () => {
            getState = () => ({
                page: { idContext: '/disk' },
                settings: { lastContext: '/disk' },
                dialogs: { selectFolder: { state: 'opened' } },
                uploader: {}
            });

            updatePage({ idContext: '/recent' })(dispatch, getState);
            expect(DialogsActions.closeDialog).toBeCalledWith('selectFolder');
        });

        it('должен перезагрузить страницу если поменялся контекст и закончился таймер', () => {
            getState = () => ({
                page: { idContext: '/disk', refreshAfter: Date.now() - 1 },
                uploader: {}
            });

            updatePage({ idContext: '/disk/folder' })(dispatch, getState);
            expect(window.location.reload).toBeCalled();
            expect(dispatch).not.toBeCalled();
        });

        it('не должен перезагружать страницу если поменялся контекст, но не закончился таймер', () => {
            getState = () => ({
                page: { idContext: '/disk', refreshAfter: Date.now() + 1 },
                settings: { lastContext: '/disk' },
                dialogs: { selectFolder: {} },
                uploader: {}
            });

            updatePage({ idContext: '/disk/folder' })(dispatch, getState);
            expect(window.location.reload).not.toBeCalled();
        });

        it('не должен перезагружать страницу если поменялся контекст и закончился таймер, но есть активные загрузки', () => {
            getState = () => ({
                page: { idContext: '/disk', refreshAfter: Date.now() - 1 },
                settings: { lastContext: '/disk' },
                dialogs: { selectFolder: {} },
                uploader: { uploadingStatus: 'ENQUEUED' }
            });

            updatePage({ idContext: '/disk/folder' })(dispatch, getState);
            expect(window.location.reload).not.toBeCalled();
        });

        it('не должен перезагружать страницу если поменялся контекст и закончился таймер, но есть активные операции', () => {
            getState = () => ({
                page: { idContext: '/disk', refreshAfter: Date.now() - 1 },
                settings: { lastContext: '/disk' },
                dialogs: { selectFolder: {} },
                uploader: {}
            });
            const originalNsModelGet = ns.Model.get;
            ns.Model.get = (modelName) => modelName === 'operations' ?
                {
                    models: [{
                        isActive: () => true
                    }]
                } :
                null;

            updatePage({ idContext: '/disk/folder' })(dispatch, getState);
            expect(window.location.reload).not.toBeCalled();

            ns.Model.get = originalNsModelGet;
        });
    });

    describe('checkVersion', () => {
        const getState = (refreshAfter) => ({
            environment: {
                session: {
                    version: '1.0.0'
                }
            },
            page: {
                idContext: '/disk',
                refreshAfter
            }
        });
        const dispatch = jest.fn((action) => typeof action === 'function' ? action(dispatch, getState) : action);

        const originalDateNow = Date.now;
        beforeEach(() => {
            Date.now = () => 1584380691799;
        });
        afterEach(() => {
            jest.clearAllMocks();
            Date.now = originalDateNow;
        });

        it('должен выставить `refreshAfter` если версия поменялась', () => {
            checkVersion('1.0.1')(dispatch, getState);
            expect(dispatch).toBeCalledTimes(2);
            expect(dispatch).toBeCalledWith({
                type: UPDATE_PAGE,
                payload: {
                    idContext: '/disk',
                    refreshAfter: Date.now() + 108e5
                }
            });
        });

        it('не должен ничего делать если уже есть `refreshAfter`', () => {
            checkVersion('1.0.1')(dispatch, getState.bind(this, Date.now() - 100));
            expect(dispatch).not.toBeCalled();
        });

        it('не должен ничего делать если версия не поменялась', () => {
            checkVersion('1.0.0')(dispatch, getState);
            expect(dispatch).not.toBeCalled();
        });

        it('не должен ничего делать если версии нет о_О', () => {
            checkVersion()(dispatch, getState);
            expect(dispatch).not.toBeCalled();
        });
    });

    describe('maybeBlockActionOverdraft', () => {
        const getState = (testState = {}) => () => Object.assign({}, {
            user: {},
            environment: { agent: {} }
        }, testState);
        const dispatch = jest.fn((action) => typeof action === 'function' ? action(dispatch, getState) : action);

        it('должен ничего не делать для не-овердрафтника и вернуть false', () => {
            const returnValue = maybeBlockActionOverdraft()(dispatch, getState());
            expect(SettingsActions.updateSettings).not.toBeCalled();
            expect(DialogsActions.openDialog).not.toBeCalled();
            expect(returnValue).toEqual(false);
        });

        it('должен ничего не делать для lite-овердрафтника без параметра forLite и вернуть false', () => {
            const returnValue = maybeBlockActionOverdraft()(
                dispatch,
                getState({ user: { overdraft_status: 1 } })
            );
            expect(SettingsActions.updateSettings).not.toBeCalled();
            expect(DialogsActions.openDialog).not.toBeCalled();
            expect(returnValue).toEqual(false);
        });

        it('должен показать диалог для lite-овердрафтника с параметром forLite и вернуть true', () => {
            const returnValue = maybeBlockActionOverdraft({ forLite: true })(
                dispatch,
                getState({ user: { overdraft_status: 1 } })
            );
            expect(SettingsActions.updateSettings).not.toBeCalled();
            expect(DialogsActions.openDialog).toBeCalledWith('overdraft');
            expect(returnValue).toEqual(true);
        });

        it('должен показать диалог для hard-овердрафтника без параметров и вернуть true', () => {
            const returnValue = maybeBlockActionOverdraft()(
                dispatch,
                getState({ user: { overdraft_status: 2 } })
            );
            expect(SettingsActions.updateSettings).not.toBeCalled();
            expect(DialogsActions.openDialog).toBeCalledWith('overdraft');
            expect(returnValue).toEqual(true);
        });

        it('должен показать диалог для hard-овердрафтника с параметром forLite и вернуть true', () => {
            const returnValue = maybeBlockActionOverdraft({ forLite: true })(
                dispatch,
                getState({ user: { overdraft_status: 2 } })
            );
            expect(SettingsActions.updateSettings).not.toBeCalled();
            expect(DialogsActions.openDialog).toBeCalledWith('overdraft');
            expect(returnValue).toEqual(true);
        });

        it('должен показать экран для hard-овердрафтника на таче и вернуть true', () => {
            const returnValue = maybeBlockActionOverdraft()(
                dispatch,
                getState({ user: { overdraft_status: 2 }, environment: { agent: { isSmartphone: true } } })
            );
            expect(SettingsActions.updateSettings).toBeCalledWith({ closedOverdraftScreen: false });
            expect(returnValue).toEqual(true);
        });
    });
});
