#!/usr/bin/env node
var assert = require('assert'),
    mycookie = require('../lib/mycookie'),
    cookie = 'YyMCAwUrAgKAsSwBAS4BATYBAQA=',
    blocks = {
        '35': [ 3, 5 ],
        '43': [ 2, 177 ],
        '44': [ 1 ],
        '46': [ 1 ],
        '54': [ 1 ]
    };

assert.deepEqual(mycookie.parse(cookie), blocks, 'parse');
assert.strictEqual(mycookie.serialize(blocks), cookie, 'serialize');
