/* global it, describe */

let assert = require('assert');
let HttpLangdetect = require('./').HttpLangdetect;

let TESTING_HOST = process.env.LANGDETECT_HOST || 'http://langdetect.qloud.yandex.ru';

let detector = new HttpLangdetect(TESTING_HOST);

/**
 * Takes field names (each field as an argument) and returns a function
 * This function takes an object and throws an exception if an object keys differ from provided ones
 * @returns {Function}
 */
function checkFields() {
    let fields = Array.prototype.slice.call(arguments).sort().toString();
    return function(obj) {
        return assert.equal(Object.keys(obj).sort().toString(), fields);
    };
}

/*
    Since this client is just a thin wrapper for HTTP(S) API,
    and API server itself is just a thin wrapper for libgeobase5 (and we need to go deeper (c)),
    I beleive that we don't need tests for actual values that we receive.
    So just test that we are able to access API methods and API provides us with all needed fields.
 */

/* jshint mocha: true */
describe('HttpLangdetect should support:', function() {
    this.timeout(7000);
    it('`find()` method', function() {
        return detector.find({
            filter: 'ru,uk,kk',
            host: 'yandex.ru',
            accept_language: 'be',
            cookie: 4,
            geo_regions: '236,11119,40,225,10001,10000',
        }).then(checkFields('cookie_value', 'name', 'code'));
    });

    it('`findWithoutDomain()` method', function() {
        return detector.findWithoutDomain({
            filter: 'ru,uk,kk',
            host: 'yandex.ru',
            accept_language: 'be',
            cookie: 4,
            geo_regions: '236,11119,40,225,10001,10000',
            def_lang: 'ru',
        }).then(checkFields('cookie_value', 'name', 'code'));
    });

    it('`findDomainEx()` method', function() {
        return detector.findDomainEx({
            filter: 'com,com.tr,by,kz,ua',
            host: 'maps.yandex.ru',
            geo_regions: '102562,29322,84,10002,10000',
        }).then(checkFields('content_region', 'domain', 'found'));
    });

    it('`findDomain()` method', function() {
        return detector.findDomain({
            filter: 'com,com.tr,by,kz,ua',
            host: 'maps.yandex.ru',
            geo_regions: '102562,29322,84,10002,10000',
        }).then(checkFields('content_region', 'domain', 'found'));
    });

    it('`list()` method', function() {
        return detector.list({
            geo_regions: '102562,29322,84,10002,10000',
            def_lang: 'ru',
        }).then(function(resp) { return resp.every(checkFields('cookie_value', 'name', 'code')) });
    });

    it('`getInfo()` method', function() {
        return detector.getInfo({ lang: 'ru' }).then(checkFields('cookie_value', 'name', 'code'));
    });
});
