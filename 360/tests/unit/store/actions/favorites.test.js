import '../../noscript';
jest.mock('../../../../components/redux/store/selectors/slider', () => ({
    getSliderResources: jest.fn()
}));
import { getSliderResources } from '../../../../components/redux/store/selectors/slider';
jest.mock('../../../../components/redux/store/actions/album', () => ({
    addToAlbum: jest.fn(),
    removeItemsFromAlbum: jest.fn()
}));
import { addToAlbum, removeItemsFromAlbum } from '../../../../components/redux/store/actions/album';
jest.mock('../../../../components/redux/store/actions/notifications', () => ({
    notify: jest.fn()
}));
import { notify } from '../../../../components/redux/store/actions/notifications';
import _ from 'lodash';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import createStore from '../../../../components/redux/store/create-store';
import { batchSetResourceData } from '../../../../components/redux/store/actions/resources';
import {
    updateFavoritesInfo,
    FIND_IN_FAVORITES_LIMIT,
    toggleFavorite
} from '../../../../components/redux/store/actions/favorites';

describe('favorites actions', () => {
    const defaultState = {
        page: {},
        statesContext: {},
        defaultFolders: {},
        environment: {
            agent: {
                hasAdblock: false,
                BrowserBase: 'Chromium',
                BrowserBaseVersion: '65.0.3319.0',
                BrowserEngine: 'WebKit',
                BrowserEngineVersion: '537.36',
                BrowserName: 'Chrome',
                BrowserVersion: '65.0.3319',
                OSFamily: 'MacOS',
                OSVersion: '10.12.6',
                isBrowser: true,
                isMobile: false,
                isTablet: false,
                isSmartphone: false,
                botSocial: false,
                isSupported: true,
                osId: 'mac'
            }
        },
        user: { states: { narod_migrated: 1 } },
        personalAlbums: {}
    };

    afterAll(() => {
        jest.clearAllMocks();
    });

    const getFakeResources = (count = 1, favorite) => _.range(count)
        .map((i) => [`/disk/resource${i}`, {
            id: `/disk/resource${i}`,
            type: 'file',
            meta: {
                hasPreview: true,
                sizes: [],
                mediatype: 'image',
                resource_id: `resource_${i}`
            },
            ...favorite ? { favorite } : {}
        }]);

    describe('updateFavoritesInfo', () => {
        const originalRawFetchModel = rawFetchModel.default;
        let store;

        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
        });

        it('should call rawFetchModel with `albumFindInFavorites` and mark resources in favorites album', () => {
            store = createStore(defaultState);
            const fakeResources = getFakeResources(10);

            store.dispatch(batchSetResourceData(fakeResources));

            const { resources } = store.getState();

            getSliderResources.mockImplementationOnce(() => ({ sliderResources: Object.values(resources) }));

            const fakeResourcesInFavorite = fakeResources.slice(3).map(([, resource]) => ({
                id: resource.id,
                resource_id: resource.meta.resource_id,
                item_id: `item_${resource.meta.resource_id}`
            }));

            rawFetchModel.default = jest.fn(() => Promise.resolve(fakeResourcesInFavorite));

            const promise = store.dispatch(updateFavoritesInfo({})).then(() => {
                const { resources } = store.getState();

                fakeResourcesInFavorite.forEach((resource) => {
                    expect(resources[resource.id].favorite).toEqual({ state: true, itemId: resource.item_id });
                });

                fakeResources.slice(0, 3).forEach(([resourceId]) => {
                    expect(resources[resourceId].favorite).toEqual({ state: false });
                });
            });

            Object.values(store.getState().resources).forEach((resource) => {
                expect(resource.favorite.state).toBe(false);
            });

            expect(rawFetchModel.default).toHaveBeenCalledWith('albumFindInFavorites', {
                resourceIds: JSON.stringify(fakeResources.map(([, resource]) => resource.meta.resource_id))
            });

            return promise;
        });

        it('should limit the call to `FIND_IN_FAVORITES_LIMIT`', () => {
            store = createStore(defaultState);
            const fakeResources = getFakeResources(200);

            store.dispatch(batchSetResourceData(fakeResources));

            const { resources } = store.getState();

            getSliderResources.mockImplementationOnce(() => ({ sliderResources: Object.values(resources) }));
            rawFetchModel.default = jest.fn(() => Promise.resolve([]));
            const promise = store.dispatch(updateFavoritesInfo(resources['/disk/resource177']));

            expect(rawFetchModel.default).toHaveBeenCalledWith('albumFindInFavorites', {
                resourceIds: JSON.stringify(
                    fakeResources.map(([, resource]) => resource.meta.resource_id).slice(-FIND_IN_FAVORITES_LIMIT)
                )
            });

            return promise;
        });
    });

    describe('toggleFavorite', () => {
        const originalRawFetchModel = rawFetchModel.default;
        let store;

        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
        });

        it('should call rawFetchModel with createFavoriteAlbum is favoriteAlbumId is not in the store', () => {
            store = createStore(defaultState);

            rawFetchModel.default = jest.fn(() => Promise.resolve(null));
            notify.mockImplementationOnce(() => () => Promise.resolve(true));

            const promise = store.dispatch(toggleFavorite({ id: '/disk/resource0', favorite: { state: false } }))
                .then(() => {
                    expect(notify).toHaveBeenCalledWith({
                        operation: 'addToAlbum',
                        status: 'failed',
                        count: 1,
                        dst: {
                            title: 'Избранные'
                        }
                    });
                });

            expect(rawFetchModel.default).toHaveBeenCalledWith('createFavoriteAlbum');
            return promise;
        });

        it('should call addToAlbum', () => {
            store = createStore(Object.assign({}, defaultState, {
                personalAlbums: {
                    favoriteAlbumId: 'favorite',
                    albumsByIds: {
                        favorite: {
                            clusters: []
                        }
                    }
                }
            }));

            const fakeResources = getFakeResources(1, { state: false });
            store.dispatch(batchSetResourceData(fakeResources));

            addToAlbum.mockImplementationOnce(() => () => Promise.resolve({
                items: [
                    {
                        id: '/disk/resource0',
                        itemId: 'itemId'
                    }
                ]
            }));

            const promise = store.dispatch(toggleFavorite(store.getState().resources['/disk/resource0']));

            expect(store.getState().resources['/disk/resource0'].favorite).toEqual({ state: true });
            return promise;
        });

        it('should call rawFetchModel with `do-remove-resource-album`', () => {
            store = createStore(Object.assign({}, defaultState, {
                personalAlbums: {
                    favoriteAlbumId: 'favorite',
                    albumsByIds: {
                        favorite: {
                            clusters: []
                        }
                    }
                }
            }));
            rawFetchModel.default = jest.fn(() => Promise.resolve({}));

            const fakeResources = getFakeResources(1, { state: true, itemId: 'item_id' });
            store.dispatch(batchSetResourceData(fakeResources));

            removeItemsFromAlbum.mockImplementationOnce(() => () => Promise.resolve(true));
            const promise = store.dispatch(toggleFavorite(store.getState().resources['/disk/resource0']));

            expect(store.getState().resources['/disk/resource0'].favorite).toEqual({ state: false, itemId: 'item_id' });
            expect(rawFetchModel.default).toHaveBeenCalledWith('do-remove-resource-album', { itemId: 'item_id' });

            return promise;
        });

        it('should revert optimistic update on error', () => {
            store = createStore(defaultState);
            rawFetchModel.default = jest.fn(() => Promise.resolve({ error: {} }));

            const fakeResources = getFakeResources(1, { state: true, itemId: 'item_id' });
            store.dispatch(batchSetResourceData(fakeResources));

            const promise = store.dispatch(toggleFavorite(store.getState().resources['/disk/resource0']))
                .then(() => {
                    expect(store.getState().resources['/disk/resource0'].favorite)
                        .toEqual({ state: true, itemId: 'item_id' });
                });

            expect(store.getState().resources['/disk/resource0'].favorite).toEqual({ state: false, itemId: 'item_id' });

            return promise;
        });
    });
});
