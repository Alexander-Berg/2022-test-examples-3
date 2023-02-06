'use strict';

const path = require('path');
const deepExtend = require('deep-extend');
const getStack = require('./get-stack.js');
const testApp = require('./test-app.js');

/**
 * Runs API model test.
 *
 * @param {object} config - Test config.
 * @param {string} config.method - HTTP method.
 * @param {string} config.path - Request path.
 * @param {object} config.reqheaders - Request headers.
 * @param {object} config.query - Request query.
 * @param {object} config.body - Request body.
 * @param {string[]} config.nocks - Nock definition locations.
 * @param {string[]} config.headerFilter - Response header filter.
 * @param {object} config.params - Model params.
 */
function testModel(config) {
    const app = require('./app.js');
    const filename = getStack()[2].getFileName();
    config = config || {};

    const routePath = filename.includes('models-touch') ? '/models/touch1' : '/models/liza1';

    return testApp(app, deepExtend({
        method: 'POST',
        path: routePath,
        reqheaders: {
            'cookie': 'Session_id=TEST_SESSION_ID; i=98765432109876543210; yandexuid=12345678901234567890',
            'user-agent': 'TEST_USER_AGENT',
            'x-https-request': 'yes',
            'x-original-host': 'TEST.HOST',
            'x-real-ip': '123.123.123.123',
            'x-real-port': '63666',
            'x-request-id': 'TEST_REQUEST_ID'
        },
        body: {
            _ckey: 'U1TQEGGbXUzUPDnwzDdlyf4KUec=!jnqa5ezs',
            _connection_id: 'TEST_CONNECTION_ID',
            _eexp: '102710,0,27;105621,0,17;94810,0,50;87574,0,87',
            _exp: '102710,0,27;105621,0,17;94581,0,67;94810,0,50;87574,0,87;85940,0,46;85008,0,9',
            _service: 'TEST_CLIENT_NAME',
            _version: 'TEST_CLIENT_VERSION',
            models: [
                {
                    name: path.basename(filename, '.integration.js'),
                    params: config.params
                }
            ]
        },
        headerFilter: [ 'content-type' ]
    }, config));
}

module.exports = testModel;
