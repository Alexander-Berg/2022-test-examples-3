import '../../noscript';
import '../../../../components/models/user-current/user-current';

import _ from 'lodash';
import createStore from '../../../../components/redux/store/create-store';
import { fetchAlbums, refuseAlbumsFaces } from '../../../../components/redux/store/actions/albums';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import getStore from '../../../../components/redux/store';
import { combineReducers } from 'redux';
import albums from '../../../../components/redux/store/reducers/albums';
import commonReducers from '../../../../components/redux/store/reducers';
import pageHelper from '../../../../components/helpers/page';
import {
    UPDATE_ALBUMS, FETCH_ALBUMS_SUCCESS, FETCH_SINGLE_GEO_ALBUM, FETCH_SINGLE_FACES_ALBUM
} from '../../../../components/redux/store/actions/albums-types';

import * as dialogsActions from '../../../../components/redux/store/actions/dialogs';
import * as confirmationQueueActions from '../../../../components/redux/store/actions/confirmation-queue';
import * as notificationsActions from '../../../../components/redux/store/actions/notifications';

jest.mock('../../../../components/helpers/page', () => ({}));

describe('albums actions', () => {
    describe('fetchAlbums', () => {
        const originalRawFetchModel = rawFetchModel.default;
        const getState = () => {
            return {
                user: { faces_indexing_state: 'reindexed' }
            };
        };

        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
        });

        it('не должен ставить isLoading если передан флаг silent', (done) => {
            rawFetchModel.default = () => Promise.resolve({ albums: {} });
            const store = getStore(true);
            store.replaceReducer(combineReducers(Object.assign({ albums }, commonReducers)));

            store.dispatch(fetchAlbums()).then(() => {
                expect(store.getState().albums.isLoading).toBe(false);

                store.dispatch(fetchAlbums({ silent: true }));
                expect(store.getState().albums.isLoading).toBe(false);
                done();
            });
            expect(store.getState().albums.isLoading).toBe(true);
        });

        it('должен получить альбомы', (done) => {
            const dispatch = jest.fn();

            rawFetchModel.default = () => Promise.resolve({ albums: {} });

            fetchAlbums()(dispatch, getState).then(() => {
                expect(dispatch.mock.calls).toContainEqual([{ type: UPDATE_ALBUMS, payload: { isLoading: true } }]);
                expect(dispatch.mock.calls).toContainEqual([{ type: FETCH_ALBUMS_SUCCESS, payload: { albums: {} } }]);
                expect(dispatch.mock.calls).toContainEqual([{ type: FETCH_SINGLE_GEO_ALBUM, payload: { albums: {} } }]);
                expect(dispatch.mock.calls).toContainEqual([{ type: FETCH_SINGLE_FACES_ALBUM, payload: {
                    album: { albums: {} }, refresh: false
                } }]);

                done();
            });
        });
        it('должен средиректить в `/client/error`, если ручка `getAlbumsSlices` вернула ошибку', (done) => {
            const dispatch = jest.fn();
            pageHelper.go = jest.fn();

            rawFetchModel.default = () => Promise.resolve({ error: { id: 'Module failed' } });

            fetchAlbums()(dispatch, getState).then(() => {
                expect(dispatch.mock.calls).toContainEqual([
                    { type: UPDATE_ALBUMS, payload: { isLoading: true } }
                ]);

                expect(pageHelper.go.mock.calls).toContainEqual(
                    ['/client/error', 'preserve', {
                        reason: 'albums2_fetch_albums_error',
                        error: { id: 'Module failed' }
                    }]
                );

                done();
            });
        });
        it('должен средиректить в `/client/error`, если ручка `getAlbumsSlices` выкинула ошибку', (done) => {
            const dispatch = jest.fn();
            pageHelper.go = jest.fn();

            const error = new Error();

            rawFetchModel.default = () => Promise.reject(error);

            fetchAlbums()(dispatch, getState).then(() => {
                expect(dispatch.mock.calls).toContainEqual([{ type: UPDATE_ALBUMS, payload: { isLoading: true } }]);

                expect(pageHelper.go.mock.calls).toContainEqual(
                    ['/client/error', 'preserve', { reason: 'albums2_fetch_albums_error', error }]
                );

                done();
            });
        });
    });

    describe('refuseAlbumsFaces', () => {
        const originalRawFetchModel = rawFetchModel.default;

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

        const mockAllActions = () => {
            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));
            confirmationQueueActions.add = jest.fn(() => ({ type: 'MOCK' }));
            notificationsActions.notify = jest.fn(() => ({ type: 'MOCK' }));
        };

        const submitRefuse = (store) => {
            store.dispatch(refuseAlbumsFaces());
            const [, { onSubmit }] = _.last(confirmationQueueActions.add.mock.calls);
            return onSubmit();
        };

        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;

            dialogsActions.openDialog = originalOpenDialog;
            dialogsActions.closeDialog = originalCloseDialog;
            confirmationQueueActions.add = originalAddConfirmation;
            notificationsActions.notify = originalNotifyNotification;
            pageHelper.go = originalHelperPageGo;
        });

        it('отказ пользователя от альбомов лиц - нажатие на отмена в предупреждении', () => {
            const store = createStore();

            dialogsActions.openDialog = jest.fn(() => ({ type: 'MOCK' }));
            dialogsActions.closeDialog = jest.fn(() => ({ type: 'MOCK' }));

            store.dispatch(refuseAlbumsFaces());
            const [openDialogName, { onReject }] = _.last(dialogsActions.openDialog.mock.calls);
            expect(openDialogName).toBe('confirmation');

            onReject();
            expect(dialogsActions.closeDialog).toHaveBeenCalledWith('confirmation');
        });

        it('отказ пользователя от альбомов лиц - успешный отказ', (done) => {
            const store = createStore({
                albums: {
                    albums: {
                        faces: {
                            id: 'faces'
                        }
                    }
                }
            });

            pageHelper.go = jest.fn();
            rawFetchModel.default = () => Promise.resolve({});
            mockAllActions();

            submitRefuse(store).then(() => {
                const { user, albums: { albums: { faces } } } = store.getState();
                expect(user.faces_indexing_state).toBe('user_refused');
                expect(faces).toBe(null);

                const [openDialogName, { dialogParams: {
                    cancelButtonText
                } }] = _.last(confirmationQueueActions.add.mock.calls);
                expect(openDialogName).toBe('confirmation');
                expect(cancelButtonText).toBe('');

                expect(pageHelper.go.mock.calls).toContainEqual(['/client/albums']);
                done();
            }).catch(done);
        });

        it('отказ пользователя от альбомов лиц - ошибка', (done) => {
            const store = createStore({
                albums: {
                    albums: {
                        faces: {
                            id: 'faces'
                        }
                    }
                }
            });

            pageHelper.go = jest.fn();
            rawFetchModel.default = () => Promise.reject({});
            mockAllActions();

            submitRefuse(store).then(() => {
                const { user, albums: { albums: { faces } } } = store.getState();
                expect(user.faces_indexing_state).not.toBe('user_refused');
                expect(faces.id).toBe('faces');

                const [{ operation, status }] = _.last(notificationsActions.notify.mock.calls);
                expect(operation).toBe('refuseAlbumsFaces');
                expect(status).toBe('failed');

                expect(pageHelper.go.mock.calls).toEqual([]);
                done();
            }).catch(done);
        });
    });
});
