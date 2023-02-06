const url = require('url');

const _ = require('lodash');
// eslint-disable-next-line import/order
const chai = require('chai');

const {assert} = chai;
const {should} = chai;
const {expect} = chai;
const Response = require('Response');
const tldjs = require('tldjs');
const mockRequire = require('mock-require');
const terror = require('terror');

const config = require('./mock/config');
// eslint-disable-next-line import/order
const mockStoutUsers = require('./mock/stoutUsers');

const moscowUser = mockStoutUsers.DEFAULT_USER;
const moscowUserWithKievLr = mockStoutUsers.KIEV_LR_USER;
const kievCrUser = mockStoutUsers.KIEV_CR_USER;
const kievUserOnlyWithHostname = mockStoutUsers.UA_HOSTNAME_ONLY;
const nonKubrUser = mockStoutUsers.NON_KUBR;
const unexistingLrUser = mockStoutUsers.UNEXISTING_LR;
const ruLrWithUaTld = mockStoutUsers.RU_LR_WITH_UA_TLD;
const userWithIp = mockStoutUsers.USER_WITH_IP;

// Мочим драйвера и фабрику

mockRequire('geobase-remote-driver', {});
mockRequire('geobase-native-driver', {});
mockRequire('../lib/geobaseLookupFactory',  './mock_modules/geobaseLookupFactory');

const Region = require('../lib/region/Region');
const initRegion = require('../lib/index');
const RegionInitializationError = require('../lib/errors');
const SOURCES = require('../lib/region/sources');

const MOSCOW_ID = 213;
const KIEV_ID = 143;
const RUSSIA_ID = 225;
const UKRAINE_ID = 187;
const YANDEX_ID = 9999;

// Устанавливаем кастомный логгер, чтобы не мусорить ожидаемыми ошибками

const InvalidProviderArgumentError = 'Provider failed with reason "Invalid argument"';
terror.setLogger(function (message, level) {
    if (level !== 'warn' && message.indexOf(InvalidProviderArgumentError) !== -1) {
        // eslint-disable-next-line no-console
        console.log(level, message);
    }
});

// eslint-disable-next-line
const geobaseFallback = initRegion.geobaseFallbackInit(config.geobase, console.log);

describe('region-initialization component', function () {
    describe('Factory', function () {
        it('should throw an exception without user object', function () {
            expect(initRegion.init)
                .to.throw(RegionInitializationError);
        });

        it('should return a function with correct user object', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback))
                .to.not.throw(RegionInitializationError)
                .and.to.be.an.instanceOf(Response.Queue);
        });

        it('should throw an exception wrong provider', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, 'I AM NOT AN OBJECT'))
                .to.throw(RegionInitializationError);
        });

        it('should throw an exception wrong provider values', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, {abc: 123}))
                .to.throw(RegionInitializationError);
        });

        it('should return a function with correct custom provider', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, {
                coolProviderMethod() {
                },
            }))
                .to.not.throw(RegionInitializationError)
                .and.to.be.an.instanceOf(Response.Queue);
        });

        it('should throw an exception with incorrect sources object', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, _, 'I AM NOT A CORRECT OBJECT'))
                .to.throw(RegionInitializationError);
        });

        it('should throw an exception with incorrect sources values', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, _, {coolSource: 'abc'}))
                .to.throw(RegionInitializationError);
        });

        it('should return a function with correct sources', function () {
            expect(_.partial(initRegion.init, moscowUser, geobaseFallback, _, {someCoolSource: 200}))
                .to.not.throw(RegionInitializationError)
                .and.to.be.an.instanceOf(Response.Queue);
        });

        it('should throw an exception with incomplete config', function () {
            const wrongConfig = {
                remote: 'abc',
                noNativeConf: 123,
            };
            // eslint-disable-next-line
            expect(_.partial(initRegion.init, moscowUser, wrongConfig, console.log, _, {coolSource: 'abc'}))
                .to.throw(RegionInitializationError);
        });
    });

    describe('Region class', function () {
        it('should return JSON in toString method', function () {
            const region = new Region(123, 321);
            assert.equal(JSON.stringify(region), region.toString());
        });
    });

    describe('initRegion', function () {
        it('should correctly resolve queue', function (done) {
            initRegion.init(moscowUser, geobaseFallback)
                .onResolve(function (region) {
                    assert.deepPropertyVal(region, 'region.info.id', MOSCOW_ID);
                    assert.deepPropertyVal(region, 'region.info.country', RUSSIA_ID);
                    assert.isNull(region.redirectUrl);
                    done();
                })
                .onReject(done);
        });

        it('should have located region (lr) as the most prior source', function (done) {
            initRegion.init(moscowUserWithKievLr, geobaseFallback)
                .onResolve(function (region) {
                    assert.deepPropertyVal(region, 'region.info.id', KIEV_ID);
                    assert.deepPropertyVal(region, 'region.info.country', UKRAINE_ID);
                    assert.isNotNull(region.redirectUrl);
                    assert.strictEqual(tldjs.getPublicSuffix(region.redirectUrl), 'ua');

                    assert.include(region.redirectUrl, 'lr=143', 'Located region should be 143');
                    assert.notInclude(region.redirectUrl, 'rtr=');
                    done();
                })
                .onReject(done);
        });

        it('should use settings (cr) if lr wasn\'t provided', function (done) {
            initRegion.init(kievCrUser, geobaseFallback)
                .onResolve(function (region) {
                    assert.deepPropertyVal(region, 'region.info.id', KIEV_ID);
                    assert.deepPropertyVal(region, 'region.info.country', UKRAINE_ID);
                    assert.isNotNull(region.redirectUrl);
                    assert.strictEqual(tldjs.getPublicSuffix(region.redirectUrl), 'ua');

                    assert.include(region.redirectUrl, 'lr=143', 'Located region should be 143');
                    assert.notInclude(region.redirectUrl, 'rtr=', 'There should be no rtr param');
                    done();
                })
                .onReject(done);
        });

        it('should use tld if both lr and cr weren\'t provided', function (done) {
            initRegion.init(kievUserOnlyWithHostname, geobaseFallback)
                .onResolve(function (region) {
                    assert.propertyVal(region, 'tld', 'ua');
                    assert.isNull(region.redirectUrl);
                    done();
                })
                .onReject(done);
        });

        it('should set tld to ru and capital to Moscow if tld is not KUBR', function (done) {
            initRegion.init(nonKubrUser, geobaseFallback)
                .onResolve(function (region) {
                    assert.propertyVal(region, 'tld', 'ru');
                    assert.deepPropertyVal(region, 'region.info.id', MOSCOW_ID);
                    assert.deepPropertyVal(region, 'region.info.country', RUSSIA_ID);
                    assert.isNull(region.redirectUrl);
                    done();
                })
                .onReject(done);
        });

        it('should throw an exception with unexisting region id', function (done) {
            initRegion.init(unexistingLrUser, geobaseFallback)
                .onResolve(done)
                .onReject(function () {
                    done();
                });
        });

        it('should resolve with russian redirectUrl if have Moscow LR and domain is UA', function (done) {
            initRegion.init(ruLrWithUaTld, geobaseFallback)
                .onResolve(function (region) {
                    assert.isNotNull(region.redirectUrl);
                    assert.strictEqual(tldjs.getPublicSuffix(region.redirectUrl), 'ru');

                    assert.include(region.redirectUrl, 'lr=213', 'Located region should be 213');
                    assert.notInclude(region.redirectUrl, 'rtr=', 'Should not include rtr');
                    done();
                })
                .onReject(done);
        });

        it('should have region by source IP', function (done) {
            initRegion.init(userWithIp, geobaseFallback)
                .onResolve(function (region) {
                    assert.deepProperty(region, 'region.user.regionAsync.idBySource');
                    assert.isFunction(region.region.user.regionAsync.idBySource);
                    assert.strictEqual(region.region.user.regionAsync.idBySource(SOURCES.IP), KIEV_ID);
                    done();
                })
                .onReject(done);
        });
    });
});
