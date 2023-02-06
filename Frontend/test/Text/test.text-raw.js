var Text = require('../../Text/Text'),
    utfStr = 'Blah \u0007[TEST\u0007] \u0007[TEST\u0007] Blah',
    utfStr2 = 'Blah \u0007(TEST\u0007) \u0007(TEST\u0007) Blah',
    surrogatePairStr = 'Hello💚',
    strWithLink = '\u0007+\u0007[Гороскоп\u0007] для \u0007[Весов\u0007] на сегодня\u0007-.',
    strWithManyLinks = '\u0007+Ссылка 0\u0007-, \u0007+Ссылка 1\u0007-, \u0007+Ссылка 2\u0007-.';

describe('Converting raw data', function() {
    describe('#convertRaw', function() {
        it('should return the same string', function() {
            var input = 'Blah blah blah Бла бла бла';

            Text.convertRaw(input).should.equal(input);
        });

        it('should return string with <b>/</b> instead of \u0007[/\u0007]', function() {
            Text.convertRaw(utfStr, 0, '<b>', '</b>').should.equal('Blah <b>TEST</b> <b>TEST</b> Blah');
        });

        it('should return string with <b>/</b> instead of \u0007(/\u0007)', function() {
            Text.convertRaw(utfStr2, '<b>', '</b>').should.equal('Blah <b>TEST</b> <b>TEST</b> Blah');
        });

        it('should cut string properly', function() {
            Text.convertRaw(utfStr, 12, '<b>', '</b>').should.equal('Blah <b>TEST</b> <b>TE...</b>');
        });

        // TODO handle length as code points, not UTF-16 code units
        it('should drop high surrogate in end when pair was split in halves', function() {
            Text.convertRaw(surrogatePairStr, 6).should.equal('Hello...');
        });

        it('should return string with markers stripped', function() {
            Text.convertRaw(utfStr).should.equal('Blah TEST TEST Blah');
        });

        it('should return converted string (custom method)', function() {
            Text
                .convertRaw(utfStr, 0, function(str) {
                    return '<b>' + str + '</b>';
                })
                .should.equal('Blah <b>TEST</b> <b>TEST</b> Blah');
        });

        it('should not break symbols like %20', function() {
            var input = 'test%20 test';

            Text.convertRaw(input).should.equal(input);
        });

        it('should not cut link-marked-up string', function() {
            Text.convertRaw(strWithManyLinks, '<b>', '</b>').should.equal(strWithManyLinks);
        });

        it('should correctly process link-marked-up string', function() {
            Text
                .convertRaw(strWithLink, '<b>', '</b>')
                .should.equal('\u0007+<b>Гороскоп</b> для <b>Весов</b> на сегодня\u0007-.');
        });

        it('should correctly process link-marked-up string with length limit', function() {
            Text.convertRaw(strWithLink, 13, '<b>', '</b>').should.equal('\u0007+<b>Гороскоп</b> дл...');
        });

        it('should properly cut string if length limit less than length of chunk between markers', function() {
            Text
                .convertRaw(
                    'A pet dogs reaction to a family member returning home after 2 years... which in fairness is 14 years in dog years.',
                    50,
                    '<b>',
                    '</b>'
                )
                .should.equal('A pet dogs reaction to a family member returning h...');
        });

        it('should correctly process backslash and quotes', function() {
            Text.convertRaw('chip\\dale').should.equal('chip\\dale');
            Text.convertRaw('chip"dale').should.equal('chip&quot;dale');
            Text.convertRaw('chip\'dale').should.equal('chip&apos;dale');
            Text.convertRaw('chip`dale').should.equal('chip&#x60;dale');
        });

        it('should correctly process unicode chars', function() {
            Text.convertRaw('chip®dale').should.equal('chip®dale');
            Text.convertRaw('chip«dale').should.equal('chip«dale');
            Text.convertRaw('chip»dale').should.equal('chip»dale');
            Text.convertRaw('chip—dale').should.equal('chip—dale');
            Text.convertRaw('chip…dale').should.equal('chip…dale');
            Text.convertRaw('chipØdale').should.equal('chipØdale');
        });
    });
});
