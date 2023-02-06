'use strict';

const deepExtend = require('deep-extend');
const getStack = require('./get-stack.js');
const testApp = require('./test-app.js');

/**
 * Runs API method test.
 *
 * @param {object} config - Test config.
 * @param {string} config.method - HTTP method.
 * @param {string} config.path - Request path.
 * @param {object} config.reqheaders - Request headers.
 * @param {object} config.query - Request query.
 * @param {object} config.body - Request body.
 * @param {string[]} config.nocks - Nock definition locations.
 * @param {string[]} config.headerFilter - Response header filter.
 * @param {object} config.params - Method params.
 */
function testMethod(config) {
    const app = require('./app.js');
    const filename = getStack()[2].getFileName();
    config = config || {};

    return testApp(app, deepExtend({
        method: 'POST',
        path: '/' + filename.split('/').slice(-3, -1).join('/'),
        reqheaders: {
            'cookie': 'Session_id=TEST_SESSION_ID; i=98765432109876543210; yandexuid=12345678901234567890',
            'user-agent': 'TEST_USER_AGENT',
            'x-https-request': 'yes',
            'x-original-host': 'TEST.HOST',
            'x-real-ip': '123.123.123.123',
            'x-real-port': '63666',
            'x-request-id': 'TEST_REQUEST_ID'
        },
        query: {
            client_name: 'TEST_CLIENT_NAME',
            client_version: 'TEST_CLIENT_VERSION',
            connection_id: 'TEST_CONNECTION_ID',
            search_client_name: 'TEST_SEARCH_CLIENT_NAME'
        },
        body: Object.assign({
            _ckey: 'U1TQEGGbXUzUPDnwzDdlyf4KUec=!jnqa5ezs',
            _eexp: '102710,0,27;105621,0,17;94810,0,50;87574,0,87',
            _exp: '102710,0,27;105621,0,17;94581,0,67;94810,0,50;87574,0,87;85940,0,46;85008,0,9'
        }, config.params),
        headerFilter: [ 'content-type' ]
    }, config));
}

module.exports = testMethod;
