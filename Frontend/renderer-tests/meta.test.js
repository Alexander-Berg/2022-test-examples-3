/* jscs:disable requireMultipleVarDecl */
global.assert = require('chai').assert;

describe('meta', function() {
    'use strict';

    const meta = require('../meta');
    const data = require('./fixtures/data.json');

    it('should return object with specific properties', function() {
        assert.deepEqual(meta(data), {
            cookie: 'key1=value1; key2=value2',
            reqid: '1488284272707434-272549716717672857936072-ws29-315',
            entry: 'post-search',
            referer: null,
            host: 'hamster.yandex.ru',
            uri: '/redir_warning?url=http%3A%2F%2Fhghltd.yandex.net%2F&exp_flags=redir_warning',
            userAgent: 'Mozilla/5.0',
            httpMethod: 'GET',
            httpXForwardedFor: '2a02:6b8:0:2307:b82c:1b4f:fe2e:4944',
            httpXRealIp: '2a02:6b8:0:2307:b82c:1b4f:fe2e:4944',
            userId: '2719843521465988424',
            userInterface: 'DESKTOP',
            userIp: '2a02:6b8:0:2307:b82c:1b4f:fe2e:4944',
            userTime: 1491583392000,
            isInternalRequest: false,
            isSuspectedRobot: false,
            staticVersion: 1
        });
    });

    it('should return object with correct property userInterface for tablet', function() {
        let tabletData = Object.assign({}, data, { reqdata: { device: 'tablet' } });

        assert.propertyVal(meta(tabletData), 'userInterface', 'PAD');
    });
});
