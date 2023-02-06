/* global describe, it */

var User = require('../lib/user'),
    assert = require('chai').assert,
    mock = require('./mock/browser');

describe('Browser component', function() {

    function browser(agentId) {
        return new User(
                { headers : { 'user-agent' : mock.headers['user-agents'][agentId] } },
                {},
                { browser : {} }
            )
            .init('browser');
    }

    it('IE7', function(done) {
        browser('desktop-ie7')
            .then(function(user) {
                assert.strictEqual(user.browser.BrowserName, 'MSIE');
                assert.strictEqual(user.browser.BrowserVersion, '7.0');
                assert.strictEqual(user.browser.BrowserEngine, 'Trident');
                assert.strictEqual(user.browser.isMobile, false);
                assert.strictEqual(user.browser.isTouch, false);
                assert.strictEqual(user.browser.isTablet, false);
                assert.strictEqual(user.browser.isOld, true);

                done();
            })
            .done();
    });

    it('desktop Safari', function(done) {
        browser('desktop-safari')
            .then(function(user) {
                assert.strictEqual(user.browser.BrowserName, 'Safari');
                assert.strictEqual(user.browser.BrowserVersion, '5.1');
                assert.strictEqual(user.browser.BrowserEngine, 'WebKit');
                assert.strictEqual(user.browser.isMobile, false);
                assert.strictEqual(user.browser.isTouch, false);
                assert.strictEqual(user.browser.isTablet, false);
                assert.strictEqual(user.browser.isOld, false);

                done();
            })
            .done();
    });

    it('iPhone Safari', function(done) {
        browser('iphone-safari')
            .then(function(user) {
                assert.strictEqual(user.browser.BrowserName, 'MobileSafari');
                assert.strictEqual(user.browser.BrowserVersion, '3.0');
                assert.strictEqual(user.browser.BrowserEngine, 'WebKit');
                assert.strictEqual(user.browser.isMobile, true);
                assert.strictEqual(user.browser.isTouch, true);
                assert.strictEqual(user.browser.isTablet, false);
                assert.strictEqual(user.browser.isOld, false);

                done();
            })
            .done();
    });

    it('iPad Safari', function(done) {
        browser('ipad-safari')
            .then(function(user) {
                assert.strictEqual(user.browser.BrowserName, 'MobileSafari');
                assert.strictEqual(user.browser.BrowserVersion, '4.0.4');
                assert.strictEqual(user.browser.BrowserEngine, 'WebKit');
                assert.strictEqual(user.browser.isMobile, true);
                assert.strictEqual(user.browser.isTouch, true);
                assert.strictEqual(user.browser.isTablet, true);
                assert.strictEqual(user.browser.isOld, false);

                done();
            })
            .done();
    });

});
