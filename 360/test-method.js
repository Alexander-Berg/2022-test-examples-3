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

    const ver = filename.split('/').slice(-3, -2);
    const method = filename.split('/').slice(-1)[0].split('.')[0];

    const opts = deepExtend({
        nocks: config.nocks || [],
        method: 'POST',
        path: `/${ver}/${method}`,
        reqheaders: {
            'user-agent': 'TEST_USER_AGENT',
            'x-https-request': 'yes',
            'x-original-host': 'TEST.HOST',
            'x-real-ip': '123.123.123.123',
            'x-request-id': 'TEST_REQUEST_ID',
            'x-api-method': method,
            'authorization': 'OAuth XXX',
            ...config.reqheaders
        },
        query: {
            client: 'aphone',
            client_version: '1.2.3',
            uuid: 'TEST_CONNECTION_ID'
        },
        body: config.params || {},
        headerFilter: [ 'content-type', 'msearch-status' ]
    }, config);

    return testApp(app, opts);
}

module.exports = testMethod;
