'use strict';

const path = require('path');
const mockRequire = require('mock-require');

mockRequire(path.resolve(__dirname, '../../lib/secrets.json'), {
    ckeyHmacKeys: []
});
