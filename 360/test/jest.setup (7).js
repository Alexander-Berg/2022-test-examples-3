'use strict';

jest.mock('@yandex-int/duffman/lib/options.js', () => ({
    dev: Object.prototype.hasOwnProperty.call(process.env, 'DEBUG')
}));

jest.mock('@yandex-int/yandex-geobase', () => () => ({
    getRegionByIp: () => 213
}));

const nock = require('nock');

nock.disableNetConnect();
nock.enableNetConnect('127.0.0.1');
