/* eslint-env node, mocha */
/* eslint-disable init-declarations */
/* eslint-disable max-statements */
/* eslint-disable camelcase */

const assert = require('assert');
const sinon = require('sinon');
const express = require('express');
const request = require('supertest');
const proxyquire = require('proxyquire').noCallThru();
const MissingMiddlewareError = require('missing-middleware-error');
const expressTld = require('express-tld');
const cookieParser = require('cookie-parser');

describe('National redirects', () => {
    let nationalRedirect;
    let config;
    let createGeobase;
    let geobase;
    let LangDetector;
    let detector;
    let logger;
    let host;
    let clock;

    beforeEach(() => {
        geobase = {
            makePinpointGeolocation: sinon.stub().returns({ region_id: 'geobase-region' }),
            getParentsIds: sinon.stub().callsFake(geoid => {
                if (geoid === 'geobase-region') {
                    return ['parent-1', 'parent-2'];
                }

                return ['parent-3', 'parent-4'];
            }),
        };
        createGeobase = {
            v6: sinon.stub().returns(geobase),
            v5: sinon.stub().returns('geobaseLookup5_instance'),
        };

        detector = {
            findDomain: sinon.stub().returns({ changed: false }),
        };
        LangDetector = sinon.stub().returns(detector);

        logger = {
            warn: sinon.stub(),
            error: sinon.stub(),
            child: sinon.stub().callsFake(() => logger),
        };

        nationalRedirect = proxyquire('../dist', {
            'yandex-geobase': createGeobase,
            'yandex-logger': () => logger,
            '/usr/lib/node_modules/langdetect': { LangDetector },
        }).default;

        config = {
            app: {
                domains: ['ru', 'ua', 'by', 'co.il', 'com.ge'],
            },
            geobase: { version: 6, options: 'geobase-options' },
            langdetect: {
                data: 'langdetect-data',
            },
        };

        host = 'www.yandex.com';

        clock = sinon.useFakeTimers(1000 * 1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('should create geobase 6 and langdetect instance', () => {
        nationalRedirect(config);

        sinon.assert.calledWithExactly(createGeobase.v6, 'geobase-options');
        sinon.assert.calledOnce(createGeobase.v6);

        sinon.assert.calledWithExactly(LangDetector, 'langdetect-data');
        sinon.assert.calledOnce(LangDetector);
    });

    it('should create geobase 5 and langdetect instance', () => {
        config = {
            app: {
                domains: ['ru', 'ua', 'by', 'co.il', 'com.ge'],
            },
            langdetect: {
                data: 'langdetect-data',
            },
            geobase: {
                version: 5,
                options: 'geobase_5_options',
            },
        };
        nationalRedirect(config);

        sinon.assert.calledWithExactly(createGeobase.v5, 'geobase_5_options');
        sinon.assert.calledOnce(createGeobase.v5);

        sinon.assert.notCalled(createGeobase.v6);

        sinon.assert.calledWithExactly(LangDetector, 'langdetect-data');
        sinon.assert.calledOnce(LangDetector);
    });

    it('should use default langdetect data path', () => {
        delete config.langdetect;

        nationalRedirect(config);

        sinon.assert.calledWithExactly(LangDetector, '/usr/share/yandex/lang_detect_data.txt');
        sinon.assert.calledOnce(LangDetector);
    });

    it('should throw MissingMiddlewareError without cookie-parser', () => {
        let error;

        const app = express();

        app.use(expressTld());
        app.use(nationalRedirect(config));

        // eslint-disable-next-line no-unused-vars, max-params
        app.use((err, req, res, next) => {
            error = err;
            res.sendStatus(500);
        });

        return request(app)
            .get('/')
            .expect(500)
            .then(() => {
                assert.ok(
                    error instanceof MissingMiddlewareError,
                    'error should be an instance of MissingMiddlewareError'
                );
            });
    });

    it('should throw MissingMiddlewareError without express-tld', () => {
        let error;

        const app = express();

        app.use(cookieParser());
        app.use(nationalRedirect(config));

        // eslint-disable-next-line no-unused-vars, max-params
        app.use((err, req, res, next) => {
            error = err;
            res.sendStatus(500);
        });

        return request(app)
            .get('/')
            .expect(500)
            .then(() => {
                assert.ok(
                    error instanceof MissingMiddlewareError,
                    'error should be an instance of MissingMiddlewareError'
                );
            });
    });

    it('should use logger from req', () => {
        let req;

        const reqLogger = {
            warn: sinon.stub(),
            error: sinon.stub(),
            child: sinon.stub().callsFake(() => reqLogger),
        };

        const app = getApp([
            (_req, res, next) => {
                _req.logger = reqLogger;
                req = _req;
                next();
            },
        ]);

        const error = new Error('geobase error');

        geobase.makePinpointGeolocation.throws(error);

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(reqLogger.child, {
                    req,
                    name: 'express-national-redirect',
                });
                sinon.assert.calledOnce(reqLogger.child);

                sinon.assert.calledWithExactly(reqLogger.warn, {
                    err: error,
                    data: {
                        id: null,
                        parents: null,
                        pinpointData: {
                            allow_yandex: true,
                            ip: '::ffff:127.0.0.1',
                            is_trusted: true,
                            x_forwarded_for: undefined,
                            x_real_ip: undefined,
                        },
                        yp: '',
                        ys: '',
                    },
                }, 'Failed to detect region parents');
                sinon.assert.calledOnce(reqLogger.warn);

                sinon.assert.notCalled(reqLogger.error);

                sinon.assert.notCalled(logger.warn);
                sinon.assert.notCalled(logger.error);
            });
    });

    it('should detect region parents with yandex_gid cookie', () => {
        const app = getApp([regionIdMiddleware]);

        app.enable('trust proxy');

        return request(app)
            .get('/')
            .set('Host', host)
            .set('Cookie', 'yandex_gid=299;yp=yp_value;ys=ys_value')
            .set('X-Forwarded-For', '1.2.3.4, 5.6.7.8')
            .set('X-Real-Ip', '127.0.0.1')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    geobase.makePinpointGeolocation,
                    {
                        yandex_gid: 299,
                        ip: '1.2.3.4',
                        x_forwarded_for: '1.2.3.4, 5.6.7.8',
                        x_real_ip: '127.0.0.1',
                        allow_yandex: true,
                        is_trusted: true,
                    },
                    'yp_value',
                    'ys_value'
                );
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledOnce(geobase.getParentsIds);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should detect region parents with req.regionId', () => {
        const app = getApp([regionIdMiddleware]);

        app.enable('trust proxy');

        return request(app)
            .get('/')
            .set('Host', host)
            .set('X-Forwarded-For', '1.2.3.4, 5.6.7.8')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    geobase.makePinpointGeolocation,
                    {
                        yandex_gid: 311,
                        ip: '1.2.3.4',
                        x_forwarded_for: '1.2.3.4, 5.6.7.8',
                        x_real_ip: undefined,
                        allow_yandex: true,
                        is_trusted: true,
                    },
                    '',
                    ''
                );
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledOnce(geobase.getParentsIds);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use fallback region if geobase.pinpointGeolocation throws', () => {
        const app = getApp();

        const error = new Error('geobase error');

        geobase.makePinpointGeolocation.throws(error);

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 87);
                sinon.assert.calledOnce(geobase.getParentsIds);

                sinon.assert.calledWithExactly(logger.warn, {
                    err: error,
                    data: {
                        id: null,
                        parents: null,
                        pinpointData: {
                            ip: '::ffff:127.0.0.1',
                            x_real_ip: undefined,
                            x_forwarded_for: undefined,
                            allow_yandex: true,
                            is_trusted: true,
                        },
                        yp: '',
                        ys: '',
                    },
                }, 'Failed to detect region parents');
                sinon.assert.calledOnce(logger.warn);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-3,parent-4',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use fallback region if geobase.parents throws', () => {
        const app = getApp();

        const error = new Error('geobase error');

        geobase.getParentsIds.callsFake(geoid => {
            if (geoid === 'geobase-region') {
                throw error;
            }

            return ['parent-3', 'parent-4'];
        });

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledWithExactly(geobase.getParentsIds, 87);
                sinon.assert.calledTwice(geobase.getParentsIds);

                sinon.assert.calledWithExactly(logger.warn, {
                    err: error,
                    data: {
                        id: 'geobase-region',
                        parents: null,
                        pinpointData: {
                            ip: '::ffff:127.0.0.1',
                            x_real_ip: undefined,
                            x_forwarded_for: undefined,
                            allow_yandex: true,
                            is_trusted: true,
                        },
                        yp: '',
                        ys: '',
                    },
                }, 'Failed to detect region parents');
                sinon.assert.calledOnce(logger.warn);
                sinon.assert.notCalled(logger.error);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-3,parent-4',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use fallback region if geobase.parents returns undefined', () => {
        const app = getApp();

        geobase.getParentsIds.callsFake(geoid => {
            if (geoid === 'geobase-region') {
                return undefined;
            }

            return ['parent-3', 'parent-4'];
        });

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledWithExactly(geobase.getParentsIds, 87);
                sinon.assert.calledTwice(geobase.getParentsIds);

                sinon.assert.notCalled(logger.warn);
                sinon.assert.notCalled(logger.error);

                sinon.assert.calledWithExactly(detector.findDomain,
                    'parent-3,parent-4',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use empty parents if geobase.parents returns undefined for fallback region', () => {
        const app = getApp();

        geobase.getParentsIds.returns(undefined);

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledWithExactly(geobase.getParentsIds, 87);
                sinon.assert.calledTwice(geobase.getParentsIds);

                sinon.assert.notCalled(logger.warn);
                sinon.assert.notCalled(logger.error);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    '',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use empty parents if geobase.parents throws for fallback region', () => {
        const app = getApp();
        const error = new Error('test');

        geobase.getParentsIds.callsFake(geoid => {
            if (geoid === 'geobase-region') {
                return undefined;
            }

            throw error;
        });

        return request(app)
            .get('/')
            .set('Host', host)
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledWithExactly(geobase.getParentsIds, 87);
                sinon.assert.calledTwice(geobase.getParentsIds);

                sinon.assert.calledWithExactly(logger.error, {
                    err: error,
                    data: { id: 87, tld: 'com' },
                }, 'Failed to detect fallback parents');
                sinon.assert.calledOnce(logger.error);

                sinon.assert.notCalled(logger.warn);

                sinon.assert.calledWithExactly(detector.findDomain,
                    '',
                    'ru,ua,by,co.il,com.ge',
                    host,
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use empty parents for unknown tld', () => {
        const app = getApp();

        geobase.getParentsIds.returns(undefined);

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.js')
            .expect(200)
            .then(() => {
                sinon.assert.calledOnce(geobase.makePinpointGeolocation);

                sinon.assert.calledWithExactly(geobase.getParentsIds, 'geobase-region');
                sinon.assert.calledOnce(geobase.getParentsIds);

                sinon.assert.notCalled(logger.warn);
                sinon.assert.notCalled(logger.error);

                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    '',
                    'ru,ua,by,co.il,com.ge',
                    'www.yandex.js',
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should use cr value from yp cookie', () => {
        const app = getApp();

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.com')
            .set('Cookie', 'yp=9999.foo.bar#1001.cr.cr_value')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    'www.yandex.com',
                    'cr_value'
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should not use cr value from expired yp cookie', () => {
        const app = getApp();

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.com')
            .set('Cookie', 'yp=9999.foo.bar#1000.cr.cr_value')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    'www.yandex.com',
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should not throw with yp cookie without cr value', () => {
        const app = getApp();

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.com')
            .set('Cookie', 'yp=9999.foo.bar')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    'www.yandex.com',
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should not throw with invalid yp cookie', () => {
        const app = getApp();

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.com')
            .set('Cookie', 'yp=asdasdas')
            .expect(200)
            .then(() => {
                sinon.assert.calledWithExactly(
                    detector.findDomain,
                    'parent-1,parent-2',
                    'ru,ua,by,co.il,com.ge',
                    'www.yandex.com',
                    ''
                );
                sinon.assert.calledOnce(detector.findDomain);
            });
    });

    it('should redirect with path from req.url', () => {
        const app = getApp([
            (req, res, next) => {
                delete req.originalUrl;
                next();
            },
        ]);

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(
                    res.headers.location,
                    'http://www.yandex.ru/some/url?query=param&foo=bar'
                );
            });
    });

    it('should redirect with path from req.originalUrl', () => {
        const app = getApp([
            (req, res, next) => {
                req.originalUrl = '/original-url';
                next();
            },
        ]);

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(res.headers.location, 'http://www.yandex.ru/original-url');
            });
    });

    it('should redirect with path from X-Original-Url header', () => {
        const app = getApp();

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .set('X-Original-Url', 'https://www.yandex.com/original-url?param=value')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(
                    res.headers.location,
                    'http://www.yandex.ru/original-url?param=value'
                );
            });
    });

    it('should not redirect to another host with X-Original-Url header', () => {
        const app = getApp();

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .set('X-Original-Url', 'https://ya.ru/path')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(res.headers.location, 'http://www.yandex.ru/path');
            });
    });

    it('should not redirect to another host when X-Original-Url header contains no slashes', () => {
        const app = getApp();

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .set('X-Original-Url', '.com')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(res.headers.location, 'http://www.yandex.ru/.com');
            });
    });

    it('should not redirect without hostname', () => {
        const app = getApp([regionIdMiddleware]);

        app.enable('trust proxy');

        return request(app)
            .get('/')
            .set('Host', '')
            .expect(200)
            .then(() => {
                sinon.assert.notCalled(geobase.makePinpointGeolocation);
                sinon.assert.notCalled(geobase.getParentsIds);
                sinon.assert.notCalled(detector.findDomain);
            });
    });

    it('should not throw with invalid url in X-Original-Url header', () => {
        const app = getApp();

        detector.findDomain.returns({
            changed: true,
            host: 'www.yandex.ru',
        });

        return request(app)
            .get('/some/url?query=param&foo=bar')
            .set('Host', 'www.yandex.com')
            .set('X-Original-Url', 'https://%c1%9c:..%c1%9c..@ya.ru/')
            .expect(302)
            .then(res => {
                assert.strictEqual(res.redirect, true);
                assert.strictEqual(
                    res.headers.location,
                    'http://www.yandex.ru/some/url?query=param&foo=bar'
                );
            });
    });

    it('should return 200 OK if langdetect throws', () => {
        const app = getApp();

        const error = new Error('detector error');

        detector.findDomain.throws(error);

        return request(app)
            .get('/')
            .set('Host', 'www.yandex.com')
            .set('Cookie', 'yp=1001.cr.cr_value')
            .expect(200)
            .then(() => {
                sinon.assert.notCalled(logger.warn);

                sinon.assert.calledWithExactly(logger.error, {
                    err: error,
                    data: {
                        parents: 'parent-1,parent-2',
                        domains: 'ru,ua,by,co.il,com.ge',
                        host,
                        cookieCr: 'cr_value',
                    },
                }, 'Failed to find domain for national redirect');
                sinon.assert.calledOnce(logger.error);
            });
    });

    function getApp(middleware) {
        const app = express();

        app.use(expressTld());
        app.use(cookieParser());

        if (middleware) {
            app.use(middleware);
        }

        app.use(nationalRedirect(config));

        app.get('/', (req, res) => res.sendStatus(200));
        app.get('/some/url', (req, res) => res.sendStatus(200));

        return app;
    }

    function regionIdMiddleware(req, res, next) {
        req.regionId = 311;
        next();
    }
});
