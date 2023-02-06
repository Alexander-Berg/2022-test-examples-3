'use strict';

const md5hash = require('./md5-hash.js');

test('returns md5 hash :)', function() {
    expect(md5hash('hello')).toBe('5d41402abc4b2a76b9719d911017c592');
});
