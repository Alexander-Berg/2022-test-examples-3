'use strict';

require('events').EventEmitter.defaultMaxListeners = 100;

// в юнит тестах модули замоканы, для интеграционных нужны честные модули
jest.unmock('@yandex-int/duffman');
jest.unmock('crypto');

// mock backend hosts
jest.mock('@ps-int/mail-lib/config/global-config/global-config.js', () => {
    return require('../../__mocks__/global-config.json');
});
jest.mock('../../internal-lib/config/global-config/global-config.js', () => {
    return require('../../__mocks__/global-config.json');
});
// enable duffman debug mode
jest.mock('@yandex-int/duffman/lib/options.js', () => ({
    dev: Object.prototype.hasOwnProperty.call(process.env, 'DEBUG')
}));
// stub vdirect keys
jest.mock('../../lib/vdirect.js');
jest.mock('../../config/vdirect.js', () => ({
    path: require.resolve('./vdirectkeys.txt')
}));
// stub shared secrets
jest.mock('../../secrets.js');

// stub tvm middleware
jest.mock('../../internal-lib/middlewares/middleware-tvm2.js');

const nock = require('nock');

// cut off network
nock.disableNetConnect();
nock.enableNetConnect('127.0.0.1');

// helpers
global.testApp = require('./lib/test-app.js');
global.testMethod = require('./lib/test-method.js');
global.testModel = require('./lib/test-model.js');

// required for stable ckey generation
Date.now = () => 1540487881000;

jest.setTimeout(90000);
