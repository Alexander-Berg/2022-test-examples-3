import {
    getAllItems,
    getItemsRange,
    getResourcePosition,
    getNextIndexPath,
    getPhotoView,
    isAlbumSlice,
    hasPhotoStub,
    getPhotosliceFilter,
    getPhotosliceNeighbour
} from '../../../../components/redux/store/selectors/photoslice';
import { PHOTO_GRID_TYPES } from '@ps-int/ufo-rocks/lib/consts';
import _ from 'lodash';

describe('photoslice selectors', () => {
    const defaultPhotoslice = {
        clusters: ['1', '2', '3', '4', '5'],
        clustersByIds: {
            1: {
                id: '1',
                size: 2,
                albums: { beautiful: 1 },
                items: [
                    { id: '/disk/1.jpg' },
                    { id: '/disk/2.jpg', albums: ['beautiful'] }
                ]
            },
            2: {
                id: '2',
                size: 3,
                albums: { beautiful: 1, unbeautiful: 1, screenshots: 1 },
                items: [
                    { id: '/disk/3.jpg', albums: ['unbeautiful', 'screenshots'] },
                    { id: '/disk/4.jpg', albums: [] },
                    { id: '/disk/5.jpg', albums: ['beautiful'] }
                ]
            },
            3: {
                id: '3',
                size: 2,
                albums: { unbeautiful: 2 },
                items: [
                    { id: '/disk/6.jpg', albums: ['unbeautiful'] },
                    { id: '/disk/7.jpg', albums: ['unbeautiful'] }
                ]
            },
            4: {
                id: '4',
                size: 4,
                albums: { beautiful: 1 }
            },
            5: {
                id: '5',
                size: 4,
                albums: null
            }
        }
    };

    describe('getAllItems', () => {
        it('Должен вернуть все элементы если нет фильтров', () => {
            const items = getAllItems({
                photoslice: defaultPhotoslice,
                page: {},
                resources: {}
            });
            expect(items).toEqual([
                {
                    id: '1',
                    size: 2,
                    albums: { beautiful: 1 },
                    items: [
                        { id: '/disk/1.jpg' },
                        { id: '/disk/2.jpg', albums: ['beautiful'] }
                    ]
                },
                {
                    id: '2',
                    size: 3,
                    albums: { beautiful: 1, unbeautiful: 1, screenshots: 1 },
                    items: [
                        { id: '/disk/3.jpg', albums: ['unbeautiful', 'screenshots'] },
                        { id: '/disk/4.jpg', albums: [] },
                        { id: '/disk/5.jpg', albums: ['beautiful'] }
                    ]
                },
                {
                    id: '3',
                    size: 2,
                    albums: { unbeautiful: 2 },
                    items: [
                        { id: '/disk/6.jpg', albums: ['unbeautiful'] },
                        { id: '/disk/7.jpg', albums: ['unbeautiful'] }
                    ]
                },
                {
                    id: '4',
                    size: 4,
                    albums: { beautiful: 1 }
                },
                {
                    id: '5',
                    size: 4,
                    albums: null
                }
            ]);
        });

        it('Должен вернуть все элементы попадающие под фильтр beautiful', () => {
            const items = getAllItems({
                photoslice: defaultPhotoslice,
                page: { filter: 'beautiful' },
                resources: { }
            });
            expect(items).toEqual([
                {
                    id: '1',
                    size: 1,
                    albums: { beautiful: 1 },
                    items: [
                        { id: '/disk/2.jpg', albums: ['beautiful'] }
                    ]
                },
                {
                    id: '2',
                    size: 1,
                    albums: { beautiful: 1, unbeautiful: 1, screenshots: 1 },
                    items: [
                        { id: '/disk/5.jpg', albums: ['beautiful'] }
                    ]
                },
                {
                    id: '4',
                    size: 1,
                    albums: { beautiful: 1 }
                }
            ]);
        });

        it('Должен вернуть все элементы попадающие под фильтр unbeautiful', () => {
            const items = getAllItems({
                photoslice: defaultPhotoslice,
                page: { filter: 'unbeautiful' },
                resources: { }
            });
            expect(items).toEqual([
                {
                    id: '2',
                    size: 1,
                    albums: { beautiful: 1, unbeautiful: 1, screenshots: 1 },
                    items: [
                        { id: '/disk/3.jpg', albums: ['unbeautiful', 'screenshots'] }
                    ]
                },
                {
                    id: '3',
                    size: 2,
                    albums: { unbeautiful: 2 },
                    items: [
                        { id: '/disk/6.jpg', albums: ['unbeautiful'] },
                        { id: '/disk/7.jpg', albums: ['unbeautiful'] }
                    ]
                }
            ]);
        });

        it('Должен вернуть все элементы попадающие под фильтр screenshots', () => {
            const items = getAllItems({
                photoslice: defaultPhotoslice,
                page: { filter: 'screenshots' },
                resources: { }
            });
            expect(items).toEqual([
                {
                    id: '2',
                    size: 1,
                    albums: { beautiful: 1, unbeautiful: 1, screenshots: 1 },
                    items: [
                        { id: '/disk/3.jpg', albums: ['unbeautiful', 'screenshots'] }
                    ]
                }
            ]);
        });
    });

    describe('getItemsRange', () => {
        const defaultState = {
            page: { },
            photoslice: _.merge({}, defaultPhotoslice, {
                clustersByIds: {
                    1: {
                        from: 1234567,
                        to: 2234567,
                        city: 'city',
                        streets: ['street']
                    }
                }
            }),
            resources: {
                '/disk/1.jpg': {
                    id: '/disk/1.jpg',
                    name: '1.jpg',
                    defaultPreview: 'preview',
                    canPlayVideo: true,
                    meta: {
                        storage_type: 'photounlim',
                        video_info: {
                            duration: 1234
                        }
                    }
                },
                '/disk/2.jpg': {
                    id: '/disk/2.jpg',
                    name: '2.jpg',
                },
                '/disk/3.jpg': {
                    id: '/disk/3.jpg',
                    name: '3.jpg',
                }
            },
            statesContext: { selected: ['/disk/2.jpg'], highlighted: [] },
            environment: { session: { experiment: { } } },
            user: { }
        };

        const defaultTitles = [{ clusterIndexes: { from: 0, to: 0 } }, { clusterIndexes: { from: 1, to: 1 } }, { clusterIndexes: { from: 2, to: 2 } }];

        it('Должен корректно возвращать элементы из диапазона без фильтров', () => {
            expect(getItemsRange(defaultState, { clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 2, resourceIndex: 0 }, defaultTitles)).toEqual({
                0: {
                    title: { from: 1234567, to: 2234567, city: 'city', streets: ['street'], clusterIds: ['1'] },
                    resources: {
                        0: { id: '/disk/1.jpg', name: '1.jpg', loaded: true, selected: false, highlighted: false, defaultPreview: 'preview', isPhotoUnlim: true, isVideo: true, videoDuration: 1234 },
                        1: { id: '/disk/2.jpg', name: '2.jpg', loaded: true, selected: true, highlighted: false, isPhotoUnlim: false }
                    }
                },
                1: {
                    title: { clusterIds: ['2'] },
                    resources: {
                        0: { id: '/disk/3.jpg', name: '3.jpg', loaded: true, selected: false, highlighted: false, isPhotoUnlim: false },
                        1: { id: '/disk/4.jpg', loaded: false, selected: false, highlighted: false, isPhotoUnlim: false },
                        2: { id: '/disk/5.jpg', loaded: false, selected: false, highlighted: false, isPhotoUnlim: false }
                    },

                },
                2: {
                    title: { clusterIds: ['3'] },
                    resources: {
                        0: { id: '/disk/6.jpg', loaded: false, selected: false, highlighted: false, isPhotoUnlim: false }
                    }
                }
            });
        });

        it('Должен корректно возвращать элементы из диапазона c учётом фильтра beautiful', () => {
            const state = Object.assign({}, defaultState, {
                page: { filter: 'beautiful' }
            });
            expect(getItemsRange(state, { clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 2, resourceIndex: 0 }, defaultTitles)).toEqual({
                0: {
                    title: { from: 1234567, to: 2234567, city: 'city', streets: ['street'], clusterIds: ['1'] },
                    resources: {
                        0: { id: '/disk/2.jpg', name: '2.jpg', loaded: true, selected: true, highlighted: false, isPhotoUnlim: false }
                    }
                },
                1: {
                    title: { clusterIds: ['2'] },
                    resources: {
                        0: { id: '/disk/5.jpg', loaded: false, selected: false, highlighted: false, isPhotoUnlim: false }
                    },

                },
                2: {
                    title: { clusterIds: ['4'] },
                    resources: {
                        0: { loaded: false, name: null, selected: false, highlighted: false, isPhotoUnlim: false }
                    }
                }
            });
        });
    });

    describe('getResourcePosition', () => {
        const defaultState = {
            photoslice: {
                clusters: ['first', 'second', 'third', 'fourth', 'fifth'],
                clustersByIds: {
                    first: {
                        size: 5,
                        albums: {
                            unbeautiful: 2
                        }
                    },
                    second: {
                        size: 1
                    },
                    third: {
                        size: 3,
                        albums: {
                            unbeautiful: 1
                        }
                    },
                    fourth: {
                        size: 100,
                        albums: {
                            unbeautiful: 30
                        }
                    },
                    fifth: {
                        size: 2,
                        albums: {
                            unbeautiful: 1
                        }
                    }
                }
            },
            page: { },
            environment: { session: { experiment: { } } },
            user: { }
        };
        describe('photoslice', () => {
            it('inside one cluster (positiove diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 3,
                    resourceIndex: 50,
                    diff: 20
                })).toEqual({
                    clusterIndex: 3,
                    resourceIndex: 70,
                    diff: 20
                });
            });
            it('inside one cluster (negative diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 3,
                    resourceIndex: 50,
                    diff: -20
                })).toEqual({
                    clusterIndex: 3,
                    resourceIndex: 30,
                    diff: -20
                });
            });
            it('through few clusters (positive diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 0,
                    resourceIndex: 2,
                    diff: 4
                })).toEqual({
                    clusterIndex: 2,
                    resourceIndex: 0,
                    diff: 4
                });
            });
            it('through few clusters (negative diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 2,
                    resourceIndex: 0,
                    diff: -5
                })).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 1,
                    diff: -5
                });
            });
            it('reach end (positive diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 3,
                    resourceIndex: 97,
                    diff: 5
                })).toEqual({
                    clusterIndex: 4,
                    resourceIndex: 1,
                    diff: 4
                });
            });
            it('reach start (negative diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 2,
                    resourceIndex: 1,
                    diff: -10
                })).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 0,
                    diff: -7
                });
            });
        });

        describe('filtered photoslice', () => {
            const state = Object.assign({}, defaultState, {
                page: { filter: 'unbeautiful' }
            });
            it('inside one cluster (positiove diff)', () => {
                expect(getResourcePosition(state, {
                    clusterIndex: 2,
                    resourceIndex: 10,
                    diff: 10
                })).toEqual({
                    clusterIndex: 2,
                    resourceIndex: 20,
                    diff: 10
                });
            });
            it('inside one cluster (negative diff)', () => {
                expect(getResourcePosition(state, {
                    clusterIndex: 2,
                    resourceIndex: 20,
                    diff: -10
                })).toEqual({
                    clusterIndex: 2,
                    resourceIndex: 10,
                    diff: -10
                });
            });
            it('through few clusters (positive diff)', () => {
                expect(getResourcePosition(state, {
                    clusterIndex: 0,
                    resourceIndex: 2,
                    diff: 4
                })).toEqual({
                    clusterIndex: 2,
                    resourceIndex: 3,
                    diff: 4
                });
            });
            it('through few clusters (negative diff)', () => {
                expect(getResourcePosition(state, {
                    clusterIndex: 2,
                    resourceIndex: 3,
                    diff: -5
                })).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 1,
                    diff: -5
                });
            });
            it('reach end (positive diff)', () => {
                expect(getResourcePosition(state, {
                    clusterIndex: 2,
                    resourceIndex: 27,
                    diff: 5
                })).toEqual({
                    clusterIndex: 3,
                    resourceIndex: 0,
                    diff: 3
                });
            });
            it('reach start (negative diff)', () => {
                expect(getResourcePosition(defaultState, {
                    clusterIndex: 2,
                    resourceIndex: 1,
                    diff: -10
                })).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 0,
                    diff: -7
                });
            });
        });
    });

    describe('getPhotosliceNeighbour', () => {
        it('Должен корректно работать, если в ресурсе лежит неактуальный clusterId', () => {
            const state = {
                page: { },
                photoslice: {
                    clusters: ['0000001527338842000_0000001527353435000', '0000001527338842000_0000001527342514000'],
                    clustersByIds: {
                        '0000001527338842000_0000001527353435000': {
                            id: '0000001527338842000_0000001527353435000',
                            from: 1527338842000,
                            to: 1527353435000,
                            size: 1,
                            items: [{ id: '/disk/1.jpg', itemId: '1' }]
                        },
                        '0000001527338842000_0000001527342514000': {
                            id: '0000001527338842000_0000001527342514000',
                            from: 1527338842000,
                            to: 1527342514000,
                            size: 1,
                            items: [{ id: '/disk/2.jpg', itemId: '2' }]
                        }
                    }
                },
                resources: {
                    '/disk/1.jpg': {
                        clusterId: '0000001527338841000_0000001527353435000',
                        id: '/disk/1.jpg',
                        meta: {
                            photoslice_time: 1527338842
                        }
                    },
                    '/disk/2.jpg': {
                        clusterId: '0000001527338842000_0000001527353435000',
                        id: '/disk/2.jpg',
                        meta: {
                            photoslice_time: 1527353435
                        }
                    },
                },
                environment: { session: { experiment: { } } },
                user: {},
                settings: {}
            };
            const { resource } = getPhotosliceNeighbour(state, '/disk/1.jpg', 1);
            expect(resource).toEqual({
                clusterId: '0000001527338842000_0000001527353435000',
                id: '/disk/2.jpg',
                meta: {
                    photoslice_time: 1527353435
                }
            });
        });
    });

    describe('getNextIndexPath', () => {
        const defaultState = {
            page: { },
            photoslice: {
                clusters: ['first', 'second'],
                clustersByIds: {
                    first: {
                        size: 2,
                        albums: {
                            unbeautiful: 1
                        }
                    },
                    second: {
                        size: 3,
                        albums: {
                            unbeautiful: 2
                        }
                    }
                }
            },
            environment: { session: { experiment: { } } },
            user: {}
        };

        describe('photoslice', () => {
            it('should return next', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 1,
                    resourceIndex: 1
                }, 1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 2
                });
            });
            it('should return previous', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 1,
                    resourceIndex: 1
                }, -1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 0
                });
            });
            it('should return next with cluster change', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 0,
                    resourceIndex: 1
                }, 1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 0
                });
            });
            it('should return previous with cluster change', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 1,
                    resourceIndex: 0
                }, -1)).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 1
                });
            });
            it('should return null if no photoslice branch in state', () => {
                expect(getNextIndexPath({}, {
                    clusterIndex: 0,
                    resourceIndex: 0
                }, 1)).toBeNull();
            });
            it('should return null if try to get previous resource of first', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 0,
                    resourceIndex: 0
                }, -1)).toBeNull();
            });
            it('should return null if try to get next resource of last', () => {
                expect(getNextIndexPath(defaultState, {
                    clusterIndex: 1,
                    resourceIndex: 2
                }, 1)).toBeNull();
            });
        });

        describe('filtered photoslice', () => {
            const state = Object.assign({}, defaultState, {
                page: { filter: 'unbeautiful' }
            });
            it('should return next', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 1,
                    resourceIndex: 0
                }, 1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 1
                });
            });
            it('should return previous', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 1,
                    resourceIndex: 1
                }, -1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 0
                });
            });
            it('should return next with cluster change', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 0,
                    resourceIndex: 0
                }, 1)).toEqual({
                    clusterIndex: 1,
                    resourceIndex: 0
                });
            });
            it('should return previous with cluster change', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 1,
                    resourceIndex: 0
                }, -1)).toEqual({
                    clusterIndex: 0,
                    resourceIndex: 0
                });
            });
            it('should return null if try to get previous resource of first', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 0,
                    resourceIndex: 0
                }, -1)).toBeNull();
            });
            it('should return null if try to get next resource of last', () => {
                expect(getNextIndexPath(state, {
                    clusterIndex: 1,
                    resourceIndex: 1
                }, 1)).toBeNull();
            });
        });
    });

    describe('getPhotoView', () => {
        it('По умолчаниию должен возвращать тип отображения для фотосреза', () => {
            expect(getPhotoView({
                settings: { photoView: PHOTO_GRID_TYPES.WOW },
                page: {}
            })).toBe(PHOTO_GRID_TYPES.WOW);
        });

        it('Если выставлен фильтр, должен возвращать тип отображения для соответствующего альбома-среза', () => {
            expect(getPhotoView({
                settings: {
                    photoView: PHOTO_GRID_TYPES.WOW,
                    beautifulAlbumPhotoView: PHOTO_GRID_TYPES.TILE,
                },
                page: { filter: 'beautiful' }
            })).toBe(PHOTO_GRID_TYPES.TILE);
        });
    });

    describe('isAlbumSlice', () => {
        it('Должен возвращать false не в фотосрезе', () => {
            expect(isAlbumSlice({ page: { idContext: '/disk/photo' } })).toBe(false);
        });

        it('Должен возвращать true если фильтр beautiful', () => {
            expect(isAlbumSlice({ page: { filter: 'beautiful', idContext: '/photo' } })).toBe(true);
        });

        it('Должен возвращать true если фильтр unbeautiful', () => {
            expect(isAlbumSlice({ page: { filter: 'unbeautiful', idContext: '/photo' } })).toBe(true);
        });

        it('Должен возвращать true если фильтр camera', () => {
            expect(isAlbumSlice({ page: { filter: 'camera', idContext: '/photo' } })).toBe(true);
        });

        it('Должен возвращать true если фильтр videos', () => {
            expect(isAlbumSlice({ page: { filter: 'videos', idContext: '/photo' } })).toBe(true);
        });

        it('Должен возвращать true если фильтр screenshots', () => {
            expect(isAlbumSlice({ page: { filter: 'screenshots', idContext: '/photo' } })).toBe(true);
        });

        it('Должен возвращать false если фильтр photounlim', () => {
            expect(isAlbumSlice({ page: { filter: 'photounlim', idContext: '/photo' } })).toBe(false);
        });

        it('Должен возвращать false если фильтр nonphotounlim', () => {
            expect(isAlbumSlice({ page: { filter: 'nonphotounlim', idContext: '/photo' } })).toBe(false);
        });
    });

    describe('hasPhotoStub', () => {
        const photoslice = {
            clusters: ['1'],
            clustersByIds: { 1: { id: '1', items: [{ id: '/disk/1.jpg' }] } }
        };

        it('Должен возвращать false не в фотосрезе', () => {
            expect(hasPhotoStub({
                page: { idContext: '/disk/photo' },
                environment: { session: { experiment: { } } },
                user: { }
            })).toBe(false);
        });

        it('Должен возвращать false для непустого фотосреза', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo' },
                environment: { session: { experiment: { } } },
                user: { }
            })).toBe(false);
        });

        it('Должен возвращать true для пустого фотосреза', () => {
            expect(hasPhotoStub({
                photoslice: {
                    clusters: []
                },
                page: { idContext: '/photo' },
                environment: { session: { experiment: { } } },
                user: { }
            })).toBe(true);
        });

        it('Должен возвращать true для пустого альбома-среза beautiful', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'beautiful' }
            })).toBe(true);
        });

        it('Должен возвращать true для пустого альбома-среза unbeautiful', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'unbeautiful' }
            })).toBe(true);
        });

        it('Должен возвращать true для пустого альбома-среза camera', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'camera' }
            })).toBe(true);
        });

        it('Должен возвращать true для пустого альбома-среза screenshots', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'screenshots' }
            })).toBe(true);
        });

        it('Должен возвращать true для пустого альбома-среза videos', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'videos' }
            })).toBe(true);
        });

        it('Должен возвращать false для пустого отфильтрованного фотосреза по photounlim', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'photounlim' }
            })).toBe(false);
        });

        it('Должен возвращать false для пустого отфильтрованного фотосреза по nonphotounlim', () => {
            expect(hasPhotoStub({
                photoslice,
                page: { idContext: '/photo', filter: 'nonphotounlim' }
            })).toBe(false);
        });
    });

    describe('getPhotosliceFilter', () => {
        it('должен возвращать значение из filter если там photounlim', () => {
            expect(getPhotosliceFilter({
                page: { filter: 'photounlim' }
            })).toBe('photounlim');
        });

        it('должен возвращать значение из filter если там nonphotounlim', () => {
            expect(getPhotosliceFilter({
                page: { filter: 'nonphotounlim' }
            })).toBe('nonphotounlim');
        });

        it('должен возвращать all если в filter beautiful', () => {
            expect(getPhotosliceFilter({
                page: { filter: 'beautiful' }
            })).toBe('all');
        });
    });
});
