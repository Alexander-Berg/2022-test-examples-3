'use strict';

const chai = require('chai');
const sinonChai = require('sinon-chai');

chai.use(sinonChai);
chai.config.includeStack = true;

global.expect = chai.expect;

beforeEach(function() {
    this.sinon = require('sinon').createSandbox();
});

afterEach(function() {
    this.sinon.restore();
});
