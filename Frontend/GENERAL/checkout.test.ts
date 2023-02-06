import request from 'supertest';
import nock from 'nock';

import app from '../app';
import { logger } from '../lib/logger';
import { deepFreeze, mockNetwork } from '../utils/tests';

describe('Router. Checkout', () => {
    const networkMock = mockNetwork();

    const commonGeocoderQuery = deepFreeze({
        results: 1,
        ms: 'pb',
        type: 'geo',
        lang: 'ru_RU',
        origin: 'tap-backend',
    });

    beforeEach(() => {
        networkMock.mockTvm();
    });

    afterEach(() => {
        expect(networkMock.getLog()).toMatchSnapshot();
    });

    describe('/geocode', () => {
        const textQuery = deepFreeze({
            ...commonGeocoderQuery,
            text: 'Россия, Свердловская область, Екатеринбург',
        });
        const llQuery = deepFreeze({
            ...commonGeocoderQuery,
            mode: 'reverse',
            ll: '60.59746547464089, 56.83801147750733',
        });

        const geocoderFromTextData = Buffer.from(
            '0a0012bd0c0ab504f201b1040a7f0a4fd0a0d0bed181d181d0b8d18f2c20d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b3100118002a280a1209279f1edb3286424011a18ab88cddb74b40121209572250fd831643401177b64fe000064c4018332a280a1209c631923d42214e4011114a6779014c4c4012120910070951be784e40113af238f3c87d4c403a7352710a530a4fd180d0bed181d181d0b8d18f2c20d181d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d0b5d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b318011033a2061709000000000000f03f10011a0a32302e31312e31382d30a2068b0208011288015a41414141416741454141614b416f5343514141414141416b485a4145514141414141416f475a414568494a4141414141414141384c38524141414141414141384c38694151416f4145442b2f2f2f2f2f2f2f2f2f2f3842534146564141434176316a2f2f2f2f2f2f2f2f2f2f2f384261674a79645841416e51484e7a4577396f414541714145411a3d313630353738383532313238393836342d313632383233343538322d736173312d343439382d7361732d61646472732d6e6d6574612d73312d38303331223d313630353738383532313238393836342d313632383233343538322d736173312d343439382d7361732d61646472732d6e6d6574612d73312d383033313282080a8d035a8a0312c7020a4fd0a0d0bed181d181d0b8d18f2c20d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b31a02525522100a0cd0a0d0bed181d181d0b8d18f100022380a34d0a3d180d0b0d0bbd18cd181d0bad0b8d0b920d184d0b5d0b4d0b5d180d0b0d0bbd18cd0bdd18bd0b920d0bed0bad180d183d0b31002222b0a27d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c100222590a55d0bcd183d0bdd0b8d186d0b8d0bfd0b0d0bbd18cd0bdd0bed0b520d0bed0b1d180d0b0d0b7d0bed0b2d0b0d0bdd0b8d0b520d093d0bed180d0bed0b420d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b31003221c0a18d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b3100422083533313636353337a20633083618052212092f09a9bf794c4e401187c8c8f5436b4c404202080042020802420208044a0d79656b61746572696e627572670ab002ca07ac020aa9020aa602796d617073626d313a2f2f67656f3f6c6c3d36302e35393725324335362e3833382673706e3d302e363833253243302e33383926746578743d2544302541302544302542452544312538312544312538312544302542382544312538462532432532302544302541312544302542322544302542352544312538302544302542342544302542422544302542452544302542322544312538312544302542412544302542302544312538462532302544302542452544302542312544302542422544302542302544312538312544312538322544312538432532432532302544302539352544302542412544302542302544312538322544302542352544312538302544302542382544302542442544302542312544312538332544312538302544302542330aab01fa01a7010aa40164486c775a54316e5a57396a6232526c636a74685a4752795a584e7a50644367304c375267644742304c6a526a797767304b48517374433130594451744e4337304c375173744742304c7251734e4750494e432b304c485175394377305948526774474d4c4344516c644336304c44526774433130594451754e4339304c485267394741304c4d764e6a41754e546b334e4459314c4455324c6a677a4f4441784d5338781218d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b31a35d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d0a0d0bed181d181d0b8d18f22280a1209c631923d42214e4011a0c37c79014c4c4012120910070951be784e4011ab7823f3c87d4c402a140a120998c0adbb794c4e40111958c7f1436b4c40',
            'hex',
        );
        const geocoderFromLlData = Buffer.from(
            '0a0012f40d0a9404f20190040a540a2436302e35393734363534373436343038392c2035362e3833383031313437373530373333100118002a280a1209279f1edb3286424011a18ab88cddb74b40121209572250fd831643401177b64fe000064c4018082a280a12099e78ce16104c4e4011062e8f35236b4c401212096d567daeb64c4e401185b01a4b586b4c403a87015284010a520a2436302e35393734363534373436343038392c2035362e383338303131343737353037333312280a12095e9d108638474e401170499ddb63684c40121209017541f9ba514e401146bd7f9e236e4c40180110081a12092f09a9bf794c4e401187c8c8f5436b4c40a2061709000000000000f03f10021a0a32302e31312e31382d30a20680020801127c5a41414141416741454141614b416f53436451526e3739355445354145654d697a765644613078414568494a4e733475426b5946745438524971363239444c2f706a38694151416f414544596e415a49415655414149432f57502f2f2f2f2f2f2f2f2f2f2f774671414841416e514541414141416f414541714145411a3e313630353739343535323531343437352d313333393631303834342d6d616e322d303433312d6d616e2d61646472732d6e6d6574612d73312d3135323430223e313630353739343535323531343437352d313333393631303834342d6d616e322d303433312d6d616e2d61646472732d6e6d6574612d73312d313532343032da090acd035aca031287030a6dd0a0d0bed181d181d0b8d18f2c20d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b32c20d0bfd0bbd0bed189d0b0d0b4d18c203139303520d0b3d0bed0b4d0b01a02525522100a0cd0a0d0bed181d181d0b8d18f100022380a34d0a3d180d0b0d0bbd18cd181d0bad0b8d0b920d184d0b5d0b4d0b5d180d0b0d0bbd18cd0bdd18bd0b920d0bed0bad180d183d0b31002222b0a27d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c100222590a55d0bcd183d0bdd0b8d186d0b8d0bfd0b0d0bbd18cd0bdd0bed0b520d0bed0b1d180d0b0d0b7d0bed0b2d0b0d0bdd0b8d0b520d093d0bed180d0bed0b420d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b31003221c0a18d095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b3100422200a1cd0bfd0bbd0bed189d0b0d0b4d18c203139303520d0b3d0bed0b4d0b01006220a31353230363335383634a20631083618002212092f09a9bf794c4e401187c8c8f5436b4c40420208064a13706c6f7368636861645f313930355f676f64610a8203ca07fe020afb020af802796d617073626d313a2f2f67656f3f6c6c3d36302e35393725324335362e3833382673706e3d302e303035253243302e30303226746578743d254430254130254430254245254431253831254431253831254430254238254431253846253243253230254430254131254430254232254430254235254431253830254430254234254430254242254430254245254430254232254431253831254430254241254430254230254431253846253230254430254245254430254231254430254242254430254230254431253831254431253832254431253843253243253230254430253935254430254241254430254230254431253832254430254235254431253830254430254238254430254244254430254231254431253833254431253830254430254233253243253230254430254246254430254242254430254245254431253839254430254230254430254234254431253843253230313930352532302544302542332544302542452544302542342544302542300ad301fa01cf010acc0164486c775a54316e5a57396a6232526c636a74685a4752795a584e7a50644367304c375267644742304c6a526a797767304b48517374433130594451744e4337304c375173744742304c7251734e4750494e432b304c485175394377305948526774474d4c4344516c644336304c44526774433130594451754e4339304c485267394741304c4d73494e432f304c76517674474a304c4451744e474d494445354d445567304c505176744330304c41764e6a41754e546b324f5463784c4455324c6a677a4e7a67774e533878121cd0bfd0bbd0bed189d0b0d0b4d18c203139303520d0b3d0bed0b4d0b01a4fd095d0bad0b0d182d0b5d180d0b8d0bdd0b1d183d180d0b32c20d0a1d0b2d0b5d180d0b4d0bbd0bed0b2d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c2c20d0a0d0bed181d181d0b8d18f22280a12099e78ce16104c4e4011062e8f35236b4c401212096d567daeb64c4e401185b01a4b586b4c402a140a120989d4b48b694c4e401170b6b9313d6b4c40',
            'hex',
        );
        const regionData = deepFreeze({ id: 54, type: 6 });

        describe('Успешный запрос', () => {
            test('С query параметром text', async() => {
                networkMock
                    .mockGeocoder()
                    .query(textQuery)
                    .reply(200, geocoderFromTextData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 54 })
                    .reply(200, regionData)
                    .get('/v1/parents')
                    .query({ id: 54 })
                    .reply(200, [54]);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(200, {
                        country: 'Россия',
                        city: 'Екатеринбург',
                        region: 'Свердловская область, Уральский федеральный округ',
                        location: [60.59746547464089, 56.83801147750733],
                        cityId: 54,
                    });
            });

            test('С query параметром ll', async() => {
                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderFromLlData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 54 })
                    .reply(200, regionData)
                    .get('/v1/parents')
                    .query({ id: 54 })
                    .reply(200, [54]);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(200, {
                        country: 'Россия',
                        city: 'Екатеринбург',
                        street: 'площадь 1905 года',
                        region: 'Свердловская область, Уральский федеральный округ',
                        location: [60.59746547464089, 56.83801147750733],
                        cityId: 54,
                    });
            });
        });

        describe('Должен вернуть 400 при неверных параметрах', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                spyLogger = jest.spyOn(logger, 'warn');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(1);
            });

            test('Неверный заголовок Accept-Language', async() => {
                await request(app)
                    .get('/v1/checkout/geocode')
                    .set('X-Request-Id', 'req-id')
                    // Кривой заголовок Accept-Language
                    .set('Accept-Language', 'ab_CD')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'BadRequestError',
                        status: 400,
                        message: 'Wrong Accept-Language header. Available values: az_AZ, hy_AM, be_BY, kk_KZ, ky_KG, mo_MD, ru_RU, tg_TJ, uz_UZ, ka_GE, uk_UA, en_US',
                    });
            });

            test('Отсутствие ll или text в query параметрах', async() => {
                await request(app)
                    .get('/v1/checkout/geocode')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'BadRequestError',
                        status: 400,
                        message: 'Wrong query params: ll or text',
                    });
            });
        });

        describe('Должен падать при 500 от geocoder', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                spyLogger = jest.spyOn(logger, 'error');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(2);
            });

            test('С query параметром text', async() => {
                networkMock
                    .mockGeocoder()
                    .query(textQuery)
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HTTPError',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });

            test('С query параметром ll', async() => {
                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HTTPError',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });
        });

        describe('Должен вернуть 404 при 404 от geocoder', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                spyLogger = jest.spyOn(logger, 'warn');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(2);
            });

            test('С query параметром text', async() => {
                networkMock
                    .mockGeocoder()
                    .query(textQuery)
                    .reply(404);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(404, {
                        name: 'HTTPError',
                        status: 404,
                        message: 'Response code 404 (Not Found)',
                    });
            });

            test('С query параметром ll', async() => {
                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(404);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(404, {
                        name: 'HTTPError',
                        status: 404,
                        message: 'Response code 404 (Not Found)',
                    });
            });
        });

        describe('Должен вернуть 400 при 400 от geocoder', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                spyLogger = jest.spyOn(logger, 'warn');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(2);
            });

            test('С query параметром text', async() => {
                networkMock
                    .mockGeocoder()
                    .query(textQuery)
                    .reply(400);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'HTTPError',
                        status: 400,
                        message: 'Response code 400 (Bad Request)',
                    });
            });

            test('С query параметром ll', async() => {
                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(400);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'HTTPError',
                        status: 400,
                        message: 'Response code 400 (Bad Request)',
                    });
            });
        });

        describe('Должен падать при 500 от geobase', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                spyLogger = jest.spyOn(logger, 'error');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(1);
            });

            test('С query параметром text', async() => {
                networkMock
                    .mockGeocoder()
                    .query(textQuery)
                    .reply(200, geocoderFromTextData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 54 })
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HttpGeobase',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });

            test('С query параметром ll', async() => {
                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderFromLlData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 54 })
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HttpGeobase',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });
        });

        describe('Должен падать при отсутствии тикета TVM для geocoder', () => {
            let spyLogger: jest.SpyInstance;

            beforeEach(() => {
                nock.cleanAll();

                networkMock.mockTvm({ geocoderErr: true });

                spyLogger = jest.spyOn(logger, 'error');
            });

            afterEach(() => {
                expect(spyLogger).toHaveBeenCalledTimes(1);
            });

            test('С query параметром text', async() => {
                const response = await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ text: textQuery.text })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500);

                expect(response.text).toEqual('Internal Server Error');
            });

            test('С query параметром ll', async() => {
                const response = await request(app)
                    .get('/v1/checkout/geocode')
                    .query({ ll: llQuery.ll })
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500);

                expect(response.text).toEqual('Internal Server Error');
            });
        });

        test('Должен корректно обработать запрос типа OPTIONS', async() => {
            await request(app)
                .options('/v1/checkout/geocode')
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect('Access-Control-Max-Age', '86400')
                .expect(204);
        });
    });

    describe('/detect-address', () => {
        const commonLaasQuery = {
            service: 'tap-backend',
        };
        const llData = deepFreeze({
            latitude: 56.129057,
            longitude: 40.406635,
        });
        const llQuery = deepFreeze({
            ...commonGeocoderQuery,
            mode: 'reverse',
            ll: `${llData.longitude}, ${llData.latitude}`,
        });

        const geocoderData = Buffer.from(
            '0a0012fe0b0aec03f201e8030a440a1434302e3430363633352c2035362e313239303537100118002a280a1209279f1edb3286424011a18ab88cddb74b40121209572250fd831643401177b64fe000064c40180a2a280a1209cb4dd4d2dc32444011a779c7293a104c40121209431cebe23636444011d76839d043114c403a7652740a420a1434302e3430363633352c2035362e31323930353712280a12097b4a751be42e444011aba13fe5a40d4c40121209ddedd01a35394440119857919464134c401801100a1a120959349d9d0c34444011f22895f084104c40a2061709000000000000f03f10021a0a32302e31312e31392d30a206fa010801127c5a41414141416741454141614b416f5343566b306e5a304d4e4552414566496f6c664345454578414568494a586d423159617168744438524971363239444c2f706a38694151416f414544414155674256514141674c39592f2f2f2f2f2f2f2f2f2f2f2f41576f416341436441514141414143674151436f4151413d1a3b313630353836383434353434343334302d38323039323936332d766c61312d303836362d766c612d61646472732d6e6d6574612d73312d38303331223b313630353836383434353434343334302d38323039323936332d766c61312d303836362d766c612d61646472732d6e6d6574612d73312d38303331328c080a9b035a980312cf020a4ed0a0d0bed181d181d0b8d18f2c20d092d0bbd0b0d0b4d0b8d0bcd0b8d1802c20d091d0bed0bbd18cd188d0b0d18f20d09cd0bed181d0bad0bed0b2d181d0bad0b0d18f20d183d0bbd0b8d186d0b01a02525522100a0cd0a0d0bed181d181d0b8d18f1000223c0a38d0a6d0b5d0bdd182d180d0b0d0bbd18cd0bdd18bd0b920d184d0b5d0b4d0b5d180d0b0d0bbd18cd0bdd18bd0b920d0bed0bad180d183d0b31002222b0a27d092d0bbd0b0d0b4d0b8d0bcd0b8d180d181d0bad0b0d18f20d0bed0b1d0bbd0b0d181d182d18c100222320a2ed0b3d0bed180d0bed0b4d181d0bad0bed0b920d0bed0bad180d183d0b320d092d0bbd0b0d0b4d0b8d0bcd0b8d180100322140a10d092d0bbd0b0d0b4d0b8d0bcd0b8d180100422320a2ed091d0bed0bbd18cd188d0b0d18f20d09cd0bed181d0bad0bed0b2d181d0bad0b0d18f20d183d0bbd0b8d186d0b01006220738303030303233a2063a08c001186d221209b0050ed80f34444011f15ea52f80104c40420208064a1b626f6c73686179615f6d6f736b6f76736b6179615f756c697473610aad02ca07a9020aa6020aa302796d617073626d313a2f2f67656f3f6c6c3d34302e34313125324335362e3133312673706e3d302e303236253243302e30303826746578743d2544302541302544302542452544312538312544312538312544302542382544312538462532432532302544302539322544302542422544302542302544302542342544302542382544302542432544302542382544312538302532432532302544302539312544302542452544302542422544312538432544312538382544302542302544312538462532302544302539432544302542452544312538312544302542412544302542452544302542322544312538312544302542412544302542302544312538462532302544312538332544302542422544302542382544312538362544302542300aab01fa01a7010aa40164486c775a54316e5a57396a6232526c636a74685a4752795a584e7a50644367304c375267644742304c6a526a797767304a4c5175394377304c5451754e4338304c6a5267437767304a48517674433730597a52694e437730593867304a7a5176744742304c725176744379305948517574437730593867305950517539433430596251734338304d4334304d5441314e6a6b734e5459754d544d774e6a41344c7a453d122ed091d0bed0bbd18cd188d0b0d18f20d09cd0bed181d0bad0bed0b2d181d0bad0b0d18f20d183d0bbd0b8d186d0b01a1ed092d0bbd0b0d0b4d0b8d0bcd0b8d1802c20d0a0d0bed181d181d0b8d18f22280a1209cb4dd4d2dc32444011a779c7293a104c40121209431cebe23636444011d76839d043114c402a140a12092fe065868d34444011494c50c3b7104c40',
            'hex',
        );
        const regionData = deepFreeze({ id: 192, type: 6 });
        const detectAddressData = deepFreeze({
            country: 'Россия',
            city: 'Владимир',
            street: 'Большая Московская улица',
            region: 'Владимирская область, Центральный федеральный округ',
            location: [40.40673351940802, 56.12891192984342],
            cityId: 192,
        });

        describe('Успешный запрос', () => {
            test('Получение адреса из latitude и longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, llData);

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 192 })
                    .reply(200, regionData)
                    .get('/v1/parents')
                    .query({ id: 192 })
                    .reply(200, [192]);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(200, detectAddressData);
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, {
                        suspected_latitude: llData.latitude,
                        suspected_longitude: llData.longitude,
                    });

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 192 })
                    .reply(200, regionData)
                    .get('/v1/parents')
                    .query({ id: 192 })
                    .reply(200, [192]);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(200, detectAddressData);
            });
        });

        describe('Должен вернуть 400 или 404 при неверных параметрах', () => {
            test('Неверный заголовок Accept-Language', async() => {
                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Кривой заголовок Accept-Language
                    .set('Accept-Language', 'ab_CD')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'BadRequestError',
                        status: 400,
                        message: 'Wrong Accept-Language header. Available values: az_AZ, hy_AM, be_BY, kk_KZ, ky_KG, mo_MD, ru_RU, tg_TJ, uz_UZ, ka_GE, uk_UA, en_US',
                    });
            });

            test('Отсутствие ll или suspected ll ', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(404, {
                        name: 'NotFoundError',
                        status: 404,
                        message: 'Not Found',
                    });
            });
        });

        describe('Должен падать при 500 от laas', () => {
            test('Получение адреса', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HTTPError',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });
        });

        describe('Должен вернуть 500 при 500 от geocoder', () => {
            test('Получение адреса из latitude и longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, llData);

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HTTPError',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, {
                        suspected_latitude: llData.latitude,
                        suspected_longitude: llData.longitude,
                    });

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HTTPError',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });
        });

        describe('Должен вернуть 404 при 404 от geocoder', () => {
            test('Получение адреса из latitude и longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, llData);

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(404);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(404, {
                        name: 'HTTPError',
                        status: 404,
                        message: 'Response code 404 (Not Found)',
                    });
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, {
                        suspected_latitude: llData.latitude,
                        suspected_longitude: llData.longitude,
                    });

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(404);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(404, {
                        name: 'HTTPError',
                        status: 404,
                        message: 'Response code 404 (Not Found)',
                    });
            });
        });

        describe('Должен вернуть 400 при 400 от geocoder', () => {
            test('Получение адреса из latitude и longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, llData);

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(400);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'HTTPError',
                        status: 400,
                        message: 'Response code 400 (Bad Request)',
                    });
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, {
                        suspected_latitude: llData.latitude,
                        suspected_longitude: llData.longitude,
                    });

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(400);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(400, {
                        name: 'HTTPError',
                        status: 400,
                        message: 'Response code 400 (Bad Request)',
                    });
            });
        });

        describe('Должен вернуть 500 при 500 от geobase', () => {
            test('Получение адреса из latitude и longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, llData);

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 192 })
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HttpGeobase',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                networkMock
                    .mockLaas()
                    .query(commonLaasQuery)
                    .reply(200, {
                        suspected_latitude: llData.latitude,
                        suspected_longitude: llData.longitude,
                    });

                networkMock
                    .mockGeocoder()
                    .query(llQuery)
                    .reply(200, geocoderData);

                networkMock
                    .mockGeobase()
                    .get('/v1/region_by_id')
                    .query({ id: 192 })
                    .reply(500);

                await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500, {
                        name: 'HttpGeobase',
                        status: 500,
                        message: 'Response code 500 (Internal Server Error)',
                    });
            });
        });

        describe('Должен падать при отсутствии тикета TVM для geocoder', () => {
            beforeEach(() => {
                nock.cleanAll();

                networkMock.mockTvm({ geocoderErr: true });
            });

            test('Получение адреса из latitude и longitude', async() => {
                const response = await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500);

                expect(response.text).toEqual('Internal Server Error');
            });

            test('Получение адреса из suspected latitude и suspected longitude', async() => {
                const response = await request(app)
                    .get('/v1/checkout/detect-address')
                    .set('X-Request-Id', 'req-id')
                    // Чтобы мидлвара cors выставила заголовки
                    .set('Origin', 'http://localhost:8080')
                    .expect('Access-Control-Allow-Credentials', 'true')
                    .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                    .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                    .expect(500);

                expect(response.text).toEqual('Internal Server Error');
            });
        });

        test('Должен корректно обработать запрос типа OPTIONS', async() => {
            await request(app)
                .options('/v1/checkout/detect-address')
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect('Access-Control-Max-Age', '86400')
                .expect(204);
        });
    });
});
