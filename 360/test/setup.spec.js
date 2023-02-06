'use strict';

// const mocha = require('mocha');
const chai = require('chai');
const sinon = require('sinon');
const nock = require('nock');

chai.use(require('sinon-chai'));
chai.config.includeStack = true;

nock.disableNetConnect();

global.expect = chai.expect;
global.nock = nock;
global.sinon = sinon;
global.clock = sinon.useFakeTimers();

before(() => {
    global.window = {};
});

after(() => {
    sinon.restore();
    nock.cleanAll();
    clock.restore();
});
