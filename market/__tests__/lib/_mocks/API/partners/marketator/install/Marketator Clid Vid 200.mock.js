'use strict';

const ApiMock = require('./../../../../../ApiMock');

const host = 'https://install.partners';

const pathname = '/Services/pixel';

const query = {
    aid: '231',
    oid: '231',
    ts: /[0-9]+/,
    pid: '777',
    sid: '',
    geo: '',
    hid: '',
    ip: '',
    dt: ''
};

const result = {
    status: 200
};

module.exports = new ApiMock(host, pathname, query, result);
