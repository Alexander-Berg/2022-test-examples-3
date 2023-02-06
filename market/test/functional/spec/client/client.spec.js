/* eslint-disable max-len */

'use strict';

const Ajv = require('ajv');

const Client = require('./../../lib/client');
const CLIENT_EVENT_ROUTE = require('../../lib/routes').CLIENT_EVENT;
const common = require('./../../data/common/index');

const ajv = new Ajv();

describe('Client event middleware', () => {
    test('should return { ok: true }', async () => {
        const client = new Client();
        const result = await client.request(CLIENT_EVENT_ROUTE);

        const isOkTrue = ajv.validate(common.OK_TRUE, result.response);
        expect(isOkTrue).toBeTruthy();
    });

    describe('static method _queryToObject', () => {
        test('should return correct result (1)', () => {
            const query =
                'v=201701181205&transaction_id=iy2q801vntybbc52n8bt5n0pxtghcyye&is_shop=true&settings=%7B"applicationName"%3A"Яндекс.Советник"%2C"affId"%3A"1400"%2C"clid"%3A2210590%2C"sovetnikExtension"%3Atrue%2C"withButton"%3Atrue%2C"extensionStorage"%3Atrue%2C"notificationStatus"%3A"yandex"%2C"notificationPermissionGranted"%3Atrue%7D&adult=true&screen_size=%7B"width"%3A1156%2C"height"%3A653%7D&screen_resolution=%7B"ratio"%3A2%2C"width"%3A1280%2C"height"%3A800%7D&viewport=%7B"width"%3A1156%2C"height"%3A3248%7D&notifications=yandex&is_debug_mode=true&url=http%3A%2F%2Fwww.holodilnik.ru%2Frefrigerator%2Fone_chamber_refrigerators%2Fsaratov%2F452_ksh_120%2F&name_by_sl=Однокамерный%20холодильник%20Саратов%20452%20(КШ-120)&price_by_sl=11205&price_by_md=11205&pictures_by_md=http%3A%2F%2Fwww.holodilnik.ru%2F%2Fholod.ru%2Fpics%2Fwatermark%2Fmedium%2F28%2F3228_0.jpg%2Chttp%3A%2F%2Fwww.holodilnik.ru%2F%2Fholod.ru%2Fpics%2Fwatermark%2Fmedium%2F28%2F3228_1.jpg&url_by_sh=http%3A%2F%2Fwww.holodilnik.ru%2Frefrigerator%2Fone_chamber_refrigerators%2Fsaratov%2F452_ksh_120%2F&h1_by_df=Однокамерный%20холодильник%20Саратов%20452%20(КШ-120)&ymclid=806565936984426184100002';

            const expected = {
                v: 201701181205,
                transaction_id: 'iy2q801vntybbc52n8bt5n0pxtghcyye',
                is_shop: true,
                settings: {
                    applicationName: 'Яндекс.Советник',
                    affId: '1400',
                    clid: 2210590,
                    sovetnikExtension: true,
                    withButton: true,
                    extensionStorage: true,
                    notificationStatus: 'yandex',
                    notificationPermissionGranted: true,
                },
                adult: true,
                screen_size: {
                    width: 1156,
                    height: 653,
                },
                screen_resolution: {
                    ratio: 2,
                    width: 1280,
                    height: 800,
                },
                viewport: {
                    width: 1156,
                    height: 3248,
                },
                notifications: 'yandex',
                is_debug_mode: true,
                url: 'http://www.holodilnik.ru/refrigerator/one_chamber_refrigerators/saratov/452_ksh_120/',
                name_by_sl: 'Однокамерный холодильник Саратов 452 (КШ-120)',
                price_by_sl: 11205,
                price_by_md: 11205,
                pictures_by_md:
                    'http://www.holodilnik.ru//holod.ru/pics/watermark/medium/28/3228_0.jpg,http://www.holodilnik.ru//holod.ru/pics/watermark/medium/28/3228_1.jpg',
                url_by_sh: 'http://www.holodilnik.ru/refrigerator/one_chamber_refrigerators/saratov/452_ksh_120/',
                h1_by_df: 'Однокамерный холодильник Саратов 452 (КШ-120)',
                ymclid: '806565936984426184100002',
            };

            const actual = Client._queryToObject(query);

            expect(actual).toEqual(expected);
        });

        test('should return correct result (2)', () => {
            const query = {
                v: 201612021540,
                transaction_id: 'iwb76q0g3uejyqpcwvvm58kd1gblk17y',
                settings: {
                    applicationName: 'Яндекс.Советник',
                    affId: '1400',
                    clid: 2210590,
                    sovetnikExtension: true,
                    withButton: true,
                    extensionStorage: true,
                    notificationStatus: 'yandex',
                    notificationPermissionGranted: true,
                },
                referrer:
                    'http://yabs.yandex.ru/count/OUKkrKFmUwm40000gP800Ow-DKc41OMV4ogL0Pi2RaEt0II8l3uC4GM9z4wPHXmGlWa7c8mddgjdtG6ThUjU29glXq5Ifb2AlVuEWGhSi81QkWkzi2Jjs0UgBgMlMcKBlAFbHGUD0Tq1tf0az96zk27S0PVmEC4JzXbD-GsJXGsP1KACcVOajf2c3hMGfWwWcmnrhvds9BEGOowqaAOEsPqrUzgGr32Kd5aRfv4e4QYmG5bp1wIm00003Qx-nG2FTBNjI0Qn0xAq40020Rcjwru8k-HB8EA_0Tq5mV__________3yBmCjZos9rWGGp5Zm_J0ku1s_I1Ag5uuhTb0T-53V8HxOWqV1y0?q=%D0%BC%D0%B8%D0%BA%D1%80%D0%BE%D1%84%D0%BE%D0%BD',
                adult: true,
                screen: '2560x1600',
                screen_size: {
                    width: 2560,
                    height: 1600,
                },
                notifications: 'yandex',
                ymclid: '806565936984426184100002',
                url: 'http://www.mvideo.ru/prigotovlenie-kofe/kofevarki-157',
                pictures_by_sl:
                    'https://ae01.alicdn.com/kf/HTB1klo6KXXXXXcAXpXXq6xXFXXX7/Пожизненная-гарантия-для-Elpida-DDR3-2-ГБ-4-ГБ-1066-мГц-PC3-8500S-оригинальной-аутентичной-ddr.jpg_220x220.jpg',
                price_by_df: 9891,
                title_by_mp: 'Оптовая микрофоны Галерея - Купить по низким ценам микрофоны Лоты на Aliexpress.com',
                h1_by_mp: 'AliExpress',
                meta_description_by_mp:
                    'Опт микрофоны из дешевых микрофоны лотов, купить у надежных микрофоны оптовиков.',
                meta_keywords_by_mp: 'Оптмикрофоны, Дешевые микрофоны лоты, микрофоны оптовики',
            };

            const expected = {
                v: 201612021540,
                transaction_id: 'iwb76q0g3uejyqpcwvvm58kd1gblk17y',
                settings: {
                    applicationName: 'Яндекс.Советник',
                    affId: '1400',
                    clid: 2210590,
                    sovetnikExtension: true,
                    withButton: true,
                    extensionStorage: true,
                    notificationStatus: 'yandex',
                    notificationPermissionGranted: true,
                },
                referrer:
                    'http://yabs.yandex.ru/count/OUKkrKFmUwm40000gP800Ow-DKc41OMV4ogL0Pi2RaEt0II8l3uC4GM9z4wPHXmGlWa7c8mddgjdtG6ThUjU29glXq5Ifb2AlVuEWGhSi81QkWkzi2Jjs0UgBgMlMcKBlAFbHGUD0Tq1tf0az96zk27S0PVmEC4JzXbD-GsJXGsP1KACcVOajf2c3hMGfWwWcmnrhvds9BEGOowqaAOEsPqrUzgGr32Kd5aRfv4e4QYmG5bp1wIm00003Qx-nG2FTBNjI0Qn0xAq40020Rcjwru8k-HB8EA_0Tq5mV__________3yBmCjZos9rWGGp5Zm_J0ku1s_I1Ag5uuhTb0T-53V8HxOWqV1y0?q=%D0%BC%D0%B8%D0%BA%D1%80%D0%BE%D1%84%D0%BE%D0%BD',
                adult: true,
                screen: '2560x1600',
                screen_size: {
                    width: 2560,
                    height: 1600,
                },
                notifications: 'yandex',
                ymclid: '806565936984426184100002',
                url: 'http://www.mvideo.ru/prigotovlenie-kofe/kofevarki-157',
                pictures_by_sl:
                    'https://ae01.alicdn.com/kf/HTB1klo6KXXXXXcAXpXXq6xXFXXX7/Пожизненная-гарантия-для-Elpida-DDR3-2-ГБ-4-ГБ-1066-мГц-PC3-8500S-оригинальной-аутентичной-ddr.jpg_220x220.jpg',
                price_by_df: 9891,
                title_by_mp: 'Оптовая микрофоны Галерея - Купить по низким ценам микрофоны Лоты на Aliexpress.com',
                h1_by_mp: 'AliExpress',
                meta_description_by_mp:
                    'Опт микрофоны из дешевых микрофоны лотов, купить у надежных микрофоны оптовиков.',
                meta_keywords_by_mp: 'Оптмикрофоны, Дешевые микрофоны лоты, микрофоны оптовики',
            };

            const actual = Client._queryToObject(query);

            expect(actual).toEqual(expected);
        });
    });
});
