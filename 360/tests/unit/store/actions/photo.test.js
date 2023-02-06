import '../../noscript';

import getStore from '../../../../components/redux/store';
import * as rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';
import { combineReducers } from 'redux';
import { getResourceData } from '../../../../components/redux/store/selectors/resource';
import { setResourceData } from '../../../../components/redux/store/actions/resources';
import { UPDATE_CLUSTERS_AND_RESOURCES } from '../../../../components/redux/store/actions/types';
import { FETCH_PHOTOSLICE_SUCCESS, UPDATE_PHOTOSLICE } from '../../../../components/redux/store/actions/photo-types';
import { PHOTO_GRID_TYPES } from '@ps-int/ufo-rocks/lib/consts';
import commonReducers from '../../../../components/redux/store/reducers';
import photoslice from '../../../../components/redux/store/reducers/photoslice';
import pageHelper from '../../../../components/helpers/page';
import resourceHelper from '../../../../components/helpers/resource';
import * as indexedDBHelper from '@ps-int/ufo-rocks/lib/helpers/indexedDB';
import _ from 'lodash';

import {
    fetchAndApplyDiff,
    fetchSnapshot,
    getClustersToLoad,
    fetchClustersRanges,
    getClustersWithMissingResources,
    scrollToPhotosliceCluster,
    scrollToPhotosliceItem,
    setPhotoView
} from '../../../../components/redux/store/actions/photo';

jest.mock('../../../../components/helpers/scroll');

import { scrollListingToPosition } from '../../../../components/helpers/scroll';

import * as photosliceHelpers from '../../../../components/helpers/photoslice';
photosliceHelpers.findClusterByTimestamp = jest.fn().mockReturnValue({ id: '0_0' }).mockReturnValueOnce(null);

import * as commonActions from '../../../../components/redux/store/actions';
import * as settingsActions from '../../../../components/redux/store/actions/settings';

describe('photo actions', () => {
    describe('fetchSnapshot', () => {
        let store;
        const originalRawFetchModel = rawFetchModel.default;
        const originalNsHttp = ns.http;
        const originalHelperPageGo = pageHelper.go;
        const originalGetDatabaseObjectStore = indexedDBHelper.getDatabaseObjectStore;
        const originalSetDatabaseObjectStore = indexedDBHelper.setDatabaseObjectStore;

        beforeEach(() => {
            store = getStore(true);
            store.replaceReducer(combineReducers(Object.assign({ photoslice }, commonReducers)));

            pageHelper.go = jest.fn();
            ns.http = jest.fn();
        });

        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
            indexedDBHelper.getDatabaseObjectStore = originalGetDatabaseObjectStore;
            indexedDBHelper.setDatabaseObjectStore = originalSetDatabaseObjectStore;
            pageHelper.go = originalHelperPageGo;
            ns.http = originalNsHttp;
        });

        const photosliceData = {
            revision: 1,
            id: 1,
            items: [{
                from: 1549896763000,
                id: '0000001549896763000_0000001549897356000',
                size: 127,
                to: 1549897356000
            }, {
                from: 1549365724000,
                id: '0000001549365724000_0000001549365724000',
                size: 1,
                to: 1549365724000
            }, {
                from: 1549115317000,
                id: '0000001549115317000_0000001549115317000',
                size: 1,
                to: 1549115317000
            }]
        };

        const mockSuccessfulPhotosliceFetch = () => {
            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'initSnapshot') {
                    return Promise.resolve({ photoslice_id: 1 });
                } else if (modelName === 'getSnapshot' && params.idSlice === 1) {
                    return Promise.resolve(photosliceData);
                }
            });
        };

        it('Успешная загрузка снапшота', (done) => {
            mockSuccessfulPhotosliceFetch();

            store.dispatch(fetchSnapshot()).then(() => {
                const { clusters, clustersByIds, isLoaded, isLoading } = store.getState().photoslice;
                const clustersIds = photosliceData.items.map(({ id }) => id);
                expect(clusters).toEqual(clustersIds);
                expect(Object.keys(clustersByIds)).toEqual(clustersIds);
                expect(isLoading).toBe(false);
                expect(isLoaded).toBe(true);
                done();
            });

            expect(store.getState().photoslice.isLoading).toBe(true);
        });

        it('Загрузка пустого снапшота и пустых альбомов', (done) => {
            rawFetchModel.default = jest.fn((modelName) => {
                if (modelName === 'initSnapshot') {
                    return Promise.resolve({ photoslice_id: 1, revision: 1 });
                } else {
                    return Promise.resolve({ items: [] });
                }
            });

            ns.http = jest.fn(() => Vow.resolve({ models: [{ model: 'albums', data: [] }] }));

            store.dispatch(fetchSnapshot()).then(() => {
                expect(store.getState().photoslice.clusters).toEqual([]);
                done();
            });
        });

        it('Должен выставить флаг isSlowLoading если загрузка длится больше 5сек', (done) => {
            jest.useFakeTimers();

            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'initSnapshot') {
                    return Promise.resolve({ photoslice_id: 1, revision: 1 });
                } else if (modelName === 'getSnapshot' && params.idSlice === 1 && params.revision === 1) {
                    return new Promise((resolve) => {
                        setTimeout(() => {
                            resolve(photosliceData);
                        }, 7000);
                        jest.runAllTimers();
                    });
                }
            });

            store.dispatch(fetchSnapshot()).then(() => {
                expect(store.getState().photoslice.isSlowLoading).toBe(false);
                done();
            });

            setTimeout(() => {
                expect(store.getState().photoslice.isSlowLoading).toBe(true);
            }, 6000);
        });

        it('Если initSnapshot завершится ошибкой, должен средиректить в /client/error', (done) => {
            rawFetchModel.default = jest.fn((modelName) => {
                if (modelName === 'initSnapshot') {
                    return Promise.reject({ id: 'HTTP_500' });
                }
            });

            store.dispatch(fetchSnapshot()).then(() => {
                const { photoslice: { isLoading, isLoaded } } = store.getState();

                expect(isLoading).toBe(false);
                expect(isLoaded).toBe(false);
                expect(pageHelper.go).toBeCalledWith('/client/error', 'preserve', {
                    reason: 'photo2_fetch_snapshot_error',
                    error: { id: 'HTTP_500' }
                });
                done();
            });
        });

        it('Если getSnapshot завершится ошибкой, должен средиректить в /client/error', (done) => {
            rawFetchModel.default = jest.fn((modelName) => {
                if (modelName === 'initSnapshot') {
                    return Promise.resolve({ photoslice_id: 1, revision: 1 });
                } else {
                    return Promise.reject({ id: 'HTTP_500' });
                }
            });

            store.dispatch(fetchSnapshot()).then(() => {
                const { photoslice: { isLoading, isLoaded } } = store.getState();

                expect(isLoading).toBe(false);
                expect(isLoaded).toBe(false);
                expect(pageHelper.go).toBeCalledWith('/client/error', 'preserve', {
                    reason: 'photo2_fetch_snapshot_error',
                    error: { id: 'HTTP_500' }
                });
                done();
            });
        });

        it('Должен перезагрузить снапшот если его не удалось восстановить из indexedDB', (done) => {
            mockSuccessfulPhotosliceFetch();

            indexedDBHelper.getDatabaseObjectStore = jest.fn(() => {
                return Promise.reject(new Error('indexedDB error'));
            });

            store.dispatch(fetchSnapshot(true)).then(() => {
                const { clusters, isLoaded } = store.getState().photoslice;
                expect(indexedDBHelper.getDatabaseObjectStore).toBeCalledTimes(1);
                expect(isLoaded).toBe(true);
                expect(clusters.length).toBe(photosliceData.items.length);
                done();
            });
        });

        it('Не должен редиректить в /client/error если не удалось записать снапшот в indexedDB', (done) => {
            mockSuccessfulPhotosliceFetch();
            indexedDBHelper.getDatabaseObjectStore = jest.fn(() => Promise.resolve({}));
            indexedDBHelper.setDatabaseObjectStore = jest.fn(() => {
                return Promise.reject(new Error('indexedDB error'));
            });

            store.dispatch(fetchSnapshot(true)).then(() => {
                const { clusters, isLoaded } = store.getState().photoslice;
                expect(indexedDBHelper.setDatabaseObjectStore).toBeCalledTimes(1);
                expect(pageHelper.go).toBeCalledTimes(0);
                expect(isLoaded).toBe(true);
                expect(clusters.length).toBe(photosliceData.items.length);
                done();
            });
        });

        it('Должен запрашивать diff если в indexedDB есть данные', (done) => {
            rawFetchModel.default = jest.fn((modelName, params) => {
                if (modelName === 'initSnapshot') {
                    return Promise.resolve({ photoslice_id: 1, revision: 2 });
                } else if (modelName === 'getDiff' && params.idSlice === 1 && params.revision === 1) {
                    return Promise.resolve({
                        items: [{
                            base_revision: 1,
                            revision: 2,
                            index_changes: [{
                                cluster_id: '0000001549115317000_0000001549115317000',
                                change_type: 'delete'
                            }]
                        }],
                        total: 1,
                        limit: 1,
                        revision: 2
                    });
                }
            });
            indexedDBHelper.getDatabaseObjectStore = jest.fn(() => Promise.resolve(_.merge({}, photosliceData)));
            indexedDBHelper.setDatabaseObjectStore = jest.fn(() => Promise.resolve());

            store.dispatch(fetchSnapshot(true)).then(() => {
                const { clusters, isLoaded } = store.getState().photoslice;
                expect(isLoaded).toBe(true);
                expect(clusters.length).toBe(photosliceData.items.length - 1);
                done();
            });
        });
    });
    describe('getClustersWithMissingResources', () => {
        it('должен вернуть пустые массивы, если кластеры не загружались', () => {
            expect(getClustersWithMissingResources({}, {}, []))
                .toEqual({ fetched: [], missing: [] });
        });
        it('должен вернуть пустые массивы, если запрашивались еще пустые кластера', () => {
            expect(getClustersWithMissingResources({ cluster1: { range: [0, 1] } }, {}, []))
                .toEqual({ fetched: [], missing: [] });
        });
        it('должен вернуть пустые массивы, если нет отсутствующих ресурсов', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1'] } },
                { cluster1: { id: 'cluster1', size: 1, items: [{ id: '/disk/resource1' }] } },
                []
            )).toEqual({ fetched: [], missing: [] });
        });
        it('должен вернуть пустые массивы, если загружаемого кластера уже нет в сторе', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1'] } },
                {},
                ['/disk/resource1']
            )).toEqual({ fetched: [], missing: [] });
        });
        it('должен заполнить `fetched`, если в кластере не оказалось части ресурсов', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1', '/disk/resource2'] } },
                { cluster1: { id: 'cluster1', size: 2, items: [{ id: '/disk/resource1' }, { id: '/disk/resource2' }] } },
                ['/disk/resource1']
            )).toEqual({
                fetched: [{ id: 'cluster1', size: 1, albums: null, items: [{ id: '/disk/resource2' }] }],
                missing: []
            });
        });
        it('должен пересчитать альбомы если в кластере не оказалось части ресурсов', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1', '/disk/resource2'] } },
                { cluster1: {
                    id: 'cluster1',
                    size: 2,
                    albums: { beautiful: 1, unbeautiful: 1 },
                    items: [{ id: '/disk/resource1', albums: ['beautiful'] }, { id: '/disk/resource2', albums: ['unbeautiful'] }]
                } },
                ['/disk/resource1']
            )).toEqual({
                fetched: [{ id: 'cluster1', size: 1, albums: { unbeautiful: 1 }, items: [{ id: '/disk/resource2', albums: ['unbeautiful'] }] }],
                missing: []
            });
        });
        it('должен затереть альбомы в null если в кластере не осталось ресурсов из альбомов-срезов', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1', '/disk/resource2'] } },
                { cluster1: {
                    id: 'cluster1',
                    size: 2,
                    albums: { beautiful: 1 },
                    items: [{ id: '/disk/resource1', albums: ['beautiful'] }, { id: '/disk/resource2', albums: [] }]
                } },
                ['/disk/resource1']
            )).toEqual({
                fetched: [{ id: 'cluster1', size: 1, albums: null, items: [{ id: '/disk/resource2', albums: [] }] }],
                missing: []
            });
        });
        it('должен заполнить `missing`, если в кластере не оказалось ни одного ресурса', () => {
            expect(getClustersWithMissingResources(
                { cluster1: { resources: ['/disk/resource1'] } },
                { cluster1: { id: 'cluster1', size: 1, items: [{ id: '/disk/resource1' }] } },
                ['/disk/resource1']
            )).toEqual({
                fetched: [],
                missing: ['cluster1']
            });
        });
    });
    describe('getClustersToLoad', () => {
        it('должен вернуть пустой объект, если не передано интервалов', () => {
            const ranges = [];

            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            size: 1
                        },
                        cluster2: {
                            size: 1
                        }
                    },
                    clusters: ['cluster1', 'cluster2']

                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            expect(getClustersToLoad(state, ranges)).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: []
            });
        });
        it('должен вернуть массив с одним объектом, если кластеров для загрузки < `splitBy`', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: 'cluster1resource0' }]
                        }
                    },
                    clusters: ['cluster1', 'cluster2']
                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const ranges = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];

            expect(getClustersToLoad(state, ranges)).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: [{
                    cluster1: { range: [0, 0] },
                    cluster2: { resources: ['cluster1resource0'] }
                }]
            });
        });
        it('должен вернуть массив с несколькими объектами, если кластеров для загрузки > `splitBy`', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: 'cluster1resource0' }]
                        }
                    },
                    clusters: ['cluster1', 'cluster2']
                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const ranges = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];

            expect(getClustersToLoad(state, ranges, [], 1)).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: [{
                    cluster1: { range: [0, 0] }
                }, {
                    cluster2: { resources: ['cluster1resource0'] }
                }]
            });
        });
        it('должен вернуть массив для загрузки только кластеров (без ресурсов)', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1
                        }
                    },
                    clusters: ['cluster1', 'cluster2']
                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            expect(getClustersToLoad(state, [], [[0, 1]])).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: [{
                    cluster1: true,
                    cluster2: true
                }]
            });
        });
        it('должен вернуть данные о ресурсах, в которых разошлась принадлежность к альбомам', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            albums: { beautiful: 1 },
                            size: 1,
                            items: [{
                                itemId: 'r1',
                                id: '/disk/image01.jpg',
                                albums: ['beautiful']
                            }]
                        }
                    },
                    clusters: ['cluster1']
                },
                resources: {
                    '/disk/image01.jpg': {
                        id: '/disk/image01.jpg',
                        clusterId: 'cluster1',
                        meta: {
                            albums_exclusions: ['beautiful']
                        }
                    }
                },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const ranges = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 0, resourceIndex: 0 }]];

            expect(getClustersToLoad(state, ranges, [], 1)).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {
                    '/disk/image01.jpg': true
                },
                clustersToLoad: []
            });
        });
        it('должен вернуть пустой массив для уже загруженных кластеров (без ресурсов)', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1,
                            items: [{ id: 'cluster1resource0' }]
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: 'cluster2resource0' }]
                        }
                    },
                    clusters: ['cluster1', 'cluster2']
                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            expect(getClustersToLoad(state, [], [[0, 1]])).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: []
            });
        });
        it('должен вернуть массив для загрузки кластеров (смешанный - с ресурсами и без ресурсов)', () => {
            const state = {
                photoslice: {
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: 'cluster1resource0' }]
                        },
                        cluster3: {
                            id: 'cluster3',
                            size: 1
                        },
                        cluster4: {
                            id: 'cluster4',
                            size: 1
                        }
                    },
                    clusters: ['cluster1', 'cluster2', 'cluster3', 'cluster4']
                },
                resources: { },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const ranges = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];

            expect(getClustersToLoad(state, ranges, [[2, 2]])).toEqual({
                resourcesWithoutClusterIds: {},
                resourcesNeededExcludeFromAlbums: {},
                clustersToLoad: [{
                    cluster1: { range: [0, 0] },
                    cluster2: { resources: ['cluster1resource0'] },
                    cluster3: true
                }]
            });
        });
    });
    describe('fetchClustersRanges', () => {
        let originalRawFetchModel;

        beforeEach(() => {
            originalRawFetchModel = rawFetchModel.default;
        });
        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
        });

        it('не должен делать запрос, если переданные кластера и ресурсы уже загружены', (done) => {
            const fetch = jest.fn();
            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];

            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue({
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1,
                            items: [{ id: '/cluster1resource1' }]
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: '/cluster2resource1' }]
                        }
                    }
                },
                resources: {
                    '/cluster1resource1': {},
                    '/cluster2resource1': {}
                },
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            });

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch).not.toHaveBeenCalled();

                done();
            });
        });

        it('должен правильно отработать, если нет пустых кластеров и отсутствующих ресурсов', (done) => {
            const store = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: '/cluster2resource1' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        id: '/cluster1resource1'
                    }, {
                        id: '/cluster2resource1'
                    }],
                    missing: []
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue(store);
            const fetch = jest.fn().mockResolvedValue(response);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 0] },
                        cluster2: { resources: ['/cluster2resource1'] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                items: [{ id: '/cluster1resource1' }]
                            }],
                            missing: []
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource1'
                        }, {
                            id: '/cluster2resource1'
                        }], store)
                    }
                }]]);

                done();
            });
        });

        it('должен правильно отработать, если есть ненайденные кластера', (done) => {
            const store = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: '/cluster2resource1' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [],
                    missing: ['cluster1']
                },
                resources: {
                    fetched: [{
                        id: '/cluster2resource1'
                    }],
                    missing: []
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue(store);
            const fetch = jest.fn().mockResolvedValue(response);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 0] },
                        cluster2: { resources: ['/cluster2resource1'] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [],
                            missing: ['cluster1']
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster2resource1'
                        }], store)
                    }
                }]]);

                done();
            });
        });

        it('должен правильно отработать, если есть незагруженные кластера с ненайденными ресурсами', (done) => {
            const store = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 2
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: '/cluster2resource1' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        id: '/cluster1resource1'
                    }, {
                        id: '/cluster2resource1'
                    }],
                    missing: ['/cluster1resource2']
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue(store);
            const fetch = jest.fn().mockResolvedValue(response);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 1] },
                        cluster2: { resources: ['/cluster2resource1'] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                items: [{ id: '/cluster1resource1' }]
                            }],
                            missing: []
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource1'
                        }, {
                            id: '/cluster2resource1'
                        }], store)
                    }
                }]]);

                done();
            });
        });

        it('должен правильно отработать, если есть загруженные кластера с ненайденными ресурсами', (done) => {
            const store = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 2,
                            items: [{ id: '/cluster2resource1' }, { id: '/cluster2resource2' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        id: '/cluster1resource1'
                    }],
                    missing: ['/cluster2resource2']
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue(store);
            const fetch = jest.fn().mockResolvedValue(response);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 0] },
                        cluster2: { resources: ['/cluster2resource1'] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                items: [{ id: '/cluster1resource1' }]
                            }, {
                                id: 'cluster2',
                                size: 1,
                                albums: null,
                                items: [{ id: '/cluster2resource1' }]
                            }],
                            missing: []
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource1'
                        }], store)
                    }
                }]]);

                done();
            });
        });

        it('должен правильно отработать, если загружаемого кластера уже нет в сторе', (done) => {
            const storeBefore = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1,
                            items: [{ id: '/cluster1resource1' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const storeAfter = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: [],
                    clustersByIds: {}
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [],
                    missing: []
                },
                resources: {
                    fetched: [],
                    missing: ['/cluster1resource1']
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 0, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const fetch = jest.fn().mockResolvedValue(response);

            const getState = jest.fn()
                .mockReturnValue(storeAfter)
                .mockReturnValueOnce(storeBefore);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { resources: ['/cluster1resource1'] }
                    })
                }]]);
                expect(dispatch).not.toHaveBeenCalled();

                done();
            });
        });

        it('должен правильно отработать, если есть загруженные кластера, для которых не нашлось ни одного ресурса', (done) => {
            const store = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1', 'cluster2'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        },
                        cluster2: {
                            id: 'cluster2',
                            size: 1,
                            items: [{ id: '/cluster2resource2' }]
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };

            const response = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        id: '/cluster1resource1'
                    }],
                    missing: ['/cluster2resource2']
                }
            };

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 0 }]];
            const dispatch = jest.fn();
            const getState = jest.fn().mockReturnValue(store);
            const fetch = jest.fn().mockResolvedValue(response);

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 0] },
                        cluster2: { resources: ['/cluster2resource2'] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                items: [{ id: '/cluster1resource1' }]
                            }],
                            missing: ['cluster2']
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource1'
                        }], store)
                    }
                }]]);

                done();
            });
        });

        it('должен правильно отработать, если загружаемый кластер уже есть в сторе после завершения запроса', (done) => {
            const storeBeforeRequest = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 3
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };
            const storeAfterRequest = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 2,
                            items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource2' }]
                        }
                    }
                },
                resources: {},
                page: { }
            };
            const response = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 2,
                        items: [{ id: '/cluster1resource2' }, { id: '/cluster1resource3' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        id: '/cluster1resource2'
                    }],
                    missing: ['/cluster1resource1']
                }
            };

            const getState = jest.fn()
                .mockReturnValue(storeAfterRequest)
                .mockReturnValueOnce(storeBeforeRequest);

            const dispatch = jest.fn();
            const fetch = jest.fn().mockResolvedValue(response);

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 0, resourceIndex: 1 }]];

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState).then(() => {
                expect(fetch.mock.calls).toEqual([['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 1] }
                    })
                }]]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls).toEqual([[{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                albums: null,
                                items: [{ id: '/cluster1resource2' }]
                            }],
                            missing: []
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource2'
                        }], storeAfterRequest)
                    }
                }]]);

                done();
            });
        });

        it('должен корректно отработать, если ресурсы пришлось ретраить', (done) => {
            const storeBeforeRequest = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 3
                        }
                    }
                },
                resources: {},
                page: { },
                user: { },
                environment: { session: { experiment: { } } }
            };
            const storeAfterRequest = {
                photoslice: {
                    photosliceId: 'my_photo_slice',
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 2,
                            items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource2' }]
                        }
                    }
                },
                resources: {},
                page: { }
            };
            const responseWithError = {
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 3,
                        items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource2' }, { id: '/cluster1resource3' }]
                    }],
                    missing: []
                },
                resources: {
                    error: {},
                    requested: ['/cluster1resource1', '/cluster1resource2']
                }
            };
            const resourcesResponse = [{
                id: '/cluster1resource2'
            }];

            const getState = jest.fn()
                .mockReturnValue(storeAfterRequest)
                .mockReturnValueOnce(storeBeforeRequest)
                .mockReturnValueOnce(storeBeforeRequest);

            const fetch = jest.fn()
                .mockResolvedValue(resourcesResponse)
                .mockResolvedValueOnce(responseWithError);

            const dispatch = jest.fn().mockImplementation(() => {
                expect(fetch.mock.calls[1]).toEqual(['getResources', {
                    ids: JSON.stringify(['/cluster1resource1', '/cluster1resource2']),
                    photoslice: true
                }]);

                const updateAction = dispatch.mock.calls[1][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls[0]).toEqual([{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 1,
                                albums: null,
                                items: [{ id: '/cluster1resource2' }]
                            }],
                            missing: []
                        },
                        resources: resourceHelper.preprocessResources([{
                            id: '/cluster1resource2'
                        }], storeAfterRequest)
                    }
                }]);
            }).mockImplementationOnce(() => {
                expect(fetch.mock.calls[0]).toEqual(['getClustersWithResources', {
                    photosliceId: 'my_photo_slice',
                    clusters: JSON.stringify({
                        cluster1: { range: [0, 1] }
                    })
                }]);

                const updateAction = dispatch.mock.calls[0][0];
                const subDispatch = jest.fn((value) => value);
                const getState = jest.fn().mockReturnValue({ resources: {} });

                updateAction(subDispatch, getState);

                expect(subDispatch.mock.calls[0]).toEqual([{
                    type: UPDATE_CLUSTERS_AND_RESOURCES,
                    payload: {
                        clusters: {
                            fetched: [{
                                id: 'cluster1',
                                size: 3,
                                items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource2' }, { id: '/cluster1resource3' }]
                            }],
                            missing: []
                        }
                    }
                }]);
                done();
            });

            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 0, resourceIndex: 1 }]];

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState);
        });

        it('должен перезапросить снепшот в случае 404', (done) => {
            const indexPaths = [[{ clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 0, resourceIndex: 0 }]];

            const state = {
                page: {},
                photoslice: {
                    photosliceId: 'my_photo_slice_old',
                    isLoaded: true,
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        }
                    }
                },
                environment: {
                    session: {
                        experiment: {}
                    }
                },
                user: { }
            };

            const responses = {
                getClustersWithResources: {
                    id: 'HTTP_404'
                },
                initSnapshot: {
                    photoslice_id: 'my_photo_slice_new',
                    revision: 2
                },
                getSnapshot: {
                    id: 'my_photo_slice_new',
                    items: [{
                        id: 'cluster1',
                        size: 2
                    }]
                }
            };

            const getState = jest.fn().mockReturnValue(state);

            const fetch = jest.fn()
                .mockRejectedValueOnce(responses.getClustersWithResources)
                .mockResolvedValueOnce(responses.initSnapshot)
                .mockResolvedValueOnce(responses.getSnapshot);

            const dispatch = jest.fn().mockImplementationOnce((action) => {
                action(dispatch, getState);
            }).mockImplementationOnce((action) => {
                expect(action).toEqual({
                    type: UPDATE_PHOTOSLICE,
                    payload: { isLoaded: true, isLoading: true, hasDiff: false }
                });
            }).mockImplementationOnce((action) => {
                expect(action).toEqual({
                    type: FETCH_PHOTOSLICE_SUCCESS,
                    payload: {
                        id: 'my_photo_slice_new',
                        photosliceId: 'my_photo_slice_new',
                        isLoaded: true,
                        items: [{
                            id: 'cluster1',
                            size: 2
                        }]
                    }
                });

                done();
            });

            rawFetchModel.default = fetch;

            fetchClustersRanges(indexPaths)(dispatch, getState);
        });
        it('должен правильно обработать не 404 ошибку', () => {
            // FIXME нужно определиться, как обрабатываем ошибку
        });
    });
    describe('scrollToPhotosliceCluster', () => {
        const HEADER_HEIGHT = 60;
        const grid = {
            getClusterAbsoluteTop: jest.fn(() => 100)
        };
        let store;

        beforeAll(() => {
            jest.useRealTimers();
            window.scrollTo_ = window.scrollTo;
            window.scrollTo = jest.fn();
            store = getStore(true);
            store.replaceReducer(combineReducers(Object.assign({ photoslice }, commonReducers)));
            store.dispatch({
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: {
                    photosliceId: 'photosliceId1',
                    revision: 1,
                    items: [
                        {
                            id: '0000001549115317000_0000001549115319000',
                            from: 1549896744000,
                            to: 1549897388000,
                            size: 1
                        }
                    ]
                }
            });
        });
        afterAll(() => {
            window.scrollTo = window.scrollTo_;
        });

        it('не должен вызвать скролл, если кластер не найден, должен вызвать onError', (done) => {
            const onError = jest.fn();
            store.dispatch(scrollToPhotosliceCluster(1549896600000, grid, onError));

            setTimeout(() => {
                expect(grid.getClusterAbsoluteTop).not.toHaveBeenCalled();
                expect(onError).toHaveBeenCalled();
                done();
            }, 20);
        });

        it('должен вызвать scrollListingToPosition', (done) => {
            store.dispatch(scrollToPhotosliceCluster(1549896744000, grid, () => null));

            setTimeout(() => {
                expect(grid.getClusterAbsoluteTop).toHaveBeenCalled();
                expect(scrollListingToPosition).toHaveBeenCalledWith(100, { itemHeight: HEADER_HEIGHT });
                done();
            }, 20);
        });
    });
    describe('scrollToPhotosliceItem', () => {
        let store;
        const timestamp = 1549896744;
        const itemHeight = 150;
        const absoluteTop = 100;
        const grid = {
            getItemAbsoluteTop: jest.fn(() => absoluteTop),
            getItemSize: jest.fn(() => ({ height: itemHeight }))
        };

        const resourceId = '/disk/photo.jpg';
        const resourceData = {
            id: resourceId,
            meta: {
                photoslice_time: timestamp
            }
        };
        const createResource = () => {
            store.dispatch(setResourceData(resourceId, resourceData));
            return getResourceData(store.getState(), resourceId);
        };
        const fetchSingleResource = jest.fn(() => () => Promise.resolve(createResource()));

        beforeAll(() => {
            jest.useRealTimers();
        });

        beforeEach(() => {
            jest.clearAllMocks();
            window.pageYOffset = 10000;
            store = getStore(true);
            store.replaceReducer(combineReducers(Object.assign({ photoslice }, commonReducers)));
            store.dispatch({
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: {
                    photosliceId: 'photosliceId1',
                    revision: 1,
                    items: [
                        {
                            id: '0000001549115317000_0000001549115319000',
                            from: 1549896744000,
                            to: 1549897388000,
                            size: 1
                        }
                    ]
                }
            });
        });

        const updateCluster = (withResource) => store.dispatch({
            type: UPDATE_CLUSTERS_AND_RESOURCES,
            payload: {
                clusters: {
                    fetched: [{
                        id: '0000001549115317000_0000001549115319000',
                        size: 1,
                        items: [{ id: resourceId }]
                    }],
                    missing: []
                },
                resources: withResource ? [resourceData] : []
            }
        });

        it('должен скроллить до нужного ресурса', (done) => {
            updateCluster();

            store.dispatch(scrollToPhotosliceItem(resourceId, grid, {}, jest.fn(), fetchSingleResource));

            setTimeout(() => {
                expect(fetchSingleResource).toHaveBeenCalledWith(resourceId, { silent: true });
                expect(grid.getItemAbsoluteTop).toHaveBeenCalledWith('0000001549115317000_0000001549115319000', 0);
                expect(scrollListingToPosition).toHaveBeenCalledWith(absoluteTop, { itemHeight });
                done();
            }, 20);
        });

        it('должен вызвать fetchCluster, если кластер еще не прогружен', (done) => {
            const fetchCluster = jest.fn(() => () => {
                updateCluster();
                return Promise.resolve();
            });

            store.dispatch(scrollToPhotosliceItem(resourceId, grid, {}, jest.fn(), fetchSingleResource, fetchCluster));
            setTimeout(() => {
                expect(fetchSingleResource).toHaveBeenCalledWith(resourceId, { silent: true });
                expect(fetchCluster).toHaveBeenCalledWith('0000001549115317000_0000001549115319000');
                expect(scrollListingToPosition).toHaveBeenCalledWith(absoluteTop, { itemHeight });
                done();
            }, 20);
        });

        it('если ресурс уже существует, то загружать его не нужно', (done) => {
            updateCluster(true);

            store.dispatch(scrollToPhotosliceItem(resourceId, grid, {}, jest.fn(), fetchSingleResource));
            setTimeout(() => {
                expect(fetchSingleResource).not.toHaveBeenCalled();
                expect(scrollListingToPosition).toHaveBeenCalledWith(absoluteTop, { itemHeight });
                done();
            }, 20);
        });

        it('должен вызвать функцию обработки ошибки, если запрошенный ресурс вернулся с ошибкой', (done) => {
            const onError = jest.fn();
            updateCluster();

            fetchSingleResource.mockImplementationOnce(() => () => {
                return Promise.resolve({ error: true });
            });

            store.dispatch(scrollToPhotosliceItem(resourceId, grid, {}, onError, fetchSingleResource));
            setTimeout(() => {
                expect(onError).toHaveBeenCalled();
                done();
            }, 20);
        });

        it('должен вызвать функцию обработки ошибки, если запрошенный ресурс не найден в items кластера', (done) => {
            const onError = jest.fn();
            store.dispatch({
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        fetched: [{
                            id: '0000001549115317000_0000001549115319000',
                            size: 1,
                            items: [{ id: '/disk/another-image.jpg' }]
                        }],
                        missing: []
                    }
                }
            });

            store.dispatch(scrollToPhotosliceItem(resourceId, grid, {}, onError, fetchSingleResource));
            setTimeout(() => {
                expect(onError).toHaveBeenCalled();
                done();
            }, 20);
        });
    });

    describe('fetchAndApplyDiff', () => {
        let originalRawFetchModel;

        beforeEach(() => {
            originalRawFetchModel = rawFetchModel.default;
        });
        afterEach(() => {
            rawFetchModel.default = originalRawFetchModel;
        });

        it('в случае ошибки - должен сделать запрос снапшота', (done) => {
            const state = {
                photoslice: {
                    photosliceId: 'my_photo_slice_old',
                    isLoaded: true,
                    clusters: ['cluster1'],
                    clustersByIds: {
                        cluster1: {
                            id: 'cluster1',
                            size: 1
                        }
                    }
                },
                environment: {
                    session: {
                        locale: 'ru',
                        experiment: {}
                    }
                }
            };

            const responses = {
                getDiff: {
                    id: 'HTTP_404'
                },
                initSnapshot: {
                    photoslice_id: 'my_photo_slice_new',
                    revision: 2
                },
                getSnapshot: {
                    id: 'my_photo_slice_new',
                    items: [{
                        id: 'cluster1',
                        size: 2
                    }]
                }
            };

            const getState = jest.fn().mockReturnValue(state);

            const fetch = jest.fn()
                .mockRejectedValueOnce(responses.getDiff)
                .mockResolvedValueOnce(responses.initSnapshot)
                .mockResolvedValueOnce(responses.getSnapshot);

            const dispatch = jest.fn()
                .mockImplementationOnce((action) => {
                    action(dispatch, getState);
                })
                .mockImplementationOnce(() => {})
                .mockImplementationOnce((action) => {
                    expect(action).toEqual({
                        type: FETCH_PHOTOSLICE_SUCCESS,
                        payload: {
                            id: 'my_photo_slice_new',
                            isLoaded: true,
                            photosliceId: 'my_photo_slice_new',
                            items: [{
                                id: 'cluster1',
                                size: 2
                            }]
                        }
                    });

                    done();
                });

            rawFetchModel.default = fetch;

            fetchAndApplyDiff()(dispatch, getState);
        });

        it('в случае успешной загрузки обновлений - должен их применить', (done) => {
            const store = getStore(true);
            store.replaceReducer(combineReducers(Object.assign({ photoslice }, commonReducers)));

            store.dispatch({
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: {
                    photosliceId: 'photosliceId1',
                    revision: 1,
                    items: [
                        {
                            from: 1549896744000,
                            to: 1549897388000,
                            id: '0000001549115317000_0000001549115319000',
                            size: 1
                        },
                        {
                            from: 1549896763000,
                            to: 1549897356000,
                            id: '0000001549115317000_0000001549115317000',
                            size: 1
                        }
                    ]
                }
            });

            rawFetchModel.default = jest.fn(() => {
                return Promise.resolve({
                    items: [{
                        base_revision: 1,
                        revision: 2,
                        index_changes: [{
                            cluster_id: '0000001549115317000_0000001549115319000',
                            change_type: 'delete'
                        }, {
                            cluster_id: '0000001549115317000_0000001549115317000',
                            change_type: 'update',
                            data: {
                                city: { change_type: 'insert', data: 'Москва' }
                            }
                        }]
                    }],
                    total: 1,
                    limit: 1,
                    revision: 2
                });
            });

            store.dispatch(fetchAndApplyDiff()).then(() => {
                const { revision, clustersByIds, clusters } = store.getState().photoslice;
                // обновилась ревизия
                expect(revision).toBe(2);

                // удалился кластер
                expect(clustersByIds['0000001549115317000_0000001549115319000']).toBeFalsy();
                expect(clusters.includes('0000001549115317000_0000001549115319000')).toBe(false);

                // обновился кластер
                expect(clustersByIds['0000001549115317000_0000001549115317000'].city).toBe('Москва');
                done();
            });
        });
    });

    describe('setPhotoView', () => {
        let store;
        const originalSaveSettings = settingsActions.saveSettings;
        beforeEach(() => {
            settingsActions.saveSettings = jest.fn(() => ({ type: 'MOCK' }));
            store = getStore(true);
        });
        afterEach(() => {
            settingsActions.saveSettings = originalSaveSettings;
        });

        it('По умолчаниию должен сохранять настройку для фотосреза', () => {
            store.dispatch(setPhotoView(PHOTO_GRID_TYPES.TILE));
            expect(settingsActions.saveSettings).toBeCalledWith({ key: 'photoView', value: PHOTO_GRID_TYPES.TILE });
        });

        it('Если выставлен фильтр, должен сохранить настройку для соотвествующего альбома-среза', () => {
            store.dispatch(commonActions.updatePage({ filter: 'beautiful' }));
            store.dispatch(setPhotoView(PHOTO_GRID_TYPES.TILE));
            expect(settingsActions.saveSettings).toBeCalledWith({ key: 'beautifulAlbumPhotoView', value: PHOTO_GRID_TYPES.TILE });
        });
    });
});
