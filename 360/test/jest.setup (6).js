'use strict';

const matchers = require('jest-extended');
expect.extend(matchers);

require('events').EventEmitter.defaultMaxListeners = 100;

jest.mock('@yandex-int/duffman/lib/options.js', () => ({
    dev: Boolean(process.env.DEBUG)
}));

const nock = require('nock');

nock.disableNetConnect();
nock.enableNetConnect('localhost');
