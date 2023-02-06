'use strict';

// mock @yandex-int/csrf
jest.mock(
  '@yandex-int/csrf',
  () => {
    return require('./lib/csrf-stub');
  },
  { virtual: true }
);

const nock = require('nock');

// cut off network
nock.disableNetConnect();
nock.enableNetConnect('127.0.0.1');

global.testApp = require('./lib/test-app.js');
global.testRoute = require('./lib/test-route');

jest.setTimeout(30000);
