/* eslint-disable max-len */

'use strict';

const match = require('./match');
const offers = require('./offers');
const outlets = require('./outlets');

module.exports = {
    match,
    offers,
    outlets,
    'Apple iPhone X 256Gb | 200': require('./Apple-iPhone-X-256Gb 200.mock'),
    'Apple iPhone SE 64Gb Silver A1662 | 200': require('./Apple-iPhone-SE-64Gb-Silver-A1662 200.mock'),
    'LG 43LJ510V | 200': require('./LG-43LJ510V 200.mock'),
};
