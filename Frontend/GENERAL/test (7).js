/**
 * Created by unikoid on 18.12.15.
 */

/* global describe, it */

/* eslint-disable camelcase */

const expect = require('chai').expect;
const init = require('./');

/**
 * Because Promises catch errors, we should not do assertions right
 * inside of Promise control flow.
 * Otherwise we would have our tests failed due to timeout as 'done' callback has not been called,
 * not due to the actual assertion error
 * @param {function} expected - function that performs assertions/expectations
 * @returns {Function}
 */
function asyncExpect(expected) {
    return function() {
        setImmediate(expected);
    };
}

/**
 * Test middleware with provided request mock and assertions
 * @param {Object} req - http request mock
 * @param {function} expected - callback that performs assertions/expectations
 * @param {Object} [initOptions] - options passed to init
 * @returns {function} - mocha test
 */
function testRequest(req, expected, initOptions) {
    return function(done) {
        init(initOptions || {})(req, null, asyncExpect(() => {
            expected(req);
            done();
        }));
    };
}

/**
 * Test request object to contain correct region id after middleware execution
 * @param {Number} id - expected region id
 * @param {Object} req - http request (mock) object
 */
function expectReqRegion(id, req) {
    expect(req.geolocation.region_id).to.be.a('number');
    expect(req.geolocation.region_id).to.equal(id);
}

function expectReqRegionParents(parents, req) {
    expect(req.geolocation.parents).to.deep.equal(parents);
}

describe('express-http-geobase', () => {
    it('should use x-real-ip if present', testRequest({
        headers: { 'x-real-ip': '92.46.77.89', 'x-forwarded-for': 'invalid' },
        cookies: {},
    }, expectReqRegion.bind(null, 163)));

    it('should use xff header if present', testRequest({
        headers: { 'x-forwarded-for': '92.46.77.89' },
        cookies: {},
    }, expectReqRegion.bind(null, 163)));

    it('should use yandex_gid if present', testRequest({
        headers: {},
        cookies: { yandex_gid: '214' },
    }, expectReqRegion.bind(null, 214)));

    it('should use yp cookie if present', testRequest({
        headers: {},
        cookies: { yp: '2147483647.ygo.146:143#2147483647.gpauto.50_4511180:30_5223010:200:1:2147483647' },
    }, expectReqRegion.bind(null, 143)));

    it('should use ys cookie if present', testRequest({
        headers: {},
        cookies: { ys: 'ygo.146:143#gpauto.50_4511180:30_5223010:200:1:' + Date.now().toString().slice(0, -3) },
    }, expectReqRegion.bind(null, 143)));

    it('should get region parents', testRequest({
        headers: { 'x-real-ip': '92.36.94.80' },
        cookies: {},
    }, req => {
        expect(req.geolocation.parents).to.be.an.instanceof(Array);
        expect(req.geolocation.parents).to.have.length.above(0);
    }));

    it('should set traits to null if option is not provided', testRequest({
        headers: { 'x-forwarded-for': '37.140.187.219' },
        cookies: {},
    }, req => {
        expect(req.geolocation.traits).to.be.null;
    }));

    it('should provide truthy `is_yandex_net(staff)` in case of internal IP', testRequest({
        headers: { 'x-forwarded-for': '37.140.187.219, 8.8.8.8' },
        cookies: {},
    }, req => {
        expect(req.geolocation.traits.is_yandex_net).to.equal(true);
        expect(req.geolocation.traits.is_yandex_staff).to.equal(true);
    }, { traits: true }));

    it('should provide traits fallback in case of no IP', testRequest({
        headers: {},
        cookies: {},
    }, req => {
        expect(req.geolocation.traits.is_yandex_net).to.equal(false);
        expect(req.geolocation.traits.is_yandex_staff).to.equal(false);
    }), { traits: true });

    it('should not set error fields on success request', testRequest({
        headers: {},
        cookies: {},
    }, req => {
        /* eslint no-unused-expressions: 0 */
        expect(req.geolocation.isError).to.be.false;
        expect(req.geolocation.errors).to.be.undefined;
    }));

    // WARNING: this tests produce a lot of log messages, this is normal behaviour
    it('should provide fallback in case of errors', function(done) {
        this.timeout(7000);
        const req = { headers: { host: 'yandex.ru' }, cookies: {} };
        init({ server: 'error' })(req, null, () => {
            expectReqRegion(213, req);
            expectReqRegionParents([213, 1, 3, 225, 10001, 10000], req);
            return done();
        });
    });

    // WARNING: this tests produce a lot of log messages, this is normal behaviour
    it('should set error fields in case of errors', function(done) {
        this.timeout(7000);
        const req = { headers: { host: 'yandex.ru' }, cookies: {} };
        init({ server: 'error' })(req, null, () => {
            expect(req.geolocation.isError).to.equal(true);
            expect(req.geolocation.errors).to.have.property('pinpointGeolocation');
            expect(req.geolocation.errors).to.have.property('parents');

            return done();
        });
    });
});
