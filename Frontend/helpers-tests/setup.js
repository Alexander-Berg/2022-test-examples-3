'use strict';

const chai = require('chai');
const sinon = require('sinon');

global.assert = chai.assert;
global.expect = chai.expect;

global.sinon = sinon;
sinon.assert.expose(chai.assert, { prefix: '' });
