var Text = require('../../Text/Text');

describe('Punycode decoding', function() {
    describe('#fromPunycode', function() {
        it('should return same string', function() {
            Text.fromPunycode('www.yandex.com').should.equal('www.yandex.com');
        });

        it('should return decoded representation', function() {
            Text.fromPunycode('xn--d1acpjx3f.com').should.equal('яндекс.com');
        });

        it('should throw TypeError', function() {
            (function() { Text.fromPunycode(null) }).should.throw(TypeError);
        });
    });
});
