/* global describe */
/* global expect */
/* global it */
/* global beforeEach */
/* global afterEach */

describe('js/daria.messageprocess', function() {

    function checkRange(range, fnode, lnode, content) {
        if (Array.isArray(range)) {
            var fnodeCheck = document.createTextNode(fnode).isEqualNode(range[0]);
            var lnodeCheck = document.createTextNode(lnode).isEqualNode(range[1]);

            if (!fnodeCheck) {
                console.log('fnode:', $.text(range[0]));
                console.log('expect:', fnode);
            }

            if (!lnodeCheck) {
                console.log('lnode:', $.text(range[1]));
                console.log('expect:', lnode);
            }

            expect(fnodeCheck).to.be.ok;
            expect(lnodeCheck).to.be.ok;
        } else {
            expect($.trim(range.toString())).to.be.equal(content);
        }
    }

    describe('Поиск автора цитаты', function() {

        var checks = {
            '1. автор цитаты находится в одной текстовой ноде': [[
                '02.03.14, 19:29 Anton &lt;bufpost@yandex.ru&gt;:'
            ], [ '02.03.14, 19:29 Anton <bufpost@yandex.ru>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:' ]],

            '2. после строки с автором может находится любой пустой текст и переносы строки': [[
                '02.03.14, 19:29 Anton &lt;bufpost@yandex.ru&gt;:     \n\n\n   '
            ], [
                '02.03.14, 19:29 Anton <bufpost@yandex.ru>:     \n\n\n   ',
                '02.03.14, 19:29 Anton <bufpost@yandex.ru>:     \n\n\n   ',
                '02.03.14, 19:29 Anton <bufpost@yandex.ru>:'
            ]],

            '3. перед строкой с автором может находится любой пустой тег': [[
                '<div><br/><br/></div>',
                '<div><br/><br/></div>',
                '02.03.14, 19:29 Anton &lt;bufpost@yandex.ru&gt;:'
            ], [ '02.03.14, 19:29 Anton <bufpost@yandex.ru>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:' ]],

            '4. переносы строк в текстовой ноде заменяются на пробел': [[
                '02.03.14,\n19:29\nuser Anton:'
            ], [ '02.03.14,\n19:29\nuser Anton:', '02.03.14,\n19:29\nuser Anton:', '02.03.14,\n19:29\nuser Anton:' ]],

            '5. больше одного пробела подряд заменяется на пробел': [[
                '02.03.14,   19:29   From Anton:'
            ], [
                '02.03.14,   19:29   From Anton:', '02.03.14,   19:29   From Anton:', '02.03.14,   19:29   From Anton:'
            ]],

            '6. описание автора может состоять из нескольких текстовых нод': [[
                'From Anton:',
                ' 19:29 ',
                '02.03.14,'
            ], [ '02.03.14,', 'From Anton:', '02.03.14, 19:29 From Anton:']],

            '7. описание автора может содержать инлайновые ноды': [[
                '&gt;:',
                '<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>',
                ' Anton &lt;',
                ' 19:29',
                '<b>02.03.14,</b>'
            ], [ '02.03.14,', '>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:' ]],

            '8. поиск автора выполняется до максимально возможного совпадения - 1': [[
                '&gt;:',
                '<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>',
                ' Anton &lt;'
            ], [ ' Anton <', '>:', 'Anton <bufpost@yandex.ru>:' ]],

            '9. поиск автора выполняется до максимально возможного совпадения - 2': [[
                '&gt;:',
                '<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>',
                ' Anton &lt;',
                ' 19:29',
                '<b>02.03.14,</b>'
            ], [ '02.03.14,', '>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:' ]],

            '10. следующиеза нодой с описанием автора, незначащие текстовые ноды игнорируются': [
                [
                    '02.03.14, 19:29 Anton wrote:',
                    'test ',
                    'qwe '
                ],
                [ '02.03.14, 19:29 Anton wrote:', '02.03.14, 19:29 Anton wrote:', '02.03.14, 19:29 Anton wrote:' ],
                false
            ],

            '11. следующие за нодой с описанием автора, незначащие текстовые ноды добавляются к имени автора, если отсутствует дата': [[
                'Anton wrote:',
                'test ',
                'qwe ',
                '<div>123</div>'
            ], [ 'qwe ', 'Anton wrote:', 'qwe test Anton wrote:' ], false],

            '12. поиск актора выполняется только в первой строке, расположенной перед цитатой': [[
                ' 19:29 Anton wrote:',
                '02.03.14,',
                '<br/>',
                'qwe '
            ], [ '02.03.14,', ' 19:29 Anton wrote:', '02.03.14, 19:29 Anton wrote:' ], false],

            '13. переходом на новую строку считается любой блочный элемент': [[
                ' 19:29 Anton пишет:',
                '02.03.14,',
                '<div>test</div>',
                'qwe '
            ], [ '02.03.14,', ' 19:29 Anton пишет:', '02.03.14, 19:29 Anton пишет:' ], false],

            '14. строка с описанием автора может находится в ноде любой вложенности': [[
                '<div><p>02.03.14, 19:29 <b>Anton</b> &lt;<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>&gt;:</p></div>',
                '<div>test</div>'
            ], [ '02.03.14, 19:29 ', '>:', '02.03.14, 19:29 Anton <bufpost@yandex.ru>:' ], false],

            '15. автор может состоять из нескольких строк (макс. 4), разделенный br - 1': [[
                'wrote:',
                '<br>',
                '&lt;<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>&gt;',
                '<br>',
                'anton',
                '<br>',
                '02.03.14 19:29'
            ], [ '02.03.14 19:29', 'wrote:', '02.03.14 19:29anton<bufpost@yandex.ru>wrote:' ]],

            '16. автор может состоять из нескольких строк (макс. 4), разделенный br - 2': [[
                '&lt;<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>&gt;:',
                '<br>',
                'anton',
                '<br>',
                '02.03.14 19:29'
            ], [ '02.03.14 19:29', '>:', '02.03.14 19:29anton<bufpost@yandex.ru>:' ]],

            '17. автор может состоять из нескольких строк (макс. 4), разделенный br - 3': [[
                '&lt;<a href="mailto:bufpost@yandex.ru">bufpost@yandex.ru</a>&gt;:',
                '<br>',
                'anton',
                '<br>',
                '02.03.14 19:29',
                '<br>',
                'some text'
            ], [ '02.03.14 19:29', '>:', '02.03.14 19:29anton<bufpost@yandex.ru>:' ], false],

            '18. если автор состоит из нескольких строк, то между ними не должно быть пустых': [[
                'Anton wrote:',
                '<br>',
                '<br>',
                'some text'
            ], [ 'Anton wrote:', 'Anton wrote:', 'Anton wrote:' ], false],

            '19. если автор состоит только из имени, то в имя автора не будут включаться строки перед ним': [[
                'Anton wrote:',
                '<br>',
                'some text'
            ], [ 'some text', 'Anton wrote:', 'Anton wrote:' ], false]
        };

        // Поиск автора перед цитатой
        describe('_findQuoteAuthor', function() {
            beforeEach(function() {
                this.$node = $('<div><blockquote></blockquote></div>');
                this.action = Daria.MessageProcess.Author._findQuoteAuthor;
            });

            afterEach(function() {
                this.$node = null;
                this.action = null;
            });

            $.each(checks, function(nameTest, params) {
                it(nameTest, function() {
                    var that = this;
                    var range = params[1];

                    params[0].forEach(function(node) {
                        that.$node.prepend(node);
                    });

                    var author = this.action(this.$node.find('blockquote').get(0));
                    expect(author).to.be.an('object');
                    expect(author).to.have.keys('author', 'range');
                    checkRange(author.range, range[0], range[1], range[2]);
                });
            });
        });

        // Поиск автора внутри цитаты
        describe('_findQuoteAuthorInner', function() {

            beforeEach(function() {
                this.$node = $('<blockquote></blockquote>');
                this.action = Daria.MessageProcess.Author._findQuoteAuthorInner;
            });

            afterEach(function() {
                this.$node = null;
                this.action = null;
            });

            $.each(checks, function(nameTest, params) {
                if (params[2] === false) {
                    return true;
                }

                it(nameTest, function() {
                    var that = this;
                    var range = params[1];

                    params[0].reverse().forEach(function(node) {
                        that.$node.append(node);
                    });

                    this.$node.append('<br>');
                    this.$node.append('some text');

                    var author = this.action(this.$node.get(0));
                    expect(author).to.be.an('object');
                    expect(author).to.have.keys('author', 'range');
                    checkRange(author.range, range[0], range[1], range[2]);
                });
            });

            it('конец текста с автором соединен с основным текстом цитаты', function() {
                this.$node.append('02.03.14 19:29 Anton &lt;<a href="mailto:asd@asd.ru">asd@asd.ru</a>');
                this.$node.append('&gt;:some text');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.email).to.be.equal('asd@asd.ru');
                expect(author.author.name).to.be.equal('Anton');
                checkRange(author.range, '02.03.14 19:29 Anton <', 'asd@asd.ru', '02.03.14 19:29 Anton <asd@asd.ru');
            });

            it('окончание текста с автором соединено с текстом цитаты и вырезается при поиске с автором - 1', function() {
                this.$node.append('02.03.14 19:29 Anton &lt;[');
                this.$node.append('<a href="mailto:asd@asd.ru">asd@asd.ru</a>');
                this.$node.append('](mailto:');
                this.$node.append('<a href="mailto:asd@asd.ru">asd@asd.ru</a>');
                this.$node.append(')');
                this.$node.append('&gt;:some text');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.email).to.be.equal('asd@asd.ru');
                expect(author.author.name).to.be.equal('Anton');
                checkRange(author.range, '02.03.14 19:29 Anton <[', ')', '02.03.14 19:29 Anton <[asd@asd.ru](mailto:asd@asd.ru)');
                expect(this.$node.get(0).lastChild.nodeValue).to.be.equal('some text');
            });

            it('окончание текста с автором соединено с текстом цитаты и вырезается при поиске с автором - 2', function() {
                this.$node.append('02.03.14 19:29 Anton &lt;[');
                this.$node.append('<a href="mailto:asd@asd.ru">asd@asd.ru</a>');
                this.$node.append('](mailto:');
                this.$node.append('<a href="mailto:asd@asd.ru">asd@asd.ru</a>');
                this.$node.append(')&gt;:some text');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.email).to.be.equal('asd@asd.ru');
                expect(author.author.name).to.be.equal('Anton');
                checkRange(author.range, '02.03.14 19:29 Anton <[', 'asd@asd.ru', '02.03.14 19:29 Anton <[asd@asd.ru](mailto:asd@asd.ru)');
                expect(this.$node.get(0).lastChild.nodeValue).to.be.equal('some text');
            });

            it('если перед автором внутри цитаты находится блочная нода, то автора не определяем', function() {
                this.$node.append('<div>123</div>');
                this.$node.append('02.03.14 19:29 user Anton:');
                this.$node.append('<br>');
                this.$node.append('some text');

                var author = this.action(this.$node.get(0));
                expect(author).to.be.equal(null);
            });

            it('автор расположен в блочной ноде', function() {
                this.$node.append('<div>02.03.14 19:29 Anton пишет:</div>');
                this.$node.append('<br>');
                this.$node.append('some text');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.name).to.be.equal('Anton');
            });

            it('автор в инлайновой ноде после блочной', function() {
                this.$node.append('<div>123</div>');
                this.$node.append('<b>02.03.14 19:29 From Anton:</b>');
                this.$node.append('<br>');
                this.$node.append('some text');

                var author = this.action(this.$node.get(0));
                expect(author).to.be.equal(null);
            });

            it('автор в инлайновой ноде внутри блочной', function() {
                this.$node.append('<div><b>02.03.14 19:29 From Anton:</b></div>');
                this.$node.append('some text');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.name).to.be.equal('Anton');
            });

            it('глубокая вложенность - 1', function() {
                this.$node.append('<div><div><div><b>02.03.14 19:29 Anton wrote:</b></div>some text</div></div>');

                var author = this.action(this.$node.get(0));

                expect(author).to.be.an('object');
                expect(author).to.have.keys('author', 'range');
                expect(author.author.name).to.be.equal('Anton');
            });

            it('глубокая вложенность - 2', function() {
                this.$node.append('<div><div>123<div><b>02.03.14 19:29 Anton wrote:</b></div>some text</div></div>');

                var author = this.action(this.$node.get(0));
                expect(author).to.be.equal(null);
            });
        });

    });

    describe('_parseAuthorText', function() {
        beforeEach(function() {
            this.action = Daria.MessageProcess.Author.parseAuthorText;
        });

        afterEach(function() {
            this.action = null;
        });

        describe('_parseAuthorText date', function() {
            var checks = [
                '    02.03.14, 19:29 Anton <bufpost@yandex.ru>',
                '02.03.14, 19:29 Anton <bufpost@yandex.ru>',
                'On 02.03.14, 19:29 Anton <bufpost@yandex.ru>',
                '02.03.2014, 19:29 Anton <bufpost@yandex.ru>',
                'On 02.03.2014, 19:29 Anton <bufpost@yandex.ru>',
                '02/03/14, 19:29 Anton <bufpost@yandex.ru>',
                'On 02/03/14, 19:29 Anton <bufpost@yandex.ru>',
                '02/03/2014, 19:29 Anton <bufpost@yandex.ru>',
                'On 02/03/2014, 19:29 Anton <bufpost@yandex.ru>',
                '02-03-14, 19:29 Anton <bufpost@yandex.ru>',
                'On 02-03-14, 19:29 Anton <bufpost@yandex.ru>',
                '02-03-2014, 19:29 Anton <bufpost@yandex.ru>',
                'On 02-03-2014, 19:29 Anton <bufpost@yandex.ru>',
                '2014.03.02, 19:29 Anton <bufpost@yandex.ru>',
                'On 2014.03.02, 19:29 Anton <bufpost@yandex.ru>',
                '2014/03/02, 19:29 Anton <bufpost@yandex.ru>',
                'On 2014/03/02, 19:29 Anton <bufpost@yandex.ru>',
                '2014-03-02, 19:29 Anton <bufpost@yandex.ru>',
                'On 2014-03-02, 19:29 Anton <bufpost@yandex.ru>',
                'Среда, 02 марта 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'On Среда, 02 марта 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                '02 марта 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'Mon, Mar 02, 2014 at 2:16 PM, Автор <345345345345@ya.ru> wrote:',
                'On Mon, Mar 02, 2014 at 2:16 PM, Автор <345345345345@ya.ru> wrote:',
                'Mar 02, 2014 at 2:16 PM, Автор <345345345345@ya.ru> wrote:',
                'On 02 марта 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'On 02 мар. 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'март 02 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'мар. 02 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'On март 02 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'On мар. 02 2014, 17:32 +02:00 от Anton <rikishi-work2@ya.ru>',
                'On 03/02/2014 07:29 PM, Anton wrote',

                '* Anton [02.03.14, 19:29]',
                '* Anton [On 02.03.14, 19:29]',
                '* Anton [02.03.2014, 19:29]',
                '* Anton [On 02.03.2014, 19:29]',
                '* Anton [02/03/14, 19:29]',
                '* Anton [On 02/03/14, 19:29]',
                '* Anton [02/03/2014, 19:29]',
                '* Anton [On 02/03/2014, 19:29]',
                '* Anton [02-03-14, 19:29]',
                '* Anton [On 02-03-14, 19:29]',
                '* Anton [02-03-2014, 19:29]',
                '* Anton [On 02-03-2014, 19:29]',
                '* Anton [2014.03.02, 19:29]',
                '* Anton [On 2014.03.02, 19:29]',
                '* Anton [2014/03/02, 19:29]',
                '* Anton [On 2014/03/02, 19:29]',
                '* Anton [2014-03-02, 19:29]',
                '* Anton [On 2014-03-02, 19:29]',
                '* Anton [Среда, 02 марта 2014, 17:32 +02:00]',
                '* Anton [Среда, 02 мар. 2014, 17:32 +02:00]',
                '* Anton [On Среда, 02 марта 2014, 17:32 +02:00]',
                '* Anton [02 марта 2014, 17:32 +02:00]',
                '* Автор [Mon, Mar 02, 2014 at 2:16 PM]',
                '* Автор [On Mon, Mar 02, 2014 at 2:16 PM]',
                '* Автор [Mar 02, 2014 at 2:16 PM]',
                '* Anton [On 02 марта 2014, 17:32 +02:00]',
                '* Anton [март 02 2014, 17:32 +02:00]',
                '* Anton [On март 02 2014, 17:32 +02:00]',
                '* Anton [On мар. 02 2014, 17:32 +02:00]',
                '* Anton [мар. 02 2014]'
            ];

            var date = Date.UTC(2014, 2, 2);

            checks.forEach(function(check) {
                it(check, function() {
                    expect(this.action(check).datetime.ts).to.be.equal(date);
                });
            });
        });

        describe('_parseAuthorText time', function() {
            var checks = [
                [ '02-03-2014, 19:29 от Anton <rikishi-work2@ya.ru>',           '19:29' ],
                [ '02-03-2014, at 19:29 от Anton <rikishi-work2@ya.ru>',        '19:29' ],
                [ '02-03-2014, в 19:29 от Anton <rikishi-work2@ya.ru>',         '19:29' ],
                [ '02-03-2014, в 19:29 +04:00 от Anton <rikishi-work2@ya.ru>',  '19:29 +04:00' ],
                [ '02-03-2014, в 19:29 -04:00 от Anton <rikishi-work2@ya.ru>',  '19:29 -04:00' ],
                [ '02-03-2014, 9:29 от Anton <rikishi-work2@ya.ru>',            '9:29' ],
                [ '02-03-2014, 12:29:12 от Anton <rikishi-work2@ya.ru>',        '12:29:12' ],
                [ '02-03-2014, 2:29 am от Anton <rikishi-work2@ya.ru>',         '2:29 am' ],
                [ '02-03-2014, 2:29 pm от Anton <rikishi-work2@ya.ru>',         '2:29 pm' ],
                [ '02-03-2014, 12:29 GMT+04:00 от Anton <rikishi-work2@ya.ru>', '12:29 GMT+04:00' ],

                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 19:29]',           '19:29' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, at 19:29]',        '19:29' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, в 19:29]',         '19:29' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, в 19:29 +04:00]',  '19:29 +04:00' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, в 19:29 -04:00]',  '19:29 -04:00' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 9:29]',            '9:29' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 12:29:12]',        '12:29:12' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 2:29 am]',         '2:29 am' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 2:29 pm]',         '2:29 pm' ],
                [ '* Anton <rikishi-work2@ya.ru> [02-03-2014, 12:29 GMT+04:00]', '12:29 GMT+04:00' ]
            ];

            checks.forEach(function(check) {
                it(check[0], function() {
                    expect(this.action(check[0]).datetime.time).to.be.equal(check[1]);
                });
            });
        });

        describe('_parseAuthorText name', function() {
            var checks = [
                [ '02-03-2014, 12:29 GMT+04:00 от Anton <rikishi-work2@ya.ru>', 'Anton', 'rikishi-work2@ya.ru' ],
                [ '02-03-2014, 12:29 GMT+04:00 Anton <rikishi-work2@ya.ru>', 'Anton', 'rikishi-work2@ya.ru' ],
                [ '02-03-2014, 12:29 GMT+04:00 "Vasiliy Pupkin" <rikishi-work2@ya.ru>', 'Vasiliy Pupkin', 'rikishi-work2@ya.ru' ],
                [ '02-03-2014, 12:29 GMT+04:00 Vasiliy Pupkin <rikishi-work2@ya.ru>', 'Vasiliy Pupkin', 'rikishi-work2@ya.ru' ],
                [ '02-03-2014, 12:29 GMT+04:00 Vasiliy Pupkin <rikishi-work2@ya.ru> написал: ', 'Vasiliy Pupkin', 'rikishi-work2@ya.ru' ],
                [ '27.02.2014 17:12, Сундиев Андрей пишет:', 'Сундиев Андрей' ],
                // [ '27.02.2014 17:12, Сундиев Андрей:', 'Сундиев Андрей' ],
                [ '27.02.2014 17:12, "<Author>" <test@ya.ru>:', '<Author>', 'test@ya.ru' ],
                [ '27.02.2014 17:12, "<Author>" <test@ya.ru>":', '<Author>', 'test@ya.ru' ],
                [ '27.02.2014 17:12, "Author" <test@ya.ru>":', 'Author', 'test@ya.ru' ],
                [ '27.02.2014 17:12, Author <test@ya.ru>":', 'Author', 'test@ya.ru' ],
                [ '27.02.2014 17:12, <test@ya.ru>":', null, 'test@ya.ru' ],
                // [ '27.02.2014 17:12, Author":', 'Author' ],
                [ '27.02.2014 17:12, \\"<Author1>\\":', 'Author1', undefined ],
                [ '02.03.14 19:29 Anton <[asd@asd.ru](mailto:asd@asd.ru)>', 'Anton', 'asd@asd.ru' ],
                [ 'Автор wrote', 'Автор' ],
                [ 'Автор пишет', 'Автор' ],
                [ 'Автор написал', 'Автор' ],
                [ 'Автор написал(а)', 'Автор' ],
                [ '<test@yandex.ru> wrote', null, 'test@yandex.ru' ],
                [ '<test@yandex.ru> пишет', null, 'test@yandex.ru' ],
                [ '<test@yandex.ru> написал', null, 'test@yandex.ru' ],
                [ '<test@yandex.ru> написал(а)', null, 'test@yandex.ru' ],
                [ '* Author <test@ya.ru> [02-03-2014, 12:29]:', 'Author', 'test@ya.ru' ],
                [ '* Author test <test@ya.ru> [02-03-2014, 12:29]:', 'Author test', 'test@ya.ru' ],
                [ '* Author [02-03-2014, 12:29]:', 'Author' ],
                [ '* Author test [02-03-2014, 12:29]:', 'Author test' ],
                [ '* <test@ya.ru> [02-03-2014, 12:29]:', null, 'test@ya.ru' ],
                [ '* Author <test@ya.ru>', 'Author', 'test@ya.ru' ],
                [ '* Author test <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* от Author test <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* пользователь Author test <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* "Author" <test@ya.ru>', 'Author', 'test@ya.ru' ],
                [ '* "Author test" <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* \\"Author test\\" <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* от "Author test" <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                [ '* пользователь "Author test" <test@ya.ru>', 'Author test', 'test@ya.ru' ],
                // двойная кавычка перед двоеточием в конце
                [ '* Author <test@ya.ru> [02-03-2014, 12:29]":', 'Author', 'test@ya.ru' ],
                [ '* Author test <test@ya.ru> [02-03-2014, 12:29]":', 'Author test', 'test@ya.ru' ],
                [ '* Author [02-03-2014, 12:29]":', 'Author' ],
                [ '* Author test [02-03-2014, 12:29]":', 'Author test' ],
                [ '* <test@ya.ru> [02-03-2014, 12:29]":', null, 'test@ya.ru' ],
                [ '* Author <test@ya.ru>":', 'Author', 'test@ya.ru' ],
                [ '* Author test <test@ya.ru>":', 'Author test', 'test@ya.ru' ],
                [ '* Author test <[asd@asd.ru](mailto:asd@asd.ru)>:', 'Author test', 'asd@asd.ru' ],

                // [ '27.02.2014 17:12, "<Author1>"', 'Author1' ],
                [ '27.02.2014 17:12, Сундиев Андрей пишет: что-то много много пишем' ],
                [ 'Обычная строка перед тегм цитирования' ],
                [ '<test@yandex.ru>' ],
                [ '* Aut[hor <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '* Aut]hor <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '* Aut<hor <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '* Aut>hor <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '* Aut"hor <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '<test@ya.ru> [02-03-2014, 12:29]:' ],
                [ 'Author [02-03-2014, 12:29]:' ],
                [ 'Author <test@ya.ru> [02-03-2014, 12:29]:' ],
                [ '* Author:' ],
                [ '* <test@ya.ru>:' ]
            ];

            checks.forEach(function(check) {
                it(check[0], function() {
                    if (check[1]) {
                        expect(this.action(check[0]).name).to.be.equal(check[1]);
                    }

                    if (check[2]) {
                        expect(this.action(check[0]).email).to.be.equal(check[2]);
                    }

                    if (!check[1] && !check[2]) {
                        expect(this.action(check[0])).to.be.equal(null);
                    }
                });
            });
        });


    });

});

