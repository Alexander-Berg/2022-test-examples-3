/* jshint -W100 */
var Text = require('../../Text/Text'),
    strWithLink = '\u0007+\u0007[Гороскоп\u0007] для \u0007[Весов\u0007] на сегодня\u0007-.',
    strWithManyLink = '\u0007+Ссылка 0\u0007-, \u0007+Ссылка 1\u0007-, \u0007+Ссылка 2\u0007-.',
    linkMaker = function(text, index) { return '<a href="//ya.ru/link/' + index + '">' + text + '</a>' };

describe('Converting raw data', function() {
    describe('#convertRawLink', function() {
        it('should wrap link-marked-up string into <a>...</a>  and work correct with convertRaw', function() {
            var expected = '<a href="//ya.ru/link/0"><b>Гороскоп</b> для <b>Весов</b> на сегодня</a>.';

            Text.convertRawLink(Text.convertRaw(strWithLink, '<b>', '</b>'), linkMaker).should.equal(expected);
        });

        it('should clear broken link-markup ', function() {
            var expected = '<b>Гороскоп</b> дл...';

            Text.convertRawLink(Text.convertRaw(strWithLink, 13, '<b>', '</b>'), linkMaker).should.equal(expected);
        });

        it('should pass correct index of link to linkMaker', function() {
            var expected = '<a href="//ya.ru/link/0">Ссылка 0</a>, <a href="//ya.ru/link/1">Ссылка 1</a>, <a href="//ya.ru/link/2">Ссылка 2</a>.';

            Text.convertRawLink(Text.convertRaw(strWithManyLink, '<b>', '</b>'), linkMaker).should.equal(expected);
        });
    });
});
