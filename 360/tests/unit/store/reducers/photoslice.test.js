import photoslice from '../../../../components/redux/store/reducers/photoslice';
import {
    UPDATE_CLUSTERS_AND_RESOURCES,
    MOVE_CLONED_RESOURCES,
    DESTROY_RESOURCE,
    APPLY_DIFF,
    RESTORE_DELETED_ITEM,
    EXCLUDE_RESOURCES_FROM_ALBUM,
    DESELECT_ALL
} from '../../../../components/redux/store/actions/types';
import { FETCH_PHOTOSLICE_SUCCESS } from '../../../../components/redux/store/actions/photo-types';
import deepFreeze from 'deep-freeze';
import clusters from '../../fixtures/clusters';

describe('photoslice reducer', () => {
    describe('UPDATE_CLUSTERS_AND_RESOURCES', () => {
        let defaultState;
        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clusters, photosliceId: '1', revision: 1 }
            });
        });

        it('Должен добавлять ресурсы в кластер', () => {
            expect(defaultState.clustersByIds['0000001431412084000_0000001431412084000'].items).toBeFalsy();

            const items = [{ id: '/disk/image1.jpg', itemId: 'r1' }];
            const newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: '0000001431412084000_0000001431412084000',
                            size: 1,
                            items
                        }]
                    }
                }
            });

            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual(items);
        });

        it('Должен удалять пропавшие кластеры, но не информацию о ниих', () => {
            expect(defaultState.clustersByIds['0000001431412084000_0000001431412084000']).not.toBeFalsy();
            expect(defaultState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(true);

            const newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: ['0000001431412084000_0000001431412084000'],
                        fetched: []
                    }
                }
            });

            expect(newState.clustersByIds['0000001431412084000_0000001431412084000']).not.toBeFalsy();
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(false);
            expect(newState.structureVersion).toBe(2);
        });

        it('Должен обновлять ресурсы кластера', () => {
            expect(defaultState.clustersByIds['0000001431412084000_0000001431412084000']).not.toBeFalsy();
            expect(defaultState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(true);

            const items = [{ id: '/disk/image1.jpg', itemId: 'r1' }];
            const newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        added: [{
                            id: '0000001431412084000_0000001431412084000',
                            size: 1,
                            items
                        }]
                    }
                }
            });

            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual(items);
            expect(newState.structureVersion).toBe(2);
        });

        it('Должен добавлять новый кластер, если он есть в clustersByIds, но нет clusters', () => {
            const cluster = {
                id: '0000001431412084000_0000001431412084001',
                size: 1,
                items: [{ id: '/disk/image1.jpg', itemId: 'r1' }]
            };
            defaultState.clustersByIds['0000001431412084000_0000001431412084001'] = cluster;

            expect(defaultState.clusters.includes('0000001431412084000_0000001431412084001')).toBe(false);

            const newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        added: [cluster]
                    }
                }
            });

            expect(newState.clustersByIds['0000001431412084000_0000001431412084001']).not.toBeFalsy();
            expect(newState.clusters.includes('0000001431412084000_0000001431412084001')).toBe(true);
            expect(newState.structureVersion).toBe(2);
        });

        it('Должен обновить кластер при добавлении фото, среди которых есть исключенные из альбомов', () => {
            let newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: '0000001431412084000_0000001431412084000',
                            size: 3,
                            albums: { beautiful: 2, camera: 2 },
                            items: [
                                { id: '/disk/image1.jpg', itemId: 'r1', albums: ['beautiful'] },
                                { id: '/disk/image2.jpg', itemId: 'r2', albums: ['beautiful', 'camera'] },
                                { id: '/disk/image3.jpg', itemId: 'r3', albums: ['camera'] }
                            ]
                        }]
                    }
                }
            });
            newState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    resources: [
                        {
                            id: '/disk/image1.jpg',
                            clusterId: '0000001431412084000_0000001431412084000',
                            meta: { albums_exclusions: ['beautiful', 'camera'] }
                        },
                        {
                            id: '/disk/image2.jpg',
                            clusterId: '0000001431412084000_0000001431412084000',
                            meta: { albums_exclusions: ['beautiful'] }
                        },
                        {
                            id: '/disk/image3.jpg',
                            clusterId: '0000001431412084000_0000001431412084000',
                            meta: { albums_exclusions: ['beautiful'] }
                        }
                    ]
                }
            });

            const cluster = newState.clustersByIds['0000001431412084000_0000001431412084000'];
            expect(cluster.albums).toEqual({ camera: 2 });
            expect(cluster.items[0].albums).toEqual([]);
            expect(cluster.items[1].albums).toEqual(['camera']);
            expect(cluster.items[2].albums).toEqual(['camera']);
            expect(newState.structureVersion).toBe(2);
        });
    });

    describe('MOVE_CLONED_RESOURCES', () => {
        let defaultState;
        const firstClusterId = '0000001099472401000_0000001099472405000';
        const secondClusterId = '0000001431412084000_0000001431412084000';
        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clusters, photosliceId: '1', revision: 1 }
            });

            defaultState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: firstClusterId,
                            size: 5,
                            items: [
                                { id: '/disk/image1.jpg', itemId: 'r5', width: 400, height: 300 },
                                { id: '/disk/image2.jpg', itemId: 'r4', width: 300, height: 400, beauty: 4.5 },
                                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                                { id: '/disk/image4.jpg', itemId: 'r2' },
                                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
                            ]
                        }, {
                            id: secondClusterId,
                            size: 1,
                            items: [
                                { id: '/disk/image6.jpg', itemId: 'r11' }
                            ]
                        }]
                    }
                }
            });
        });

        it('Если перемещается ресурс из кластера, должен обновить id', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/disk/subfolder/image2.jpg', meta: { photoslice_time: 1 } }
                    }]
                }
            });
            expect(newState.clustersByIds[firstClusterId].items).toEqual([
                { id: '/disk/image1.jpg', itemId: 'r5', width: 400, height: 300 },
                { id: '/disk/subfolder/image2.jpg', itemId: 'r4', width: 300, height: 400, beauty: 4.5 },
                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                { id: '/disk/image4.jpg', itemId: 'r2' },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
        });

        it('Если у перемещённого из кластера ресурса нет photoslice_time, ресурс должен удалиться из кластера', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/disk/subfolder/image2.jpg' }
                    }]
                }
            });

            expect(newState.clustersByIds[firstClusterId].items).toEqual([
                { id: '/disk/image1.jpg', itemId: 'r5', width: 400, height: 300 },
                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                { id: '/disk/image4.jpg', itemId: 'r2' },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
        });

        it('Если удаляетя ресурс из кластера, должен удалить из списка ресурсов кластера и добавить в список удаленных item`ов', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image2.jpg', isInTrash: true }
                    }]
                }
            });

            const cluster = newState.clustersByIds[firstClusterId];
            expect(cluster.items).toEqual([
                { id: '/disk/image1.jpg', itemId: 'r5', width: 400, height: 300 },
                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                { id: '/disk/image4.jpg', itemId: 'r2' },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
            expect(cluster.size).toBe(4);
            expect(newState.clustersDeletedItems[firstClusterId])
                .toEqual([{ id: '/disk/image2.jpg', itemId: 'r4', width: 300, height: 400, beauty: 4.5 }]);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });

        it('Если удаляется последний ресурс в кластере, должен удалиться кластер из clusters, но не из clustersByIds', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image6.jpg', clusterId: secondClusterId },
                        dst: { id: '/trash/image6.jpg', isInTrash: true }
                    }]
                }
            });

            const cluster = newState.clustersByIds[secondClusterId];
            expect(cluster).toBeTruthy();
            expect(newState.clusters.includes(secondClusterId)).toBe(false);
            expect(newState.clustersDeletedItems[secondClusterId]).toEqual([{ id: '/disk/image6.jpg', itemId: 'r11' }]);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });

        it('Должен удалить дубли если dst уже есть в кластере', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: firstClusterId },
                        dst: { id: '/disk/image4.jpg', meta: { photoslice_time: 1 } }
                    }]
                }
            });
            const cluster = newState.clustersByIds[firstClusterId];
            expect(cluster.items).toEqual([
                { id: '/disk/image4.jpg', itemId: 'r5', width: 400, height: 300 },
                { id: '/disk/image2.jpg', itemId: 'r4', width: 300, height: 400, beauty: 4.5 },
                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });

        it('Удаление пачки ресурсов из одного кластера', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image1.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/imag2.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image3.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image3.jpg', isInTrash: true }
                    }]
                }
            });
            const cluster = newState.clustersByIds[firstClusterId];
            expect(cluster.items).toEqual([
                { id: '/disk/image4.jpg', itemId: 'r2' },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });

        it('Удаление всего кластера', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image1.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/imag2.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image3.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image3.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image4.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image4.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image5.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image5.jpg', isInTrash: true }
                    }]
                }
            });
            expect(newState.clustersByIds[firstClusterId]).toBeTruthy();
            expect(newState.clusters.includes(firstClusterId)).toBe(false);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });

        it('Удаление из нескольких кластеров', () => {
            const newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/image1.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image2.jpg', clusterId: firstClusterId },
                        dst: { id: '/trash/imag2.jpg', isInTrash: true }
                    }, {
                        src: { id: '/disk/image6.jpg', clusterId: secondClusterId },
                        dst: { id: '/trash/image6.jpg', isInTrash: true }
                    }]
                }
            });
            expect(newState.clustersByIds[firstClusterId].items).toEqual([
                { id: '/disk/image3.jpg', itemId: 'r3', width: 100, height: 100, beauty: -1 },
                { id: '/disk/image4.jpg', itemId: 'r2' },
                { id: '/disk/image5.jpg', itemId: 'r1', width: 5000, height: 3000, beauty: 123 }
            ]);
            expect(newState.clustersByIds[secondClusterId]).toBeTruthy();
            expect(newState.clusters.includes(secondClusterId)).toBe(false);
            expect(newState.structureVersion).toBe(defaultState.structureVersion + 1);
        });
    });

    describe('RESTORE_DELETED_ITEM', () => {
        let defaultState;
        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clusters, photosliceId: '1', revision: 1 }
            });

            defaultState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: '0000001099472401000_0000001099472405000',
                            size: 2,
                            items: [
                                { id: '/disk/image1.jpg', itemId: 'r2' },
                                { id: '/disk/image2.jpg', itemId: 'r1' }
                            ]
                        }, {
                            id: '0000001431412084000_0000001431412084000',
                            size: 1,
                            items: [
                                { id: '/disk/image6.jpg', itemId: 'r11' }
                            ]
                        }]
                    }
                }
            });
        });

        it('При восстановлении удаленного ресурса, если его нет в списке удаленных - он не должен добавиться в items', () => {
            const newState = photoslice(defaultState, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    id: '/disk/image3.jpg',
                    clusterId: '0000001431412084000_0000001431412084000'
                }
            });
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual([
                { id: '/disk/image6.jpg', itemId: 'r11' }
            ]);
        });

        it('При восстановлении удаленного ресурса, он должен добавиться в items соответствующего кластера', () => {
            let newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image6.jpg', clusterId: '0000001431412084000_0000001431412084000' },
                        dst: { id: '/trash/image6.jpg', isInTrash: true }
                    }]
                }
            });

            expect(newState.clustersDeletedItems['0000001431412084000_0000001431412084000'])
                .toEqual([{ id: '/disk/image6.jpg', itemId: 'r11' }]);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000']).toBeTruthy();
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(false);

            newState = photoslice(newState, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    id: '/disk/image6.jpg',
                    clusterId: '0000001431412084000_0000001431412084000'
                }
            });
            expect(newState.clustersDeletedItems['0000001431412084000_0000001431412084000']).toEqual([]);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual([
                { id: '/disk/image6.jpg', itemId: 'r11' }
            ]);
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(true);
        });
    });

    describe('DESTROY_RESOURCE', () => {
        let defaultState;
        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clusters, photosliceId: '1', revision: 1 }
            });

            defaultState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: '0000001099472401000_0000001099472405000',
                            size: 2,
                            items: [
                                { id: '/disk/image1.jpg', itemId: 'r2' },
                                { id: '/disk/image2.jpg', itemId: 'r1' }
                            ]
                        }, {
                            id: '0000001431412084000_0000001431412084000',
                            size: 1,
                            items: [
                                { id: '/disk/image6.jpg', itemId: 'r11' }
                            ]
                        }]
                    }
                }
            });
        });

        it('Удаляющийся ресурс должен удалиться из items соответствующего кластера и повысить structureVersion', () => {
            const newState = photoslice(defaultState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image1.jpg',
                    data: { clusterId: '0000001099472401000_0000001099472405000' }
                }
            });
            expect(newState.clustersByIds['0000001099472401000_0000001099472405000'].items).toEqual([
                { id: '/disk/image2.jpg', itemId: 'r1' }
            ]);
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });

        it('Если удаляется последний ресурс в кластере, должен удалиться кластер из clusters, но не из clustersByIds, и повысить structureVersion', () => {
            const newState = photoslice(defaultState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image6.jpg',
                    data: { clusterId: '0000001431412084000_0000001431412084000' }
                }
            });
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].size).toBe(0);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual([]);
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(false);
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });

        it('Если удаляется ресурс который уже удален из кластера, store не должен поменяться', () => {
            const newState = photoslice(defaultState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image8.jpg',
                    data: { clusterId: '0000001431412084000_0000001431412084000' }
                }
            });
            expect(newState).toBe(defaultState);
        });

        it('Если удаляется ресурс не из среза - store не должен поменяться', () => {
            const newState = photoslice(defaultState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image7.jpg',
                    data: {}
                }
            });
            expect(newState).toBe(defaultState);
        });

        it('Если в списке удаляемых item`ов кластера, при удалении ресурса, он оттуда должен удалиться, structureVersion не должен измениться', () => {
            let newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: '0000001099472401000_0000001099472405000' },
                        dst: { id: '/trash/image1.jpg', isInTrash: true }
                    }]
                }
            });
            expect(newState.clustersDeletedItems['0000001099472401000_0000001099472405000'])
                .toEqual([{ id: '/disk/image1.jpg', itemId: 'r2' }]);

            const newStructureVersion = defaultState.structureVersion + 1;
            expect(newState.structureVersion).toEqual(newStructureVersion);

            newState = photoslice(newState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image1.jpg',
                    data: {
                        clusterId: '0000001099472401000_0000001099472405000'
                    }
                }
            });
            expect(newState.clustersDeletedItems['0000001099472401000_0000001099472405000']).toEqual([]);
            expect(newState.structureVersion).toEqual(newStructureVersion);
        });

        it('Если в списке удаляемых item`ов кластера - несколько, при удалении ресурса, он оттуда должен удалиться, structureVersion не должен измениться', () => {
            let newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image2.jpg', clusterId: '0000001099472401000_0000001099472405000' },
                        dst: { id: '/trash/image2.jpg', isInTrash: true }
                    }]
                }
            });
            newState = photoslice(newState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image1.jpg', clusterId: '0000001099472401000_0000001099472405000' },
                        dst: { id: '/trash/image1.jpg', isInTrash: true }
                    }]
                }
            });
            expect(newState.clustersDeletedItems['0000001099472401000_0000001099472405000'])
                .toEqual([{ id: '/disk/image2.jpg', itemId: 'r1' }, { id: '/disk/image1.jpg', itemId: 'r2' }]);
            expect(newState.clustersByIds['0000001099472401000_0000001099472405000']).toBeTruthy();

            const newStructureVersion = defaultState.structureVersion + 2;
            expect(newState.structureVersion).toEqual(newStructureVersion);

            newState = photoslice(newState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image1.jpg',
                    data: {
                        clusterId: '0000001099472401000_0000001099472405000'
                    }
                }
            });
            expect(newState.clustersDeletedItems['0000001099472401000_0000001099472405000'])
                .toEqual([{ id: '/disk/image2.jpg', itemId: 'r1' }]);
            expect(newState.structureVersion).toEqual(newStructureVersion);
        });

        it('Если в списке удаляемых item`ов кластера - 1 item и это последний ресурс кластера, то при удалении ресурса, список для этого кластера должен очиститься, structureVersion не должен измениться', () => {
            let newState = photoslice(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/image6.jpg', clusterId: '0000001431412084000_0000001431412084000' },
                        dst: { id: '/trash/image6.jpg', isInTrash: true }
                    }]
                }
            });
            expect(newState.clustersDeletedItems['0000001431412084000_0000001431412084000'])
                .toEqual([{ id: '/disk/image6.jpg', itemId: 'r11' }]);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000']).toBeTruthy();
            const newStructureVersion = defaultState.structureVersion + 1;
            expect(newState.structureVersion).toEqual(newStructureVersion);

            newState = photoslice(newState, {
                type: DESTROY_RESOURCE,
                payload: {
                    id: '/disk/image6.jpg',
                    data: {
                        clusterId: '0000001431412084000_0000001431412084000'
                    }
                }
            });
            expect(newState.clustersDeletedItems['0000001431412084000_0000001431412084000']).toEqual([]);
            expect(newState.structureVersion).toEqual(newStructureVersion);
        });
    });

    describe('APPLY_DIFF', () => {
        let defaultState;
        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clusters, photosliceId: '1', revision: 1 }
            });

            defaultState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: '0000001099472401000_0000001099472405000',
                            size: 2,
                            albums: { beautiful: 1, unbeautiful: 1 },
                            items: [
                                { id: '/disk/image1.jpg', itemId: 'r2', albums: ['beautiful'] },
                                { id: '/disk/image2.jpg', itemId: 'r1', albums: ['unbeautiful'] }
                            ]
                        }, {
                            id: '0000001431412084000_0000001431412084000',
                            size: 1,
                            items: [
                                { id: '/disk/image6.jpg', itemId: 'r11' }
                            ]
                        }]
                    }
                }
            });
        });

        it('Правильно применяются - добавление, обновление и удаление кластера', () => {
            const newState = photoslice(defaultState, {
                type: APPLY_DIFF,
                payload: {
                    diff: {
                        revision: 2,
                        clusters: {
                            '0000001431412084000_0000001431412083000': {
                                insert: {
                                    id: '0000001431412084000_0000001431412083000',
                                    from: 1100250001000,
                                    to: 1100250010000,
                                    size: 1,
                                    albums: [{ change_type: 'insert', album: 'beautiful', count: 1 }],
                                    items: [
                                        {
                                            id: 'image3.jpg',
                                            itemId: 'r25',
                                            albums: ['beautiful']
                                        }
                                    ]
                                },
                                items: {
                                    r25: {}
                                }
                            },
                            '0000001099472401000_0000001099472405000': {
                                update: {
                                    id: '0000001099472401000_0000001099472405000',
                                    city: { change_type: 'insert', data: 'Москва' },
                                    streets: [{ change_type: 'insert', data: 'Большая Переяславская', place_index: 0 }],
                                    albums: [{ change_type: 'update', album: 'beautiful', count: 1 }],
                                }
                            },
                            '0000001431412084000_0000001431412084000': {
                                delete: true
                            }
                        }
                    }
                }
            });

            // кластер добавился
            expect(newState.clusters.includes('0000001431412084000_0000001431412083000')).toBe(true);
            expect(newState.clustersByIds['0000001431412084000_0000001431412083000']).toEqual({
                id: '0000001431412084000_0000001431412083000',
                from: 1100250001000,
                to: 1100250010000,
                size: 1,
                albums: { beautiful: 1 },
                items: [
                    {
                        id: 'image3.jpg',
                        itemId: 'r25',
                        albums: ['beautiful']
                    }
                ]
            });

            // кластер обновился
            expect(newState.clusters.includes('0000001099472401000_0000001099472405000')).toBe(true);
            expect(newState.clustersByIds['0000001099472401000_0000001099472405000']).toEqual({
                id: '0000001099472401000_0000001099472405000',
                size: 2,
                albums: { beautiful: 1, unbeautiful: 1 },
                from: 1099472401000,
                to: 1099472405000,
                items: [
                    { id: '/disk/image1.jpg', itemId: 'r2', albums: ['beautiful'] },
                    { id: '/disk/image2.jpg', itemId: 'r1', albums: ['unbeautiful'] }
                ],
                city: 'Москва',
                streets: ['Большая Переяславская']
            });

            // кластер удалился
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(false);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000']).toBeFalsy();
        });

        it('Правильно применяются - добавление, обновдение и удаление item`ов', () => {
            const newState = photoslice(defaultState, {
                type: APPLY_DIFF,
                payload: {
                    diff: {
                        revision: 2,
                        clusters: {
                            '0000001099472401000_0000001099472405000': {
                                update: {
                                    id: '0000001099472401000_0000001099472405000',
                                    albums: [
                                        { change_type: 'delete', album: 'unbeautiful' },
                                        { change_type: 'update', album: 'beautiful', count: 2 },
                                        { change_type: 'insert', album: 'camera', count: 2 },
                                    ]
                                },
                                items: {
                                    r0: {
                                        insert: {
                                            id: '/disk/image3.jpg',
                                            itemId: 'r0',
                                            albums: ['beautiful', 'camera']
                                        }
                                    },
                                    r4: {
                                        insert: {
                                            id: '/disk/image4.jpg',
                                            itemId: 'r4',
                                            albums: ['camera']
                                        }
                                    },
                                    r2: {
                                        update: {
                                            id: '/disk/image7.jpg',
                                            itemId: 'r2'
                                        }
                                    },
                                    r1: {
                                        delete: true
                                    }
                                }
                            },
                            '0000001431412084000_0000001431412084000': {
                                items: {
                                    r11: {
                                        delete: true
                                    }
                                }
                            }
                        }
                    }
                }
            });

            // в кластер добавлены item'ы и удален item
            expect(newState.clusters.includes('0000001099472401000_0000001099472405000')).toBe(true);
            expect(newState.clustersByIds['0000001099472401000_0000001099472405000']).toEqual({
                id: '0000001099472401000_0000001099472405000',
                size: 3,
                albums: { beautiful: 2, camera: 2 },
                from: 1099472401000,
                to: 1099472405000,
                items: [
                    { id: '/disk/image4.jpg', itemId: 'r4', albums: ['camera'] },
                    { id: '/disk/image7.jpg', itemId: 'r2', albums: ['beautiful'] },
                    { id: '/disk/image3.jpg', itemId: 'r0', albums: ['beautiful', 'camera'] }
                ]
            });

            // после удаления последнего item'а кластер удалился из списка кластеров,
            // но информация о нём должна остаться
            expect(newState.clusters.includes('0000001431412084000_0000001431412084000')).toBe(false);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].size).toBe(0);
            expect(newState.clustersByIds['0000001431412084000_0000001431412084000'].items).toEqual([]);
        });

        it('Правильно применяются - обновление альбомов кластера при обновлении альбомов айтема', () => {
            const newState = photoslice(defaultState, {
                type: APPLY_DIFF,
                payload: {
                    diff: {
                        revision: 2,
                        clusters: {
                            '0000001099472401000_0000001099472405000': {
                                update: {
                                    id: '0000001099472401000_0000001099472405000',
                                    albums: []
                                },
                                items: {
                                    r2: {
                                        update: {
                                            id: '/disk/image1.jpg',
                                            itemId: 'r2',
                                            albums: ['camera']
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });

            expect(newState.clusters.includes('0000001099472401000_0000001099472405000')).toBe(true);
            expect(newState.clustersByIds['0000001099472401000_0000001099472405000']).toEqual({
                id: '0000001099472401000_0000001099472405000',
                size: 2,
                albums: { unbeautiful: 1, camera: 1 },
                from: 1099472401000,
                to: 1099472405000,
                items: [
                    { id: '/disk/image1.jpg', itemId: 'r2', albums: ['camera'] },
                    { id: '/disk/image2.jpg', itemId: 'r1', albums: ['unbeautiful'] }
                ]
            });
        });

        it('Правильно применится на незагруженный кластер', () => {
            const newState = photoslice(defaultState, {
                type: APPLY_DIFF,
                payload: {
                    diff: {
                        revision: 2,
                        clusters: {
                            '0000001099213201000_0000001099213215000': {
                                update: {
                                    id: '0000001099213201000_0000001099213215000',
                                    from: 1099213101000,
                                    to: 1099213215000,
                                    size: 12,
                                    city: { change_type: 'insert', data: 'Москва' },
                                    streets: [{ change_type: 'insert', data: 'Большая Переяславская', place_index: 0 }],
                                    albums: [{ change_type: 'insert', album: 'beautiful', count: 2 }]
                                },
                                items: {
                                    r1: {
                                        delete: true
                                    }
                                }
                            }
                        }
                    }
                }
            });

            expect(newState.clusters.includes('0000001099213201000_0000001099213215000')).toBe(true);
            expect(newState.clustersByIds['0000001099213201000_0000001099213215000']).toEqual({
                id: '0000001099213201000_0000001099213215000',
                from: 1099213101000,
                to: 1099213215000,
                size: 12,
                albums: { beautiful: 2 },
                city: 'Москва',
                streets: ['Большая Переяславская']
            });
        });
    });

    describe('EXCLUDE_RESOURCES_FROM_ALBUM', () => {
        let defaultState;
        const clustersWithAlbums = [{
            from: 1100250001000,
            to: 1100250010000,
            size: 4,
            id: '0000001100250001000_0000001100250010000'
        }, {
            from: 1100163601000,
            to: 1100163613000,
            size: 2,
            id: '0000001100163601000_0000001100163613000'
        }, {
            from: 1100077201000,
            to: 1100077211000,
            size: 2,
            id: '0000001100077201000_0000001100077211000'
        }];

        beforeEach(() => {
            defaultState = photoslice(undefined, {
                type: FETCH_PHOTOSLICE_SUCCESS,
                payload: { items: clustersWithAlbums.slice(0), photosliceId: '1', revision: 1 }
            });

            defaultState = photoslice(defaultState, {
                type: UPDATE_CLUSTERS_AND_RESOURCES,
                payload: {
                    clusters: {
                        missing: [],
                        fetched: [{
                            id: clustersWithAlbums[0].id,
                            albums: { beautiful: 1, unbeautiful: 2 },
                            size: 4,
                            items: [
                                { id: '/disk/image10.jpg', itemId: 'r10', albums: ['unbeautiful'] },
                                { id: '/disk/image11.jpg', itemId: 'r11', albums: [] },
                                { id: '/disk/image12.jpg', itemId: 'r12', albums: ['unbeautiful'] },
                                { id: '/disk/image13.jpg', itemId: 'r13', albums: ['beautiful'] }
                            ]
                        }, {
                            id: clustersWithAlbums[1].id,
                            albums: { beautiful: 1, unbeautiful: 1 },
                            size: 2,
                            items: [
                                { id: '/disk/image20.jpg', itemId: 'r20', albums: ['unbeautiful'] },
                                { id: '/disk/image21.jpg', itemId: 'r21', albums: ['beautiful'] }
                            ]
                        }, {
                            id: clustersWithAlbums[2].id,
                            albums: { beautiful: 2 },
                            size: 2,
                            items: [
                                { id: '/disk/image30.jpg', itemId: 'r30', albums: ['beautiful'] },
                                { id: '/disk/image31.jpg', itemId: 'r31', albums: ['beautiful'] }
                            ]
                        }]
                    }
                }
            });
        });

        it('Исключается не последнее фото из альбома', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [{ clusterId: clustersWithAlbums[0].id, id: '/disk/image12.jpg' }],
                    filter: 'unbeautiful'
                }
            });
            expect(newState.clustersByIds[clustersWithAlbums[0].id].items).toEqual([
                { id: '/disk/image10.jpg', itemId: 'r10', albums: ['unbeautiful'] },
                { id: '/disk/image11.jpg', itemId: 'r11', albums: [] },
                { id: '/disk/image12.jpg', itemId: 'r12', albums: [] },
                { id: '/disk/image13.jpg', itemId: 'r13', albums: ['beautiful'] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[0].id].albums).toEqual({ beautiful: 1, unbeautiful: 1 });
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });

        it('Исключается последнее фото из альбома', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [{ clusterId: clustersWithAlbums[1].id, id: '/disk/image21.jpg' }],
                    filter: 'beautiful'
                }
            });

            expect(newState.clustersByIds[clustersWithAlbums[1].id].items).toEqual([
                { id: '/disk/image20.jpg', itemId: 'r20', albums: ['unbeautiful'] },
                { id: '/disk/image21.jpg', itemId: 'r21', albums: [] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[1].id].albums).toEqual({ unbeautiful: 1 });
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });

        it('Исключается фото из альбома, которого уже нет в кластере', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [{ clusterId: clustersWithAlbums[0].id, id: '/disk/image14.jpg' }],
                    filter: 'beautiful'
                }
            });

            expect(newState.clustersByIds[clustersWithAlbums[0].id].items).toEqual([
                { id: '/disk/image10.jpg', itemId: 'r10', albums: ['unbeautiful'] },
                { id: '/disk/image11.jpg', itemId: 'r11', albums: [] },
                { id: '/disk/image12.jpg', itemId: 'r12', albums: ['unbeautiful'] },
                { id: '/disk/image13.jpg', itemId: 'r13', albums: ['beautiful'] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[0].id].albums).toEqual({ beautiful: 1, unbeautiful: 2 });
            expect(newState.structureVersion).toEqual(defaultState.structureVersion);
        });

        it('Исключается несколько фото из альбома, так что в кластере больше нет фото из данного альбома', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [
                        { clusterId: clustersWithAlbums[2].id, id: '/disk/image30.jpg' },
                        { clusterId: clustersWithAlbums[2].id, id: '/disk/image31.jpg' }
                    ],
                    filter: 'beautiful'
                }
            });

            expect(newState.clustersByIds[clustersWithAlbums[2].id].items).toEqual([
                { id: '/disk/image30.jpg', itemId: 'r30', albums: [] },
                { id: '/disk/image31.jpg', itemId: 'r31', albums: [] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[2].id].albums).toEqual({});
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });

        it('Исключается фото из альбома, которое уже исключено из альбома', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [
                        { clusterId: clustersWithAlbums[2].id, id: '/disk/image30.jpg' }
                    ],
                    filter: 'camera'
                }
            });

            expect(newState.clustersByIds[clustersWithAlbums[2].id].items).toEqual([
                { id: '/disk/image30.jpg', itemId: 'r30', albums: ['beautiful'] },
                { id: '/disk/image31.jpg', itemId: 'r31', albums: ['beautiful'] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[2].id].albums).toEqual({ beautiful: 2 });
            expect(newState.structureVersion).toEqual(defaultState.structureVersion);
        });

        it('Отмена исключения фото из альбома', () => {
            const newState = photoslice(defaultState, {
                type: EXCLUDE_RESOURCES_FROM_ALBUM,
                payload: {
                    resources: [{ clusterId: clustersWithAlbums[1].id, id: '/disk/image20.jpg' }],
                    filter: 'camera',
                    cancel: true
                }
            });

            expect(newState.clustersByIds[clustersWithAlbums[1].id].items).toEqual([
                { id: '/disk/image20.jpg', itemId: 'r20', albums: ['unbeautiful', 'camera'] },
                { id: '/disk/image21.jpg', itemId: 'r21', albums: ['beautiful'] }
            ]);
            expect(newState.clustersByIds[clustersWithAlbums[1].id].albums)
                .toEqual({ beautiful: 1, unbeautiful: 1, camera: 1 });
            expect(newState.structureVersion).toEqual(defaultState.structureVersion + 1);
        });
    });

    describe('DESELECT_ALL', () => {
        it('Должен сбрасывать выделение', () => {
            const state = {
                isLoading: false,
                structureVersion: 0,
                itemsVersion: 0,
                resourcesVersion: 0,
                totalSelected: 10,
                unfilteredTotalSelected: 10,
                missingSelected: 5
            };
            deepFreeze(state);
            const newState = photoslice(state, {
                type: DESELECT_ALL
            });
            expect(newState.totalSelected).toBe(0);
            expect(newState.unfilteredTotalSelected).toBe(0);
            expect(newState.missingSelected).toBe(0);
        });
    });
});
