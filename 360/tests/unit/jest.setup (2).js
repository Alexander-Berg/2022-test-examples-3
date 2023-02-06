const path = require('path');
const sinon = require('sinon');

const { rootDir } = require('./jest.config');

require('module-alias').addAliases({
  '@': path.resolve(__dirname, rootDir, 'server'),
  Constants: path.resolve(__dirname, rootDir, 'constants')
});

beforeEach(() => {
  global.sinon = sinon.createSandbox();
});

afterEach(() => {
  if (global.sinon) {
    global.sinon.restore();
  }
});

// Стаб csrf ключа
process.env.CSRF_KEY = 'XXX';

jest.setTimeout(30000);
