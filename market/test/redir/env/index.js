'use strict';

const { config } = require('dotenv');
const { resolve } = require('path');

const NODE_ENV = process.env.NODE_ENV || 'development';

let envPath = '';

switch (NODE_ENV) {
  case 'development':
    envPath = resolve(__dirname, './dev.env');
    break;

  default:
    envPath = resolve(__dirname, './dev.env');
    break;
}

module.exports = config({ path: envPath }).parsed;
