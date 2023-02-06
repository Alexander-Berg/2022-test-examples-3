var Text = require('../../Text/Text'),
    str = '  TEST  ';

describe('String trim methods', function() {
    describe('#trim', function() {
        it('should return string without start/end spaces', function() {
            Text.trim(str).should.equal('TEST');
        });

        it('should throw TypeError', function() {
            (function() { Text.trim(null) }).should.throw(TypeError);
        });
    });

    describe('#ltrim', function() {
        it('should return string without start spaces', function() {
            Text.ltrim(str).should.equal('TEST  ');
        });

        it('should throw TypeError', function() {
            (function() { Text.ltrim(0) }).should.throw(TypeError);
        });
    });

    describe('#rtrim', function() {
        it('should return string without end spaces', function() {
            Text.rtrim(str).should.equal('  TEST');
        });

        it('should throw TypeError', function() {
            (function() { Text.rtrim({}) }).should.throw(TypeError);
        });
    });
});
