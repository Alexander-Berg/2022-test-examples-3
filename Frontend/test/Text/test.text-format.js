var Text = require('../../Text/Text');

describe('String format methods', function() {
    describe('#supplant', function() {
        it('should return formatted string', function() {
            Text
                .supplant('Hello {first}, hello {second}, hello {third}', {
                    first: 1,
                    second: 'two'
                })
                .should
                .equal('Hello 1, hello two, hello {third}');
        });

        it('should throw TypeError', function() {
            (function() { Text.supplant(null, {}) }).should.throw(TypeError);
        });
    });

    describe('#firstUp', function() {
        it('should return string with first letter capitalized', function() {
            Text.firstUp('hello, hello').should.equal('Hello, hello');
        });

        it('should throw TypeError', function() {
            (function() { Text.firstUp(null) }).should.throw(TypeError);
        });

        describe('with highlight markers in string', function() {
            it('should return string with first letter capitalized if marker in the beginning', function() {
                Text.firstUp('\u0007[hello\u0007], there').should.equal('\u0007[Hello\u0007], there');
            });

            it('should return string with first letter capitalized if marker in the middle/end', function() {
                Text.firstUp('hello, \u0007[there\u0007]').should.equal('Hello, \u0007[there\u0007]');
            });

            it('should return empty string if empty string given', function() {
                Text.firstUp('').should.equal('');
            });
        });
    });
});
