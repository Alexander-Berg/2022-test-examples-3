jest.mock('../../../../components/helpers/operation', () => ({
    create: jest.fn()
}));

import '../../noscript';
import _ from 'lodash';
import createStore from '../../../../components/redux/store/create-store';
import * as settingsActions from '../../../../components/redux/store/actions/settings';
import * as resourcesActions from '../../../../components/redux/store/actions/resources';
import * as selectionActions from '../../../../components/redux/store/actions/selection';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import * as dialogsActions from '../../../../components/redux/store/actions/dialogs';
import * as notificationsActions from '../../../../components/redux/store/actions/notifications';
import * as confirmationQueueActions from '../../../../components/redux/store/actions/confirmation-queue';
import pageHelper from '../../../../components/helpers/page';
import albumHelper from '../../../../components/helpers/album';
import { create } from '../../../../components/helpers/operation';

import {
    excludeFromAlbum,
    excludeFromPersonalAlbum,
    deleteAlbum,
    requestNewAlbumTitle,
    createAlbum, ALBUMS_SCROLL
} from '../../../../components/redux/store/actions/album';

describe('album actions', () => {
    describe('excludeFromAlbum', () => {
        const createTestStore = (state) => (createStore(_.merge({
            page: {
                idContext: '/photo'
            },
            settings: {
                confirmExcludeFromAlbumShowed: '0'
            },
            resources: {
                '/disk/one': {
                    id: '/disk/one'
                }
            },
            environment: { session: { experiment: {} }, agent: {} }
        }, state)));

        const originalSaveSettings = settingsActions.saveSettings;
        const originalDeselectAllStatesContext = selectionActions.deselectAllStatesContext;
        const originalExcludeResourceFromAlbum = resourcesActions.excludeResourceFromAlbum;

        beforeEach(() => {
            jest.resetAllMocks();

            settingsActions.saveSettings = jest.fn(() => ({ type: 'MOCK' }));
            selectionActions.deselectAllStatesContext = jest.fn(() => ({ type: 'MOCK' }));
            resourcesActions.excludeResourceFromAlbum = jest.fn(() => ({ type: 'MOCK' }));
        });

        afterEach(() => {
            selectionActions.deselectAllStatesContext = originalDeselectAllStatesContext;
            resourcesActions.excludeResourceFromAlbum = originalExcludeResourceFromAlbum;
            settingsActions.saveSettings = originalSaveSettings;
        });

        it('показ предупреждения при исключении фото из альбома первый раз', async() => {
            const store = createTestStore({ page: { filter: 'beautiful' } });
            confirmationQueueActions.add = jest.fn((type, { onSubmit }) => onSubmit());

            await store.dispatch(excludeFromAlbum('/disk/one'));

            expect(settingsActions.saveSettings).toBeCalledWith({ key: 'confirmExcludeFromAlbumShowed', value: '1' });
        });

        it('исключение 1 фото', async() => {
            const store = createTestStore({
                page: { filter: 'beautiful', dialog: 'slider' },
                settings: { confirmExcludeFromAlbumShowed: '1' }
            });
            await store.dispatch(excludeFromAlbum('/disk/one'));

            expect(selectionActions.deselectAllStatesContext).toBeCalled();
            expect(resourcesActions.excludeResourceFromAlbum).toBeCalled();
            expect(create).toHaveBeenCalledWith('excludeFromAlbum', [{
                idSrc: '/disk/one',
                albumType: 'beautiful',
                srcContextId: '/photo'
            }]);
        });

        it('исключение нескольких фото, среди которых есть те, которые исключать нельзя', async() => {
            const store = createTestStore({
                page: { filter: 'beautiful', dialog: 'slider' },
                settings: { confirmExcludeFromAlbumShowed: '1' },
                resources: {
                    '/disk/two': {
                        id: '/disk/two',
                        meta: {
                            group: {
                                is_owner: false
                            }
                        }
                    }
                }
            });
            await store.dispatch(excludeFromAlbum(['/disk/one', '/disk/two']));
            expect(create).toHaveBeenCalledWith('excludeFromAlbum', [{
                idSrc: '/disk/one',
                albumType: 'beautiful',
                srcContextId: '/photo'
            }]);
        });
    });

    describe('excludeFromPersonalAlbum', () => {
        const createTestStore = (state) => (createStore(_.merge({
            resources: {
                '/disk/one': {
                    id: '/disk/one'
                },
                '/disk/two': {
                    id: '/disk/two'
                }
            },
            environment: { session: { experiment: {} }, agent: {} },
            personalAlbums: {
                albumsByIds: {
                    a1: {
                        id: 'a1',
                        coverResourceId: '/disk/two',
                        clusters: [
                            {
                                id: 'c1',
                                items: [
                                    { itemId: 'i11', id: '/disk/one' },
                                    { itemId: 'i12', id: '/disk/two' }
                                ]
                            }
                        ]
                    }
                }
            }
        }, state)));

        it('исключение 1 фото', async() => {
            const store = createTestStore({
                page: { albumId: 'a1' }
            });
            await store.dispatch(excludeFromPersonalAlbum('/disk/one'));

            expect(create).toHaveBeenCalledWith('removeResourceAlbum', [{
                itemId: 'i11', resourceId: '/disk/one', albumId: 'a1'
            }]);
        });
    });

    describe('deleteAlbum', () => {
        let originalOpenDialog;
        let originalCloseDialog;
        let originalAddConfirmation;
        let originalNotifyNotification;
        let originalHelperPageGo;

        beforeEach(() => {
            originalOpenDialog = dialogsActions.openDialog;
            originalCloseDialog = dialogsActions.closeDialog;
            originalAddConfirmation = confirmationQueueActions.add;
            originalNotifyNotification = notificationsActions.notify;
            originalHelperPageGo = pageHelper.go;

            ns.page.current.params = {};
        });
        afterEach(() => {
            dialogsActions.openDialog = originalOpenDialog;
            dialogsActions.closeDialog = originalCloseDialog;
            confirmationQueueActions.add = originalAddConfirmation;
            notificationsActions.notify = originalNotifyNotification;
            pageHelper.go = originalHelperPageGo;

            delete ns.page.current.params;
        });

        it('удаление альбома - успешное', async(done) => {
            const albumId = 'a1';
            const albumTitle = 'Album';
            const store = createStore({
                personalAlbums: {
                    ids: [albumId],
                    albumsByIds: {
                        [albumId]: {
                            id: albumId,
                            title: albumTitle
                        }
                    }
                }
            });

            pageHelper.go = jest.fn();
            rawFetchModel.default = () => Promise.resolve({});

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            confirmationQueueActions.add = jest.fn(() => ({ type: 'MOCK' }));
            notificationsActions.notify = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(deleteAlbum(albumId));

            const [, { onSubmit }] = _.last(confirmationQueueActions.add.mock.calls);

            onSubmit().then(() => {
                const { personalAlbums: { albumsByIds } } = store.getState();
                expect(albumsByIds[albumId]).toBe(undefined);

                const [{ operation, status, title }] = _.last(notificationsActions.notify.mock.calls);
                expect(operation).toBe('removeAlbum');
                expect(status).toBe('done');
                expect(title).toBe(albumTitle);

                expect(pageHelper.go.mock.calls).toContainEqual(['/client/albums']);
                done();
            });
        });

        it('удаление альбома - ошибка', async(done) => {
            const albumId = 'a1';
            const albumTitle = 'Album';
            const store = createStore({
                personalAlbums: {
                    ids: [albumId],
                    albumsByIds: {
                        [albumId]: {
                            id: albumId,
                            title: albumTitle
                        }
                    }
                }
            });

            pageHelper.go = jest.fn();
            rawFetchModel.default = () => Promise.reject({});

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            confirmationQueueActions.add = jest.fn(() => ({ type: 'MOCK' }));
            notificationsActions.notify = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(deleteAlbum(albumId));

            const [, { onSubmit }] = _.last(confirmationQueueActions.add.mock.calls);

            onSubmit().then(() => {
                const { personalAlbums: { albumsByIds } } = store.getState();
                expect(albumsByIds[albumId]).not.toBe(undefined);

                const [{ operation, status, title }] = _.last(notificationsActions.notify.mock.calls);
                expect(operation).toBe('removeAlbum');
                expect(status).toBe('failed');
                expect(title).toBe(albumTitle);

                expect(pageHelper.go.mock.calls).toEqual([]);
                done();
            });
        });

        it('удаление альбома-лица - успешное', async(done) => {
            const albumFaceId = 'albumFace';
            const albumId = 'a1';
            const albumTitle = 'Album';
            const store = createStore({
                user: { faces_indexing_state: 'reindexed' },
                albums: {
                    albums: {
                        faces: {
                            id: 'faces',
                            previewAlbumId: albumId
                        }
                    }
                },
                personalAlbums: {
                    ids: [albumId],
                    albumsByIds: {
                        [albumId]: {
                            id: albumId,
                            title: albumTitle,
                            album_type: 'faces'
                        }
                    }
                }
            });

            pageHelper.go = jest.fn();
            rawFetchModel.default = jest.fn((modelName) => {
                if (modelName === 'do-remove-album') {
                    return Promise.resolve({});
                } else if (modelName === 'getAlbumFaces') {
                    return Promise.resolve([{
                        id: albumFaceId,
                        cover: { object: { id: 'coverResourceId', meta: { sizes: [] } } }
                    }]);
                }
            });

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            confirmationQueueActions.add = jest.fn(() => ({ type: 'MOCK' }));
            notificationsActions.notify = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(deleteAlbum(albumId));

            const [, { onSubmit }] = _.last(confirmationQueueActions.add.mock.calls);

            onSubmit().then(() => {
                const { personalAlbums: { albumsByIds }, albums: { albums: { faces } } } = store.getState();
                expect(albumsByIds[albumId]).toBe(undefined);
                expect(faces.previewAlbumId).toBe(albumFaceId);

                const [{ operation, status }] = _.last(notificationsActions.notify.mock.calls);
                expect(operation).toBe('removeAlbum');
                expect(status).toBe('done');

                expect(pageHelper.go.mock.calls).toContainEqual(['/client/albums/faces']);
                done();
            });
        });
    });

    describe('requestNewAlbumTitle', () => {
        let originalOpenDialog;
        let originalCloseDialog;
        let originalHelperPageGo;

        const createTestStore = (state) => (createStore(_.merge({
            environment: { session: { experiment: {} }, agent: {} }
        }, state)));

        beforeEach(() => {
            originalOpenDialog = dialogsActions.openDialog;
            originalCloseDialog = dialogsActions.closeDialog;
            originalHelperPageGo = pageHelper.go;

            ns.page.current.params = {};
        });
        afterEach(() => {
            dialogsActions.openDialog = originalOpenDialog;
            dialogsActions.closeDialog = originalCloseDialog;
            pageHelper.go = originalHelperPageGo;

            delete ns.page.current.params;
        });
        it('should open album title dialog', () => {
            const store = createTestStore({});

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(requestNewAlbumTitle());

            expect(_.last(dialogsActions.openDialog.mock.calls)[0]).toBe('album-title');
        });
        it('should close album title dialog on cancel', () => {
            const store = createTestStore({});

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            confirmationQueueActions.decideSelectAlbum = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(requestNewAlbumTitle());

            const [openDialogName, { onClose }] = _.last(dialogsActions.openDialog.mock.calls);

            expect(openDialogName).toBe('album-title');

            onClose();

            const [closeDialogName] = _.last(dialogsActions.closeDialog.mock.calls);

            expect(closeDialogName).toBe('album-title');
            expect(confirmationQueueActions.decideSelectAlbum).toBeCalledTimes(0);
        });
        it('should set new album title and go to to photo selection', () => {
            const store = createTestStore({});

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            pageHelper.go = jest.fn();

            store.dispatch(requestNewAlbumTitle());

            const [openDialogName, { onSubmit }] = _.last(dialogsActions.openDialog.mock.calls);

            expect(openDialogName).toBe('album-title');

            const newTitle = 'my new album';

            onSubmit(newTitle);

            expect(store.getState().personalAlbums.newAlbumTitle).toBe(newTitle);
            expect(dialogsActions.closeDialog).toHaveBeenCalledWith('album-title');
            expect(pageHelper.go).toHaveBeenCalledWith('/client/photo|create-album');
        });
    });

    describe('createAlbum', () => {
        let notifySpy;
        let openDialogSpy;
        let closeDialogSpy;
        let originalNSModelGet;
        let originalPageHelperGo;
        let originalPreprocessId;
        let originalRawFetchModel;
        let originalNSModelGetValid;

        const makeControlledPromise = (value) => {
            const handler = { resolve: _.noop, reject: _.noop };

            const promise = new Promise((resolve, reject) => {
                handler.resolve = () => resolve(value);
                handler.reject = () => reject(value);
            });

            return Object.assign(handler, { promise });
        };

        beforeEach(() => {
            ns.page.current.params = {};

            originalRawFetchModel = rawFetchModel.default;
            originalPageHelperGo = pageHelper.go;
            originalNSModelGetValid = ns.Model.getValid;
            originalNSModelGet = ns.Model.get;
            originalPreprocessId = albumHelper.preprocessId;
        });
        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
            pageHelper.go = originalPageHelperGo;
            albumHelper.preprocessId = originalPreprocessId;
            ns.Model.getValid = originalNSModelGetValid;
            ns.Model.get = originalNSModelGet;

            if (notifySpy) {
                notifySpy.mockRestore();
                notifySpy = null;
            }
            if (openDialogSpy) {
                openDialogSpy.mockRestore();
                openDialogSpy = null;
            }
            if (closeDialogSpy) {
                closeDialogSpy.mockRestore();
                closeDialogSpy = null;
            }
        });
        it('should create album if title is passed and not redirect to albums', (done) => {
            const state = {
                page: { idContext: '/disk', originalNSParams: {} },
                environment: { session: { experiment: {} }, agent: {} }
            };

            const store = createStore(state);
            const id = 'my_new_album';
            const title = 'my new album';
            const pic = '/disk/picture.jpg';
            const { resolve, promise } = makeControlledPromise({ id, title, public: { short_url: 'url' } });

            notifySpy = jest.spyOn(notificationsActions, 'notify');
            openDialogSpy = jest.spyOn(dialogsActions, 'openDialog');
            closeDialogSpy = jest.spyOn(dialogsActions, 'closeDialog');
            rawFetchModel.default = jest.fn().mockReturnValue(promise);
            selectionActions.deselectAllStatesContext = jest.fn(() => ({ type: 'MOCK' }));
            pageHelper.go = jest.fn();

            store.dispatch(createAlbum([pic], title)).then(() => {
                const [{ operation, status, url, name }] = _.last(notifySpy.mock.calls);
                const { personalAlbums: { creatingAlbumsCount } } = store.getState();

                expect(creatingAlbumsCount).toBe(0);
                expect(operation).toBe('createAlbum');
                expect(status).toBe('success');
                expect(url).toBe(`/client/albums/${id}`);
                expect(name).toBe(title);

                done();
            });

            const { personalAlbums: { scrollInAlbums, creatingAlbumsCount } } = store.getState();

            expect(scrollInAlbums).toBe(null);
            expect(creatingAlbumsCount).toBe(1);
            expect(_.flatten(closeDialogSpy.mock.calls)).toEqual(['album-title']);
            expect(pageHelper.go).not.toHaveBeenCalled();
            expect(selectionActions.deselectAllStatesContext).not.toHaveBeenCalled();

            expect(rawFetchModel.default).toBeCalledWith(
                'do-create-album',
                { title, noItems: '1', isPublic: 'false', idsResources: JSON.stringify([pic]) },
                { retriesLeft: 0, retryOffline: false }
            );

            resolve();
        });
        it('should create album if title is passed and redirect to albums', (done) => {
            const state = {
                page: { idContext: '/photo', originalNSParams: { action: 'create-album' } },
                environment: { session: { experiment: {} }, agent: {} }
            };

            const store = createStore(state);
            const id = 'my_new_album';
            const title = 'my new album';
            const pic = '/disk/picture.jpg';
            const { resolve, promise } = makeControlledPromise({ id, title, public: { short_url: 'url' } });

            notifySpy = jest.spyOn(notificationsActions, 'notify');
            openDialogSpy = jest.spyOn(dialogsActions, 'openDialog');
            closeDialogSpy = jest.spyOn(dialogsActions, 'closeDialog');
            rawFetchModel.default = jest.fn().mockReturnValue(promise);
            selectionActions.deselectAllStatesContext = jest.fn(() => ({ type: 'MOCK' }));
            pageHelper.go = jest.fn();

            store.dispatch(createAlbum([pic], title)).then(() => {
                const [{ operation, status, url, name }] = _.last(notifySpy.mock.calls);
                const { personalAlbums: { creatingAlbumsCount } } = store.getState();

                expect(creatingAlbumsCount).toBe(0);
                expect(operation).toBe('createAlbum');
                expect(status).toBe('success');
                expect(url).toBe(`/client/albums/${id}`);
                expect(name).toBe(title);

                done();
            });

            const { personalAlbums: { scrollInAlbums, creatingAlbumsCount } } = store.getState();

            expect(scrollInAlbums).toBe(ALBUMS_SCROLL.RESTORE);
            expect(creatingAlbumsCount).toBe(1);
            expect(_.flatten(closeDialogSpy.mock.calls)).toEqual(['album-title']);
            expect(pageHelper.go).toHaveBeenCalledWith('/client/albums');
            expect(selectionActions.deselectAllStatesContext).toHaveBeenCalled();

            expect(rawFetchModel.default).toBeCalledWith(
                'do-create-album',
                { title, noItems: '1', isPublic: 'false', idsResources: JSON.stringify([pic]) },
                { retriesLeft: 0, retryOffline: false }
            );

            resolve();
        });
        it('should not create album', (done) => {
            const state = {
                page: { idContext: '/photo', originalNSParams: { action: 'create-album' } },
                environment: { session: { experiment: {} }, agent: {} }
            };

            const store = createStore(state);
            const id = 'my_new_album';
            const title = 'my new album';
            const pic = '/disk/picture.jpg';
            const { reject, promise } = makeControlledPromise({ id, title, public: { short_url: 'url' } });

            notifySpy = jest.spyOn(notificationsActions, 'notify');
            openDialogSpy = jest.spyOn(dialogsActions, 'openDialog');
            closeDialogSpy = jest.spyOn(dialogsActions, 'closeDialog');
            rawFetchModel.default = jest.fn().mockReturnValue(promise);
            pageHelper.go = jest.fn();

            store.dispatch(createAlbum([pic], title)).then(() => {
                const [{ operation, status }] = _.last(notifySpy.mock.calls);
                const { personalAlbums: { creatingAlbumsCount } } = store.getState();

                expect(creatingAlbumsCount).toBe(0);
                expect(operation).toBe('createAlbum');
                expect(status).toBe('failed');

                done();
            });

            const { personalAlbums: { scrollInAlbums, creatingAlbumsCount } } = store.getState();

            expect(scrollInAlbums).toBe(ALBUMS_SCROLL.RESTORE);
            expect(creatingAlbumsCount).toBe(1);
            expect(_.flatten(closeDialogSpy.mock.calls)).toEqual(['album-title']);
            expect(pageHelper.go).toHaveBeenCalledWith('/client/albums');

            expect(rawFetchModel.default).toBeCalledWith(
                'do-create-album',
                { title, noItems: '1', isPublic: 'false', idsResources: JSON.stringify([pic]) },
                { retriesLeft: 0, retryOffline: false }
            );

            reject();
        });
        it('should display album title dialog if title is not passed', (done) => {
            const state = {
                page: { idContext: '/disk', originalNSParams: {} },
                environment: { session: { experiment: {} }, agent: {} }
            };

            const store = createStore(state);
            const id = 'my_new_album';
            const title = 'my new album';
            const pic = '/disk/picture.jpg';
            const { resolve, promise } = makeControlledPromise({ id, title, public: { short_url: 'url' } });

            notifySpy = jest.spyOn(notificationsActions, 'notify');
            openDialogSpy = jest.spyOn(dialogsActions, 'openDialog');
            closeDialogSpy = jest.spyOn(dialogsActions, 'closeDialog');
            rawFetchModel.default = jest.fn().mockReturnValue(promise);
            pageHelper.go = jest.fn();

            store.dispatch(createAlbum([pic])).then(() => {
                expect(openDialogSpy).toBeCalled();

                const [, { onSubmit }] = _.last(openDialogSpy.mock.calls);

                onSubmit(title).then(() => {
                    const [{ operation, status, url, name }] = _.last(notifySpy.mock.calls);
                    const { personalAlbums: { creatingAlbumsCount } } = store.getState();

                    expect(creatingAlbumsCount).toBe(0);
                    expect(operation).toBe('createAlbum');
                    expect(status).toBe('success');
                    expect(url).toBe(`/client/albums/${id}`);
                    expect(name).toBe(title);

                    done();
                });

                const { personalAlbums: { scrollInAlbums, creatingAlbumsCount } } = store.getState();

                expect(scrollInAlbums).toBe(null);
                expect(creatingAlbumsCount).toBe(1);
                expect(_.flatten(closeDialogSpy.mock.calls)).toEqual(['album-title']);
                expect(pageHelper.go).not.toHaveBeenCalled();

                expect(rawFetchModel.default).toBeCalledWith(
                    'do-create-album',
                    { title, noItems: '1', isPublic: 'false', idsResources: JSON.stringify([pic]) },
                    { retriesLeft: 0, retryOffline: false }
                );

                resolve();
            });
        });
    });
});
