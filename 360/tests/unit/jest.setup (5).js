'use strict';

const _sinon = require('sinon');

// Init sinon sandbox
beforeEach(() => {
  global.sinon = _sinon.createSandbox();
});

afterEach(() => {
  if (global.sinon) {
    global.sinon.restore();
  }
});
