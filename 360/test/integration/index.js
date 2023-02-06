'use strict';

process.env.ENVIRONMENT_NAME = 'ci';
process.env.ENVIRONMENT_TYPE = 'testing';

process.env.QLOUD_TVM_INTERFACE_ORIGIN = 'http://tvm:1';
process.env.QLOUD_TVM_TOKEN = 'TVM_TOKEN';

const url = require('url');
const nock = require('nock');
const mockHttp = require('mock-http');
const _pick = require('lodash/pick');

const loadNockDefs = require('./load-nocks.js');

nock.disableNetConnect();

jest.mock('../../middlewares/tvm-headers.js');
jest.mock('../../config/environment.js');
jest.mock('/etc/yamail/u2709-conf.json', () => require('../../__mocks__/u2709-conf.json'), { virtual: true });

global.testApp = require('../../app.js');

global.testMethod = (config) => {
    const scopes = loadNockDefs(config.nocks, nock);

    const pathname = '/' + config.name;
    const query = Object.assign({
        client_name: 'test',
        client_version: '0.0.0-test',
        connection_id: 'connection_id'
    }, config.params);

    const req = {
        url: url.format({ pathname, query }),
        method: config.method || 'GET',
        headers: {
            'host': 'intapi-host',
            'x-real-ip': '1.2.3.4',
            'x-request-id': 'requestid',
            'x-ya-service-ticket': '3:serv:test',
            'x-ya-user-ticket': '3:user:test'
        }
    };

    const res = {};

    mockHttp(req, res);

    return new Promise((resolve, reject) => {
        res.on('end', () => resolve(res));
        global.testApp(req, res, reject);
    }).then((res) => {
        try {
            scopes.forEach((scope) => scope.done());
        } catch (e) {
            nock.cleanAll();
            throw e;
        }

        let data = res.getBuffer();
        if (/application\/json/.test(res.getHeader('Content-Type'))) {
            data = JSON.parse(data);
        }
        return {
            status: res.statusCode,
            headers: _pick(res._internal.headers, [ 'content-type' ]),
            body: data
        };
    }).then((result) => {
        expect(result).toMatchSnapshot();
    });
};
