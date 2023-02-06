'use strict';

const mock = require('mock-require');
const configMock = require('../../__mocks__/global-config.json');

mock(require.resolve('@ps-int/mail-lib/config/global-config/global-config.js'), configMock);
