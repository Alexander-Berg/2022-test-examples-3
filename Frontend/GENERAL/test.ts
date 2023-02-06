import assert from 'assert';
import LRU from 'lru-cache';

import HttpGeobase from '.';

const GEOBASE_HOST = process.env.GEOBASE_HOST || 'http://geobase.qloud.yandex.ru';

const defaultCacheParamsGeobase = new HttpGeobase(GEOBASE_HOST, {
    timeout: 500,
});

const ownCacheTimeoutGeobase = new HttpGeobase(GEOBASE_HOST, {
    cacheTimeout: 500,
    timeout: 500,
});

const ownCacheSizeGeobase = new HttpGeobase(GEOBASE_HOST, {
    cacheSize: 1,
    timeout: 500,
});

const ownCache = new LRU<string, Promise<any>>({
    max: 500,
    maxAge: 1000 * 60 * 60,
});

const ownCacheGeobase = new HttpGeobase(GEOBASE_HOST, {
    cache: ownCache,
});

const ownSmallCache = new LRU<string, Promise<any>>({
    max: 1,
    maxAge: 500,
});

const ownSmallCacheGeobase = new HttpGeobase(GEOBASE_HOST, {
    cache: ownSmallCache,
});

describe('Qloud http geobase caching', () => {
    it('cache timeout', async() => {
        const key = 'http://geobase.qloud.yandex.ru/v1/find_country?id=213';

        await defaultCacheParamsGeobase.findCountry(213);
        await ownCacheTimeoutGeobase.findCountry(213);

        const country = await defaultCacheParamsGeobase.cache.get(key);

        setTimeout(async() => {
            const defaultCacheParamsCountry = await defaultCacheParamsGeobase.cache.get(key);
            const ownCacheTimeoutcountry = await ownCacheTimeoutGeobase.cache.get(key);

            assert.strictEqual(country === defaultCacheParamsCountry, true);
            assert.strictEqual(country === ownCacheTimeoutcountry, false);
        }, 1000);
    });

    it('cache size', async() => {
        const firstKey = 'http://geobase.qloud.yandex.ru/v1/find_country?id=213';
        const secondKey = 'http://geobase.qloud.yandex.ru/v1/find_country?id=146';

        await ownCacheSizeGeobase.findCountry(213);
        await ownCacheSizeGeobase.findCountry(146, { crimeaStatus: 'ua' });

        const firstCacheItem = await ownCacheSizeGeobase.cache.get(firstKey);
        const secondCacheItem = await ownCacheSizeGeobase.cache.get(secondKey);

        assert.strictEqual(typeof firstCacheItem, 'undefined');
        assert.strictEqual(secondCacheItem, 187);
    });

    it('own geobase cache timeout', async() => {
        const key = 'http://geobase.qloud.yandex.ru/v1/find_country?id=213';

        await ownCacheGeobase.findCountry(213);
        await ownSmallCacheGeobase.findCountry(213);

        const country = await ownCacheGeobase.cache.get(key);

        setTimeout(async() => {
            const defaultCacheParamsCountry = await ownCacheGeobase.cache.get(key);
            const ownCacheTimeoutcountry = await ownSmallCacheGeobase.cache.get(key);

            assert.strictEqual(country === defaultCacheParamsCountry, true);
            assert.strictEqual(country === ownCacheTimeoutcountry, false);
        }, 1000);
    });

    it('own geobase cache size', async() => {
        const firstKey = 'http://geobase.qloud.yandex.ru/v1/find_country?id=213';
        const secondKey = 'http://geobase.qloud.yandex.ru/v1/find_country?id=146';

        await ownCacheGeobase.findCountry(213);
        await ownCacheGeobase.findCountry(146, { crimeaStatus: 'ua' });
        await ownSmallCacheGeobase.findCountry(213);
        await ownSmallCacheGeobase.findCountry(146, { crimeaStatus: 'ua' });

        const firstCacheItem = await ownCacheGeobase.cache.get(firstKey);
        const secondCacheItem = await ownCacheGeobase.cache.get(secondKey);
        const firstSmallCacheItem = await ownSmallCacheGeobase.cache.get(firstKey);
        const secondSmallCacheItem = await ownSmallCacheGeobase.cache.get(secondKey);

        assert.strictEqual(firstCacheItem, 225);
        assert.strictEqual(secondCacheItem, 187);
        assert.strictEqual(typeof firstSmallCacheItem, 'undefined');
        assert.strictEqual(secondSmallCacheItem, 187);
    });
});

describe('Qloud http geobase functionality', () => {
    it('asset', async() => {
        const asset = await defaultCacheParamsGeobase.asset('92.36.94.80');
        assert.strictEqual(/AS/.test(asset), true);
    });

    it('calculatePointsDistance', async() => {
        const distance = await defaultCacheParamsGeobase.calculatePointsDistance(65, 36, 55, 37);
        assert.strictEqual(distance, 900562.6489);
    });

    it('chiefRegionId', async() => {
        const id = await defaultCacheParamsGeobase.chiefRegionId(213);
        assert.strictEqual(id, 0);
    });

    it('children', async() => {
        const children = await defaultCacheParamsGeobase.children(213);
        assert.deepStrictEqual(children, [
            216,
            9000,
            9999,
            20279,
            20356,
            20357,
            20358,
            20359,
            20360,
            20361,
            20362,
            20363,
            114619,
            114620,
        ]);
    });

    it('findCountry', async() => {
        const countryRu = await defaultCacheParamsGeobase.findCountry(213);
        assert.strictEqual(countryRu, 225);

        const countryUa = await defaultCacheParamsGeobase.findCountry(146, {
            crimeaStatus: 'ua',
        });
        assert.strictEqual(countryUa, 187);
    });

    it('idIn', async() => {
        const isIn = await defaultCacheParamsGeobase.idIn(213, 225);
        assert.strictEqual(isIn, true);
    });

    it('ipIn', async() => {
        const isIn = await defaultCacheParamsGeobase.ipIn('92.36.94.80', 213);
        assert.strictEqual(isIn, true);
    });

    it('isTor', async() => {
        const isTor = await defaultCacheParamsGeobase.isTor('8.8.8.8');
        assert.strictEqual(isTor, false);
    });

    it('linguisticsForRegion', async() => {
        const linguistics = await defaultCacheParamsGeobase.linguisticsForRegion(213, 'ru');
        assert.deepStrictEqual(linguistics, {
            ablative_case: '',
            accusative_case: 'Москву',
            dative_case: 'Москве',
            directional_case: '',
            genitive_case: 'Москвы',
            instrumental_case: 'Москвой',
            locative_case: '',
            nominative_case: 'Москва',
            preposition: 'в',
            prepositional_case: 'Москве',
        });
    });

    it('parentId', async() => {
        const parent = await defaultCacheParamsGeobase.parentId(213);
        assert.strictEqual(parent, 1);
    });

    it('parents', async() => {
        const parents = await defaultCacheParamsGeobase.parents(213);
        assert.deepStrictEqual(parents, [213, 1, 3, 225, 10001, 10000]);
    });

    it('pinpointGeolocation', async() => {
        const geolocation = await defaultCacheParamsGeobase.pinpointGeolocation({ ip: '92.36.94.80' });

        assert.deepStrictEqual(geolocation, {
            region_id: 213,
            region_id_by_ip: 213,
            precision_by_ip: 2,
            suspected_region_id: -1,
            precision: 2,
            point_id: -1,
            should_update_cookie: false,
            gid_is_trusted: false,
            region_id_by_gp: -1,
            location: {
                lat: 55.753215,
                lon: 37.622504,
            },
        });
    });

    it('regionsByType', async() => {
        const regions = await defaultCacheParamsGeobase.regionsByType(1);
        assert.strictEqual(regions.length, 8);
    });

    it('regionById', async() => {
        const region = await defaultCacheParamsGeobase.regionById(51);
        assert.deepStrictEqual(region, {
            id: 51,
            type: 6,
            parent_id: 120861,
            geo_parent_id: 0,
            capital_id: 0,
            name: 'Самара',
            native_name: '',
            iso_name: 'RU KUF',
            is_main: true,
            en_name: 'Samara',
            short_en_name: 'KUF',
            phone_code: '846',
            zip_code: '',
            position: 0,
            population: 1163399,
            synonyms: '',
            latitude: 53.195538,
            longitude: 50.101783,
            latitude_size: 0.459122,
            latitiude_size: 0.459122,
            longitude_size: 0.659112,
            zoom: 12,
            tzname: 'Europe/Samara',
            official_languages: 'ru',
            widespread_languages: 'ba,cv,ru,tt',
            services: ['bs', 'yaca', 'weather', 'afisha', 'maps', 'tv', 'ad', 'etrain', 'delivery', 'route'],
        });
    });

    it('regionByIp', async() => {
        const region = await defaultCacheParamsGeobase.regionByIp('85.26.235.232');
        assert.deepStrictEqual(region, {
            id: 40,
            type: 4,
            parent_id: 225,
            geo_parent_id: 0,
            capital_id: 47,
            name: 'Приволжский федеральный округ',
            native_name: '',
            iso_name: '',
            is_main: false,
            en_name: 'Volga Federal District',
            short_en_name: '',
            phone_code: '',
            zip_code: '',
            position: 0,
            population: 29542696,
            synonyms: 'поволжье, приволжский округ, приволжский фо, приволжский регион, пфо, Privolzhsky Federal District',
            latitude: 55.485362,
            longitude: 51.524283,
            latitude_size: 11.861451,
            latitiude_size: 11.861451,
            longitude_size: 19.914168,
            zoom: 13,
            tzname: '',
            official_languages: 'ru',
            widespread_languages: 'ba,cv,ru,tt',
            services: ['bs', 'yaca', 'tv', 'ad'],
        });

        const ipv6region = await defaultCacheParamsGeobase.regionByIp('2a02:6b8:0:2309:854c:9381:3bd3:6c24');
        ipv6region ? assert.strictEqual(ipv6region.id, 2) : assert.strictEqual(undefined, 2);
    });

    it('regionId', async() => {
        const region = await defaultCacheParamsGeobase.regionId('92.36.94.80');
        assert.strictEqual(region, 213);
    });

    it('regionIdByLocation', async() => {
        const region = await defaultCacheParamsGeobase.regionIdByLocation(55.733684, 37.588496);
        assert.strictEqual(region, 120542);
    });

    it('reliabilitiesByIp', async() => {
        const region = await defaultCacheParamsGeobase.reliabilitiesByIp('92.242.58.13');

        assert.strictEqual(region[0].region_id, 213);
        assert.strictEqual(region[0].value, 0);
    });

    it('supportedLinguistics', async() => {
        const linguistics = await defaultCacheParamsGeobase.supportedLinguistics();
        assert.deepStrictEqual(linguistics, ['be', 'en', 'kk', 'ru', 'tr', 'tt', 'uk', 'uz', 'lt', 'lv', 'fi', 'pl', 'et'],
        );
    });

    it('subtree', async() => {
        const subtree = await defaultCacheParamsGeobase.subtree(213);
        assert.strictEqual(subtree.length > 0, true);
    });

    it('timezone', async() => {
        const tz = await defaultCacheParamsGeobase.timezone(213);
        assert.strictEqual(tz, 'Europe/Moscow');
    });

    it('tzinfo', async() => {
        const tzinfo = await defaultCacheParamsGeobase.tzinfo('Europe/Moscow');
        assert.strictEqual(tzinfo.abbr, 'MSK');
    });

    it('traitsByIp', async() => {
        const traits = await defaultCacheParamsGeobase.traitsByIp('141.8.146.103');

        const expected = {
            region_id: 213,
            is_stub: false,
            is_reserved: false,
            is_yandex_net: true,
            is_yandex_staff: false,
            is_yandex_turbo: false,
            is_tor: false,
            is_proxy: false,
            is_vpn: false,
            is_hosting: false,
            isp_name: 'yandex llc',
            org_name: 'yandex llc',
            asn_list: '13238',
        };

        assert.deepStrictEqual(traits, expected);
    });

    it('returns HttpGeobaseError', async() => {
        try {
            await defaultCacheParamsGeobase.regionById(0);
        } catch (err) {
            assert.strictEqual(err.message.indexOf('(TFromStringException)') !== -1, true);
            assert.strictEqual(err.statusCode, 400);
        }
    });

    it('cache works', () => {
        const promise = defaultCacheParamsGeobase.regionById(213);
        const promise2 = defaultCacheParamsGeobase.regionById(213);
        assert.deepStrictEqual(promise, promise2);
    });

    it('error is not cacheable', async() => {
        const promise = defaultCacheParamsGeobase.regionById(0);
        try {
            await promise;
        } catch (err) {
            // Ignore error
        }

        const promise2 = defaultCacheParamsGeobase.regionById(0).catch();
        assert.notStrictEqual(promise, promise2);
    });
});
