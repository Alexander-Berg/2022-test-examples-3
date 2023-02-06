import {
    findClusterIndex,
    findClusterByTimestamp,
    getNewItemPosition,
    findItemIndex,
    findItemIndexById,
    formatDataDiff,
    applyDiffDataCluster,
    iterateThroughClustersInIndexPaths
} from '../../../components/helpers/photoslice';

import { photoslice } from '../fixtures/photoslice';

const clusters = ['0000001431412084008_0000001431412084000', '0000001431412084005_0000001431412084000', '0000001431412084003_0000001431412084000', '-000000000010801000_-000000000010801000', '-000001733723824000_-000001733723824000'];
const items = [
    { id: '/disk/8', itemId: '2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661108' },
    { id: '/disk/5', itemId: '2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661105' },
    { id: '/disk/3', itemId: '2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661103' }
];

/**
 * @returns {Cluster[]}
 */
const getAllClusters = () => photoslice.clusters.map((clusterId) => photoslice.clustersByIds[clusterId]);

const diffs = {
    formatDataDiff: {
        'добавился кластер с 1 записью': {
            diffData: {
                items: [{
                    base_revision: 1,
                    revision: 2,
                    index_changes: [{
                        cluster_id: 'inserted_cluster',
                        change_type: 'insert',
                        data: {
                            to: 1435482214000,
                            from: 1435482214000,
                            city: {
                                data: 'Москва',
                                change_type: 'insert'
                            },
                            size: 1,
                            albums: [{ change_type: 'insert', album: 'beautiful', count: 1 }]
                        }
                    }],
                    items_changes: [{
                        item_id: 'item_new',
                        cluster_id: 'inserted_cluster',
                        change_type: 'insert',
                        data: {
                            path: '/disk/item_new',
                            albums: ['beautiful']
                        }
                    }]
                }],
                total: 1,
                limit: 1,
                revision: 2

            },
            formattedData: {
                revision: 2,
                clusters: {
                    inserted_cluster: {
                        insert: {
                            id: 'inserted_cluster',
                            city: { change_type: 'insert', data: 'Москва' },
                            from: 1435482214000,
                            to: 1435482214000,
                            size: 1,
                            albums: {
                                beautiful: {
                                    change_type: 'insert',
                                    album: 'beautiful',
                                    count: 1
                                }
                            },
                            items: [{ id: '/disk/item_new', itemId: 'item_new', albums: ['beautiful'] }]
                        },
                        items: { item_new: {} }
                    }
                }
            }
        },
        'обновились данные кластера': {
            diffData: {
                items: [{
                    base_revision: 1,
                    revision: 2,
                    index_changes: [{
                        cluster_id: 'updated_cluster',
                        change_type: 'update',
                        data: {
                            to: 1435482214000,
                            from: 1435482214000,
                            city: {
                                data: 'Москва',
                                change_type: 'update'
                            },
                            streets: [{
                                data: 'Большая Переяславская',
                                change_type: 'insert',
                                place_index: 0
                            }],
                            size: 1
                        }
                    }]
                }],
                total: 1,
                limit: 1,
                revision: 2
            },
            formattedData: {
                revision: 2,
                clusters: {
                    updated_cluster: {
                        update: {
                            id: 'updated_cluster',
                            albums: {},
                            city: { change_type: 'update', data: 'Москва' },
                            streets: [{ change_type: 'insert', data: 'Большая Переяславская', place_index: 0 }],
                            from: 1435482214000,
                            to: 1435482214000,
                            size: 1
                        }
                    }
                }
            }
        },
        'удален 1 кластер и 1 item из другого кластера': {
            diffData: {
                items: [{
                    base_revision: 1,
                    revision: 2,
                    index_changes: [{
                        cluster_id: 'deleted_cluster',
                        change_type: 'delete'
                    }, {
                        cluster_id: 'other_cluster',
                        change_type: 'update',
                        data: {
                            albums: [{ change_type: 'delete', album: 'beautiful' }],
                        }
                    }],
                    items_changes: [{
                        item_id: 'item_deleted_cluster',
                        cluster_id: 'deleted_cluster',
                        change_type: 'delete',
                        data: {
                            path: '/disk/item'
                        }
                    }, {
                        item_id: 'item_deleted',
                        cluster_id: 'other_cluster',
                        change_type: 'delete',
                        data: {
                            path: '/disk/item_deleted'
                        }
                    }]
                }],
                total: 1,
                limit: 1,
                revision: 2
            },
            formattedData: {
                revision: 2,
                clusters: {
                    deleted_cluster: { delete: true },
                    other_cluster: {
                        update: {
                            id: 'other_cluster',
                            albums: {
                                beautiful: {
                                    album: 'beautiful',
                                    change_type: 'delete'
                                }
                            }
                        },
                        items: { item_deleted: { delete: true } }
                    }
                }
            }
        },
        'обновлен item, добавлен item и удален item': {
            diffData: {
                items: [{
                    base_revision: 1,
                    revision: 2,
                    items_changes: [{
                        item_id: 'c1_r1',
                        cluster_id: 'c1',
                        change_type: 'update',
                        data: {
                            path: '/disk/c1/c1_r1'
                        }
                    }]
                }, {
                    base_revision: 1,
                    revision: 2,
                    items_changes: [{
                        item_id: 'c2_r1',
                        cluster_id: 'c2',
                        change_type: 'insert',
                        data: {
                            path: '/disk/c2/c2_r1'
                        }
                    }]
                }, {
                    base_revision: 1,
                    revision: 2,
                    items_changes: [{
                        item_id: 'c3_r1',
                        cluster_id: 'c3',
                        change_type: 'delete'
                    }]
                }],
                total: 1,
                limit: 1,
                revision: 2
            },
            formattedData: {
                revision: 2,
                clusters: {
                    c1: { items: { c1_r1: { update: { id: '/disk/c1/c1_r1', itemId: 'c1_r1' } } } },
                    c2: { items: { c2_r1: { insert: { id: '/disk/c2/c2_r1', itemId: 'c2_r1' } } } },
                    c3: { items: { c3_r1: { delete: true } } }
                }
            }
        },
        'добавился 1 кластер с 2 альбомами, а потом добавился еще один альбом': {
            diffData: {
                items: [{
                    base_revision: 1,
                    revision: 2,
                    index_changes: [{
                        cluster_id: 'cluster1',
                        change_type: 'insert',
                        data: {
                            albums: [
                                {
                                    count: 1,
                                    album: 'photounlim',
                                    change_type: 'insert'
                                },
                                {
                                    count: 1,
                                    album: 'unbeautiful',
                                    change_type: 'insert'
                                }
                            ],
                        }
                    }]
                }, {
                    base_revision: 2,
                    revision: 3,
                    index_changes: [{
                        cluster_id: 'cluster1',
                        change_type: 'update',
                        data: {
                            albums: [
                                {
                                    count: 1,
                                    album: 'screenshots',
                                    change_type: 'insert'
                                },
                                {
                                    count: 2,
                                    album: 'photounlim',
                                    change_type: 'insert'
                                }
                            ]
                        }
                    }]
                }],
                total: 2,
                limit: 1,
                revision: 3
            },
            formattedData: {
                clusters: {
                    cluster1: {
                        insert: {
                            albums: {
                                photounlim: {
                                    album: 'photounlim',
                                    change_type: 'insert',
                                    count: 2,
                                },
                                screenshots: {
                                    album: 'screenshots',
                                    change_type: 'insert',
                                    count: 1,
                                },
                                unbeautiful: {
                                    album: 'unbeautiful',
                                    change_type: 'insert',
                                    count: 1,
                                },
                            },
                            id: 'cluster1',
                        },
                    },
                },
                revision: 3
            }
        },
        'добавилось 2 фото, а потом еще одно, которое должно встать в начало': {
            diffData: {
                items: [{
                    base_revision: 2309,
                    items_changes: [{
                        item_id: '2_0000001579978783000_12cbacb5a01f2df9a3889deb8461ca46b47ff09a9f195a42e666885ec2c9c442_1579988672792237',
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'insert',
                        data: {
                            path: 'photounlim:/2020-01-25 21-59-43.JPG',
                            height: 1960,
                            beauty: -0.365591,
                            width: 4032
                        }
                    }, {
                        item_id: '2_0000001579978778000_c3f0a56c9b5877d5caaa3cdbf082c96553de8310230dbd1e17967cc7d49e1997_1579988676660555',
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'insert',
                        data: {
                            path: 'photounlim:/2020-01-25 21-59-38.JPG',
                            height: 4032,
                            width: 1960
                        }
                    }],
                    index_changes: [{
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'insert',
                        data: {
                            to: 1579978783000,
                            albums: [],
                            from: 1579978778000,
                            size: 2
                        }
                    }],
                    revision: 2310
                },
                {
                    base_revision: 2310,
                    items_changes: [{
                        item_id: '2_0000001579978778000_c3f0a56c9b5877d5caaa3cdbf082c96553de8310230dbd1e17967cc7d49e1997_1579988676660555',
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'update',
                        data: {
                            path: 'photounlim:/2020-01-25 21-59-38.JPG',
                            height: 4032,
                            beauty: -2.32276,
                            width: 1960
                        }
                    }],
                    index_changes: [{
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'update',
                        data: {}
                    }],
                    revision: 2311
                },
                {
                    base_revision: 2311,
                    items_changes: [{
                        item_id: '2_0000001579978810000_bcf56a43a6bc850be5e87e8c1cf2225631fb1bd7b9d17770cca4704272dda701_1579988717563182',
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'insert',
                        data: {
                            path: 'photounlim:/2020-01-25 22-00-11.MP4',
                            albums: [],
                            height: 1080,
                            width: 2224
                        }
                    }],
                    index_changes: [{
                        cluster_id: '0000001579978778000_0000001579978783000',
                        change_type: 'update',
                        data: {
                            to: 1579978810000,
                            albums: [],
                            size: 3
                        }
                    }],
                    revision: 2312
                }],
                total: 3,
                limit: 1,
                revision: 2312
            },
            formattedData: {
                clusters: {
                    '0000001579978778000_0000001579978783000': {
                        insert: {
                            id: '0000001579978778000_0000001579978783000',
                            size: 3,
                            from: 1579978778000,
                            to: 1579978810000,
                            albums: {},
                            items: [{
                                id: '/photounlim/2020-01-25 22-00-11.MP4',
                                itemId: '2_0000001579978810000_bcf56a43a6bc850be5e87e8c1cf2225631fb1bd7b9d17770cca4704272dda701_1579988717563182',
                                height: 1080,
                                width: 2224,
                                albums: []
                            }, {
                                id: '/photounlim/2020-01-25 21-59-43.JPG',
                                itemId: '2_0000001579978783000_12cbacb5a01f2df9a3889deb8461ca46b47ff09a9f195a42e666885ec2c9c442_1579988672792237',
                                height: 1960,
                                width: 4032,
                                beauty: -0.365591
                            }, {
                                id: '/photounlim/2020-01-25 21-59-38.JPG',
                                itemId: '2_0000001579978778000_c3f0a56c9b5877d5caaa3cdbf082c96553de8310230dbd1e17967cc7d49e1997_1579988676660555',
                                height: 4032,
                                width: 1960
                            }]
                        },
                        items: {
                            '2_0000001579978783000_12cbacb5a01f2df9a3889deb8461ca46b47ff09a9f195a42e666885ec2c9c442_1579988672792237': {},
                            '2_0000001579978778000_c3f0a56c9b5877d5caaa3cdbf082c96553de8310230dbd1e17967cc7d49e1997_1579988676660555': {
                                update: {
                                    id: '/photounlim/2020-01-25 21-59-38.JPG',
                                    itemId: '2_0000001579978778000_c3f0a56c9b5877d5caaa3cdbf082c96553de8310230dbd1e17967cc7d49e1997_1579988676660555'
                                }
                            },
                            '2_0000001579978810000_bcf56a43a6bc850be5e87e8c1cf2225631fb1bd7b9d17770cca4704272dda701_1579988717563182': {}
                        }
                    }
                },
                revision: 2312
            }
        }
    },
    applyDiffDataCluster: {
        'примение изменений к кластеру без данных': {
            cluster: { id: 'c1' },
            diffData: {
                id: 'c1',
                size: 1,
                albums: [{ change_type: 'insert', album: 'beautiful', count: 1 }],
                to: 1435482214000,
                from: 1435482214000,
                items: [{ id: '/disk/c1/r1', itemId: 'c1_r1', albums: ['beautiful'] }],
                city: { change_type: 'insert', data: 'Москва' }
            },
            clusterNew: {
                id: 'c1',
                size: 1,
                albums: { beautiful: 1 },
                to: 1435482214000,
                from: 1435482214000,
                items: [{ id: '/disk/c1/r1', itemId: 'c1_r1', albums: ['beautiful'] }],
                city: 'Москва'
            }
        },
        'примение изменений данных size, from, to  к кластеру c данными': {
            cluster: {
                id: 'c1',
                size: 2,
                from: 1437782214000,
                to: 1437782214000,
                items: [{ id: '/disk/c1/r1', itemId: 'c1_r1' }]
            },
            diffData: {
                id: 'c1',
                size: 3,
                to: 1438882214000,
                from: 1438882214000
            },
            clusterNew: {
                id: 'c1',
                size: 3,
                to: 1438882214000,
                from: 1438882214000,
                items: [{ id: '/disk/c1/r1', itemId: 'c1_r1' }]
            }
        },
        'примение изменений - добавление города и улиц': {
            cluster: { id: 'c1', from: 1437782214000, to: 1437782214000 },
            diffData: {
                id: 'c1',
                city: { change_type: 'insert', data: 'Москва' },
                streets: [{ change_type: 'insert', data: 'Большая Переяславская', place_index: 0 }]
            },
            clusterNew: {
                id: 'c1',
                to: 1437782214000,
                from: 1437782214000,
                city: 'Москва',
                streets: ['Большая Переяславская']
            }
        },
        'примение изменений - обновление города и улиц': {
            cluster: {
                id: 'c1',
                from: 1437782214000,
                to: 1437782214000,
                city: 'Москва',
                streets: ['Большая Переяславская']
            },
            diffData: {
                id: 'c1',
                city: { change_type: 'update', data: 'ЦАО Москвы' },
                streets: [{ change_type: 'update', data: 'Проспект Мира', place_index: 0 }, {
                    change_type: 'insert',
                    data: 'Средняя Переяславская',
                    place_index: 1
                }]
            },
            clusterNew: {
                id: 'c1',
                to: 1437782214000,
                from: 1437782214000,
                city: 'ЦАО Москвы',
                streets: ['Проспект Мира', 'Средняя Переяславская']
            }
        },
        'примение изменений - удаление города и улицы': {
            cluster: {
                id: 'c1',
                from: 1437782214000,
                to: 1437782214000,
                city: 'Москва',
                streets: ['Большая Переяславская']
            },
            diffData: {
                id: 'c1',
                city: { change_type: 'delete' },
                streets: [{ change_type: 'delete', place_index: 0 }]
            },
            clusterNew: {
                id: 'c1',
                to: 1437782214000,
                from: 1437782214000
            }
        },
        'обновление количества фото в альбоме-срезе': {
            cluster: {
                id: 'c1',
                size: 2,
                albums: { beautiful: 1 },
            },
            diffData: {
                id: 'c1',
                size: 3,
                albums: [{ change_type: 'update', album: 'beautiful', count: 2 }],
            },
            clusterNew: {
                id: 'c1',
                size: 3,
                albums: { beautiful: 2 }
            }
        },
        'в кластере появились фото из альбома-среза': {
            cluster: {
                id: 'c1',
                size: 2,
                albums: { beautiful: 1 },
            },
            diffData: {
                id: 'c1',
                size: 3,
                albums: [{ change_type: 'insert', album: 'unbeautiful', count: 1 }],
            },
            clusterNew: {
                id: 'c1',
                size: 3,
                albums: { beautiful: 1, unbeautiful: 1 }
            }
        },
        'в кластере в котором не было фото из альбомов-срезов появились фото из альбомов-срезов': {
            cluster: {
                id: 'c1',
                size: 2
            },
            diffData: {
                id: 'c1',
                size: 3,
                albums: [
                    { change_type: 'insert', album: 'beautiful', count: 1 },
                    { change_type: 'insert', album: 'unbeautiful', count: 1 }

                ],
            },
            clusterNew: {
                id: 'c1',
                size: 3,
                albums: { beautiful: 1, unbeautiful: 1 }
            }
        },
        'из кластера были удалены все фото из альбома-среза beautiful': {
            cluster: {
                id: 'c1',
                size: 2,
                albums: { beautiful: 1, unbeautiful: 1 }
            },
            diffData: {
                id: 'c1',
                size: 1,
                albums: [{ change_type: 'delete', album: 'beautiful' }],
            },
            clusterNew: {
                id: 'c1',
                size: 1,
                albums: { unbeautiful: 1 }
            }
        },
        'из кластера были удалены все фото попадавшие в альбомы-срезы': {
            cluster: {
                id: 'c1',
                size: 3,
                albums: { beautiful: 1, unbeautiful: 1 }
            },
            diffData: {
                id: 'c1',
                size: 1,
                albums: [
                    { change_type: 'delete', album: 'beautiful' },
                    { change_type: 'delete', album: 'unbeautiful' }
                ],
            },
            clusterNew: {
                id: 'c1',
                size: 1,
                albums: null
            }
        }
    }
};

describe('photosliceHelper', () => {
    describe('Метод `findClusterIndex`', () => {
        it('должен вернуть верный индекс для нового id', () => {
            expect(findClusterIndex(clusters, '-000001733723825000_-000001733723825000')).toEqual({ found: false, index: 5 });
            expect(findClusterIndex(clusters, '-000001733723823000_-000001733723823000')).toEqual({ found: false, index: 4 });
            expect(findClusterIndex(clusters, '-000000000010802000_-000000000010802000')).toEqual({ found: false, index: 4 });
            expect(findClusterIndex(clusters, '-000000000010800000_-000000000010800000')).toEqual({ found: false, index: 3 });
            expect(findClusterIndex(clusters, '0000001431412084001_0000001431412084000')).toEqual({ found: false, index: 3 });
            expect(findClusterIndex(clusters, '0000001431412084004_0000001431412084000')).toEqual({ found: false, index: 2 });
            expect(findClusterIndex(clusters, '0000001431412084006_0000001431412084000')).toEqual({ found: false, index: 1 });
            expect(findClusterIndex(clusters, '0000001431412084009_0000001431412084000')).toEqual({ found: false, index: 0 });
        });

        it('должен вернуть верный индекс для id, который уже есть в списке ', () => {
            expect(findClusterIndex(clusters, '0000001431412084008_0000001431412084000')).toEqual({ found: true, index: 0 });
            expect(findClusterIndex(clusters, '0000001431412084005_0000001431412084000')).toEqual({ found: true, index: 1 });
            expect(findClusterIndex(clusters, '0000001431412084003_0000001431412084000')).toEqual({ found: true, index: 2 });
            expect(findClusterIndex(clusters, '-000000000010801000_-000000000010801000')).toEqual({ found: true, index: 3 });
            expect(findClusterIndex(clusters, '-000001733723824000_-000001733723824000')).toEqual({ found: true, index: 4 });
        });
    });

    describe('Метод `getNewItemPosition`', () => {
        it('должен вернуть верный индекс для нового id', () => {
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661101', items)).toBe(3);
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661104', items)).toBe(2);
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661106', items)).toBe(1);
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661109', items)).toBe(0);
        });

        it('должен вернуть верный индекс для id, который уже есть в списке (верный - следующий за существующим)', () => {
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661108', items)).toBe(1);
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661105', items)).toBe(2);
            expect(getNewItemPosition('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661103', items)).toBe(3);
        });
    });

    describe('Метод `findItemIndex`', () => {
        it('должен найти позицию переданных item`ов', () => {
            expect(findItemIndex('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661100', items)).toBe(-1);
            expect(findItemIndex('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661108', items)).toBe(0);
            expect(findItemIndex('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661105', items)).toBe(1);
            expect(findItemIndex('2_0000001533908995000_1f0aafab98a16e5aad40fc6f5ce9a5e5b272aa97172d203d777f86b6113d0919_1553591135661103', items)).toBe(2);
        });
    });

    describe('Метод `findItemIndexById`', () => {
        it('должен найти позицию переданных item`ов', () => {
            expect(findItemIndexById('/disk/0', items)).toBe(-1);
            expect(findItemIndexById('/disk/8', items)).toBe(0);
            expect(findItemIndexById('/disk/5', items)).toBe(1);
            expect(findItemIndexById('/disk/3', items)).toBe(2);
        });
    });

    describe('Метод `formatDataDiff`', () => {
        Object.keys(diffs.formatDataDiff).forEach((testTitle) => {
            const testData = diffs.formatDataDiff[testTitle];
            it(testTitle, () => {
                expect(formatDataDiff(testData.diffData, 1)).toEqual(testData.formattedData);
            });
        });
    });

    describe('Метод `applyDiffDataCluster`', () => {
        Object.keys(diffs.applyDiffDataCluster).forEach((testTitle) => {
            const testData = diffs.applyDiffDataCluster[testTitle];
            it(testTitle, () => {
                const clusterNew = testData.cluster;
                applyDiffDataCluster(clusterNew, testData.diffData);
                expect(clusterNew).toEqual(testData.clusterNew);
            });
        });
    });

    describe('Функция `findClusterByTimestamp`', () => {
        it('должен найти кластер по timestamp', () => {
            const { clusters, clustersByIds } = photoslice;
            clusters.forEach((clusterId) => {
                let [from, to] = clusterId.split('_');
                from = Number(from);
                to = Number(to);
                const average = ((from + to) / 2);
                expect(findClusterByTimestamp(getAllClusters(), from)).toEqual(clustersByIds[clusterId]);
                expect(findClusterByTimestamp(getAllClusters(), average)).toEqual(clustersByIds[clusterId]);
                expect(findClusterByTimestamp(getAllClusters(), to)).toEqual(clustersByIds[clusterId]);
            });
        });
        it('должен вернуть undefined, если кластер не найден', () => {
            expect(findClusterByTimestamp(getAllClusters(), 1000)).toEqual(undefined);
        });
    });

    describe('Метод `iterateThroughClustersInIndexPaths`', () => {
        it('правильно проитерируется по кластерам', () => {
            const clusters = [
                { size: 7 },
                { size: 2, items: [{ id: 'cluster2item1' }, { id: 'cluster2item2' }] },
                { size: 4 },
                { size: 2, items: [{ id: 'cluster4item1' }, { id: 'cluster4item2' }] },
                { size: 2 }
            ];
            const itemCallback = jest.fn();
            const clusterCallback = jest.fn();

            const indexPaths = [
                [{ clusterIndex: 0, resourceIndex: 5 }, { clusterIndex: 2, resourceIndex: 2 }],
                [{ clusterIndex: 2, resourceIndex: 3 }, { clusterIndex: 4, resourceIndex: 0 }]
            ];

            iterateThroughClustersInIndexPaths(indexPaths, clusters, itemCallback, clusterCallback);

            expect(itemCallback.mock.calls).toEqual([
                [
                    { size: 2, items: [{ id: 'cluster2item1' }, { id: 'cluster2item2' }] },
                    { id: 'cluster2item1' },
                    { clusterIndex: 1, resourceIndex: 0 }
                ],
                [
                    { size: 2, items: [{ id: 'cluster2item1' }, { id: 'cluster2item2' }] },
                    { id: 'cluster2item2' },
                    { clusterIndex: 1, resourceIndex: 1 }
                ],
                [
                    { size: 2, items: [{ id: 'cluster4item1' }, { id: 'cluster4item2' }] },
                    { id: 'cluster4item1' },
                    { clusterIndex: 3, resourceIndex: 0 }
                ],
                [
                    { size: 2, items: [{ id: 'cluster4item1' }, { id: 'cluster4item2' }] },
                    { id: 'cluster4item2' },
                    { clusterIndex: 3, resourceIndex: 1 }
                ]
            ]);
            expect(clusterCallback.mock.calls).toEqual([
                [{ size: 7 }, 5, 6],
                [{ size: 4 }, 0, 2],
                [{ size: 4 }, 3, 3],
                [{ size: 2 }, 0, 0]
            ]);
        });
    });
});
