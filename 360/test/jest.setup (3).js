'use strict';

// @see https://github.com/facebook/jest/issues/3884
jest.unmock('debug');

const nock = require('nock');

nock.disableNetConnect();
nock.enableNetConnect('127.0.0.1');
