var Text = require('../../Text/Text');

describe('String/Num format methods', function() {
    describe('#splitThousands', function() {
        it('should return same number', function() {
            Text.splitThousands('1234', ' ', true).should.equal('1234');
        });

        it('should return formatted string (space between group of 3 numbers)', function() {
            Text.splitThousands(123456789).should.equal('123 456 789');
        });

        it('should return formatted string (space between group of 3 numbers)', function() {
            Text.splitThousands('123456789').should.equal('123 456 789');
        });

        it('should return formatted string (- sign between group of 3 numbers)', function() {
            Text.splitThousands(12345678, '-').should.equal('12-345-678');
        });
    });
});
