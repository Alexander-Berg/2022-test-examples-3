const Text = require('../../Text/Text');
const assert = require('assert');

describe('String hash methods', function() {
    describe('#hash', function() {
        it('should return string "45h"', function() {
            assert.equal(Text.hash(''), '45h');
        });

        it('should return string "375kp1"', function() {
            assert.equal(Text.hash('abc'), '375kp1');
        });

        it('should throw TypeError', function() {
            (function() { Text.hash(null) }).should.throw(TypeError);
        });
    });

    describe('#hash2', function() {
        it('should return string "45h"', function() {
            assert.equal(Text.hash2(''), '45h');
        });

        it('should return string "375fut"', function() {
            assert.equal(Text.hash2('abc'), '375fut');
        });

        it('should throw TypeError', function() {
            (function() { Text.hash2(null) }).should.throw(TypeError);
        });
    });
});
