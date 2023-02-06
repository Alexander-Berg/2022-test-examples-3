/* global it, describe */

/**
 * Created by unikoid on 15.01.16.
 */

let assert = require('assert');
let HttpUatraits = require('./').HttpUatraits;

let TESTING_HOST = process.env.UATRAITS_HOST || 'http://uatraits.qloud.yandex.ru';

let uatraits = new HttpUatraits(TESTING_HOST);

/* jshint mocha: true */
describe('HttpUatraits', function() {
    this.timeout(7000);
    it('should just work', function() {
        return uatraits.detect(
            'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1'
        ).then(function(traits) {
            assert.equal(traits.OSName, 'Windows 7');
            assert.equal(traits.BrowserName, 'Firefox');
        });
    });

    it('should support `X-OperaMini-Phone-UA` header', function() {
        let ua = 'Opera/9.80 (Android; Opera Mini/8.0.1807/36.1609; U; en) Presto/2.12.423 Version/12.16';
        return uatraits.detect(ua).then(function(traits1) {
            return uatraits.detect({
                'user-agent': ua,
                'X-OperaMini-Phone-UA': 'SonyEricssonK750i/R1AA Browser/SEMC-Browser/4.2 Profile/MIDP-2.0 Configuration/CLDC-1.1',
            }).then(function(traits2) {
                assert.notEqual(traits1.OSFamily, traits2.OSFamily);
                assert.equal(traits2.OSFamily, 'Unknown');
            });
        });
    });
});
