'use strict';

/* global describe, it */

var assert = require('chai').assert;
var mockUser = require('./mock/user');
var mockRegion = require('./mock/region');
var MockDriver = require('./mock/geobase-driver');
var User = require('nodules-user');
var nock = require('nock');
var SOURCES = require('../').SOURCES;
var RegionAsync = require('../');
var vow = require('vow');
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');
var sinon = require('sinon');
var geobaseResponse = require('./mock/geobase-response');
var _ = require('lodash');
var helpers = require('../lib/helpers');

chai.use(chaiAsPromised);

var DEFAULT_USER = 'noob';
var DEFAULT_LOCATION = 'yandex';
var COMPONENT_NAME = 'regionAsync';
var MOSCOW_ID = 213;
var KIEV_ID = 143;
var RUSSIA_ID = 225;
var YANDEX_ID = 9999;
var VLADIVOSTOK_ID = 75;

function createRequest(mock) {
    return {
        cookies: mock.cookies,
        headers: {}
    };
}

function createResponse() {
    return {
        setHeader: function() {
        },
        getHeader: function() {
        }
    };
}

function createDriver() {
    return new MockDriver(geobaseResponse);
}

function initRegion(params) {
    params = params || {};
    var userData = mockUser[ params.user || DEFAULT_USER ];
    var regionData = mockRegion[ params.location || DEFAULT_LOCATION ];

    var request = createRequest(userData);
    var response = createResponse();

    if (userData.passportData) {
        nock('http://blackbox-mimino.yandex.net')
            .filteringPath(function() {
                return '/blackbox';
            })
            .get('/blackbox')
            .reply(200, JSON.parse(JSON.stringify(userData.passportData)));
    }

    var regionParams = params.regionParams || {};
    if (!regionParams.driver) {
        regionParams.driver = createDriver();
    }

    User.registerComponent(COMPONENT_NAME, RegionAsync);

    return new User(request, response, {
        clientIp: regionData.clientIp,
        url: params.url || regionData.url,
        auth: {},
        l10n: {},
        regionAsync: regionParams
    })
    .init('auth')
    .then(function(user) {
        return user.init(COMPONENT_NAME);
    }).then(function(user) {
        return user.regionAsync;
    });
}

function getExpected(method, params) {
    params = params.join('_');
    return geobaseResponse[ method ][ params ];
}

function getExpectedInfo(regionId) {
    var lang = 'ru';
    return {
        id: regionId,
        name: getExpected('linguistics', [ regionId, lang ]).nominative,
        country: getExpected('getCountryInfoByRegionId', [ regionId, lang ]).id,
        linguistics: getExpected('linguistics', [ regionId, lang ]),
        data: getExpected('regionById', [ regionId, lang ])
    };
}

describe('RegionAsync component', function() {

    describe('Constructor', function() {
        it('should init geobase driver', function(done) {
            var driver = createDriver();
            driver.init = function(initDone) {
                initDone();
                done();
            };

            initRegion({
                regionParams: { driver: driver }
            }).fail(done);
        });

        it('should initialize correctly if driver has no init method', function(done) {
            var driver = createDriver();
            driver.init = null;

            initRegion({
                regionParams: { driver: driver }
            }).then(function() {
                done();
            }).fail(done);
        });

        it('should set default region id', function(done) {
            initRegion({ location: 'local' }).then(function(region) {
                assert.deepEqual(region.idsBySource(SOURCES.DEFAULT), [ MOSCOW_ID ]);
                assert.strictEqual(region.source, SOURCES.DEFAULT);

                done();
            }).fail(done);
        });

        it('should set only default source if cannot detect by ip or url', function(done) {
            initRegion({ location: 'local' }).then(function(region) {
                assert.deepEqual(region.idsBySource(SOURCES.DEFAULT), [ MOSCOW_ID ]);
                assert.strictEqual(region.source, SOURCES.DEFAULT);

                assert.isUndefined(region.idsBySource(SOURCES.DETECTED));
                assert.isUndefined(region.idsBySource(SOURCES.SETTINGS));
                assert.isUndefined(region.idsBySource(SOURCES.URL));
                assert.isUndefined(region.idsBySource(SOURCES.EXTRA));

                done();
            }).fail(done);
        });

        it('should detect region from url', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/?rid=' + KIEV_ID
            }).then(function(region) {
                assert.deepEqual(region.idsBySource(SOURCES.URL), [ KIEV_ID ]);
                assert.strictEqual(region.source, SOURCES.URL);
                done();
            }).fail(done);
        });

        it('should detect region by ip and cookies', function(done) {
            initRegion().then(function(region) {
                assert.deepEqual(region.idsBySource(SOURCES.DETECTED), [ MOSCOW_ID ]);
                assert.strictEqual(region.source, SOURCES.DETECTED);
                done();
            }).fail(done);
        });

        it('should detect location by ip and cookies', function(done) {
            initRegion().then(function(region) {
                assert.deepEqual(region.location, getExpected('location', [ MOSCOW_ID ]));
                done();
            }).fail(done);
        });

        it('should not set region name after init', function(done) {
            initRegion().then(function(region) {
                assert.throws(function() {
                    region.name;
                });
                done();
            }).fail(done);
        });

        it('should not set region names after init', function(done) {
            initRegion().then(function(region) {
                assert.throws(function() {
                    region.names.length;
                });
                done();
            }).fail(done);
        });

        it('should rewrite yandex region to moscow by default', function(done) {
            initRegion().then(function(region) {
                assert.strictEqual(region.id, MOSCOW_ID);
                done();
            }).fail(done);
        });

        it('should allow to not rewrite yandex region to moscow', function(done) {
            initRegion({
                regionParams: { isRewriteYandexRegion: false }
            }).then(function(region) {
                assert.strictEqual(region.id, 9999);
                done();
            }).fail(done);
        });

        it('should overwrite user.region property if specified in config', function(done) {
            initRegion({
                regionParams: { overwriteRegionComponent: true }
            }).then(function(region) {
                var user = region.user;
                assert.strictEqual(user.region, region);
                done();
            }).fail(done);
        });
    });

    describe('Common geobase methods', function() {
        var geobase;

        beforeEach(function(done) {
            initRegion({
                url: 'http://market.yandex.ru?rid=' + MOSCOW_ID
            }).then(function(initializedRegion) {
                geobase = initializedRegion.geobase;
                done();
            }).fail(done);
        });

        it('getLinguistics', function() {
            return assert.eventually.deepEqual(
                geobase.getLinguistics(MOSCOW_ID),
                getExpected('linguistics', [ MOSCOW_ID, 'ru' ])
            );
        });

        it('getRegionName', function() {
            return assert.eventually.deepEqual(
                geobase.getRegionName(MOSCOW_ID),
                getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative
            );
        });

        it('getRegionNames', function() {
            return assert.eventually.deepEqual(
                geobase.getRegionNames([ MOSCOW_ID ]),
                [ (getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative) ]
            );
        });

        it('getInfo', function() {
            return assert.eventually.deepEqual(
                geobase.getInfo(MOSCOW_ID),
                getExpectedInfo(MOSCOW_ID)
            );
        });

        it('getIsRegionIdIn', function() {
            return assert.eventually.ok(
                geobase.getIsRegionIdIn(MOSCOW_ID, RUSSIA_ID)
            );
        });

        it('getTimezone', function() {
            return assert.eventually.deepEqual(
                geobase.getTimezone(MOSCOW_ID),
                getExpected('tzinfo', [ MOSCOW_ID ])
            );
        });

        describe('getPinpointGeolocation', function() {
            it('should get locaition', function() {
                return assert.eventually.deepEqual(
                    geobase.getPinpointGeolocation({
                        ip: '77.88.2.223',
                        yandex_gid: MOSCOW_ID,
                        x_forwarded_for: '',
                        allow_yandex: false
                    }),
                    getExpected('pinpointGeolocation', [ '77.88.2.223', false ])
                );
            });

            it('should reject on bad yandex_gid', function() {
                return assert.isRejected(geobase.getPinpointGeolocation({
                    ip: '77.88.2.223',
                    yandex_gid: '',
                    x_forwarded_for: '',
                    allow_yandex: false
                }));
            });
        });

        it('setNewGeolocation', function(done) {
            var driver = createDriver();
            sinon.spy(driver, 'setNewGeolocation');

            initRegion({
                regionParams: { driver: driver }
            }).then(function(region) {
                region.geobase.setNewGeolocation(true).then(function() {
                    assert.ok(driver.setNewGeolocation.called, 'setNewGeolocation was not called');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should get data about hidden region', function() {
            return assert.eventually.deepEqual(
                geobase.getInfo(YANDEX_ID),
                getExpectedInfo(YANDEX_ID)
            );
        });
    });

    describe('Methods for working with current state', function() {
        var region;

        beforeEach(function(done) {
            initRegion({
                url: 'http://market.yandex.ru?rid=' + MOSCOW_ID
            }).then(function(initializedRegion) {
                region = initializedRegion;
                done();
            }).fail(done);
        });

        it('sources', function() {
            assert.strictEqual(region.source, SOURCES.URL);
        });

        it('getCountryId', function() {
            return assert.eventually.strictEqual(
                region.getCountryId(),
                RUSSIA_ID
            );
        });

        it('getInfo', function() {
            return assert.eventually.deepEqual(
                region.getInfo(),
                getExpectedInfo(MOSCOW_ID)
            );
        });

        it('getLinguistics', function() {
            return assert.eventually.strictEqual(
                region.getLinguistics(),
                getExpected('linguistics', [ MOSCOW_ID, 'ru' ])
            );
        });

        it('getRegionName', function() {
            return assert.eventually.strictEqual(
                region.getRegionName(),
                getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative
            );
        });

        it('getRegionNames', function() {
            return assert.eventually.deepEqual(
                region.getRegionNames(),
                [ getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative ]
            );
        });

        it('getTimezone', function() {
            return assert.eventually.strictEqual(
                region.getTimezone(),
                getExpected('tzinfo', [ MOSCOW_ID ])
            );
        });

        it('isInternalNetwork', function() {
            return assert.eventually.isTrue(
                region.getIsInternalNetwork()
            );
        });

        it('idBySources', function() {
            assert.strictEqual(
                region.idBySource(SOURCES.DEFAULT),
                MOSCOW_ID
            );
        });

        it('idsBySources', function() {
            assert.deepEqual(
                region.idsBySource(SOURCES.DEFAULT),
                [ MOSCOW_ID ]
            );
        });

        it('portalId', function(done) {
            initRegion().then(function(region) {
                assert.strictEqual(region.portalId, MOSCOW_ID);
                region.setId(KIEV_ID, SOURCES.DETECTED).then(function() {
                    assert.strictEqual(region.portalId, KIEV_ID);
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('setLang', function(done) {
            initRegion().then(function(region) {
                region.setLang(RegionAsync.LANG.EN);
                region.getLinguistics().then(function(data) {
                    assert.deepEqual(data, getExpected('linguistics', [ MOSCOW_ID, 'en' ]));
                    done();
                }).fail(done);
            }).fail(done);
        });
    });

    describe('setId and setIds methods', function() {
        var region;

        beforeEach(function(done) {
            initRegion().then(function(initializedRegion) {
                region = initializedRegion;
                done();
            }).fail(done);
        });

        it('should use same method for setId and setIds', function() {
            assert.strictEqual(region.setId, region.setIds);
        });

        it('should call regionById', function(done) {
            var driver = createDriver();
            sinon.spy(driver, 'regionById');

            initRegion({
                regionParams: { driver: driver }
            }).then(function(region) {
                region.setId(MOSCOW_ID).then(function() {
                    assert.ok(driver.regionById.called, 'regionById was not called');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should return promise', function() {
            var maybePromise = region.setId(MOSCOW_ID);
            assert.ok(vow.isPromise(maybePromise));
        });

        it('should reject if attempt was made to set invalid id', function(done) {
            assert.strictEqual(region.id, MOSCOW_ID);
            region.setId(8888888).fail(function() {
                assert.strictEqual(region.id, MOSCOW_ID);
                done();
            }).fail(done);
        });

        it('should reject if one of ids is invalid', function(done) {
            assert.deepEqual(region.ids, [ MOSCOW_ID ]);
            region.setIds([ MOSCOW_ID, 8888888, KIEV_ID ]).fail(function() {
                assert.deepEqual(region.ids, [ MOSCOW_ID ]);
                done();
            }).fail(done);
        });

        it('should set correct id and resolve promise', function(done) {
            assert.strictEqual(region.id, MOSCOW_ID);
            region.setId(KIEV_ID).then(function() {
                assert.strictEqual(region.id, KIEV_ID);
                done();
            }).fail(done);
        });

        it('should set correct ids and resolve promise', function(done) {
            assert.deepEqual(region.ids, [ MOSCOW_ID ]);
            region.setIds([ KIEV_ID, MOSCOW_ID ]).then(function() {
                assert.deepEqual(region.ids, [ KIEV_ID, MOSCOW_ID ]);
                done();
            }).fail(done);
        });

        it('should set id for source', function(done) {
            assert.isUndefined(region.idsBySource(SOURCES.SETTINGS));
            region.setId(KIEV_ID, SOURCES.SETTINGS).then(function() {
                assert.deepEqual(region.idsBySource(SOURCES.SETTINGS), [ KIEV_ID ]);
                done();
            }).fail(done);
        });

        it('should not allow to set unknown (invalid) source', function(done) {
            region.setId(KIEV_ID, 80).then(function() {
                done(new Error('it did not rejected invalid source'));
            }, function() {
                done();
            });
        });

        it('should not change current source if new has lower priority', function(done) {
            assert.strictEqual(region.source, SOURCES.DETECTED);
            region.setId(KIEV_ID, SOURCES.DEFAULT).then(function() {
                assert.strictEqual(region.source, SOURCES.DETECTED);
                done();
            }).fail(done);
        });

        it('should change current source if new has higher priority', function(done) {
            assert.strictEqual(region.source, SOURCES.DETECTED);
            region.setId(KIEV_ID, SOURCES.EXTRA).then(function() {
                assert.strictEqual(region.source, SOURCES.EXTRA);
                done();
            }).fail(done);
        });

        it('should not affect ids of other sources when set for one', function(done) {
            var defaultId = region.idBySource(SOURCES.DEFAULT);
            var detectedId = region.idBySource(SOURCES.DETECTED);
            region.setId(KIEV_ID, SOURCES.SETTINGS).then(function() {
                assert.strictEqual(region.idBySource(SOURCES.DEFAULT), defaultId);
                assert.strictEqual(region.idBySource(SOURCES.DETECTED), detectedId);
                done();
            }).fail(done);
        });

        it('should flush properties if id was changed', function(done) {
            region.ensureProps([
                'country',
                'info',
                'isInternalNetwork',
                'timezone',
                'linguistics',
                'location'
            ]).then(function(region) {
                region.setId(KIEV_ID).then(function() {
                    assert.throws(function() {
                        region.country;
                    }, 'country');
                    assert.throws(function() {
                        region.info;
                    }, 'info');
                    assert.throws(function() {
                        region.isInternalNetwork;
                    }, 'isInternalNetwork');
                    assert.throws(function() {
                        region.timezone;
                    }, 'timezone');
                    assert.throws(function() {
                        region.linguistics;
                    }, 'linguistics');
                    assert.throws(function() {
                        region.location;
                    }, 'location');

                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should set id for source "extra" by default', function(done) {
            region.setId(KIEV_ID).then(function() {
                assert.strictEqual(region.source, SOURCES.EXTRA);
                done();
            }).fail(done);
        });

        it('should not flush properties if source priority is lower', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([
                    'country',
                    'info',
                    'timezone',
                    'linguistics'
                ]).then(function(region) {
                    var prevCountry = region.country;
                    var prevInfo = region.info;
                    var prevTimezone = region.timezone;
                    var prevLinguistics = region.linguistics;

                    region.setId(KIEV_ID, SOURCES.DEFAULT).then(function() {
                        assert.strictEqual(region.country, prevCountry);
                        assert.strictEqual(region.info, prevInfo);
                        assert.strictEqual(region.timezone, prevTimezone);
                        assert.strictEqual(region.linguistics, prevLinguistics);

                        done();
                    }).fail(done);
                }).fail(done);
            }).fail(done);
        });

        it('should flush certain property if flushPropCache called', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([
                    'country',
                    'info',
                    'timezone',
                    'linguistics'
                ]).then(function(region) {
                    var prevCountry = region.country;
                    var prevInfo = region.info;
                    var prevLinguistics = region.linguistics;

                    region.flushPropCache('timezone');

                    assert.strictEqual(region.country, prevCountry);
                    assert.strictEqual(region.info, prevInfo);
                    assert.strictEqual(region.linguistics, prevLinguistics);

                    assert.throws(function() {
                        region.timezone;
                    }, 'timezone');

                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should throw an exception if flushPropCache called with wrong property name', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([
                    'country',
                    'info',
                    'timezone',
                    'linguistics'
                ]).then(function(region) {

                    assert.throws(function() {
                        region.flushPropCache('I_AM_NOT_USED_PROPNAME');
                    }, 'Invalid property name');

                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should reject if region not found in geobase', function(done) {
            region.setId(174)
                .then(
                    function() {
                        done(new Error('should not resolve property'));
                    },
                    function(error) {
                        assert.instanceOf(error, RegionAsync.Error, 'wrong error type');
                        assert.strictEqual(error.code, RegionAsync.Error.CODES.INVALID_REGION_ID, 'wrong error code');
                        done();
                    }
                ).fail(done);
        });
    });

    describe('ensureProps', function() {
        var propsExpected = [
            {
                name: 'name',
                initial: undefined,
                expected: getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative
            },
            {
                name: 'names',
                initial: [],
                expected: [ getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative ]
            },
            {
                name: 'country',
                initial: undefined,
                expected: getExpected('getCountryInfoByRegionId', [ MOSCOW_ID, 'ru' ]).id
            },
            {
                name: 'info',
                initial: undefined,
                expected: getExpectedInfo(MOSCOW_ID)
            },
            {
                name: 'isInternalNetwork',
                initial: undefined,
                expected: true
            },
            {
                name: 'timezone',
                initial: undefined,
                expected: getExpected('tzinfo', [ MOSCOW_ID ])
            },
            {
                name: 'linguistics',
                initial: undefined,
                expected: getExpected('linguistics', [ MOSCOW_ID, 'ru' ]),
            }
        ];

        propsExpected.forEach(function(prop) {
            it('region.' + prop.name + ' should throw by default', function(done) {
                initRegion().then(function(region) {
                    assert.throws(function() {
                        region[prop.name];
                    }, /init/);
                    done();
                }).fail(done);
            });
        });

        propsExpected.forEach(function(prop) {
            it('region.' + prop.name + ' should init property through ensureProps', function(done) {
                initRegion().then(function(region) {
                    region.ensureProps([ prop.name ]).then(function(region) {
                        if (Array.isArray(prop.expected) || _.isObject(prop.expected)) {
                            assert.deepEqual(region[prop.name], prop.expected);
                        } else {
                            assert.strictEqual(region[prop.name], prop.expected);
                        }
                        done();
                    }).fail(done);
                });
            });
        });

        it('should initialize multiple properties correctly', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([
                    'name',
                    'names',
                    'country',
                    'info',
                    'isInternalNetwork',
                    'timezone',
                    'linguistics'
                ]).then(function(region) {
                    assert.strictEqual(
                        region.name,
                        getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative
                    );
                    assert.deepEqual(
                        region.names,
                        [ getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative ]
                    );
                    assert.strictEqual(
                        region.country,
                        getExpected('getCountryInfoByRegionId', [ MOSCOW_ID, 'ru' ]).id,
                        'country after init'
                    );
                    assert.deepEqual(region.info, getExpectedInfo(MOSCOW_ID), 'info after init');
                    assert.isTrue(region.isInternalNetwork, 'isInternalNetwork after init');
                    assert.deepEqual(region.timezone, getExpected('tzinfo', [ MOSCOW_ID ]), 'timezone after init');
                    assert.deepEqual(
                        region.linguistics,
                        getExpected('linguistics', [ MOSCOW_ID, 'ru' ]),
                        'linguistics after init'
                    );

                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should get data from driver', function(done) {
            var driver = createDriver();
            sinon.spy(driver, 'setNewGeolocation');
            sinon.spy(driver, 'pinpointGeolocation');
            sinon.spy(driver, 'linguistics');
            sinon.spy(driver, 'regionByIp');
            sinon.spy(driver, 'getCountryInfoByRegionId');
            sinon.spy(driver, 'regionById');
            sinon.spy(driver, 'tzinfo');

            initRegion({
                regionParams: { driver: driver }
            }).then(function(region) {
                region.ensureProps([
                    'isInternalNetwork',
                    'linguistics',
                    'country',
                    'info',
                    'timezone'
                ]).then(function() {
                    assert.ok(driver.setNewGeolocation.called, 'setNewGeolocation');
                    assert.ok(driver.pinpointGeolocation.called, 'pinpointGeolocation');
                    assert.deepEqual(driver.pinpointGeolocation.getCall(0).args[0], {
                        ip: '77.88.2.223',
                        x_forwarded_for: '',
                        yandex_gid: MOSCOW_ID,
                        allow_yandex: false
                    }, 'pinpointGeolocation with correct first argument');
                    assert.ok(driver.linguistics.called, 'linguistics');
                    assert.ok(driver.regionByIp.called, 'regionByIp');
                    assert.ok(driver.getCountryInfoByRegionId.called, 'getCountryInfoByRegionId');
                    assert.ok(driver.regionById.called, 'regionById');
                    assert.ok(driver.tzinfo.called, 'tzinfo');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should cache data after first call', function(done) {
            var driver = createDriver();
            sinon.spy(driver, 'region');
            sinon.spy(driver, 'linguistics');
            sinon.spy(driver, 'regionByIp');
            sinon.spy(driver, 'getCountryInfoByRegionId');
            sinon.spy(driver, 'regionById');
            sinon.spy(driver, 'tzinfo');

            initRegion({
                regionParams: { driver: driver }
            }).then(function(region) {
                region.ensureProps([
                    'info',
                    'country',
                    'timezone',
                    'linguistics'
                ]).then(function() {
                    // сбрасываем счетчики вызовов
                    driver.region.reset();
                    driver.linguistics.reset();
                    driver.regionByIp.reset();
                    driver.getCountryInfoByRegionId.reset();
                    driver.regionById.reset();
                    driver.tzinfo.reset();

                    region.ensureProps([
                        'info',
                        'country',
                        'timezone',
                        'linguistics'
                    ]).then(function() {
                        assert.notOk(driver.region.called, 'region');
                        assert.notOk(driver.linguistics.called, 'linguistics');
                        assert.notOk(driver.regionByIp.called, 'regionByIp');
                        assert.notOk(driver.getCountryInfoByRegionId.called, 'getCountryInfoByRegionId');
                        assert.notOk(driver.regionById.called, 'regionById');
                        assert.notOk(driver.tzinfo.called, 'tzinfo');
                        done();
                    }).fail(done);
                }).fail(done);
            }).fail(done);
        });

        it('should not request for properties, that already fulfilled with info request', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([
                    'info'
                ]).then(function() {
                    assert.ok(region.info, 'info');
                    assert.ok(region.name, 'name');
                    assert.ok(region.names, 'names');
                    assert.ok(region.linguistics, 'linguistics');
                    assert.ok(region.country, 'country');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should not fill "names" prop after requesting info if user defined multiple regions', function(done) {
            initRegion({
                url: 'http://market.yandex.ru?rid=' + MOSCOW_ID + '&rid=' + KIEV_ID
            }).then(function(region) {
                region.ensureProps([
                    'info'
                ]).then(function() {
                    assert.throws(function() {
                        region.names;
                    });
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should fail if incorrect property name was specified', function(done) {
            var invalidProp = 'nonexisting';
            var expectedError = RegionAsync.Error.createError(
                RegionAsync.Error.CODES.INVALID_PROPERTY_NAME,
                {
                    name: invalidProp
                }
            );
            initRegion().then(function(region) {
                region.ensureProps([ invalidProp ]).then(function() {
                    done(new Error('ensureProps did not failed with incorrect property name'));
                }, function(error) {
                    assert.instanceOf(error, RegionAsync.Error, 'wrong error type');
                    assert.strictEqual(error.message, expectedError.message, 'wrong error message');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should fail if argument is not an array', function(done) {
            var validPropName = 'country';
            var expectedError = RegionAsync.Error.createError(
                RegionAsync.Error.CODES.INVALID_ARGUMENT_TYPE
            );

            initRegion().then(function(region) {
                region.ensureProps(validPropName).then(function() {
                    done(new Error('ensureProps did not failed with incorrect argument'));
                }, function(error) {
                    assert.instanceOf(error, RegionAsync.Error, 'wrong error type');
                    assert.strictEqual(error.message, expectedError.message, 'wrong error message');
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should initialize all props if ensureAllProps called', function(done) {
            initRegion().then(function(region) {
                region.ensureAllProps().then(function(region) {
                    assert.strictEqual(
                        region.name,
                        getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative
                    );
                    assert.deepEqual(
                        region.names,
                        [ getExpected('linguistics', [ MOSCOW_ID, 'ru' ]).nominative ]
                    );
                    assert.strictEqual(
                        region.country,
                        getExpected('getCountryInfoByRegionId', [ MOSCOW_ID, 'ru' ]).id,
                        'country after init'
                    );
                    assert.deepEqual(region.info, getExpectedInfo(MOSCOW_ID), 'info after init');
                    assert.isTrue(region.isInternalNetwork, 'isInternalNetwork after init');
                    assert.deepEqual(region.timezone, getExpected('tzinfo', [ MOSCOW_ID ]), 'timezone after init');
                    assert.deepEqual(
                        region.linguistics,
                        getExpected('linguistics', [ MOSCOW_ID, 'ru' ]),
                        'linguistics after init'
                    );
                    assert.deepEqual(
                        region.location,
                        getExpected('location', [ MOSCOW_ID ]),
                        'location after init'
                    );

                    done();
                }).fail(done);
            }).fail(done);
        });

        it('if id was changed should update', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([ 'location' ])
                    .then(function() {
                        return region.setId(KIEV_ID);
                    })
                    .then(function() {
                        return region.ensureProps([ 'location' ]);
                    })
                    .then(function() {
                        assert.deepEqual(region.location, getExpected('location', [ KIEV_ID ]), 'location');
                        done();
                    })
                    .fail(done);
            });
        });

        it('if change id with same source cache should clear', function(done) {
            initRegion().then(function(region) {
                region.ensureProps([ 'location' ])
                    .then(function() {
                        return region.setId(KIEV_ID, SOURCES.EXTRA)
                            .then(() => region.ensureAllProps());
                    })
                    .then(function() {
                        assert.equal(region.name, getExpected('name', [ KIEV_ID, 'name' ]), 'name must be "Киев"');
                    })
                    .then(function() {
                        return region.setId(MOSCOW_ID, SOURCES.EXTRA)
                            .then(() => region.ensureAllProps());
                    })
                    .then(function() {
                        assert.equal(region.name, getExpected('name', [ MOSCOW_ID, 'name' ]), 'name must be "Москва"');
                        done();
                    })
                    .fail(done);
            });
        });
    });

    describe('filterIds', function() {
        var region;

        beforeEach(function(done) {
            initRegion().then(function(initializedRegion) {
                region = initializedRegion;
                done();
            }).fail(done);
        });

        it('should retain only valid ids', function() {
            return assert.eventually.deepEqual(
                region.filterIds([ MOSCOW_ID, 888888, KIEV_ID, 999999 ]),
                [ MOSCOW_ID, KIEV_ID ]
            );
        });

        it('should work when all ids are valid', function() {
            var validIds = [ MOSCOW_ID, KIEV_ID ];
            return assert.eventually.deepEqual(
                region.filterIds(validIds),
                validIds
            );
        });

        it('should work with empty ids', function() {
            return assert.eventually.deepEqual(
                region.filterIds([]),
                []
            );
        });

        it('should work when all ids are invalid', function() {
            return assert.eventually.deepEqual(
                region.filterIds([ 888888, 999999 ]),
                []
            );
        });

        it('should filter invalid data types', function() {
            return assert.eventually.deepEqual(
                region.filterIds([ {}, KIEV_ID, [] ]),
                [ KIEV_ID ]
            );
        });

        it('should work correctly if argument is not an array', function() {
            return vow.all([
                assert.eventually.deepEqual(region.filterIds([ {} ]), []),
                assert.eventually.deepEqual(region.filterIds([ KIEV_ID ]), [ KIEV_ID ])
            ]);
        });

        it('should filter hidden region ids', function() {
            return assert.eventually.deepEqual(
                region.filterIds([ YANDEX_ID, MOSCOW_ID ]),
                [ MOSCOW_ID ]
            );
        });

        it('should accept variable number of arguments', function() {
            return assert.eventually.deepEqual(
                region.filterIds([ 888888, MOSCOW_ID, 999999 ]),
                [ MOSCOW_ID ]
            );
        });
    });

    describe('_getLocationPrecision', function() {
        var region;

        beforeEach(function(done) {
            initRegion().then(function(initializedRegion) {
                region = initializedRegion;
                done();
            });
        });

        it('should detect location precision by region type', function() {
            assert.strictEqual(region._getLocationPrecision(3), 4);
        });

        it('should return default precision in case of unknown region type', function() {
            assert.strictEqual(region._getLocationPrecision(31415), 0);
        });
    });

    describe('detecting ids from url', function() {
        it('should not set any url ids if url was not provided', function(done) {
            initRegion().then(function(region) {
                assert.isUndefined(region.urlId, 'urlId');
                assert.isUndefined(region.idsBySource(SOURCES.URL), 'idsBySource');
                assert.strictEqual(region.urlIds.length, 0, 'urlIds');
                done();
            }).fail(done);
        });

        it('should set id, source and urlId, if url contains id', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/?rid=' + KIEV_ID
            }).then(function(region) {
                assert.strictEqual(region.id, KIEV_ID, 'id');
                assert.strictEqual(region.urlId, KIEV_ID, 'urlId');
                assert.deepEqual(region.urlIds, [ KIEV_ID ], 'urlIds');
                assert.deepEqual(region.idsBySource(SOURCES.URL), [ KIEV_ID ], 'idsBySource');
                done();
            }).fail(done);
        });

        it('should set current source to url, if region was provided in url', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/'
            }).then(function(region) {
                assert.strictEqual(region.source, SOURCES.DETECTED); // source from ip
            }).then(function() {
                initRegion({
                    url: 'http://market.yandex.ru/?rid=' + KIEV_ID
                }).then(function(region) {
                    assert.strictEqual(region.source, SOURCES.URL);
                    done();
                }).fail(done);
            }).fail(done);
        });

        it('should be able to determine region id from custom url parameter', function(done) {
            initRegion({
                regionParams: { regionParamName: 'lr' },
                url: 'http://market.yandex.ru/?lr=' + KIEV_ID
            }).then(function(region) {
                assert.strictEqual(region.urlId, KIEV_ID);
                done();
            }).fail(done);
        });

        it('should handle multiple ids in url', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/?rid=' + KIEV_ID + '&rid=' + MOSCOW_ID
            }).then(function(region) {
                assert.deepEqual(region.urlIds, [ KIEV_ID, MOSCOW_ID ]);
                done();
            }).fail(done);
        });

        it('should filter invalid ids', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/?rid=888888&rid=' + KIEV_ID + '&rid=999999&rid=' + MOSCOW_ID
            }).then(function(region) {
                assert.deepEqual(region.urlIds, [ KIEV_ID, MOSCOW_ID ]);
                done();
            }).fail(done);
        });

        it('should not set ids and source if all of them are invalid', function(done) {
            initRegion({
                url: 'http://market.yandex.ru/?rid=888888&rid=999999'
            }).then(function(region) {
                assert.strictEqual(region.id, MOSCOW_ID, 'id');
                assert.isUndefined(region.urlId, 'urlId');
                assert.deepEqual(region.urlIds, [], 'urlIds');
                assert.isUndefined(region.idBySource(SOURCES.URL), 'idBySource');
                assert.strictEqual(region.source, SOURCES.DETECTED, 'source');
                done();
            }).fail(done);
        });
    });

    describe('SOURCES', function() {
        var region;

        beforeEach(function(done) {
            initRegion().then(function(initializedRegion) {
                region = initializedRegion;
                done();
            });
        });

        it('should allow adding own sources', function(done) {
            var CUSTOM_SOURCE_PRIORITY = 60;
            region.SOURCES.CUSTOM = CUSTOM_SOURCE_PRIORITY;
            region.setId(KIEV_ID, region.SOURCES.CUSTOM).then(function() {
                assert.strictEqual(region.id, KIEV_ID);
                assert.strictEqual(region.source, CUSTOM_SOURCE_PRIORITY);
                done();
            }).fail(done);
        });
    });

    describe('Portal cookies treatment', function() {
        // TODO: Добавить проверку вызовов мока геобазы через sinon
        var regionAsyncMock = {
            geobase: {
                getRegionByIp: function(ip) {
                    if (ip === mockRegion.kiev.clientIp) {
                        return vow.fulfill({ id: KIEV_ID });
                    }

                    if (ip === mockRegion.yandex.clientIp) {
                        return vow.fulfill({ id: YANDEX_ID, parent_id: MOSCOW_ID });
                    }

                    return vow.fulfill({ id: MOSCOW_ID });
                }
            }
        };

        it('should use yandex_gid value for pinpoint geolocation taken from cookie if cookie does exist', function(done) {
            helpers.getYandexGidWithFallback(regionAsyncMock, { yandex_gid: VLADIVOSTOK_ID }, mockRegion.yandex.clientIp).then(function(gid) {
                assert.strictEqual(gid, VLADIVOSTOK_ID);
                done();
            }).fail(done);
        });

        it('should use yandex_gid value for pinpoint geolocation taken from ip if cookie does not exist', function(done) {
            helpers.getYandexGidWithFallback(regionAsyncMock, {}, mockRegion.kiev.clientIp).then(function(gid) {
                assert.strictEqual(gid, KIEV_ID);
                done();
            }).fail(done);
        });

        it('should use real non-yandex region id for pinpoint if no yandex_gid cookie and user has yandex client ip', function(done) {
            helpers.getYandexGidWithFallback(regionAsyncMock, {}, mockRegion.yandex.clientIp).then(function(gid) {
                assert.strictEqual(gid, MOSCOW_ID);
                done();
            }).fail(done);
        });
    });
});
