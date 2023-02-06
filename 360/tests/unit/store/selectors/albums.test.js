const mockShouldNotifyOverdraft = false;

jest.mock('../../../../components/redux/store/selectors/space', () => ({
    shouldNotifyOverdraft: jest.fn(() => mockShouldNotifyOverdraft)
}));

import {
    getUntitledAlbums,
    getGatheredAlbums,
    hasAlbumsStub,
    getCurrentAlbumItemsRange
} from '../../../../components/redux/store/selectors/albums';

describe('albums selectors', () => {
    describe('getUntitledAlbums', () => {
        it('должен вернуть все безымянные альбомы, если они не загружены', () => {
            const store = {
                albums: {
                    albums: {
                        camera: {
                            id: 'camera'
                        },
                        videos: {
                            id: 'videos'
                        },
                        screenshots: {
                            id: 'screenshots'
                        },
                        favorite: {
                            id: 'favorites'
                        }
                    },
                    isLoaded: false
                }
            };

            expect(getUntitledAlbums(store)).toEqual([
                { id: 'favorites' },
                { id: 'camera' },
                { id: 'videos' },
                { id: 'screenshots' }
            ]);
        });
        it('должен вернуть все безымянные альбомы, если они загружены(избранные, если есть превью)', () => {
            const store = {
                albums: {
                    albums: {
                        favorite: {
                            id: 'favorites',
                            preview: 'preview'
                        },
                        camera: {
                            id: 'camera',
                            count: 1
                        },
                        videos: {
                            id: 'videos',
                            count: 1
                        },
                        screenshots: {
                            id: 'screenshots',
                            count: 2
                        }
                    },
                    isLoaded: true
                }
            };

            expect(getUntitledAlbums(store)).toEqual([
                { id: 'favorites', preview: 'preview' },
                { id: 'camera', count: 1 },
                { id: 'videos', count: 1 },
                { id: 'screenshots', count: 2 }
            ]);
        });
        it('должен вернуть только те безымянные альбомы, в которых есть фото', () => {
            const store = {
                albums: {
                    albums: {
                        favorite: {
                            id: 'favorites'
                        },
                        camera: {
                            id: 'camera',
                            count: 0
                        },
                        videos: {
                            id: 'videos',
                            count: 1
                        },
                        screenshots: {
                            id: 'screenshots',
                            count: 2
                        }
                    },
                    isLoaded: true
                }
            };

            expect(getUntitledAlbums(store)).toEqual([
                { id: 'videos', count: 1 },
                { id: 'screenshots', count: 2 }
            ]);
        });
    });

    describe('getGatheredAlbums', () => {
        it('должен вернуть все собранные альбомы, если они не загружены', () => {
            const store = {
                albums: {
                    albums: {
                        beautiful: {
                            id: 'beautiful'
                        },
                        unbeautiful: {
                            id: 'unbeautiful'
                        },
                        geo: {
                            id: 'geo'
                        }
                    },
                    isLoaded: false
                }
            };

            expect(getGatheredAlbums(store)).toEqual([
                { id: 'geo' },
                { id: 'beautiful' },
                { id: 'unbeautiful' }
            ]);
        });
        it('должен вернуть все собранные альбомы, если они загружены', () => {
            const store = {
                albums: {
                    albums: {
                        beautiful: {
                            id: 'beautiful',
                            count: 1
                        },
                        unbeautiful: {
                            id: 'unbeautiful',
                            count: 1
                        },
                        geo: {
                            id: 'geo',
                            preview: 'someurl'
                        }
                    },
                    isLoaded: true
                }
            };

            expect(getGatheredAlbums(store)).toEqual([
                { id: 'geo', preview: 'someurl' },
                { id: 'beautiful', count: 1 },
                { id: 'unbeautiful', count: 1 }
            ]);
        });
        it('должен вернуть только те безымянные альбомы, в которых есть фото', () => {
            const store = {
                albums: {
                    albums: {
                        beautiful: {
                            id: 'beautiful',
                            count: 0
                        },
                        unbeautiful: {
                            id: 'unbeautiful',
                            count: 1
                        },
                    },
                    isLoaded: true
                }
            };

            expect(getGatheredAlbums(store)).toEqual([
                { id: 'unbeautiful', count: 1 },
            ]);
        });
    });

    describe('hasAlbumsStub', () => {
        it('не должен показывать заглушку, если не находимся в альбомах', () => {
            const store = {
                page: { idContext: '/recent' }
            };

            expect(hasAlbumsStub(store)).toBe(false);
        });

        it('должен показать заглушку, если нет вообще никаких альбомов', () => {
            const store = {
                albums: {
                    isLoaded: true, albums: {
                        camera: { count: 0 },
                        videos: { count: 0 },
                        screenshots: { count: 0 },
                        beautiful: { count: 0 },
                        unbeautiful: { count: 0 }
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: []
                },
                page: { idContext: '/albums' }
            };
            expect(hasAlbumsStub(store)).toBe(true);
        });
        it('не должен показать заглушку, если альбомы-срезы загружаются', () => {
            const store = {
                albums: {
                    isLoaded: false, albums: {
                        camera: {},
                        videos: {},
                        screenshots: {},
                        beautiful: {},
                        unbeautiful: {},
                        geo: {}
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: []
                },
                page: { idContext: '/albums' }
            };

            expect(hasAlbumsStub(store)).toBe(false);
        });
        it('не должен показать заглушку, если есть хотя бы один альбом-срез', () => {
            const store = {
                albums: {
                    isLoaded: true, albums: {
                        camera: { count: 1 },
                        videos: { count: 0 },
                        screenshots: { count: 0 },
                        beautiful: { count: 0 },
                        unbeautiful: { count: 0 }
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: []
                },
                page: { idContext: '/albums' }
            };

            expect(hasAlbumsStub(store)).toBe(false);
        });
        it('не должен показать заглушку, если есть гео альбом', () => {
            const store = {
                albums: {
                    isLoaded: true, albums: {
                        camera: { count: 0 },
                        videos: { count: 0 },
                        screenshots: { count: 0 },
                        beautiful: { count: 0 },
                        unbeautiful: { count: 0 },
                        geo: { id: 'geo', preview: 'someurl' }
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: []
                },
                page: { idContext: '/albums' }
            };
            expect(hasAlbumsStub(store)).toBe(false);
        });
        it('не должен показать заглушку, если есть альбом-лица', () => {
            const store = {
                albums: {
                    isLoaded: true, albums: {
                        camera: { count: 0 },
                        videos: { count: 0 },
                        screenshots: { count: 0 },
                        beautiful: { count: 0 },
                        unbeautiful: { count: 0 },
                        faces: { id: 'faces', preview: 'someurl' }
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: []
                },
                page: { idContext: '/albums' }
            };
            expect(hasAlbumsStub(store)).toBe(false);
        });
        it('не должен показать заглушку, если есть хотя бы один личный альбом', () => {
            const store = {
                albums: {
                    isLoaded: true, albums: {
                        camera: { count: 0 },
                        videos: { count: 0 },
                        screenshots: { count: 0 },
                        beautiful: { count: 0 },
                        unbeautiful: { count: 0 }
                    }
                },
                personalAlbums: {
                    isLoaded: true,
                    ids: ['albumid1']
                },
                page: { idContext: '/albums' }
            };

            expect(hasAlbumsStub(store)).toBe(false);
        });
    });

    describe('getCurrentAlbumItemsRange', () => {
        it('Должен возвращать данные для текущего альбома', () => {
            const data = getCurrentAlbumItemsRange({
                page: { albumId: '5de665f7e4d70e06400bc91b' },
                statesContext: {
                    selected: [
                        '/disk/IMG_1528.HEIC',
                        '/photounlim/2019-06-17 16-59-09.MP4',
                        '/photounlim/2019-10-22 18-30-58.JPG'
                    ]
                },
                personalAlbums: {
                    albumsByIds: {
                        '5de665f7e4d70e06400bc91b': {
                            clusters: [{
                                id: 1,
                                size: 2,
                                items: [
                                    {
                                        itemId: '5de665f7e4d70e06400bc88c',
                                        id: '/disk/IMG_1528.HEIC',
                                        width: 3024,
                                        height: 4032,
                                        beauty: -1.69977
                                    },
                                    {
                                        itemId: '5de665f7e4d70e06400bc8a5',
                                        id: '/photounlim/2019-06-17 16-59-09.MP4'
                                    }
                                ]
                            }, {
                                id: 2,
                                size: 2,
                                items: [
                                    {
                                        itemId: '5de665f7e4d70e06400bc896',
                                        id: '/photounlim/2019-10-22 18-31-03.JPG'
                                    },
                                    {
                                        itemId: '5de665f7e4d70e06400bc897',
                                        id: '/photounlim/2019-10-22 18-30-58.JPG'
                                    }
                                ]
                            }]
                        }
                    }
                },
                resources: {
                    '/disk/IMG_1528.HEIC': {
                        id: '/disk/IMG_1528.HEIC',
                        name: 'IMG_1528.HEIC',
                        /* eslint-disable-next-line */
                        defaultPreview: '//downloader.dst.yandex.ru/preview/3ab775d0c504f1039bef8ed3aee2f1164d8f474e347d54d7ed57d8b5ee3d8f7a/inf/4KYG7ri2EUobEhkXaSKy-lq0tWqxCwGC3GbhTuXLTNwFmS0h2-5_HRbSSR_hRiRO_vXMlvwg0jgGkjsrUYRmcw%3D%3D?uid=4023076962&filename=IMG_1528.HEIC&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2',
                        canPlayVideo: false
                    },
                    '/photounlim/2019-06-17 16-59-09.MP4': {
                        id: '/photounlim/2019-06-17 16-59-09.MP4',
                        name: '2019-06-17 16-59-09.MP4',
                        /* eslint-disable-next-line */
                        defaultPreview: '//downloader.dst.yandex.ru/preview/79593914eada7a891841051dfd2efb92db5372e19d87954f25a1b13c747f30cd/inf/6rzWpN5a2dKgI21yATtq96Tna8OssXhDo2athr-QlcRWf9J6M4inoLTe4--BK_kHOiy-ef_Hc_Gc5Rky11y8Ow%3D%3D?uid=4023076962&filename=2019-06-17%2016-59-09.MP4&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2',
                        canPlayVideo: true,
                        meta: { video_info: { duration: 4100 } }
                    },
                    '/photounlim/2019-10-22 18-31-03.JPG': {
                        id: '/photounlim/2019-10-22 18-31-03.JPG',
                        name: '2019-10-22 18-31-03.JPG',
                        /* eslint-disable-next-line */
                        defaultPreview: '//downloader.dst.yandex.ru/preview/786711fdc62513925afe9aa46d7e1b5e861a92f63d19d295154b4290a08d47b8/inf/3HBUlA-wVdmlRjMjVe_vWRs1z158u8uyc5OnMi5qHG7F_sgGOE6X_8V5m5FJ4B2NgnF6RuI4m1HTCfasv69EWA%3D%3D?uid=4023076962&filename=2019-10-22%2018-31-03.JPG&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2',
                        canPlayVideo: false
                    },
                    '/photounlim/2019-10-22 18-30-58.JPG': {
                        id: '/photounlim/2019-10-22 18-30-58.JPG',
                        name: '2019-10-22 18-30-58.JPG'
                    }
                }

            }, { clusterIndex: 0, resourceIndex: 0 }, { clusterIndex: 1, resourceIndex: 1 });

            expect(data).toEqual({
                0: {
                    0: {
                        id: '/disk/IMG_1528.HEIC',
                        name: 'IMG_1528.HEIC',
                        selected: true,
                        highlighted: false,
                        // eslint-disable-next-line max-len
                        defaultPreview: '//downloader.dst.yandex.ru/preview/3ab775d0c504f1039bef8ed3aee2f1164d8f474e347d54d7ed57d8b5ee3d8f7a/inf/4KYG7ri2EUobEhkXaSKy-lq0tWqxCwGC3GbhTuXLTNwFmS0h2-5_HRbSSR_hRiRO_vXMlvwg0jgGkjsrUYRmcw%3D%3D?uid=4023076962&filename=IMG_1528.HEIC&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2',
                        isVideo: false,
                        videoDuration: undefined
                    },
                    1: {
                        id: '/photounlim/2019-06-17 16-59-09.MP4',
                        name: '2019-06-17 16-59-09.MP4',
                        selected: true,
                        highlighted: false,
                        isVideo: true,
                        videoDuration: 4100,
                        // eslint-disable-next-line max-len
                        defaultPreview: '//downloader.dst.yandex.ru/preview/79593914eada7a891841051dfd2efb92db5372e19d87954f25a1b13c747f30cd/inf/6rzWpN5a2dKgI21yATtq96Tna8OssXhDo2athr-QlcRWf9J6M4inoLTe4--BK_kHOiy-ef_Hc_Gc5Rky11y8Ow%3D%3D?uid=4023076962&filename=2019-06-17%2016-59-09.MP4&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2'
                    }
                },
                1: {
                    0: {
                        id: '/photounlim/2019-10-22 18-31-03.JPG',
                        name: '2019-10-22 18-31-03.JPG',
                        selected: false,
                        highlighted: false,
                        isVideo: false,
                        // eslint-disable-next-line max-len
                        defaultPreview: '//downloader.dst.yandex.ru/preview/786711fdc62513925afe9aa46d7e1b5e861a92f63d19d295154b4290a08d47b8/inf/3HBUlA-wVdmlRjMjVe_vWRs1z158u8uyc5OnMi5qHG7F_sgGOE6X_8V5m5FJ4B2NgnF6RuI4m1HTCfasv69EWA%3D%3D?uid=4023076962&filename=2019-10-22%2018-31-03.JPG&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&owner_uid=4023076962&tknv=v2',
                        videoDuration: undefined
                    },
                    1: {
                        id: '/photounlim/2019-10-22 18-30-58.JPG',
                        name: '2019-10-22 18-30-58.JPG',
                        defaultPreview: undefined,
                        selected: true,
                        highlighted: false,
                        isVideo: undefined,
                        videoDuration: undefined
                    }
                }
            });
        });
    });
});
