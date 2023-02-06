'use strict';

const mock = require('mock-require');
const secretsMock = require('../../__mocks__/secrets.js');

mock(require.resolve('../../secrets.js'), secretsMock);
