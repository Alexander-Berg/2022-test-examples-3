import _ from 'lodash';
import deepFreeze from 'deep-freeze';

import '../../noscript';
import personalAlbums, { MIN_PHOTOS_IN_CLUSTER } from '../../../../components/redux/store/reducers/personal-albums';
import albumWithItems from '../../../fixtures/album-with-items';
import { PORTION_SIZE } from '../../../../components/consts';

import {
    CREATE_ALBUM,
    CREATE_ALBUM_FAIL,
    CREATE_ALBUM_SUCCESS,
    DESTROY_RESOURCE,
    FETCH_ALBUM_SUCCESS,
    MOVE_CLONED_RESOURCES,
    REMOVE_ITEMS_FROM_ALBUM,
    RESTORE_DELETED_ITEM,
    UPDATE_ALBUM,
    DESELECT_ALL,
    SET_SELECTED
} from '../../../../components/redux/store/actions/types';
import { getDateWithoutTime } from '../../../../components/helpers/date';

const ALBUM_ID = albumWithItems.album.id;

/**
 * @param {number} start
 * @param {number} end
 * @returns {Object}
 */
const getAlbumPortion = (start, end) => {
    const result = Object.assign({}, albumWithItems, {
        album: Object.assign({}, albumWithItems.album, { items: albumWithItems.album.items.slice(start, end) }),
        resources: albumWithItems.resources.slice(start, end)
    });
    result.resources.forEach((resource) => {
        resource.albumIds = [ALBUM_ID];
    });
    return result;
};

describe('personal albums reducer', () => {
    describe('UPDATE_ALBUM', () => {
        it('Обновление данных альбома', () => {
            const state = { albumsByIds: {} };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: UPDATE_ALBUM,
                payload: { id: ALBUM_ID, isLoading: true }
            });

            expect(newState.albumsByIds[ALBUM_ID].isLoading).toBe(true);
        });
    });

    describe('FETCH_ALBUM_SUCCESS', () => {
        it('Загрузка первой порции альбома', () => {
            const state = { albumsByIds: {} };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: FETCH_ALBUM_SUCCESS,
                payload: getAlbumPortion(0, 40)
            });
            const album = newState.albumsByIds[ALBUM_ID];
            expect(album.isLoading).toBe(false);
            expect(album.isCompleted).toBe(false);
            expect(album.structureVersion).toBe(1);
            expect(album.clusters[0].size).toBe(40);
            expect(album.clusters[0].items.length).toBe(40);
        });

        it('Загрузка второй порции альбома', () => {
            const state = personalAlbums({ albumsByIds: {} }, {
                type: FETCH_ALBUM_SUCCESS,
                payload: getAlbumPortion(0, 40)
            });
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: FETCH_ALBUM_SUCCESS,
                payload: getAlbumPortion(40, 45)
            });

            const album = newState.albumsByIds[ALBUM_ID];
            expect(album.isCompleted).toBe(true);
            expect(album.structureVersion).toBe(2);
            expect(album.clusters[1].size).toBe(5);
            expect(album.clusters[1].items.length).toBe(5);
        });

        it('Загрузка порции не должна добавлять дубли', () => {
            const state = personalAlbums({ albumsByIds: {} }, {
                type: FETCH_ALBUM_SUCCESS,
                payload: getAlbumPortion(0, 2)
            });
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: FETCH_ALBUM_SUCCESS,
                payload: getAlbumPortion(0, 40)
            });
            const album = newState.albumsByIds[ALBUM_ID];
            expect(album.clusters.length).toBe(1);
            expect(album.clusters[0].size).toBe(40);
            expect(album.clusters[0].items.length).toBe(40);
            const ids = album.clusters[0].items.map(({ itemId }) => itemId);
            expect(_.uniq(ids).length).toBe(ids.length);
        });

        it('Избранные: должен обновить favoriteAlbumId, если получили информацию об альбоме Избранное', () => {
            const state = deepFreeze({ albumsByIds: {}, favoriteAlbumId: null });
            expect(state.favoriteAlbumId).toBe(null);

            const newState = personalAlbums(state, {
                type: FETCH_ALBUM_SUCCESS,
                payload: {
                    album: { id: 'my-favorite-album-id', album_type: 'favorites', items: [] }
                }
            });

            expect(newState.favoriteAlbumId).toBe('my-favorite-album-id');
        });

        describe('Гео-альбомы', () => {
            /**
             * Добавляет порцию фейковых данных для экшна гео-альбома
             *
             * @param {Object} payload Объект-состояние
             * @param {number} count Количество элементов
             * @param {Date|number} startDate Стартовая дата
             * @param {number} diffTimestamp Смещение между датами элементов
             *
             * @returns {Object} payload
             */
            const addGeoPayloadPortion = (payload, count, startDate = 0, diffTimestamp = 0) => {
                payload.resources = payload.resources || [];
                payload.album = payload.album || { id: 'geo-id', album_type: 'geo', items: [] };

                let curDate = startDate instanceof Date ? startDate.getTime() : startDate;

                for (let i = 0; i < count; i++) {
                    payload.resources.push({ etime: curDate / 1000 });
                    payload.album.items.push({});

                    curDate += diffTimestamp;
                }

                return payload;
            };

            /**
             * Генерирует последовательность элементов гео-альбома с фотографиями через равный промежуток времени
             *
             * @param {number} count Количество фотографий
             * @param {number} startTime Таймстемп первой фотографии
             * @param {number} diff Время в секундах между фотографиями
             * @returns {Array} Последовательность фото
             */
            const getGeoSequenceItems = (count, startTime = 0, diff = 0) => {
                return new Array(count).fill(0)
                    .map((value, index) => ({ resourceTime: startTime + index * diff }));
            };

            describe('Формирование и объединение кластеров по датам', () => {
                it('Все элементы из одного дня => 1 кластер', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const date = new Date(2020, 10, 10, 13);
                    const diffTs = 60;
                    const size = 5;

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: addGeoPayloadPortion({}, size, date, diffTs)
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Все фото должны попасть в один кластер
                    expect(geoAlbum.clusters).toEqual([{
                        id: new Date(2020, 10, 10),
                        from: date.getTime(), fromDay: new Date(2020, 10, 10),
                        to: date.getTime() + (size - 1) * diffTs, toDay: new Date(2020, 10, 10),
                        size,
                        items: getGeoSequenceItems(size, date.getTime(), diffTs)
                    }]);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Все элементы из разных дней одного месяца => должны объединиться', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const date = new Date(2020, 10, 10, 13);
                    const diffTs = 24 * 60 * 60 * 1000;
                    const size = 5;

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: addGeoPayloadPortion({}, size, date, diffTs)
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Все фото должны попасть в один кластер
                    expect(geoAlbum.clusters).toEqual([{
                        id: new Date(2020, 10, 10),
                        from: date.getTime(), fromDay: new Date(2020, 10, 10),
                        to: new Date(2020, 10, 14, 13).getTime(), toDay: new Date(2020, 10, 14),
                        size,
                        items: getGeoSequenceItems(size, date.getTime(), diffTs)
                    }]);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Все элементы из разных месяцев => отдельные кластеры', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: {
                            album: {
                                id: 'geo-id',
                                album_type: 'geo',
                                items: new Array(5).fill({})
                            },
                            resources: [
                                { etime: new Date(2020, 10, 10).getTime() / 1000 },
                                { etime: new Date(2020, 9, 10).getTime() / 1000 },
                                { etime: new Date(2020, 8, 10).getTime() / 1000 },
                                { etime: new Date(2020, 7, 10).getTime() / 1000 },
                                { etime: new Date(2020, 6, 10).getTime() / 1000 }
                            ]
                        }
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Все фото должны попасть в разные кластеры
                    expect(geoAlbum.clusters).toHaveLength(5);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Все кластеры в течение месяца достаточно большие => без объединения', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const size = MIN_PHOTOS_IN_CLUSTER;
                    const dates = [
                        new Date(2020, 10, 10),
                        new Date(2020, 10, 9),
                        new Date(2020, 10, 5)
                    ];

                    // Добавляем по 10 фото в разные дни месяца
                    const payload = {};
                    for (const date of dates) {
                        addGeoPayloadPortion(payload, size, date);
                    }

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Должны отдельно показываться 3 кластера
                    expect(geoAlbum.clusters).toEqual(dates.map((date) => {
                        return {
                            id: date,
                            from: date.getTime(), fromDay: date,
                            to: date.getTime(), toDay: date,
                            size,
                            items: getGeoSequenceItems(size, date.getTime())
                        };
                    }));

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Маленький кластер первый в месяце => должен объединиться со следующим', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    // 1 фото за 10ноя + 10 фото за 7ноя
                    const payload = {};
                    addGeoPayloadPortion(payload, 1, new Date(2020, 10, 10));
                    addGeoPayloadPortion(payload, MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 7));

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Должны отдельно показываться 3 кластера
                    expect(geoAlbum.clusters).toEqual([
                        {
                            id: new Date(2020, 10, 10),
                            from: new Date(2020, 10, 10).getTime(), fromDay: new Date(2020, 10, 10),
                            to: new Date(2020, 10, 7).getTime(), toDay: new Date(2020, 10, 7),
                            size: 1 + MIN_PHOTOS_IN_CLUSTER,
                            items: [
                                ...getGeoSequenceItems(1, new Date(2020, 10, 10).getTime()),
                                ...getGeoSequenceItems(MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 7).getTime())
                            ]
                        }
                    ]);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Маленький в середине месяца => должен объединиться с предыдущим', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    // 10 фото за 15ноя + 1 фото за 10ноя + 10 фото за 7ноя
                    const payload = {};
                    addGeoPayloadPortion(payload, MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 15));
                    addGeoPayloadPortion(payload, 1, new Date(2020, 10, 10));
                    addGeoPayloadPortion(payload, MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 7));

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Должны отдельно показываться 2 кластера
                    expect(geoAlbum.clusters).toEqual([
                        {
                            id: new Date(2020, 10, 15),
                            from: new Date(2020, 10, 15).getTime(), fromDay: new Date(2020, 10, 15),
                            to: new Date(2020, 10, 10).getTime(), toDay: new Date(2020, 10, 10),
                            size: MIN_PHOTOS_IN_CLUSTER + 1,
                            items: [
                                ...getGeoSequenceItems(MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 15).getTime()),
                                ...getGeoSequenceItems(1, new Date(2020, 10, 10).getTime())
                            ]
                        },
                        {
                            id: new Date(2020, 10, 7),
                            from: new Date(2020, 10, 7).getTime(), fromDay: new Date(2020, 10, 7),
                            to: new Date(2020, 10, 7).getTime(), toDay: new Date(2020, 10, 7),
                            size: MIN_PHOTOS_IN_CLUSTER,
                            items: getGeoSequenceItems(MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 7).getTime())
                        }
                    ]);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Маленький кластер последний в месяце => должен объединиться с предыдущим', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    // 10 фото за 15ноя + 1 фото за 10ноя + 10 фото за 7июл
                    const payload = {};
                    addGeoPayloadPortion(payload, MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 15));
                    addGeoPayloadPortion(payload, 1, new Date(2020, 10, 10));
                    addGeoPayloadPortion(payload, MIN_PHOTOS_IN_CLUSTER, new Date(2020, 5, 7));

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Должны отдельно показываться 2 кластера
                    expect(geoAlbum.clusters).toEqual([
                        {
                            id: new Date(2020, 10, 15),
                            from: new Date(2020, 10, 15).getTime(), fromDay: new Date(2020, 10, 15),
                            to: new Date(2020, 10, 10).getTime(), toDay: new Date(2020, 10, 10),
                            size: MIN_PHOTOS_IN_CLUSTER + 1,
                            items: [
                                ...getGeoSequenceItems(MIN_PHOTOS_IN_CLUSTER, new Date(2020, 10, 15).getTime()),
                                ...getGeoSequenceItems(1, new Date(2020, 10, 10).getTime())
                            ]
                        },
                        {
                            id: new Date(2020, 5, 7),
                            from: new Date(2020, 5, 7).getTime(), fromDay: new Date(2020, 5, 7),
                            to: new Date(2020, 5, 7).getTime(), toDay: new Date(2020, 5, 7),
                            size: MIN_PHOTOS_IN_CLUSTER,
                            items: getGeoSequenceItems(MIN_PHOTOS_IN_CLUSTER, new Date(2020, 5, 7).getTime())
                        }
                    ]);

                    // буфер пуст
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });
            });

            describe('Обработка буфера', () => {
                /**
                 * Хелпер для генерации альбома в стейте
                 *
                 * @param {Array<{to: Date, items: Array<Date>}>} clusters Кластеры альбома
                 * @param {Array<Date>} buffer Буфер
                 * @returns {Object}
                 */
                const getGeoAlbumState = ({ clusters = [], buffer = [] }) => {
                    return {
                        isCompleted: false,
                        structureVersion: 1,
                        clusters: clusters.map(({ to, items }) => {
                            const lastItem = items[items.length - 1];
                            const toDate = to || lastItem.getTime();

                            return {
                                id: items[0],
                                from: items[0].getTime(), fromDay: getDateWithoutTime(items[0].getTime()),
                                to: toDate, toDay: getDateWithoutTime(toDate),
                                items: items.map((date) => ({ resourceTime: date.getTime() })),
                                size: items.length
                            };
                        }),
                        bufferedPhotos: buffer.map((date) => ({ resourceTime: date.getTime() }))
                    };
                };

                it('Порция из одного дня. Часть кластера должна попасть в буфер', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const size = PORTION_SIZE;
                    const date = new Date(2020, 1, 1);
                    const diffTs = 60;

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: addGeoPayloadPortion({}, size, date, diffTs)
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Проверяем информацию об альбоме
                    expect(geoAlbum).toEqual(expect.objectContaining({
                        isCompleted: false,
                        structureVersion: 1
                    }));

                    // Проверяем кластер
                    const clusterSize = size - MIN_PHOTOS_IN_CLUSTER;

                    // В to должна попасть дата последнего фото в буфере
                    expect(geoAlbum.clusters).toEqual([{
                        id: date,
                        from: date.getTime(), fromDay: date,
                        to: date.getTime() + (PORTION_SIZE - 1) * diffTs, toDay: date,
                        size: clusterSize,
                        items: getGeoSequenceItems(clusterSize, date.getTime(), diffTs)
                    }]);

                    // Последние фото попали в буфер
                    expect(geoAlbum.bufferedPhotos).toHaveLength(MIN_PHOTOS_IN_CLUSTER);
                });

                it('Порция из разных месяцев, в которой полностью кластер должен попасть в буфер', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    // Размеры кластеров, например, 30 и 10. В этом случае кластер из 10 должен полностью уехать в буфер
                    const size1 = PORTION_SIZE - MIN_PHOTOS_IN_CLUSTER;
                    const date1 = new Date(2020, 6, 1);

                    const size2 = MIN_PHOTOS_IN_CLUSTER;
                    const date2 = new Date(2020, 1, 1);

                    const diffTs = 60;

                    let payload = addGeoPayloadPortion({}, size1, date1, diffTs);
                    payload = addGeoPayloadPortion(payload, size2, date2, diffTs);

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Проверяем информацию об альбоме
                    expect(geoAlbum).toEqual(expect.objectContaining({
                        isCompleted: false,
                        structureVersion: 1
                    }));

                    // Проверяем кластер
                    // Один кластер должен остаться в
                    expect(geoAlbum.clusters).toEqual([{
                        id: date1,
                        from: date1.getTime(), fromDay: date1,
                        to: date1.getTime() + (size1 - 1) * diffTs, toDay: date1,
                        size: size1,
                        items: getGeoSequenceItems(size1, date1.getTime(), diffTs),
                    }]);

                    // Последние фото попали в буфер
                    expect(geoAlbum.bufferedPhotos).toEqual(getGeoSequenceItems(size2, date2.getTime(), diffTs));
                });

                it('В буфере часть кластера -> должен корректно сброситься и новые элементы добавиться', () => {
                    const state = {
                        albumsByIds: {
                            'geo-id': getGeoAlbumState({
                                clusters: [
                                    {
                                        to: new Date(2020, 1, 1, 15),
                                        items: [
                                            new Date(2020, 1, 1, 12),
                                            new Date(2020, 1, 1, 13)]
                                    }
                                ],
                                buffer: [
                                    new Date(2020, 1, 1, 14),
                                    new Date(2020, 1, 1, 15)]
                            })
                        }
                    };

                    const newState = personalAlbums(deepFreeze(state), {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: {
                            album: { id: 'geo-id', album_type: 'geo', items: [{}, {}] },
                            resources: [
                                { etime: new Date(2020, 1, 1, 16).getTime() / 1000 },
                                { etime: new Date(2020, 1, 1, 17).getTime() / 1000 }
                            ]
                        }
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Альбом завершен
                    expect(geoAlbum).toEqual(expect.objectContaining({
                        isCompleted: true,
                        structureVersion: 2,
                        bufferedPhotos: [], // буфер сбросился
                        clusters: [{
                            id: new Date(2020, 1, 1, 12),
                            from: new Date(2020, 1, 1, 12).getTime(), fromDay: new Date(2020, 1, 1),
                            to: new Date(2020, 1, 1, 17).getTime(), toDay: new Date(2020, 1, 1),
                            size: 6,
                            items: [
                                { resourceTime: new Date(2020, 1, 1, 12).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 13).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 14).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 15).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 16).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 17).getTime() }
                            ]
                        }]
                    }));
                });

                it('В буфере отдельный кластер -> должен корректно сброситься и новые элементы добавиться', () => {
                    const state = {
                        albumsByIds: {
                            'geo-id': getGeoAlbumState({
                                clusters: [{
                                    items: [
                                        new Date(2020, 10, 2, 13),
                                        new Date(2020, 10, 1, 12)
                                    ]
                                }],
                                buffer: [
                                    new Date(2020, 6, 2, 6),
                                    new Date(2020, 6, 1, 5)
                                ]
                            })
                        }
                    };

                    const newState = personalAlbums(deepFreeze(state), {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: {
                            album: { id: 'geo-id', album_type: 'geo', items: [{}, {}] },
                            resources: [
                                { etime: new Date(2020, 1, 1, 17).getTime() / 1000 },
                                { etime: new Date(2020, 1, 1, 16).getTime() / 1000 }
                            ]
                        }
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Альбом завершен
                    expect(geoAlbum).toEqual({
                        id: 'geo-id',
                        album_type: 'geo',
                        isLoading: false,
                        isCompleted: true,
                        structureVersion: 2,
                        bufferedPhotos: [], // буфер сбросился
                        clusters: [
                            {
                                id: new Date(2020, 10, 2, 13),
                                from: new Date(2020, 10, 2, 13).getTime(), fromDay: new Date(2020, 10, 2),
                                to: new Date(2020, 10, 1, 12).getTime(), toDay: new Date(2020, 10, 1),
                                size: 2,
                                items: [
                                    { resourceTime: new Date(2020, 10, 2, 13).getTime() },
                                    { resourceTime: new Date(2020, 10, 1, 12).getTime() }
                                ]
                            },
                            {
                                id: new Date(2020, 6, 2),
                                from: new Date(2020, 6, 2, 6).getTime(), fromDay: new Date(2020, 6, 2),
                                to: new Date(2020, 6, 1, 5).getTime(), toDay: new Date(2020, 6, 1),
                                size: 2,
                                items: [
                                    { resourceTime: new Date(2020, 6, 2, 6).getTime() },
                                    { resourceTime: new Date(2020, 6, 1, 5).getTime() }
                                ]
                            },
                            {
                                id: new Date(2020, 1, 1),
                                from: new Date(2020, 1, 1, 17).getTime(), fromDay: new Date(2020, 1, 1),
                                to: new Date(2020, 1, 1, 16).getTime(), toDay: new Date(2020, 1, 1),
                                size: 2,
                                items: [
                                    { resourceTime: new Date(2020, 1, 1, 17).getTime() },
                                    { resourceTime: new Date(2020, 1, 1, 16).getTime() }
                                ]
                            },
                        ]
                    });
                });

                it('Последняя порция данных -> буфер часть кластера -> должен сброситься', () => {
                    const state = {
                        albumsByIds: {
                            'geo-id': getGeoAlbumState({
                                clusters: [
                                    { items: [new Date(2020, 1, 1, 13)] }
                                ],
                                buffer: [
                                    new Date(2020, 1, 1, 14),
                                    new Date(2020, 1, 1, 15)
                                ]
                            })
                        }
                    };

                    const newState = personalAlbums(deepFreeze(state), {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: {
                            album: { id: 'geo-id', album_type: 'geo', items: [] },
                            resources: []
                        }
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Альбом завершен
                    expect(geoAlbum).toEqual(expect.objectContaining({
                        isCompleted: true,
                        structureVersion: 2,
                        bufferedPhotos: [], // буфер сбросился
                        clusters: [{
                            id: new Date(2020, 1, 1, 13),
                            from: new Date(2020, 1, 1, 13).getTime(), fromDay: new Date(2020, 1, 1),
                            to: new Date(2020, 1, 1, 15).getTime(), toDay: new Date(2020, 1, 1),
                            size: 3,
                            items: [
                                { resourceTime: new Date(2020, 1, 1, 13).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 14).getTime() },
                                { resourceTime: new Date(2020, 1, 1, 15).getTime() },
                            ]
                        }]
                    }));
                });

                it('Последняя порция данных -> буфер отдельный кластер -> должен сброситься', () => {
                    const state = {
                        albumsByIds: {
                            'geo-id': getGeoAlbumState({
                                clusters: [
                                    { items: [new Date(2020, 6, 15)] }
                                ],
                                buffer: [
                                    new Date(2020, 1, 1, 14),
                                    new Date(2020, 1, 1, 15)
                                ]
                            })
                        }
                    };

                    const newState = personalAlbums(deepFreeze(state), {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: {
                            album: { id: 'geo-id', album_type: 'geo', items: [] },
                            resources: []
                        }
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Альбом завершен
                    expect(geoAlbum).toEqual({
                        id: 'geo-id',
                        album_type: 'geo',
                        isLoading: false,
                        isCompleted: true,
                        structureVersion: 2,
                        bufferedPhotos: [], // буфер сбросился
                        clusters: [
                            {
                                id: new Date(2020, 6, 15),
                                from: new Date(2020, 6, 15).getTime(), fromDay: new Date(2020, 6, 15),
                                to: new Date(2020, 6, 15).getTime(), toDay: new Date(2020, 6, 15),
                                size: 1,
                                items: [
                                    { resourceTime: new Date(2020, 6, 15).getTime() }
                                ]
                            },
                            {
                                id: new Date(2020, 1, 1),
                                from: new Date(2020, 1, 1, 14).getTime(), fromDay: new Date(2020, 1, 1),
                                to: new Date(2020, 1, 1, 15).getTime(), toDay: new Date(2020, 1, 1),
                                size: 2,
                                items: [
                                    { resourceTime: new Date(2020, 1, 1, 14).getTime() },
                                    { resourceTime: new Date(2020, 1, 1, 15).getTime() },
                                ]
                            }
                        ]
                    });
                });
            });

            describe('Комплексные случаи', () => {
                it('Все фото за один день -> 1 кластер. Кол-во фото: < 1 порции', () => {
                    const state = deepFreeze({ albumsByIds: {} });

                    const size = PORTION_SIZE - 1;
                    const date = new Date(2020, 1, 1);
                    const diffTs = 60;

                    const newState = personalAlbums(state, {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: addGeoPayloadPortion({}, size, date, diffTs)
                    });

                    const geoAlbum = newState.albumsByIds['geo-id'];

                    // Добавился гео-альбом
                    expect(geoAlbum).toBeDefined();

                    // Проверяем информацию об альбоме
                    expect(geoAlbum).toEqual(expect.objectContaining({
                        isCompleted: true,
                        structureVersion: 1
                    }));

                    // Проверяем кластеры
                    expect(geoAlbum.clusters).toEqual([{
                        id: date,
                        from: date.getTime(), fromDay: date,
                        to: date.getTime() + (size - 1) * diffTs, toDay: date,
                        size,
                        items: getGeoSequenceItems(size, date.getTime(), diffTs)
                    }]);

                    // Нет буферизованных фото
                    expect(geoAlbum.bufferedPhotos).toHaveLength(0);
                });

                it('Все фото за один день. Кол-во фото: 5 полных порций + 10 фото', () => {
                    const date = new Date(2020, 1, 1);
                    const diffTs = 60; // фото идут через минуту каждая

                    let curState = { albumsByIds: {} };

                    // Возьмем 5 порций
                    for (let i = 0; i <= 4; i++) {
                        // Формируем данные экшна. Все фото идут поминутно подряд
                        const payload = addGeoPayloadPortion({},
                            PORTION_SIZE,
                            date.getTime() + i * PORTION_SIZE * diffTs,
                            diffTs);

                        curState = personalAlbums(deepFreeze(curState), {
                            type: FETCH_ALBUM_SUCCESS,
                            payload
                        });

                        const geoAlbum = curState.albumsByIds['geo-id'];

                        // Проверяем информацию об альбоме
                        expect(geoAlbum).toEqual(expect.objectContaining({
                            isCompleted: false,
                            structureVersion: i + 1
                        }));

                        // Проверяем кластер
                        // Последние фото попали в буфер
                        const size = PORTION_SIZE * (i + 1) - MIN_PHOTOS_IN_CLUSTER;
                        const items = getGeoSequenceItems(size, date.getTime(), diffTs);

                        expect(geoAlbum.clusters).toEqual([{
                            id: date,
                            from: date.getTime(), fromDay: date,
                            to: date.getTime() + (PORTION_SIZE * (i + 1) - 1) * diffTs, toDay: date,
                            size,
                            items
                        }]);

                        // Последние фото попали в буфер
                        expect(geoAlbum.bufferedPhotos).toHaveLength(MIN_PHOTOS_IN_CLUSTER);
                    }

                    // ПОСЛЕДНЯЯ ПОРЦИЯ: 10 фото
                    const newState2 = personalAlbums(deepFreeze(curState), {
                        type: FETCH_ALBUM_SUCCESS,
                        payload: addGeoPayloadPortion({}, 10, date.getTime() + PORTION_SIZE * 5 * diffTs, diffTs)
                    });

                    const geoAlbum2 = newState2.albumsByIds['geo-id'];

                    // Альбом завершен
                    expect(geoAlbum2).toEqual(expect.objectContaining({
                        isCompleted: true,
                        structureVersion: 6
                    }));

                    // Проверяем кластер
                    const size = PORTION_SIZE * 5 + 10;
                    const items = getGeoSequenceItems(size, date.getTime(), diffTs);

                    expect(geoAlbum2.clusters).toEqual([{
                        id: date,
                        from: date.getTime(), fromDay: date,
                        to: items[items.length - 1].resourceTime, toDay: date,
                        size,
                        items
                    }]);

                    // Буфер пустой
                    expect(geoAlbum2.bufferedPhotos).toHaveLength(0);
                });
            });
        });
    });

    describe('CREATE_ALBUM', () => {
        it('Увеличение счетчика создаваемых альбомов', () => {
            const state = { creatingAlbumsCount: 0 };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: CREATE_ALBUM,
                payload: { isPublishSelected: false }
            });

            expect(newState.creatingAlbumsCount).toBe(1);
        });
    });

    describe('CREATE_ALBUM_SUCCESS', () => {
        it('Уменьшение счетчика создаваемых альбомов и добавление нового альбома', () => {
            const state = {
                albumsByIds: { a: { id: 'a', idAlbum: 'a-processed', title: 'album a' } },
                creatingAlbumsCount: 1
            };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: CREATE_ALBUM_SUCCESS,
                payload: { id: 'b', idAlbum: 'b-processed', title: 'album b' }
            });

            expect(newState.creatingAlbumsCount).toBe(0);
            expect(newState.albumsByIds).toEqual({
                a: { id: 'a', idAlbum: 'a-processed', title: 'album a' },
                b: { id: 'b', idAlbum: 'b-processed', title: 'album b' }
            });
            expect(newState.lastCreatedAlbumId).toBe('b');
        });
    });

    describe('CREATE_ALBUM_FAIL', () => {
        it('Уменьшение счетчика создаваемых альбомов', () => {
            const state = { creatingAlbumsCount: 1 };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: CREATE_ALBUM_FAIL
            });

            expect(newState.creatingAlbumsCount).toBe(0);
        });
    });

    describe('DESELECT_ALL', () => {
        it('Сброс признака публикации выделенных фото при развыделении', () => {
            const state = { isPublishSelected: true };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: DESELECT_ALL
            });

            expect(newState.isPublishSelected).toBe(false);
        });

        it('Сброс последнего созданного альбома путем публикации выделенных фото при развыделении', () => {
            const state = { lastCreatedAlbumId: 'a', isPublishSelected: true };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: DESELECT_ALL
            });

            expect(newState.lastCreatedAlbumId).toBe(null);
            expect(newState.isPublishSelected).toBe(false);
        });
    });

    describe('SET_SELECTED', () => {
        it('Сброс признака публикации выделенных фото при смене выделения', () => {
            const state = { isPublishSelected: true };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: SET_SELECTED,
                payload: { selected: ['/disk/1'] }
            });

            expect(newState.isPublishSelected).toBe(false);
        });

        it('Сброс последнего созданного альбома путем публикации выделенных фото при смене выделения', () => {
            const state = { lastCreatedAlbumId: 'a', isPublishSelected: true };
            deepFreeze(state);
            const newState = personalAlbums(state, {
                type: SET_SELECTED,
                payload: { selected: ['/disk/1'] }
            });

            expect(newState.lastCreatedAlbumId).toBe(null);
            expect(newState.isPublishSelected).toBe(false);
        });
    });

    /**
     * @param {number} [secondPortionResourcesCount=40]
     * @returns {Object}
     */
    const prepareState = (secondPortionResourcesCount = 40) => {
        let state = personalAlbums({ albumsByIds: {} }, { type: FETCH_ALBUM_SUCCESS, payload: getAlbumPortion(0, 40) });
        state = personalAlbums(state, {
            type: FETCH_ALBUM_SUCCESS,
            payload: getAlbumPortion(40, 40 + secondPortionResourcesCount)
        });
        deepFreeze(state);
        return state;
    };

    describe('MOVE_CLONED_RESOURCES', () => {
        it('Должен менять id при перемещении ресурса', () => {
            const state = prepareState();
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/photounlim/2019-06-04 13-05-54.JPG', albumIds: [ALBUM_ID] },
                        dst: { id: '/disk/move-into/2019-06-04 13-05-54.JPG', albumIds: [ALBUM_ID] }
                    }]
                }
            });

            expect(newState.albumsByIds[ALBUM_ID].clusters[1].items[19].id)
                .toBe('/disk/move-into/2019-06-04 13-05-54.JPG');
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
        });

        it('Не должен менять state при перемещении ресурса из альбома которого нет в сторе', () => {
            const state = prepareState();
            const newState = personalAlbums(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/photounlim/2020-06-04 13-05-54.JPG', albumIds: ['other-album'] },
                        dst: { id: '/disk/move-into/2020-06-04 13-05-54.JPG', albumIds: ['other-album'] }
                    }]
                }
            });

            expect(newState).toBe(state);
        });

        it('Не должен менять state при перемещении ресурса не из альбома', () => {
            const state = prepareState();
            const newState = personalAlbums(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/photounlim/2020-06-04 13-05-54.JPG' },
                        dst: { id: '/disk/move-into/2020-06-04 13-05-54.JPG' }
                    }]
                }
            });

            expect(newState).toBe(state);
        });

        it('Должен безопасно удалять ресурс при перемещениии в корзину', () => {
            const state = prepareState();
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/IMG_1528.HEIC', albumIds: [ALBUM_ID] },
                        dst: { id: '/trash/IMG_1528.HEIC', albumIds: [ALBUM_ID], isInTrash: true }
                    }]
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[0];
            expect(cluster.items[0].id).toBe('/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG');
            expect(cluster.items.length).toBe(40);
            expect(cluster.size).toBe(40);
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
            expect(newState.deletedItems[ALBUM_ID]['/disk/IMG_1528.HEIC']).toEqual({
                itemId: '5de665f7e4d70e06400bc88c',
                id: '/disk/IMG_1528.HEIC',
                orderIndex: 1,
                width: 3024,
                height: 4032,
                beauty: -1.69977
            });
        });

        it('Должен объединять кластеры, если в псоледнем кластере осталось мало элементов при перемещениии в корзину', () => {
            const state = prepareState(10);
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/photounlim/2019-06-04 13-08-01.JPG', albumIds: [ALBUM_ID] },
                        dst: { id: '/trash/2019-06-04 13-08-01.JPG', albumIds: [ALBUM_ID], isInTrash: true }
                    }]
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[0];
            expect(newState.albumsByIds[ALBUM_ID].clusters.length).toBe(1);
            expect(cluster.items.length).toBe(49);
            expect(cluster.size).toBe(49);
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
        });
    });

    describe('REMOVE_ITEMS_FROM_ALBUM', () => {
        it('Должен безопасно удалять элемент из альбома при исключении', () => {
            const state = prepareState();
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: {
                    albumId: ALBUM_ID,
                    resourcesIds: ['/disk/IMG_1528.HEIC']
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[0];
            expect(cluster.items[0].id).toBe('/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG');
            expect(cluster.items.length).toBe(40);
            expect(cluster.size).toBe(40);
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
            expect(newState.deletedItems[ALBUM_ID]['/disk/IMG_1528.HEIC']).toEqual({
                itemId: '5de665f7e4d70e06400bc88c',
                id: '/disk/IMG_1528.HEIC',
                orderIndex: 1,
                width: 3024,
                height: 4032,
                beauty: -1.69977
            });
        });

        it('Не должен менять state при исключении элемента которого нет в альбоме', () => {
            const state = prepareState();
            const newState = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: {
                    albumId: ALBUM_ID,
                    resourcesIds: ['/disk/IMG.HEIC']
                }
            });

            expect(newState).toBe(state);
        });

        it('Должен объединять кластеры, если в псоледнем кластере осталось мало элементов при исключении из альбома', () => {
            const state = prepareState(10);
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: {
                    albumId: ALBUM_ID,
                    resourcesIds: ['/photounlim/2019-06-04 13-08-01.JPG']
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[0];
            expect(newState.albumsByIds[ALBUM_ID].clusters.length).toBe(1);
            expect(cluster.items.length).toBe(49);
            expect(cluster.size).toBe(49);
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
        });

        it('Должен корректно перестраивать кластеры, если второй кластер полностью удалился, а в первом остались ресурсы', () => {
            const state = prepareState();

            const albumBefore = state.albumsByIds[ALBUM_ID];
            expect(albumBefore.clusters.length).toBe(2);
            expect(albumBefore.clusters[0].size).toBe(40);
            expect(albumBefore.clusters[1].size).toBe(40);

            const resourcesIds = albumWithItems.resources
                .map(({ id }) => id)
                .slice(2);

            const newState = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: {
                    albumId: ALBUM_ID,
                    resourcesIds
                }
            });

            const album = newState.albumsByIds[ALBUM_ID];
            expect(album.clusters.length).toBe(1);
            expect(album.clusters[0].size).toBe(2);
        });
    });

    describe('DESTROY_RESOURCE', () => {
        it('Должен удалять элемент из альбома', () => {
            const state = prepareState();
            const { structureVersion } = state.albumsByIds[ALBUM_ID];
            const newState = personalAlbums(state, {
                type: DESTROY_RESOURCE,
                payload: {
                    data: { id: '/disk/IMG_1528.HEIC', albumIds: [ALBUM_ID] }
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[0];
            expect(cluster.items[0].id).toBe('/disk/kri0-gen test folder/2019-10-22 18-38-44.JPG');
            expect(cluster.items.length).toBe(40);
            expect(cluster.size).toBe(40);
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
            expect(newState.deletedItems).toBeFalsy();
        });

        it('Должен очистиить deletedItems', () => {
            const state = prepareState();

            const stateWithRemovedItem = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: { albumId: ALBUM_ID, resourcesIds: ['/photounlim/2019-06-04 13-07-10.JPG'] }
            });

            const { structureVersion } = stateWithRemovedItem.albumsByIds[ALBUM_ID];
            deepFreeze(stateWithRemovedItem);

            const newState = personalAlbums(stateWithRemovedItem, {
                type: DESTROY_RESOURCE,
                payload: {
                    data: { id: '/photounlim/2019-06-04 13-07-10.JPG', albumIds: [ALBUM_ID] }
                }
            });

            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion);
            expect(newState.deletedItems[ALBUM_ID]).toBeFalsy();
        });

        it('Не должен менять state если удалился элемент не из альбома', () => {
            const state = prepareState();

            const newState = personalAlbums(state, {
                type: DESTROY_RESOURCE,
                payload: {
                    data: { id: '/disk/IMG.HEIC' }
                }
            });

            expect(newState).toBe(state);
        });
    });

    describe('RESTORE_DELETED_ITEM', () => {
        it('Должен возвращать элемент элемент на правильное место', () => {
            const state = prepareState();

            const stateWithRemovedItem = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: { albumId: ALBUM_ID, resourcesIds: ['/photounlim/2019-06-04 13-07-10.JPG'] }
            });

            const { structureVersion } = stateWithRemovedItem.albumsByIds[ALBUM_ID];
            deepFreeze(stateWithRemovedItem);

            expect(stateWithRemovedItem.albumsByIds[ALBUM_ID].clusters[1].items[9])
                .toEqual({
                    itemId: '5de665f7e4d70e06400bc8c6',
                    id: '/photounlim/2019-06-04 13-06-47.JPG',
                    orderIndex: 51
                });

            const newState = personalAlbums(stateWithRemovedItem, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    albumIds: [ALBUM_ID],
                    id: '/photounlim/2019-06-04 13-07-10.JPG'
                }
            });

            const cluster = newState.albumsByIds[ALBUM_ID].clusters[1];
            expect(cluster.items[9]).toEqual({
                itemId: '5de665f7e4d70e06400bc8c5',
                id: '/photounlim/2019-06-04 13-07-10.JPG',
                orderIndex: 50
            });
            expect(newState.albumsByIds[ALBUM_ID].structureVersion).toBe(structureVersion + 1);
        });

        it('Должен корректно возвращать элемент в начало альбома', () => {
            const state = prepareState();

            const stateWithRemovedItem = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: { albumId: ALBUM_ID, resourcesIds: ['/disk/IMG_1528.HEIC'] }
            });

            deepFreeze(stateWithRemovedItem);

            const newState = personalAlbums(stateWithRemovedItem, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    albumIds: [ALBUM_ID],
                    id: '/disk/IMG_1528.HEIC'
                }
            });

            expect(newState.albumsByIds[ALBUM_ID].clusters[0].items[0]).toEqual({
                itemId: '5de665f7e4d70e06400bc88c',
                id: '/disk/IMG_1528.HEIC',
                orderIndex: 1,
                width: 3024,
                height: 4032,
                beauty: -1.69977
            });
        });

        it('Должен корректно возвращать элемент в конец альбома', () => {
            const state = prepareState();

            const stateWithRemovedItem = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: { albumId: ALBUM_ID, resourcesIds: ['/disk/Test upload/10-4111_1.jpg'] }
            });

            deepFreeze(stateWithRemovedItem);

            const newState = personalAlbums(stateWithRemovedItem, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    albumIds: [ALBUM_ID],
                    id: '/disk/Test upload/10-4111_1.jpg'
                }
            });

            expect(newState.albumsByIds[ALBUM_ID].clusters[1].items[39]).toEqual({
                itemId: '5de665f7e4d70e06400bc8e3',
                id: '/disk/Test upload/10-4111_1.jpg',
                orderIndex: 80
            });
        });

        it('Должен корректно возвращать элемент в пустой альбом', () => {
            const state = personalAlbums(
                { albumsByIds: {} },
                { type: FETCH_ALBUM_SUCCESS, payload: getAlbumPortion(0, 1) }
            );

            const stateWithRemovedItem = personalAlbums(state, {
                type: REMOVE_ITEMS_FROM_ALBUM,
                payload: { albumId: ALBUM_ID, resourcesIds: ['/disk/IMG_1528.HEIC'] }
            });

            deepFreeze(stateWithRemovedItem);

            const newState = personalAlbums(stateWithRemovedItem, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    albumIds: [ALBUM_ID],
                    id: '/disk/IMG_1528.HEIC'
                }
            });

            expect(newState.albumsByIds[ALBUM_ID].clusters[0].items[0]).toEqual({
                itemId: '5de665f7e4d70e06400bc88c',
                id: '/disk/IMG_1528.HEIC',
                orderIndex: 1,
                width: 3024,
                height: 4032,
                beauty: -1.69977
            });
        });

        it('Не должен изменить state если восстановился элемент не из альбома', () => {
            const state = prepareState();

            const newState = personalAlbums(state, {
                type: RESTORE_DELETED_ITEM,
                payload: {
                    id: '/photounlim/2020-06-04 13-07-10.JPG'
                }
            });

            expect(newState).toBe(state);
        });
    });
});
