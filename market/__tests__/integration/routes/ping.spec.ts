import supertest from 'supertest';
import app from '../../../src/app';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
jest.mock('../../../utils/logs');

const request = supertest(app);

type MockResponseType = { text: string; body: any };

/**
 * @description returns parsed response body
 * @param {Object} response
 * @param {Object} cb - `done` callback
 * @returns {Object}
 */
function getResponseBody(response: MockResponseType, cb: { fail: Function }) {
    let body;
    try {
        body = JSON.parse(response.text);
    } catch (e) {
        cb.fail(e);
    }
    return body;
}

describe('All routes availability', () => {
    describe('GET /products', () => {
        it('should response with ok', (done) => {
            request
                .get('/products?client_id=00')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.settings).toBeDefined();

                    done();
                });
        });
    });

    describe('GET /ping-balancer', () => {
        it('should response with ok', (done) => {
            request
                .get('/ping-balancer')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();
                    expect(response.text).toBe('0;OK');

                    done();
                });
        });
    });

    describe('GET /promo', () => {
        it('should response with ok', (done) => {
            request
                .get('/promo')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.urls).toBeDefined();
                    expect(body.status).toBeTruthy();

                    done();
                });
        });
    });

    describe('GET /avia-search-start', () => {
        const query = {
            depart_by_av: 'MOW',
            arrive_by_av: 'BOJ',
        };

        describe('v1.0', () => {
            it('should response with ok', (done) => {
                request
                    .get('/avia-search-start')
                    .query(query)
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.settings).toBeDefined();

                        done();
                    });
            });
        });

        describe('v2.0', () => {
            it('should response with ok', (done) => {
                request
                    .get('/v2.0/avia-search-start')
                    .query(query)
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.settings).toBeDefined();

                        done();
                    });
            });
        });
    });

    describe('GET /avia-search-check', () => {
        const query = {
            depart_by_av: 'MOW',
            arrive_by_av: 'BOJ',
        };

        describe('v1.0', () => {
            it('should response with ok', (done) => {
                request
                    .get('/avia-search-check')
                    .query(query)
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.settings).toBeDefined();

                        done();
                    });
            });
        });

        describe('v2.0', () => {
            it('should response with ok', (done) => {
                request
                    .get('/v2.0/avia-search-check')
                    .query(query)
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.settings).toBeDefined();

                        done();
                    });
            });
        });
    });

    describe('GET /georegions', () => {
        it('should response with ok', (done) => {
            const query = {
                query: 'Моск',
                country: 225,
            };
            const stub = [
                {
                    id: 213,
                    name: 'Москва',
                },
                {
                    id: 103817,
                    name: 'Московский',
                },
                {
                    id: 100964,
                    name: 'Москаленки',
                },
            ];

            request
                .get('/georegions')
                .query(query)
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.regions).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /georegion', () => {
        it('should response with ok', (done) => {
            const query = { settings: { clid: '1337' } };
            const cookie = ['yandex_gid=213'];
            const stub = 'Москва';

            request
                .get('/georegion')
                .set('Cookie', cookie)
                .query(query)
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.region).toBe(stub);

                    done();
                });
        });
    });

    describe('POST /settings', () => {
        it('should response with ok', (done) => {
            const query = {
                settings: {
                    clid: '1337-322',
                },
            };
            const stub = {
                clid: '1337',
                vid: '322',
            };

            request
                .post('/settings')
                .query(query)
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.ok).toBeTruthy();
                    expect(body.settings).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /settings', () => {
        it('should response with ok', (done) => {
            const stub = {
                canRemoveSovetnik: false,
                showProductNotifications: true,
                showAviaNotifications: true,
                showAutoNotifications: true,
                showPriceTrendDown: true,
                showPriceTrendUp: false,
            };

            request
                .get('/settings')
                .expect(200)
                .end((err, { body }) => {
                    expect(err).toBeNull();
                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /settings/check', () => {
        it('should response with ok', (done) => {
            const stub = {
                status: false,
            };

            request
                .get('/settings/check')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('/support', () => {
        describe('GET /support', () => {
            it('should response with ok', (done) => {
                request
                    .get('/support')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });

        describe('POST /support', () => {
            it('should response with ok', (done) => {
                request
                    .post('/sovetnik')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });
    });

    describe('/client', () => {
        describe('GET /client', () => {
            it('should response with ok', (done) => {
                request
                    .get('/client')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });

        describe('POST /client', () => {
            it('should response with ok', (done) => {
                request
                    .post('/client')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });
    });

    describe('GET /sovetnik-disabled', () => {
        it('should response with ok', (done) => {
            request
                .get('/sovetnik-disabled')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.ok).toBeTruthy();

                    done();
                });
        });
    });

    describe('/uninstall-reason', () => {
        describe('GET /uninstall-reason', () => {
            it('should response with ok', (done) => {
                request
                    .get('/uninstall-reason')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });

        describe('POST /uninstall-reason', () => {
            it('should response with ok', (done) => {
                request
                    .post('/uninstall-reason')
                    .expect(200)
                    .end((err: any, response: MockResponseType) => {
                        expect(err).toBeNull();

                        const body = getResponseBody(response, done);

                        expect(body).toBeDefined();
                        expect(body.ok).toBeTruthy();

                        done();
                    });
            });
        });
    });

    describe('POST /feedback', () => {
        it('should response with ok', (done) => {
            request
                .post('/feedback')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.ok).toBeTruthy();

                    done();
                });
        });
    });

    describe('POST /feedback-avia', () => {
        it('should response with ok', (done) => {
            request
                .post('/feedback-avia')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.ok).toBeTruthy();

                    done();
                });
        });
    });

    describe('POST /save-campaign', () => {
        it('should response with ok', (done) => {
            request
                .post('/save-campaign')
                .expect(200)
                .end((err, { body }) => {
                    expect(err).toBeNull();
                    expect(body).toBeDefined();
                    expect(body.ok).toBeTruthy();

                    done();
                });
        });
    });

    describe('GET /chps', () => {
        it('should response with ok', (done) => {
            request
                .get('/chps')
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body.ok).toBeFalsy();

                    done();
                });
        });
    });

    describe('GET /pp/check', () => {
        it('should response with ok', (done) => {
            const query = {
                clid: '1337',
            };
            const stub = {
                status: 'UNSUPPORTED_DESKTOP',
            };

            request
                .get('/pp/check')
                .query(query)
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /pp/landing-check', () => {
        it('should response with ok', (done) => {
            const query = {
                clid: '1337',
                settings: {
                    install_id: 'sovetnik',
                },
            };
            const stub = expect.objectContaining({
                browser: 'other',
                status: 'UNSUPPORTED_DESKTOP',
                install_id: expect.any(String),
            });

            request
                .get('/pp/landing-check')
                .query(query)
                .expect(200)
                .end((err: any, response: MockResponseType) => {
                    expect(err).toBeNull();

                    const body = getResponseBody(response, done);

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /pp/installer', () => {
        it('should response with ok', (done) => {
            const query = {
                clid: 1337,
                affId: 1,
                vid: 1,
                browser: 'Yandex',
                timestamp: Date.now(),
            };
            const stub = expect.objectContaining({
                ok: expect.any(Boolean),
            });

            request
                .get('/pp/installer')
                .query(query)
                .expect(200)
                .end((err: any, body) => {
                    expect(err).toBeNull();

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /tc', () => {
        it('should response with ok', (done) => {
            // eslint-disable-next-line max-len
            const query =
                '?dds=93abd0aec3f188793420928a24929d79&v=201912041225&isSelectorExists=true&transaction_id=k4bcifwhc213mcj63g4ypkyhaueml5kd&is_shop=true&settings=%7B%22applicationName%22%3A%22%D0%A1%D0%BE%D0%B2%D0%B5%D1%82%D0%BD%D0%B8%D0%BA%20%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9C%D0%B0%D1%80%D0%BA%D0%B5%D1%82%D0%B0%22%2C%22affId%22%3A%221048%22%2C%22clid%22%3A2210393%2C%22sovetnikExtension%22%3Atrue%2C%22withButton%22%3Atrue%2C%22extensionStorage%22%3Atrue%2C%22installTime%22%3A%221573040253775%22%2C%22installId%22%3A%22faf0ed18-273c-4976-9563-25e315d52eaa%22%2C%22notificationStatus%22%3A%22yandex%22%2C%22notificationPermissionGranted%22%3Atrue%7D&referrer=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&adult=true&screen_size=%7B%22width%22%3A1396%2C%22height%22%3A961%7D&screen_resolution=%7B%22ratio%22%3A1%2C%22width%22%3A1920%2C%22height%22%3A1080%7D&viewport=%7B%22width%22%3A1396%2C%22height%22%3A2392%7D&notifications=yandex&ui_language=ru&is_debug_mode=true&url=https%3A%2F%2Fwww.eldorado.ru%2Fpersonal%2Fbasket.php&cart=%7B%22products%22%3A%5B%7B%22title%22%3A%22%D0%A1%D0%BC%D0%B0%D1%80%D1%82%D1%84%D0%BE%D0%BD%20Apple%20iPhone%208%2064Gb%20Silver%20(MQ6H2RU%2FA)%22%2C%22quantity%22%3A1%2C%22price%22%3A39990%7D%5D%2C%22main%22%3A%7B%22price%22%3A39990%2C%22multiplyItemsPrice%22%3Afalse%7D%7D';
            const stub = expect.objectContaining({
                ok: expect.any(Boolean),
            });

            request
                .get('/tc')
                .query(query)
                .expect(200)
                .end((err: any, body) => {
                    expect(err).toBeNull();

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /tch', () => {
        it('should response with ok', (done) => {
            // eslint-disable-next-line max-len
            const query =
                '?dds=93abd0aec3f188793420928a24929d79&v=201912041225&isSelectorExists=true&transaction_id=k4bdlxsptzrvhphzhu6l4605wzwukb18&is_shop=true&settings=%7B%22applicationName%22%3A%22%D0%A1%D0%BE%D0%B2%D0%B5%D1%82%D0%BD%D0%B8%D0%BA%20%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9C%D0%B0%D1%80%D0%BA%D0%B5%D1%82%D0%B0%22%2C%22affId%22%3A%221048%22%2C%22clid%22%3A2210393%2C%22sovetnikExtension%22%3Atrue%2C%22withButton%22%3Atrue%2C%22extensionStorage%22%3Atrue%2C%22installTime%22%3A%221573040253775%22%2C%22installId%22%3A%22faf0ed18-273c-4976-9563-25e315d52eaa%22%2C%22notificationStatus%22%3A%22yandex%22%2C%22notificationPermissionGranted%22%3Atrue%7D&referrer=https%3A%2F%2Fwww.eldorado.ru%2Fpersonal%2Forder.php%3Fstep%3Dsd_confirm&adult=true&screen_size=%7B%22width%22%3A1920%2C%22height%22%3A961%7D&screen_resolution=%7B%22ratio%22%3A1%2C%22width%22%3A1920%2C%22height%22%3A1080%7D&viewport=%7B%22width%22%3A1920%2C%22height%22%3A1086%7D&notifications=yandex&ui_language=ru&is_debug_mode=true&url=https%3A%2F%2Fwww.eldorado.ru%2Fpersonal%2Forder.php%3Fstep%3Dorder_confirm&type-checkout=button';
            const stub = expect.objectContaining({
                ok: expect.any(Boolean),
            });

            request
                .get('/tch')
                .query(query)
                .expect(200)
                .end((err: any, body) => {
                    expect(err).toBeNull();

                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });

    describe('GET /router and /shops', () => {
        it('should response with ok', (done) => {
            const stub = expect.objectContaining({
                ok: expect.any(Boolean),
            });

            request
                .get('/router')
                .expect(200)
                .end((err: any, body) => {
                    expect(err).toBeNull();
                    expect(body).toBeDefined();
                    expect(body).toEqual(stub);

                    done();
                });
        });
    });
});
