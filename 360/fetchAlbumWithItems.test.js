import '../../../../../tests/unit/noscript';
jest.mock('../../selectors/slider');
jest.mock('../../selectors/resources');

import _ from 'lodash';

import { getContext, select } from 'redux-saga/effects';
import { combineReducers } from 'redux';

import { testSaga, expectSaga } from 'redux-saga-test-plan';

import resourceHelper from '../../../../helpers/resource';
import getStore from '../../index';
import albumWithItems from '../../../../../tests/fixtures/album-with-items';
import { fetchAlbumWithItemsSaga } from './fetchAlbumWithItems';

import { getCurrentAlbumId } from '../../selectors/albums';
import { updatePage } from '../../actions';
import { updateAlbum } from '../../actions/albums/index';

describe('Sagas/fetchAlbumWithItems', () => {
    describe('обработка ошибок', () => {
        const pageHelper = {
            go: jest.fn(),
        };

        it('ошибка должна приводить к редиректу на /client/error', () => {
            const error = new Error();
            const albumId = 'albumId';

            testSaga(fetchAlbumWithItemsSaga)
                .next()
                .throw(error)
                .select(getCurrentAlbumId)
                .next(albumId)
                .put(
                    updateAlbum({
                        id: albumId,
                        isLoading: false,
                    })
                )
                .next()
                .getContext('pageHelper')
                .next(pageHelper)
                .call(pageHelper.go, '/client/error', 'preserve', {
                    reason: 'album2_fetch_error',
                    error,
                })
                .next()
                .cancelled()
                .next()
                .isDone();
        });

        it('если альбома нет, должен произойти редирект в список альбомов', () => {
            /**
             * @type {any}
             */
            const error = {
                id: 'HTTP_404',
            };

            const albumId = 'albumId';

            testSaga(fetchAlbumWithItemsSaga)
                .next()
                .throw(error)
                .select(getCurrentAlbumId)
                .next(albumId)
                .put(
                    updateAlbum({
                        id: albumId,
                        isLoading: false,
                    })
                )
                .next()
                .getContext('pageHelper')
                .next(pageHelper)
                .call(pageHelper.go, '/client/albums')
                .next()
                .cancelled()
                .next()
                .isDone();
        });
    });

    describe('загрузка порций', () => {
        const ALBUM_ID = albumWithItems.album.id;
        const rawFetchModel = jest.fn((modelName, params) => {
            if (modelName === 'albumWithItems' && params.albumId === ALBUM_ID) {
                const albumData =
                    params.loadAlbumData === '1' ? albumWithItems.album : _.pick(albumWithItems.album, ['id', 'items']);

                const start = params.lastItemId ?
                    albumWithItems.album.items.findIndex(({ itemId }) => itemId === params.lastItemId) + 1 :
                    0;

                return Promise.resolve(
                    Object.assign({}, albumWithItems, {
                        album: Object.assign({}, albumData, {
                            items: albumWithItems.album.items.slice(
                                start,
                                start + params.amount
                            ),
                        }),
                        resources: albumWithItems.resources.slice(
                            start,
                            start + params.amount
                        ),
                    })
                );
            }

            return Promise.resolve({});
        });
        const pageHelper = {
            go: jest.fn(),
        };

        it('успешная загрузка первой порции', () => {
            const store = getStore(true);

            store.dispatch(updatePage({ albumId: ALBUM_ID }));

            return expectSaga(fetchAlbumWithItemsSaga)
                .withReducer(combineReducers(store.reducers), store.getState())
                .provide([
                    [select(getCurrentAlbumId), store.getState().page.albumId],
                    [getContext('resourceHelper'), resourceHelper],
                    [getContext('rawFetchModel'), rawFetchModel],
                    [getContext('pageHelper'), pageHelper],
                ])
                .run()
                .then((result) => {
                    const { personalAlbums, resources } = result.storeState;

                    expect(personalAlbums.albumsByIds[ALBUM_ID].isLoading).toBe(
                        false
                    );
                    expect(
                        personalAlbums.albumsByIds[ALBUM_ID].isCompleted
                    ).toBe(false);
                    expect(resources['/disk/IMG_1528.HEIC'].albumIds).toEqual([
                        ALBUM_ID,
                    ]);
                    expect(resources['/disk/IMG_1528.HEIC'].name).toBeTruthy();
                    expect(
                        resources['/photounlim/2019-10-22 18-38-34.JPG']
                            .albumIds
                    ).toEqual([ALBUM_ID]);
                    expect(
                        resources['/photounlim/2019-10-22 18-38-34.JPG'].name
                    ).toBeTruthy();
                });
        });

        it('загрузка второй порции должна обновлять только элементы', async () => {
            const store = getStore(true);
            const ALBUM_ID = albumWithItems.album.id;
            store.dispatch(updatePage({ albumId: ALBUM_ID }));

            let stateAfterFirstDispatch = {};

            // Честно прогоним сагу первый раз
            await expectSaga(fetchAlbumWithItemsSaga)
                .withReducer(combineReducers(store.reducers), store.getState())
                .provide([
                    [select(getCurrentAlbumId), store.getState().page.albumId],
                    [getContext('resourceHelper'), resourceHelper],
                    [getContext('rawFetchModel'), rawFetchModel],
                    [getContext('pageHelper'), pageHelper],
                ])
                .run()
                .then((result) => {
                    stateAfterFirstDispatch = result.storeState;
                });

            const { personalAlbums } = stateAfterFirstDispatch;
            expect(personalAlbums.albumsByIds[ALBUM_ID].layout).toBe(
                'waterfall'
            );
            personalAlbums.albumsByIds[ALBUM_ID].layout = 'squares';

            return expectSaga(fetchAlbumWithItemsSaga)
                .withReducer(
                    combineReducers(store.reducers),
                    stateAfterFirstDispatch
                )
                .provide([
                    [select(getCurrentAlbumId), store.getState().page.albumId],
                    [getContext('resourceHelper'), resourceHelper],
                    [getContext('rawFetchModel'), rawFetchModel],
                    [getContext('pageHelper'), pageHelper],
                ])
                .run()
                .then((result) => {
                    const { personalAlbums } = result.storeState;

                    expect(
                        personalAlbums.albumsByIds[ALBUM_ID].clusters.length
                    ).toBe(2);
                    expect(personalAlbums.albumsByIds[ALBUM_ID].layout).toBe(
                        'squares'
                    );
                });
        });
    });
});
