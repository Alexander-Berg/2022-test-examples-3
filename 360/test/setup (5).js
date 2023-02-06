'use strict';

const chai = require('chai');
const sinon = require('sinon');

chai.use(require('sinon-chai'));
chai.use(require('chai-as-promised'));
chai.config.includeStack = true;

global.expect = chai.expect;
global.sinon = sinon;
