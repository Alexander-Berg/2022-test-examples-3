import '../../noscript';
import createStore from '../../../../components/redux/store/create-store';
import {
    moveClonedResource,
    destroyResource,
    fetchCurrentListing
} from '../../../../components/redux/store/actions/resources';

import commonReducers from '../../../../components/redux/store/reducers';
import photosliceReducer from '../../../../components/redux/store/reducers/photoslice';
import { FETCH_ALBUM_SUCCESS, MOVE_CLONED_RESOURCES } from '../../../../components/redux/store/actions/types';
import albumWithItems from '../../../fixtures/album-with-items';
import folderResources from '../../../fixtures/resources/folder';
import rootResources from '../../../fixtures/resources/root';
import resourceHelper from '../../../../components/helpers/resource';
import extractPreloadedData from '../../../../components/extract-preloaded-data';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import _ from 'lodash';
import pageHelper from '../../../../components/helpers/page';
import {
    currentResourceChildrenIds,
    currentResourceIsComplete
} from '../../../../components/redux/store/selectors/resources';

jest.mock('../../../../components/helpers/performance', () => ({
    withResourceTimingsPreserved: (promise) => promise.then((data) => Promise.resolve([data, []])),
    collectTimings: () => {},
    sendHeroElement: () => {}
}));

const getPhotosliceStore = ({ idDialog } = {}) => createStore({
    environment: { agent: {}, session: { experiment: {} } },
    page: {
        idContext: '/photo',
        dialog: 'slider',
        idDialog
    },
    resources: {
        '/photo': {
            id: '/photo'
        },
        '/trash': {
            id: '/trash'
        },
        '/disk/1': {
            id: '/disk/1',
            name: '1',
            clusterId: '000001566838659356_000001566838659356',
            type: 'file',
            state: {}
        },
        '/disk/2': {
            id: '/disk/2',
            name: '2',
            clusterId: '000001566838659356_000001566838659356',
            type: 'file',
            state: {}
        }
    },
    photoslice: {
        clusters: ['000001566838659356_000001566838659356'],
        clustersByIds: {
            '000001566838659356_000001566838659356': {
                id: '000001566838659356_000001566838659356',
                size: 2,
                items: [{
                    id: '/disk/1',
                    itemId: 'cluster1_002'
                }, {
                    id: '/disk/2',
                    itemId: 'cluster1_001'
                }]
            }
        }
    },
    config: {}
}, Object.assign({ photoslice: photosliceReducer }, commonReducers));

const ALBUM_ID = albumWithItems.album.id;
const getAlbumStore = ({ idDialog } = {}) => {
    const data = extractPreloadedData();
    data.user = data.userCurrent;
    delete data.userCurrent;
    const store = createStore(Object.assign(data, {
        environment: { agent: {}, session: { experiment: { } } },
        page: {
            idContext: '/album/',
            albumId: ALBUM_ID,
            dialog: 'slider',
            idDialog
        }
    }));

    const resources = albumWithItems.resources.map((resource) => Object.assign({ albumIds: [ALBUM_ID] }, resource));

    store.dispatch({
        type: FETCH_ALBUM_SUCCESS,
        payload: {
            album: albumWithItems.album,
            resources: resourceHelper.preprocessResources(resources, store.getState())
        }
    });

    return store;
};

describe('resources actions', () => {
    let nsActionRun;
    beforeEach(() => {
        nsActionRun = ns.action.run;
        ns.action.run = jest.fn();
    });
    afterEach(() => {
        ns.action.run = nsActionRun;
    });

    describe('moveClonedResource', () => {
        it('должен поменять children и idDialog если запущен в блоке воспоминаний', (done) => {
            const store = createStore({
                environment: { agent: { isMobile: true }, session: { experiment: {} } },
                page: {
                    idContext: '/remember/id',
                    dialog: 'slider',
                    idDialog: '/disk/2'
                },
                resources: {
                    '/remember/id': {
                        id: '/remember/id',
                        children: ['/disk/1', '/disk/2', '/disk/3']
                    },
                    '/disk/1': { id: '/disk/1', name: '1' },
                    '/disk/2': { id: '/disk/2', name: '2' },
                    '/disk/3': { id: '/disk/3', name: '3' }
                }
            });

            store.dispatch(moveClonedResource(
                { id: '/disk/2', name: '2' },
                { id: '/disk/2__', name: '2__' }
            )).then(() => {
                expect(store.getState().resources['/remember/id'].children)
                    .toEqual(['/disk/1', '/disk/2__', '/disk/3']);
                expect(store.getState().page.idDialog).toBe('/disk/2__');
                expect(ns.action.run).toBeCalledWith('app.openSlider', { id: '/disk/2__' });
                done();
            });
        });

        it('должен поменять idDialog если запущен в фотосрезе и ресурс отправился в корзину', (done) => {
            const store = getPhotosliceStore({ idDialog: '/disk/1' });

            store.dispatch(moveClonedResource(
                { id: '/disk/1', name: '1' },
                { id: '/trash/1', name: '1', isInTrash: true }
            )).then(() => {
                expect(store.getState().page.idDialog).toBe('/disk/2');
                expect(ns.action.run).toBeCalledWith('app.openSlider', { id: '/disk/2' });
                done();
            });
        });

        it('должен поменять idDialog если запущен в альбоме и ресурс отправился в корзину', (done) => {
            const store = getAlbumStore({ idDialog: '/disk/IMG_1528.HEIC' });

            store.dispatch(moveClonedResource(
                { id: '/disk/IMG_1528.HEIC', albumIds: [ALBUM_ID] },
                { id: '/trash/IMG_1528.HEIC', isInTrash: true, albumIds: [ALBUM_ID] }
            )).then(() => {
                expect(store.getState().page.idDialog).toBe('/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG');
                expect(ns.action.run)
                    .toBeCalledWith('app.openSlider', { id: '/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG' });
                done();
            });
        });

        it('должен сбросить idDialog если запущен в фотосрезе, ресурс отправился в корзину и это был последний ресурс фотосреза', (done) => {
            const store = getPhotosliceStore({ idDialog: '/disk/2' });

            store.dispatch(moveClonedResource(
                { id: '/disk/2', name: '2' },
                { id: '/trash/2', name: '2', isInTrash: true }
            )).then(() => {
                expect(store.getState().page.idDialog).toBe('');
                expect(ns.action.run).toBeCalledWith('dialog.close');
                done();
            });
        });

        it('должен сбросить idDialog если запущен в альбоме, ресурс отправился в корзину и это был последний ресурс в альбоме', (done) => {
            const store = getAlbumStore({ idDialog: '/disk/Test upload/10-4111_1.jpg' });

            store.dispatch(moveClonedResource(
                { id: '/disk/Test upload/10-4111_1.jpg', albumIds: [ALBUM_ID] },
                { id: '/trash/Test upload/10-4111_1.jpg', isInTrash: true, albumIds: [ALBUM_ID] }
            )).then(() => {
                expect(store.getState().page.idDialog).toBe('');
                expect(ns.action.run).toBeCalledWith('dialog.close');
                done();
            });
        });

        it('должен поменять idDialog если запущен в альбоме', (done) => {
            const store = getAlbumStore({ idDialog: '/disk/IMG_1528.HEIC' });

            store.dispatch(moveClonedResource(
                { id: '/disk/IMG_1528.HEIC', albumIds: [ALBUM_ID] },
                { id: '/disk/IMG_1528__.HEIC', albumIds: [ALBUM_ID] }
            )).then(() => {
                expect(store.getState().page.idDialog).toBe('/disk/IMG_1528__.HEIC');
                expect(ns.action.run).toBeCalledWith('app.openSlider', { id: '/disk/IMG_1528__.HEIC' });
                done();
            });
        });

        it('В листинге должен менять idDialog', (done) => {
            const store = createStore({
                environment: { agent: { isMobile: true }, session: { experiment: {} } },
                page: {
                    idContext: '/disk',
                    dialog: 'slider',
                    idDialog: '/disk/2'
                },
                resources: {
                    '/disk': {
                        id: '/disk',
                        children: {
                            name_1: ['/disk/1', '/disk/2', '/disk/3']
                        }
                    },
                    '/disk/1': { id: '/disk/1', name: '1' },
                    '/disk/2': { id: '/disk/2', name: '2' },
                    '/disk/3': { id: '/disk/3', name: '3' }
                }
            });

            store.dispatch(moveClonedResource(
                { id: '/disk/2', name: '2' },
                { id: '/disk/2__', name: '2__' }
            )).then(() => {
                expect(store.getState().resources['/disk'].children.name_1).toEqual(['/disk/1', '/disk/2', '/disk/3']);
                expect(store.getState().page.idDialog).toBe('/disk/2__');
                done();
            });
        });

        it('Должен объединять несколько moveClonedResource в один экшн', (done) => {
            const store = getPhotosliceStore();
            const mockedDispatch = jest.fn((arg) => (
                typeof arg === 'function' ? arg(mockedDispatch, store.getState) : arg)
            );
            moveClonedResource(
                { id: '/disk/1', name: '1' },
                { id: '/trash/1', name: '1', isInTrash: true }
            )(mockedDispatch, store.getState);
            moveClonedResource(
                { id: '/disk/2', name: '2' },
                { id: '/trash/2', name: '2', isInTrash: true }
            )(mockedDispatch, store.getState);
            moveClonedResource(
                { id: '/disk/3', name: '3' },
                { id: '/disk/__3', name: '__3' }
            )(mockedDispatch, store.getState);
            Promise.resolve().then(() => {
                const moveClonedResourcesCalls = mockedDispatch.mock.calls
                    .filter(([action]) => action.type === MOVE_CLONED_RESOURCES);
                expect(moveClonedResourcesCalls.length).toEqual(1);
                expect(moveClonedResourcesCalls[0][0].payload).toEqual({
                    idContext: '/photo',
                    resources: [{
                        src: { id: '/disk/1', name: '1' },
                        dst: { id: '/trash/1', name: '1', isInTrash: true }
                    }, {
                        src: { id: '/disk/2', name: '2' },
                        dst: { id: '/trash/2', name: '2', isInTrash: true }
                    }, {
                        src: { id: '/disk/3', name: '3' },
                        dst: { id: '/disk/__3', name: '__3' }
                    }]
                });
                done();
            });
        });
    });

    describe('destroyResource', () => {
        it('должен поменять idDialog если запущен в альбоме', () => {
            const store = getAlbumStore({ idDialog: '/disk/IMG_1528.HEIC' });

            store.dispatch(destroyResource('/disk/IMG_1528.HEIC', store.getState().resources['/disk/IMG_1528.HEIC']));
            expect(store.getState().page.idDialog).toBe('/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG');
            expect(ns.action.run)
                .toBeCalledWith('app.openSlider', { id: '/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG' });
        });

        it('должен поменять idDialog если запущен в фотосрезе', () => {
            const store = getPhotosliceStore({ idDialog: '/disk/1' });

            store.dispatch(destroyResource('/disk/1', store.getState().resources['/disk/1']));
            expect(store.getState().page.idDialog).toBe('/disk/2');
            expect(ns.action.run).toBeCalledWith('app.openSlider', { id: '/disk/2' });
        });

        it('должен сбросить idDialog если запущен в альбоме и это был последний ресурс в альбоме', () => {
            const store = getAlbumStore({ idDialog: '/disk/Test upload/10-4111_1.jpg' });

            store.dispatch(destroyResource(
                '/disk/Test upload/10-4111_1.jpg',
                store.getState().resources['/disk/Test upload/10-4111_1.jpg'])
            );
            expect(store.getState().page.idDialog).toBe('');
            expect(ns.action.run).toBeCalledWith('dialog.close');
        });

        it('должен сбросить idDialog если запущен в фотосрезе и это был последний ресурс в альбоме', () => {
            const store = getPhotosliceStore({ idDialog: '/disk/2' });

            store.dispatch(destroyResource('/disk/2', store.getState().resources['/disk/2']));
            expect(store.getState().page.idDialog).toBe('');
            expect(ns.action.run).toBeCalledWith('dialog.close');
        });
    });

    describe('fetchCurrentListing', () => {
        const createStoreForListing = (state) => {
            const data = extractPreloadedData();
            data.user = data.userCurrent;
            delete data.userCurrent;
            const store = createStore(_.merge({}, data, {
                resources: {
                    '/disk': {
                        id: '/disk'
                    },
                    '/trash': {
                        id: '/trash'
                    }
                },
                statesContext: { sort: { } }
            }, state));

            return store;
        };

        const originalRawFetchModel = rawFetchModel.default;
        const originalHelperPageGo = pageHelper.go;
        beforeEach(() => {
            pageHelper.go = jest.fn(() => Promise.resolve({}));
        });
        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
            pageHelper.go = originalHelperPageGo;
        });

        it('Должен добавлять корзину при загрузке корня', (done) => {
            const store = createStoreForListing({
                page: { idContext: '/disk' }
            });

            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'resources' && params.idContext === '/disk') {
                    return Promise.resolve(rootResources);
                }
            });

            store.dispatch(fetchCurrentListing()).then(() => {
                const { resources } = store.getState();

                expect(resources['/disk'].isLoading).toBe(false);

                expect(currentResourceChildrenIds(store.getState())).toEqual([
                    '/disk/2019-12-24 15-25-07.jpg',
                    '/disk/2020-02-21 15-20-03 (1).PNG',
                    '/disk/2020-02-21 15-20-03.PNG',
                    '/disk/2106-02-07 06-28-15.JPG',
                    '/disk/91-13.jpg',
                    '/trash'
                ]);
                expect(currentResourceIsComplete(store.getState())).toBe(true);

                // вызов на загруженном листинге должен ничего не делать
                store.dispatch(fetchCurrentListing());
                expect(store.getState().resources['/disk'].isLoading).toBe(false);
                expect(rawFetchModel.default).toBeCalledTimes(1);
                done();
            });

            const { resources } = store.getState();
            expect(resources['/disk'].isLoading).toBe(true);
        });

        it('Успешная загрузка папки', (done) => {
            const store = createStoreForListing({
                page: { idContext: '/disk/500px/popular4' }
            });

            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'resources' && params.idContext === '/disk/500px/popular4') {
                    const offset = params.offset === 0 ? 0 : params.offset + 1;
                    const amount = params.offset === 0 ? params.amount + 1 : params.amount;
                    return Promise.resolve({
                        resources: folderResources.resources.slice(offset, offset + amount)
                    });
                }
            });

            store.dispatch(fetchCurrentListing()).then(() => {
                const ids = currentResourceChildrenIds(store.getState());
                expect(ids.length).toBe(40);
                expect(ids[0]).toBe('/disk/500px/popular4/1-1.jpg');
                expect(ids[39]).toBe('/disk/500px/popular4/1-45.jpg');
                expect(currentResourceIsComplete(store.getState())).toBe(false);

                store.dispatch(fetchCurrentListing()).then(() => {
                    const ids = currentResourceChildrenIds(store.getState());
                    expect(ids.length).toBe(78);
                    expect(ids[77]).toBe('/disk/500px/popular4/10-35.jpg');
                    done();
                });
            });
        });

        it('Обработка последней порции в последних файлах', () => {
            const store = createStoreForListing({
                page: { idContext: '/recent' }
            });

            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'resources' && params.idContext === '/recent') {
                    if (params.offset === 0) {
                        return Promise.resolve({
                            resources: folderResources.resources.slice(1, params.amount + 1)
                        });
                    } else {
                        return Promise.reject({
                            id: 'HTTP_400',
                            message: 'Bad Request',
                            body: {
                                code: 221,
                                title: 'BadRequestError: Can\'t get over 1000 item'
                            }
                        });
                    }
                }
            });

            return store.dispatch(fetchCurrentListing()).then(() => {
                expect(currentResourceIsComplete(store.getState())).toBe(false);
                expect(currentResourceChildrenIds(store.getState()).length).toBe(40);
                return store.dispatch(fetchCurrentListing());
            }).then(() => {
                expect(currentResourceIsComplete(store.getState())).toBe(true);
                expect(currentResourceChildrenIds(store.getState()).length).toBe(40);
            });
        });

        it('Должен фильтровать дубли', (done) => {
            const store = createStoreForListing({
                page: { idContext: '/disk/500px/popular4' }
            });

            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'resources' && params.idContext === '/disk/500px/popular4') {
                    // первые пять ресурсов во второй порции будут такими же как последние пять в первой
                    const offset = Math.max(params.offset - 5, 0);
                    return Promise.resolve({
                        resources: folderResources.resources.slice(offset, offset + params.amount)
                    });
                }
            });

            store.dispatch(fetchCurrentListing()).then(() => {
                store.dispatch(fetchCurrentListing()).then(() => {
                    const ids = currentResourceChildrenIds(store.getState());
                    expect(ids).toEqual(_.uniq(ids));
                    done();
                });
            });
        });

        it('Загрузка несуществующей папки должна редиректить в корень', (done) => {
            const store = createStoreForListing({
                page: { idContext: '/disk/404' }
            });

            rawFetchModel.default = jest.fn(() => {
                return Promise.reject({ id: 'HTTP_404' });
            });

            store.dispatch(fetchCurrentListing()).then(() => {
                expect(pageHelper.go).toBeCalledWith('/client/disk');
                done();
            });
        });

        it('Пятисотка должна редиректить в /client/error', (done) => {
            const store = createStoreForListing({
                page: { idContext: '/disk/500' }
            });

            rawFetchModel.default = jest.fn(() => {
                return Promise.reject({ id: 'HTTP_500' });
            });

            store.dispatch(fetchCurrentListing()).then(() => {
                expect(pageHelper.go).toBeCalledWith('/client/error', 'preserve', {
                    error: { id: 'HTTP_500' },
                    reason: 'fetch_resource_error'
                });
                done();
            });
        });
    });
});
