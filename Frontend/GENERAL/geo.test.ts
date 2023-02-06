import { yandex } from '@yandex-int/maps-proto-schemas/types';

import { deepFreeze, mockNetwork } from '../utils/tests';
import { getCityId, getRegionName, generateGeoAddress, buildCoordinates } from './geo';

describe('Lib. geo', () => {
    const networkMock = mockNetwork();

    describe('getCityId', () => {
        beforeEach(() => {
            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=1')
                .reply(200, { id: 1, type: 6, name: 'City' })
                .get('/v1/region_by_id?id=2')
                .reply(200, { id: 2, type: 7, name: 'Village' })
                .get('/v1/region_by_id?id=3')
                .reply(200, { id: 3, type: 15, name: 'Village locality' })
                .get('/v1/region_by_id?id=4')
                .reply(200, { id: 4, type: 8, name: 'First district' })
                .get('/v1/region_by_id?id=5')
                .reply(200, { id: 5, type: 8, name: 'Second district' })
                .get('/v1/region_by_id?id=6')
                .reply(200, { id: 6, type: 8, name: 'Third district' })
                .get('/v1/region_by_id?id=7')
                .reply(200, { id: 7, type: 3, name: 'Country' })
                .get('/v1/region_by_id?id=8')
                .reply(200, { id: 8, type: 1, name: 'Continent' })
                .get('/v1/parents?id=1')
                .reply(200, [])
                .get('/v1/parents?id=2')
                .reply(200, [])
                .get('/v1/parents?id=3')
                .reply(200, [])
                .get('/v1/parents?id=4')
                .reply(200, [1])
                .get('/v1/parents?id=5')
                .reply(200, [2])
                .get('/v1/parents?id=6')
                .reply(200, [3])
                .get('/v1/parents?id=7')
                .reply(200, [8])
                .get('/v1/parents?id=8')
                .reply(200, []);
        });

        test('должен возвращать не пустой geoId для города', async() => {
            await expect(getCityId(1)).resolves.toBe(1);
            await expect(getCityId(2)).resolves.toBe(2);
            await expect(getCityId(3)).resolves.toBe(3);
        });

        test('должен возвращать не пустой geoId для региона внутри города', async() => {
            await expect(getCityId(4)).resolves.toBe(1);
            await expect(getCityId(5)).resolves.toBe(2);
            await expect(getCityId(6)).resolves.toBe(3);
        });

        test('должен возвращать пустой ответ, если не удалось определить город', async() => {
            await expect(getCityId(7)).resolves.toBeUndefined();
            await expect(getCityId(8)).resolves.toBeUndefined();
        });
    });

    describe('getRegionName', () => {
        beforeEach(() => {
            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=21')
                .reply(200, { id: 21, type: 4, name: 'Federal district' })
                .get('/v1/region_by_id?id=22')
                .reply(200, { id: 22, type: 5, name: 'Federal subject' })
                .get('/v1/region_by_id?id=23')
                .reply(200, { id: 23, type: 6, name: 'First city' })
                .get('/v1/region_by_id?id=24')
                .reply(200, { id: 24, type: 6, name: 'Second city' })
                .get('/v1/region_by_id?id=25')
                .reply(200, { id: 25, type: 3, name: 'Country' })
                .get('/v1/region_by_id?id=26')
                .reply(200, { id: 26, type: 1, name: 'Continent' })
                .get('/v1/parents?id=21')
                .reply(200, [])
                .get('/v1/parents?id=22')
                .reply(200, [])
                .get('/v1/parents?id=23')
                .reply(200, [21, 22])
                .get('/v1/parents?id=24')
                .reply(200, [22, 21])
                .get('/v1/parents?id=25')
                .reply(200, [26])
                .get('/v1/parents?id=26')
                .reply(200, []);
        });

        test('должен возвращать правильное имя региона', async() => {
            await expect(getRegionName(21)).resolves.toBe('Federal district');
            await expect(getRegionName(22)).resolves.toBe('Federal subject');
        });

        test('должен возвращать правильное имя региона, cклеивая его из родительских регионов', async() => {
            await expect(getRegionName(23)).resolves.toBe('Federal district, Federal subject');
            await expect(getRegionName(24)).resolves.toBe('Federal subject, Federal district');
        });

        test('должен возвращать пустой ответ, если не удалось определить регион', async() => {
            await expect(getRegionName(25)).resolves.toBeUndefined();
            await expect(getRegionName(26)).resolves.toBeUndefined();
        });
    });

    describe('generateGeoAddress', () => {
        const buildGeocoderResponse = (address: yandex.maps.proto.search.address.IAddress) => {
            return deepFreeze({
                reply: {
                    geoObject: [
                        {
                            metadata: [
                                {
                                    '.yandex.maps.proto.search.geocoder.GEO_OBJECT_METADATA': {
                                        address: address,
                                        '.yandex.maps.proto.search.geocoder_internal.TOPONYM_INFO': {
                                            geoid: 31,
                                            point: { lon: 50, lat: 60 },
                                        },
                                    },
                                    __dummy: {},
                                },
                            ],
                        },
                    ],
                },
            });
        };

        beforeEach(() => {
            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=31')
                .reply(200, { id: 31, type: 6, name: 'Locality' })
                .get('/v1/parents?id=31')
                .reply(200, []);
        });

        test('должен возвращать правильный geoAddress', async() => {
            const geocoderResponse = buildGeocoderResponse({
                component: [
                    { name: 'Country', kind: [0] },
                    { name: 'Province', kind: [2] },
                    { name: 'Locality', kind: [4] },
                    { name: 'District', kind: [5] },
                    { name: 'Street', kind: [6] },
                    { name: 'House', kind: [7] },
                    { name: 'Entrance', kind: [16] },
                ],
                formattedAddress: 'Formatted address',
            });

            await expect(generateGeoAddress(geocoderResponse)).resolves.toEqual({
                country: 'Country',
                region: 'Province',
                city: 'Locality',
                cityId: 31,
                street: 'Street',
                house: 'House',
                porch: 'Entrance',
                location: [50, 60],
            });
        });

        test('должен возвращать район вместо улицы, если не удалось определить её', async() => {
            const geocoderResponse = buildGeocoderResponse({
                component: [
                    { name: 'Country', kind: [0] },
                    { name: 'Province', kind: [2] },
                    { name: 'Locality', kind: [4] },
                    { name: 'District', kind: [5] },
                    { name: 'House', kind: [7] },
                    { name: 'Entrance', kind: [16] },
                ],
                formattedAddress: 'Formatted address',
            });

            await expect(generateGeoAddress(geocoderResponse)).resolves.toEqual({
                country: 'Country',
                region: 'Province',
                city: 'Locality',
                cityId: 31,
                street: 'District',
                house: 'House',
                porch: 'Entrance',
                location: [50, 60],
            });
        });

        test('должен правильно склеивать регионы если их несколько', async() => {
            const geocoderResponse = buildGeocoderResponse({
                component: [
                    { name: 'Country', kind: [0] },
                    { name: 'Outer province', kind: [2] },
                    { name: 'Inner province', kind: [2] },
                    { name: 'Locality', kind: [4] },
                    { name: 'District', kind: [5] },
                    { name: 'Street', kind: [6] },
                    { name: 'House', kind: [7] },
                    { name: 'Entrance', kind: [16] },
                ],
                formattedAddress: 'Formatted address',
            });

            await expect(generateGeoAddress(geocoderResponse)).resolves.toEqual({
                country: 'Country',
                region: 'Inner province, Outer province',
                city: 'Locality',
                cityId: 31,
                street: 'Street',
                house: 'House',
                porch: 'Entrance',
                location: [50, 60],
            });
        });

        test('должен возвращать внутренний регион вместо города, если не удалось определить его', async() => {
            const geocoderResponse = buildGeocoderResponse({
                component: [
                    { name: 'Country', kind: [0] },
                    { name: 'Outer province', kind: [2] },
                    { name: 'Inner province', kind: [2] },
                    { name: 'District', kind: [5] },
                    { name: 'Street', kind: [6] },
                    { name: 'House', kind: [7] },
                    { name: 'Entrance', kind: [16] },
                ],
                formattedAddress: 'Formatted address',
            });

            await expect(generateGeoAddress(geocoderResponse)).resolves.toEqual({
                country: 'Country',
                region: 'Inner province, Outer province',
                city: 'Inner province',
                cityId: 31,
                street: 'Street',
                house: 'House',
                porch: 'Entrance',
                location: [50, 60],
            });
        });
    });

    describe('buildCoordinates', () => {
        test('должен возвращать правильное строковое представление координат', () => {
            expect(buildCoordinates({ latitude: 0, longitude: 6 })).toBe('6, 0');
            expect(buildCoordinates({ latitude: 5, longitude: 0 })).toBe('0, 5');
            expect(buildCoordinates({ latitude: 50, longitude: 60 })).toBe('60, 50');
            expect(buildCoordinates({ latitude: 50.5, longitude: 60.6 })).toBe('60.6, 50.5');
        });
    });
});
