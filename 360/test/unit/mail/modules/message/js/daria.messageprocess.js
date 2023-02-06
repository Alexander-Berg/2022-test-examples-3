/* global describe */
/* global expect */
/* global it */
/* global beforeEach */
/* global afterEach */

describe('js/daria.messageprocess', function() {

    function htmlToNode(html) {
        return $(html)[0];
    }

    function compareNodes(resultNode, expectNode) {
        var checkResult = resultNode.isEqualNode(expectNode);
        if (!checkResult) {
            console.group(this.test.title);
            console.log('resultNode', resultNode);
            console.log('expectNode', expectNode);
            console.log('\n' + resultNode.innerHTML);
            console.log('\n' + expectNode.innerHTML);
            console.groupEnd(this.test.title);
        }
        expect(checkResult).to.be.ok;
    }

    describe('#attachmentsSelect', function() {
        beforeEach(function() {
            this.action = Daria.MessageProcess.attachmentsSelect;
        });

        $.each({
            'поиск ключевых слов в текстовых нодах письма': [
                {
                    'html': '<div>аттач</div>',
                    'result': 1
                },
                {
                    'html': '<div>аттач аттач</div>',
                    'result': 2
                },
                {
                    'html': '<div>аттач\nаттач</div>',
                    'result': 2
                },
                {
                    'html': '<div>qwe.аттач\nasd.аттач\n</div>',
                    'result': 2
                },
                {
                    'html': '<div><span>qwe.аттач\n</span><span>asd.аттач\n</span></div>',
                    'result': 2
                },
                {
                    'html': '<div><a html="#">qwe.аттач\n</a><span>asd.аттач\n</span></div>',
                    'result': 1
                },
                {
                    'html': '<div><blockquote>qwe.аттач\n</blockquote><span>asd.аттач\n</span></div>',
                    'result': 1
                },
                {
                    'html': '<div>Располагается на 10-м этаже 12-ти этажного дома, в котором, кстати, был капитальный ремонт, благодаря чему он обзавелся новыми коммуникациями и стеклопакетами во всех квартирах. Сама квартира очень уютная. В комнате и коридоре сделан косметический ремонт\n' +
                            'в светлых тонах (покрашены стены, &nbsp;новый пол, чистый потолок). Окна комнаты выходят на солнечную сторону (вид на необыкновенные рассветы прилагается). Установлен кондиционер, так что любова. В ванной комнате\n' +
                            'сделан капитальный ремонт, включающий новую плитку и новую сантехнику.</div>',
                    'result': 1
                }
            ]
        }, function(nameTest, checksList) {
            checksList.forEach(function(check, idx) {
                it(nameTest + ' ' + idx, function() {
                    var $node = $(check.html);
                    this.action($node[0]);
                    expect($node.find('.js-attachments-scroll').length).to.be.equal(check.result);
                });
            });
        });
    });

    describe('_findQuoteFirstLine', function() {
        beforeEach(function() {
            this.$node = $('<blockquote></blockquote>');
            this.action = Daria.MessageProcess._findQuoteFirstLine;
        });

        afterEach(function() {
            this.$node = null;
            this.action = null;
        });

        it('фестлайн - не более 200 первых символов текста', function() {
            this.$node.append('какой-то текст');
            expect(this.action(this.$node.get(0))).to.be.equal('какой-то текст');
        });

        it('берет только содержимое тегов', function() {
            this.$node.append('какая-то ');
            this.$node.append('<a href="#">ссылка</a>');
            expect(this.action(this.$node.get(0))).to.be.equal('какая-то ссылка');
        });

        it('вложенность тегов не влияет на поиск', function() {
            this.$node.append('<div><div><div>какой-то текст</div></div></div>');
            expect(this.action(this.$node.get(0))).to.be.equal('какой-то текст');
        });

        it('при каждом переходе на новую строчку проверяется длина фестлайна', function() {
            this.$node.append('<div>строка текста 1</div>');
            this.$node.append('<div>строка текста 2</div>');
            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1 строка текста 2');
        });

        it('при каждом переходе на новую строчку проверяется длина фестлайна, и поиск заканчивается, если длина более 60 символов', function() {
            this.$node.append('<div>строка текста 1</div>');
            this.$node.append('<div>строка текста 2</div>');
            this.$node.append('<div>строка текста 3</div>');
            this.$node.append('<div>строка текста 4</div>');
            this.$node.append('<div>строка текста 5</div>');
            this.$node.append('<div>строка текста 6</div>');
            this.$node.append('<div>строка текста 7</div>'); // это будет лишняя строка
            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1 строка текста 2 строка текста 3 строка текста 4 строка текста 5 строка текста 6');
        });

        it('если строка одна, то длинна фестлайна не более 200 символов строки, округленных до последнего слова', function() {
            this.$node.append('строка текста 1 строка текста 2 строка текста 3 строка текста 4 ' +
                'строка текста 5 строка текста 6 строка текста 7 строка текста 8 ' +
                'строка текста 9 строка текста 10 строка текста 11 строка текста 12 ' +
                'строка текста 13 строка текста 14 строка текста 15 строка текста 16');

            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1 строка текста 2 строка текста 3 строка текста 4 строка текста 5 строка текста 6 ' +
                'строка текста 7 строка текста 8 строка текста 9 строка текста 10 строка текста 11 строка текста 12');
        });

        it('если встречена не буквенная строка, то все строки после отбрасываются', function() {
            this.$node.append('<div>строка текста 1</div>');
            this.$node.append('<div>строка текста 2</div>');
            this.$node.append('<div>--</div>');
            this.$node.append('<div>строка текста 3</div>');

            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1 строка текста 2');
        });

        it('пустые строки игнорируются', function() {
            this.$node.append('<div>строка текста 1</div>');
            this.$node.append('<div>    </div>');
            this.$node.append('<div>    </div>');
            this.$node.append('<div>строка текста 2</div>');
            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1 строка текста 2');
        });

        it('поиск фестлайна прерывается, если встречен тег цитирования', function() {
            this.$node.append('<div>строка текста 1</div>');
            this.$node.append('<blockquote>строка в цитате</blockquote>');
            expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1');
        });

        describe('_findQuoteFirstLine and _parseAuthorText', function() {
            beforeEach(function() {
                var parseAuthorText = sinon.stub(Daria.MessageProcess.Author, 'parseAuthorText');
                parseAuthorText.withArgs('строка текста 1').returns(false);
                parseAuthorText.withArgs('строка с автором').returns(true);
            });

            afterEach(function() {
                Daria.MessageProcess.Author.parseAuthorText.restore();
            });

            it('последняя строка фестлайна игнорируется, если это автор цитаты', function() {
                this.$node.append('<div>строка текста 1</div>');
                this.$node.append('<div>строка с автором</div>');
                expect(this.action(this.$node.get(0))).to.be.equal('строка текста 1');
            });
        });
    });

    describe('_concatQuotes', function() {
        beforeEach(function() {
            this.action = Daria.MessageProcess._concatQuotes;
        });

        afterEach(function() {
            this.action = null;
        });

        it('бездетный предок не обрабатывается', function() {
            var el = htmlToNode('<div></div>');
            compareNodes.call(this, this.action(el), htmlToNode('<div></div>'));
        });

        it('по умолчанию склеиваются цитаты на всех уровнях вложенности', function() {
            var el = htmlToNode(
                '<div>' +
                    '<div>' +
                        '<blockquote>1' +
                            '<blockquote>1.1</blockquote>' +
                            '<blockquote>1.2</blockquote>' +
                        '</blockquote>' +
                        '<blockquote>2</blockquote>' +
                    '</div>' +
                '</div>'
            );

            compareNodes.call(this, this.action(el), htmlToNode(
                '<div>' +
                    '<div data-processed="cq">' +
                        '<blockquote data-processed="cq">1' +
                            '<blockquote>1.1<br>1.2</blockquote>' +
                        '2</blockquote>' +
                    '</div>' +
                '</div>'
            ));
        });

        it('если передан уровень, то ищет и объединяет только цитаты с совпавшим уровнем', function() {
            var el = htmlToNode(
                '<div>' +
                    '<div>' +
                        '<blockquote data-level="1">1' +
                            '<blockquote data-level="2">1.1</blockquote>' +
                            '<blockquote data-level="2">1.2</blockquote>' +
                        '</blockquote>' +
                        '<blockquote data-level="1">2</blockquote>' +
                    '</div>' +
                '</div>'
            );

            compareNodes.call(this, this.action(el, 1), htmlToNode(
                '<div>' +
                    '<div data-processed="cq">' +
                        '<blockquote data-level="1">1' +
                            '<blockquote data-level="2">1.1</blockquote>' +
                            '<blockquote data-level="2">1.2</blockquote>' +
                        '2</blockquote>' +
                    '</div>' +
                '</div>'
            ));
        });

        it('если передан уровень, то ищет и объединяет только цитаты с совпавшим уровнем', function() {
            var el = htmlToNode(
                '<div>' +
                    '<div>' +
                        '<blockquote data-level="1">1' +
                            '<blockquote data-level="2">1.1</blockquote>' +
                            '<blockquote data-level="2">1.2</blockquote>' +
                        '</blockquote>' +
                        '<blockquote data-level="1">2</blockquote>' +
                    '</div>' +
                '</div>'
            );

            compareNodes.call(this, this.action(el, 2), htmlToNode(
                '<div>' +
                    '<div>' +
                        '<blockquote data-level="1" data-processed="cq">1' +
                            '<blockquote data-level="2">1.1<br>1.2</blockquote>' +
                        '</blockquote>' +
                        '<blockquote data-level="1">2</blockquote>' +
                    '</div>' +
                '</div>'
            ));
        });
    });

    describe('_concatChildQuotes', function() {
        beforeEach(function() {
            this.action = Daria.MessageProcess._concatChildQuotes;
        });

        afterEach(function() {
            this.action = null;
        });

        var checks = {
            'бездетный предок не обрабатывается': [
                {
                    check: '<div></div>',
                    expect: '<div></div>'
                }
            ],
            'рассположенные на одном уровне ноды цитирования, разделенные пустимы тегами, склеиваются в одну ноду': [
                {
                    check: '<div><blockquote></blockquote><blockquote></blockquote><blockquote></blockquote></div>',
                    expect: '<div data-processed="cq"><blockquote></blockquote></div>'
                },
                {
                    params: [ true ],
                    check:
                        '<div>' +
                            '<blockquote>' +
                                '<blockquote></blockquote>' +
                                '<blockquote></blockquote>' +
                            '</blockquote>' +
                            '<blockquote></blockquote>' +
                            '<blockquote></blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote data-processed="cq">' +
                                '<blockquote></blockquote>' +
                            '</blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote>' +
                                '<blockquote></blockquote>' +
                                '<blockquote></blockquote>' +
                            '</blockquote>' +
                            '<blockquote></blockquote>' +
                            '<blockquote></blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>' +
                                '<blockquote></blockquote>' +
                                '<blockquote></blockquote>' +
                            '</blockquote>' +
                        '</div>'
                }
            ],
            'при объединении между не пустыми текстовыми нодами добавляется перенос строки': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1<br>2</blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<blockquote>2</blockquote>' +
                            '<blockquote>3</blockquote>' +
                            '<blockquote>   </blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1<br>2<br>3<br>   </blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote>   </blockquote>' +
                            '<blockquote>1</blockquote>' +
                            '<blockquote>2</blockquote>' +
                            '<blockquote>3</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>   <br>1<br>2<br>3</blockquote>' +
                        '</div>'
                }
            ],
            'между блочными нодами и нодой переноса строки дополнительный перенос строки не ставится': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1<br></blockquote>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1<br>2</blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<blockquote><br>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1<br>2</blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote><p>1</p></blockquote>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote><p>1</p>2</blockquote>' +
                        '</div>'
                },
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<blockquote><div>2</div></blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1<div>2</div></blockquote>' +
                        '</div>'
                }
            ],
            'br между нодами цитирования не являются разделителем и учитываются при объединении': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<br>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq"><blockquote>1<br>2</blockquote></div>'
                }
            ],
            'pre без дочерних между нодами цитирования не являются разделителем и учитываются при объединении': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<pre></pre>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq"><blockquote>1<pre></pre>2</blockquote></div>'
                }
            ],
            'pre с пустым дочерним между нодами цитирования не являются разделителем и учитываются при объединении': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<pre>  </pre>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq"><blockquote>1<pre>  </pre>2</blockquote></div>'
                }
            ],
            'pre с не пустым дочерним между нодами цитирования являются разделителем': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '<pre>--</pre>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1</blockquote>' +
                            '<pre>--</pre>' +
                            '<blockquote>2</blockquote>' +
                        '</div>'
                }
            ],
            'не пустая текстовая нода является разделителем': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1</blockquote>' +
                            '--' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1</blockquote>' +
                            '--' +
                            '<blockquote>2</blockquote>' +
                        '</div>'
                }
            ],
            'по умолчанию обрабатывается только список детей переданного потомка': [
                {
                    check:
                        '<div>' +
                            '<blockquote>1' +
                                '<blockquote>1.1</blockquote>' +
                                '<blockquote>1.2</blockquote>' +
                            '</blockquote>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote>1' +
                                '<blockquote>1.1</blockquote>' +
                                '<blockquote>1.2</blockquote>' +
                            '2</blockquote>' +
                        '</div>'
                }
            ],
            'если второй агрумент приводится к true, то выполняется объединение всех дочерних цитат': [
                {
                    params: [ true ],
                    check:
                        '<div>' +
                            '<blockquote>1' +
                                '<blockquote>1.1</blockquote>' +
                                '<blockquote>1.2</blockquote>' +
                            '</blockquote>' +
                            '<blockquote>2</blockquote>' +
                        '</div>',
                    expect:
                        '<div data-processed="cq">' +
                            '<blockquote data-processed="cq">' +
                                '1' +
                                '<blockquote>1.1<br>1.2</blockquote>' +
                                '2' +
                            '</blockquote>' +
                        '</div>'
                }
            ]
        };


        $.each(checks, function(nameTest, checksList) {
            checksList.forEach(function(check, idx) {
                it(nameTest + ' ' + idx, function() {
                    var args = [ htmlToNode(check.check) ];
                    args = args.concat(check.params || []);

                    compareNodes.call(this, this.action.apply(null, args), htmlToNode(check.expect));
                });
            });
        });


        it('пустые ноды между нодами цитирования не являются разделителем и учитываются при объединении', function() {
            var el = htmlToNode('<div><blockquote>1</blockquote> <blockquote>2</blockquote></div>');
            expect(this.action(el).innerHTML).to.be.equal('<blockquote>1 <br>2</blockquote>');
        });


        it('old tests', function() {
            var tests = [
                ['<blockquote>1</blockquote><blockquote>2</blockquote>', '<blockquote>1<br>2</blockquote>'],
                ['<blockquote>1</blockquote><blockquote><p>2</p></blockquote>', '<blockquote>1<p>2</p></blockquote>'],
                ['<blockquote><p>1</p></blockquote><blockquote>2</blockquote>', '<blockquote><p>1</p>2</blockquote>'],
                ['<blockquote><p>1</p></blockquote><blockquote><p>2</p></blockquote>', '<blockquote><p>1</p><p>2</p></blockquote>'],
                ['<blockquote><p>1</p></blockquote><blockquote><p>2</p></blockquote>', '<blockquote><p>1</p><p>2</p></blockquote>'],
                ['<blockquote><p>1</p><p>2</p></blockquote><blockquote><p>3</p></blockquote>', '<blockquote><p>1</p><p>2</p><p>3</p></blockquote>'],
                ['<blockquote><p>1</p></blockquote><blockquote><p>2</p><p>3</p></blockquote>', '<blockquote><p>1</p><p>2</p><p>3</p></blockquote>']
            ];

            var that = this;
            tests.forEach(function(arr) {
                var el = htmlToNode('<div>' + arr[0] + '</div>');
                expect(that.action(el).innerHTML).to.be.equal(arr[1]);
            });
        });
    });

    describe('flagAdd', function() {
        beforeEach(function() {
            this.el = document.createElement('div');
            this.action = Daria.MessageProcess.flagAdd;
        });

        afterEach(function() {
            this.el = null;
            this.action = null;
        });

        it('после выполнения должен добавиться атрибут data-processed', function() {
            expect(this.action(this.el, 'test')).to.be.equal(true);
            expect(this.el.getAttribute('data-processed')).to.be.equal('test');
        });

        it('для текстовой ноды выполнить операцию нельзя', function() {
            expect(this.action(document.createTextNode('qwe'), 'test')).to.be.equal(false);
        });

        it('нельзя дважды добавить один и тот же признак', function() {
            expect(this.action(this.el, 'test')).to.be.equal(true);
            expect(this.el.getAttribute('data-processed')).to.be.equal('test');
            expect(this.action(this.el, 'test')).to.be.equal(false);
            expect(this.el.getAttribute('data-processed')).to.be.equal('test');
        });

        it('добавленные признаки разделяются пробелами', function() {
            expect(this.action(this.el, 'test')).to.be.equal(true);
            expect(this.action(this.el, 'test1')).to.be.equal(true);
            expect(this.action(this.el, 'test2')).to.be.equal(true);
            expect(this.el.getAttribute('data-processed')).to.be.equal('test test1 test2');
        });

    });

    describe('flagContains', function() {
        beforeEach(function() {
            this.el = document.createElement('div');
            this.action = Daria.MessageProcess.flagContains;
        });

        afterEach(function() {
            this.el = null;
            this.action = null;
        });

        it('элемент без атрибута data-processed не содержит списка флагов обработки', function() {
            expect(this.action(this.el, 'test')).to.be.equal(false);
        });

        it('наличие флага обработки проверяется по содержимому тега data-processed', function() {
            this.el.setAttribute('data-processed', 'test');
            expect(this.action(this.el, 'test')).to.be.equal(true);
        });

        it('список флагов в теге data-processed должен быть разделен пробелом', function() {
            this.el.setAttribute('data-processed', 'test1 test2 test3');
            expect(this.action(this.el, 'test')).to.be.equal(false);
            expect(this.action(this.el, 'test1')).to.be.equal(true);
            expect(this.action(this.el, 'test2')).to.be.equal(true);
            expect(this.action(this.el, 'test3')).to.be.equal(true);
            expect(this.action(this.el, 'test4')).to.be.equal(false);
        });
    });

    describe('#processBody', function() {
        beforeEach(function() {
            Jane.ErrorLog.sendException.restore();
            this.action = Daria.MessageProcess.processBody;
        });

        afterEach(function() {
            this.action = null;
        });

        it('Вызов без параметров создает исключение, оно должно быть перехвачено', function() {
            this.sinon.stub(Jane.ErrorLog, 'sendException').callsFake(function(errorName) {
                expect(errorName).to.be.equal('PROCESS_BODY_ERROR');
            });

            expect(this.action().isEqualNode($('<div />')[0])).to.be.ok;
        });

        it('Ошибка на этапе обработки регулярками вызывает исключение, оно должно быть перехвачено, и тело письма не возвращается', function() {
            this.sinon.stub(Jane.ErrorLog, 'sendException').callsFake(function(errorName) {
                expect(errorName).to.be.equal('PROCESS_BODY_ERROR');
            });

            this.sinon.stub(Daria.MessageProcess.BODY_TRANSFORMERS, 'regs').value([ null ]);

            expect(this.action('', {}).isEqualNode($('<div />')[0])).to.be.ok;
        });

        it('Ошибка на этапе парсинга вызывает исключение, оно должно быть перехвачено, тело письма возвращается преобразованным до момента возникновения ошибки', function() {
            this.sinon.stub(Jane.ErrorLog, 'sendException').callsFake(function(errorName) {
                expect(errorName).to.be.equal('PROCESS_BODY_ERROR');
            });

            var tmpTrnsformersDOM = Daria.MessageProcess.BODY_TRANSFORMERS.dom;
            Daria.MessageProcess.BODY_TRANSFORMERS.dom = [
                function() {
                    throw 'parse error';
                }
            ];

            expect(this.action('test message', {}).isEqualNode($('<div>test message</div>')[0])).to.be.ok;
            Daria.MessageProcess.BODY_TRANSFORMERS.dom = tmpTrnsformersDOM;
        });
    });

    describe('#_quoteWrapSeparator', function() {
        beforeEach(function() {
            this.action = Daria.MessageProcess._quoteWrapSeparator;
        });

        afterEach(function() {
            this.action = null;
        });

        describe('удаление пустых нод справа и слева от обрабатываемой ноды', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.MessageProcess, 'flagContains').returns(false);
                this.sinon.stub(Daria.MessageProcess, 'flagAdd');
                this.sinon.stub(Jane.DOM, 'eachBefore');
                this.sinon.stub(Jane.DOM, 'eachAfter');
            });

            it('прерывает обработку если втретились blockquote перед обрабатываемой цитатой', function() {
                var $html = $('<div>' +
                    '<blockquote>foo</blockquote><div></div><blockquote></blockquote><br><div></div><p>&nbsp;</p><blockquote id="start">bar</blockquote>' +
                '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote>foo</blockquote><div></div><blockquote></blockquote><blockquote id="start">bar</blockquote>');
            });

            it('прерывает обработку если втретились blockquote после обрабатываемой цитаты', function() {
                var $html = $('<div>' +
                    '<blockquote id="start">foo</blockquote><br><div></div><p>&nbsp;</p><blockquote></blockquote><div></div><blockquote>bar</blockquote>' +
                    '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote id="start">foo</blockquote><blockquote></blockquote><div></div><blockquote>bar</blockquote>');
            });

            it('прерывает обработку если втретились img перед обрабатываемой цитатой', function() {
                var $html = $('<div>' +
                    '<blockquote>foo</blockquote><div></div><img><br><div></div><p>&nbsp;</p><blockquote id="start">bar</blockquote>' +
                '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote>foo</blockquote><div></div><img><blockquote id="start">bar</blockquote>');
            });

            it('прерывает обработку если втретились img после обрабатываемой цитаты', function() {
                var $html = $('<div>' +
                    '<blockquote id="start">foo</blockquote><br><div></div><p>&nbsp;</p><img><div></div><blockquote>bar</blockquote>' +
                    '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote id="start">foo</blockquote><img><div></div><blockquote>bar</blockquote>');
            });

            it('прерывает обработку если втретились hr перед обрабатываемой цитатой', function() {
                var $html = $('<div>' +
                    '<blockquote>foo</blockquote><div></div><hr><br><div></div><p>&nbsp;</p><blockquote id="start">bar</blockquote>' +
                '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote>foo</blockquote><div></div><hr><blockquote id="start">bar</blockquote>');
            });

            it('прерывает обработку если втретились hr после обрабатываемой цитаты', function() {
                var $html = $('<div>' +
                    '<blockquote id="start">foo</blockquote><br><div></div><p>&nbsp;</p><hr><div></div><blockquote>bar</blockquote>' +
                    '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote id="start">foo</blockquote><hr><div></div><blockquote>bar</blockquote>');
            });

            it('прерывает обработку если втретились ноды с атрибутом style перед обрабатываемой цитатой', function() {
                var $html = $('<div>' +
                    '<blockquote>foo</blockquote><div></div><div style="color:#000;"></div><br><div></div><p>&nbsp;</p><blockquote id="start">bar</blockquote>' +
                '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote>foo</blockquote><div></div><div style="color:#000;"></div><blockquote id="start">bar</blockquote>');
            });

            it('прерывает обработку если втретились ноды с атрибутом style после обрабатываемой цитаты', function() {
                var $html = $('<div>' +
                    '<blockquote id="start">foo</blockquote><br><div></div><p>&nbsp;</p><div style="color:#000;"></div><div></div><blockquote>bar</blockquote>' +
                    '</div>');
                this.action( $html.find('#start')[0] );
                expect($html.html()).to.be.equal('<blockquote id="start">foo</blockquote><div style="color:#000;"></div><div></div><blockquote>bar</blockquote>');
            });

        });

    });

    describe('Разметка адресов', function() {
        beforeEach(function() {
            this.addressExtractor = _.find(Daria.MessageProcess.BODY_TRANSFORMERS.dom, { transformerName: 'location-facts' });

            this.addressFact = {
                "index":    "105523",
                "geo":      {
                    "country": "россия",
                    "city":    "пемза",
                    "g_descr": "город"
                },
                "geo_addr": "Город Пемза, улица Великого Ктулху, владение 15.",
                "addr":     {
                    "numbers": "владение 15",
                    "descr":   "улица",
                    "street":  "Великого Ктулху"
                }
            };

            this.params = {};
            this.data = {};

            this.dd = Daria.DEBUG;
            Daria.DEBUG = this.addressExtractor.debugMode;
        });
        afterEach(function() {
            Daria.DEBUG = this.dd;
        });

        describe('trimPunctuation', function() {
            beforeEach(function() {
                this.trim = this.addressExtractor.trimPunctuation;
                this.str = 'hello world';
            });
            _.each({
                space: ' ',
                nbsp: ' ',
                dash: '-',
                mdash: '—',
                equal: '=',
                underscore: '_',
                asterisc: '*'
            }, function(char, description) {
                it('should trim ' + description + ' char', function() {
                    expect(this.trim(char + char + this.str + char + char)).to.be.equal(this.str);
                });
            });

            ['.', ',', '!', '?'].forEach(function(char) {
                it('should trim ' + char + ' from the beginning of the string and not from the end', function() {
                    expect(this.trim(char + char + this.str + char + char)).to.be.equal(this.str + char + char);
                });
            });

            ["'", "\"", "<", ">", "(", ")", "«", "»"].forEach(function(char) {
                it('should not trim ' + char + ' from both sides', function() {
                    var str = char + char + this.str + char + char;
                    expect(this.trim(str)).to.be.equal(str);
                });
            });
        });

        describe('nodes', function() {
            describe('contains', function() {
                beforeEach(function() {
                    this.contains = this.addressExtractor.Nodes.contains;
                });

                describe('contract', function() {
                    beforeEach(function() {
                        this.node = document.createElement('div');
                        this.otherNode = document.createElement('div');
                        this.topNode = document.createElement('div');
                    });
                    it('should assert node to search in is a node', function() {
                        var that = this;
                        expect(function() {
                            that.contains(null, that.otherNode, that.topNode);
                        }).to.throw('All arguments should be nodes: FAIL');
                    });

                    it('should assert node to search for is a node', function() {
                        var that = this;
                        expect(function() {
                            that.contains(that.node, null, that.topNode);
                        }).to.throw('All arguments should be nodes: FAIL');
                    });

                    it('should assert top node is a node', function() {
                        var that = this;
                        expect(function() {
                            that.contains(that.node, that.otherNode, null);
                        }).to.throw('All arguments should be nodes: FAIL');
                    });
                });

                it('should return true if a node is directly inside another node', function() {
                    var outer = document.createElement('div');
                    var inner = document.createElement('span');
                    outer.appendChild(inner);

                    expect(this.contains(outer, inner, outer)).to.be.equal(true);
                });

                it('should return false if nodes are both children of another node', function() {
                    var one = document.createElement('span');
                    var another = document.createElement('span');

                    var parent = document.createElement('div');
                    parent.appendChild(one);
                    parent.appendChild(another);

                    expect(this.contains(one, another, parent)).to.be.equal(false);
                });

                it('should return true if node is deep inside another node', function() {
                    var parent = document.createElement('div');
                    var last = parent;
                    for (var i = 0; i < 10; i++) {
                        var current = document.createElement('div');
                        last.appendChild(current);
                        last = current;
                    }

                    expect(this.contains(parent, last, parent)).to.be.equal(true);
                });

                it('should return false if node is inside another node, but is lower than the limiting node', function() {
                    var outer = document.createElement('div');
                    var middle = document.createElement('div');
                    var inner = document.createElement('div');

                    outer.appendChild(middle);
                    middle.appendChild(inner);

                    expect(this.contains(outer, inner, middle)).to.be.equal(false);
                });

                it('should return true if textnode is deep inside another node', function() {
                    var parent = document.createElement('div');
                    var last = parent;
                    for (var i = 0; i < 10; i++) {
                        var current = document.createElement('div');
                        last.appendChild(current);
                        last = current;
                    }

                    var textNode = document.createTextNode('inside');
                    last.appendChild(textNode);
                    expect(this.contains(parent, textNode, parent)).to.be.equal(true);
                });
            });
            describe('traverse', function() {
                beforeEach(function() {
                    this.traverse = this.addressExtractor.Nodes.traverse;
                });

                describe('contract', function() {
                    beforeEach(function() {
                        this.node = document.createElement('div');
                        this.criterion = function() {};
                        this.action = function() {};
                    });

                    it('should assert node is not a Node', function() {
                        var that = this;
                        expect(function() {
                            that.traverse(null, that.criterion, that.action);
                        }).to.throw('Top node should be an instance of Node: FAIL');
                    });

                    it('should assert criterion is not a function', function() {
                        var that = this;
                        expect(function() {
                            that.traverse(that.node, null, that.action);
                        }).to.throw('Criterion should be a function: FAIL');
                    });

                    it('should assert action is not a function', function() {
                        var that = this;
                        expect(function() {
                            that.traverse(that.node, that.criterion, null);
                        }).to.throw('Action should be a function: FAIL');
                    });
                });


                it('should call criterion with the current node', function() {
                    var node = document.createElement('div');
                    var criterion = sinon.stub();
                    this.traverse(node, criterion, sinon.stub());
                    expect(criterion.calledOnce).to.be.equal(true);
                    expect(criterion.calledWithExactly(node)).to.be.equal(true);
                });

                it('should call action with the current node if criterion returns true', function() {
                    var node = document.createElement('div');
                    var action = sinon.stub();
                    this.traverse(node, sinon.stub().returns(true), action);
                    expect(action.calledOnce).to.be.equal(true);
                    expect(action.calledWithExactly(node)).to.be.equal(true);
                });

                it('should not call action if criterion returned false for the current node', function() {
                    var node = document.createElement('div');
                    var action = sinon.stub();
                    this.traverse(node, sinon.stub().returns(false), action);
                    expect(action.called).to.be.equal(false);
                });

                it('should visit all the nodes in the subtree if criterion always returns true', function() {
                    var topNode = document.createElement('div');
                    var secondLevelNodes = _.map(new Array(3), function() {
                        var node = document.createElement('div');
                        topNode.appendChild(node);
                        return node;
                    });

                    var thirdLevelNodes = _.flatten(_.map(secondLevelNodes, function(slnode) {
                        return _.map(new Array(3), function() {
                            var node = document.createElement('div');
                            slnode.appendChild(node);
                            return node;
                        });
                    }));

                    var visited = [];
                    this.traverse(
                        topNode,
                        function() {
                            return true;
                        },
                        function(node) {
                            visited.push(node);
                        }
                    );

                    thirdLevelNodes.concat(secondLevelNodes).concat([topNode]).forEach(function(node) {
                        expect(visited).to.contain(node);
                    });
                });
            });
            describe('getSmallestContainingNode', function() {
                beforeEach(function() {
                    this.getSmallestNode = this.addressExtractor.Nodes.getSmallestContainingNodes;

                    this.matchText = 'match';
                    this.re = new RegExp('match', 'g');

                    /*
                     <div>
                         <span>match</span>
                         <span>irrelevant</span>
                     </div>
                     */

                    this.topNode = document.createElement('div');
                    this.matchingContainer = document.createElement('span');
                    this.matchingNode = document.createTextNode(this.matchText);
                    this.matchingContainer.appendChild(this.matchingNode);

                    this.irrelevantNode = document.createElement('span');
                    this.irrelevantNode.appendChild(document.createTextNode('irrelevant'));

                    this.topNode.appendChild(this.matchingContainer);
                    this.topNode.appendChild(this.irrelevantNode);
                });

                describe('contract', function() {
                    it('should assert the topNode is a Node', function() {
                        var that = this;
                        expect(function() {
                            that.getSmallestNode(null, that.re);
                        }).to.throw('TopNode should be a Node: FAIL');
                    });

                    it('should assert regexp is a RegExp', function() {
                        var that = this;
                        expect(function() {
                            that.getSmallestNode(that.topNode);
                        }).to.throw('Regexp should be a RegExp: FAIL');
                    });
                });

                it('should return a deepest node as a match', function() {
                    expect(this.getSmallestNode(this.topNode, this.re)).to.contain(this.matchingNode);
                });

                it('should not return an element node containing a matching text node', function() {
                    expect(this.getSmallestNode(this.topNode, this.re)).to.not.contain(this.matchingContainer);
                });

                it('should not return top node', function() {
                    expect(this.getSmallestNode(this.topNode, this.re)).to.not.contain(this.topNode);
                });

                it('should not return nodes that do not contain matching text', function() {
                    expect(this.getSmallestNode(this.topNode, this.re)).to.not.contain(this.irrelevantNode);
                });

                it('should return a top node if the match is scattered across several child nodes', function() {
                    var firstNode = document.createElement('span');
                    firstNode.appendChild(document.createTextNode('split'));

                    var secondNode = document.createElement('span');
                    secondNode.appendChild(document.createTextNode('match'));

                    var topNode = document.createElement('div');
                    topNode.appendChild(firstNode);
                    topNode.appendChild(secondNode);

                    var re = new RegExp('splitmatch');
                    expect(this.getSmallestNode(topNode, re)).to.contain(topNode);
                });

                it('should return a node who\'s matches regexp thought it is split over several subnodes', function() {
                    var top = document.createElement('p');
                    var firstPart = document.createTextNode('hipsters');

                    var middle = document.createElement('span');
                    var middleText = document.createTextNode('love');
                    middle.appendChild(middleText);

                    var lastPart = document.createTextNode('beards');

                    top.appendChild(firstPart);
                    top.appendChild(middle);
                    top.appendChild(lastPart);

                    var re = new RegExp(top.textContent, 'gi');

                    var nodes = this.getSmallestNode(top, re);
                    expect(nodes).to.contain(top);
                    expect(nodes).to.not.contain(firstPart);
                    expect(nodes).to.not.contain(middle);
                    expect(nodes).to.not.contain(lastPart);
                });

                it('should return both the parent node and the inner node if address is scattered across several elements and repeated', function() {
                    /*
                     <div> //Should not match
                         <div> //This should match
                             <div> //And this one should too
                                parts of <span>the whole</span>
                             </div>

                             parts of <span>the whole</span>
                         </div>

                         <p>irrelevant</p>
                     </div>
                     */

                    var first = document.createTextNode('parts of ');
                    var second = document.createElement('span');
                    second.appendChild(document.createTextNode('the whole'));

                    var re = new RegExp('parts of the whole');

                    var outer = document.createElement('div');
                    var middle = document.createElement('div');
                    var inner = document.createElement('div');
                    outer.appendChild(middle);
                    middle.appendChild(inner);
                    outer.appendChild($('<p>irrelevant</p>').get(0));

                    middle.appendChild(first.cloneNode(true));
                    middle.appendChild(second.cloneNode(true));

                    inner.appendChild(first.cloneNode(true));
                    inner.appendChild(second.cloneNode(true));

                    var nodes = this.getSmallestNode(middle, re);
                    expect(nodes).to.contain(inner);
                    expect(nodes).to.contain(middle);
                    expect(nodes).to.not.contain(outer);
                });
            });
        });

        describe('Address', function() {
            beforeEach(function() {
                this.addressFact = {
                    geo:      {
                        "city":    "новосибирск",
                        "g_descr": "город"
                    },
                    geo_addr: "г.Новосибирск, ул. Коминтерна д.65",
                    addr:     {
                        "numbers": "д.65",
                        "descr":   "улица",
                        "street":  "коминтерна"
                    }
                };

                this.Address = this.addressExtractor.Address;
                this.address = new this.Address(this.addressFact);
            });

            describe('wrapAddress', function() {
                beforeEach(function() {
                    this.parentNode = document.createElement('div');

                    this.match = "Эта строка сматчится и будет адресом";

                    this.address.addPart();
                    this.part = this.address.getCurrentPart();

                    this.Symbol = this.addressExtractor.Symbol;
                    var Symbol = this.Symbol;

                    this.pushTextFromNode = function(part, node, text) {
                        if (node.nodeType !== 3) {
                            throw new Error('Text node expected');
                        }

                        var startPosition = node.textContent.indexOf(text);
                        if (startPosition === -1) {
                            throw new Error('node does not contains text');
                        }

                        var position = startPosition - 1;
                        while (++position < startPosition + text.length) {
                            part.push(new Symbol(node.textContent.charAt(position), position, node));
                        }
                    };
                });

                it('должен оборачивать в разметку ноду, целиком совпадающую с искомым текстом', function() {
                    var matchingNode = document.createTextNode(this.match);
                    this.parentNode.appendChild(matchingNode);
                    this.pushTextFromNode(this.part, matchingNode, this.match);

                    this.address.wrap('123');

                    expect(this.parentNode.childNodes.length).to.be.equal(1);
                    expect(this.parentNode.firstChild.nodeType).to.be.equal(1);
                    expect(this.parentNode.firstChild.nodeName.toLowerCase()).to.be.equal('span');
                });

                it('должен обернуть только искомую часть, если она находится среди другого текста', function() {
                    var matchingNode = document.createTextNode('before ' + this.match + ' after');
                    this.parentNode.appendChild(matchingNode);
                    this.pushTextFromNode(this.part, matchingNode, this.match);

                    this.address.wrap('123');

                    expect(this.parentNode.childNodes.length).to.be.equal(3);
                    expect(this.parentNode.childNodes.item(1).nodeType).to.be.equal(1);
                    expect(this.parentNode.childNodes.item(1).nodeName.toLowerCase()).to.be.equal('span');
                });

                it('не должен изменять контент', function() {
                    var matchingNode = document.createTextNode('before ' + this.match + ' after');
                    this.parentNode.appendChild(matchingNode);
                    this.pushTextFromNode(this.part, matchingNode, this.match);

                    var contentsBefore = this.parentNode.textContent;
                    this.address.wrap('123');
                    expect(this.parentNode.textContent).to.be.equal(contentsBefore);
                });

                it('должен игнорировать ноду, находящуюся внутри ссылки', function() {
                    var matchingNode = document.createTextNode(this.match);
                    this.parentNode.appendChild(matchingNode);
                    this.pushTextFromNode(this.part, matchingNode, this.match);

                    document.createElement('a').appendChild(this.parentNode);

                    this.address.wrap('123');
                    expect($(this.parentNode).find('.js-extracted-address').size()).to.be.equal(0);
                });

                it('должен игнорировать ноду, находящуюся внутри другого адреса', function() {
                    var matchingNode = document.createTextNode(this.match);
                    this.parentNode.appendChild(matchingNode);
                    this.pushTextFromNode(this.part, matchingNode, this.match);

                    var anotherAddress = document.createElement('span');
                    anotherAddress.setAttribute('class', 'js-extracted-address');
                    anotherAddress.appendChild(this.parentNode);

                    this.address.wrap('123');
                    expect($(this.parentNode).find('.js-extracted-address').size()).to.be.equal(0);
                });

                it('должен уметь оборачивать часть адреса, содержащую wbr и br', function() {
                    var beginning = document.createTextNode('before ');
                    var middle = document.createTextNode(' middle ');
                    var end = document.createTextNode(' end');

                    var br = document.createElement('br');
                    var wbr = document.createElement('wbr');

                    var that = this;
                    this.parentNode.innerHTML = 'beginning<br />middle<wbr />end';
                    _.each(this.parentNode.childNodes, function(node) {
                        if (node.nodeType === 3) {
                            that.pushTextFromNode(that.part, node, node.textContent);
                        } else {
                            that.part.push(new that.Symbol(null, 0, node));
                        }
                    });

                    this.address.wrap('123');
                    expect($(this.parentNode).find('.js-extracted-address').html()).to.be.equal('beginning<br>middle<wbr>end');
                });

                describe('аттрибуты тега', function() {
                    beforeEach(function() {
                        var matchingNode = document.createTextNode(this.match);
                        this.parentNode.appendChild(matchingNode);
                        this.pushTextFromNode(this.part, matchingNode, this.match);

                        this.address.wrap('123');
                        this.wrapped = $(this.parentNode.firstChild);
                    });

                    it('должен выставить класс js-extracted-address', function() {
                        expect(this.wrapped.hasClass('js-extracted-address')).to.be.equal(true);
                    });

                    it('должен положить адрес, который нашла томита в address в data-params', function() {
                        expect(this.wrapped.data('address')).to.be.equal(this.addressFact.geo_addr);
                    });
                });
            });
        });

        describe('transformer', function() {
            beforeEach(function() {
                //DARIA-37486 - Открыть карты только на 669 сид и внутреннюю сеть
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                //Done with DARIA-37486 - Открыть карты только на 669 сид и внутреннюю сеть

                this.bodyString = '<div>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Город Пемза, улица Великого Ктулху, владение 15. Vivamus eleifend sagittis venenatis.</div>';
                this.body = document.createElement('div');
                this.body.innerHTML = this.bodyString;

                this.ids = '10000000001123123';
                this.params = { ids: this.ids };
                this.sinon.stub(ns.Model.get('message-body', this.params), 'getLocations').returns([this.addressFact]);
                this.addressExtractor(this.body, this.params);

                this.markupTag = 'span';
                this.markupClass = 'js-extracted-address';
                this.selector = this.markupTag + '.' + this.markupClass;
                this.$address = $(this.body).find(this.selector);
            });

            it('должен оборачивать адрес из письма в разметку', function() {
                expect(this.$address.html()).to.be.equal('Город Пемза, улица Великого Ктулху, владение 15.');
            });

            it('должен оставить остальную часть сообщения нетронутой', function() {
                var split = this.body.innerHTML.split(this.$address.get(0).outerHTML);
                expect(split.length).to.be.equal(2);
                expect(split[0]).to.be.equal('<div>Lorem ipsum dolor sit amet, consectetur adipiscing elit. ');
                expect(split[1]).to.be.equal(' Vivamus eleifend sagittis venenatis.</div>');
            });

            it('должен положить полный адрес в data-address', function() {
                expect(this.$address.attr('data-address')).to.be.equal(this.addressFact.geo_addr);
            });

            it('должен положить полный адрес в data-address-query', function() {
                expect(this.$address.attr('data-address-query')).to.be.equal(this.addressFact.geo_addr);
            });

            it('должен положить адрес запроса геокодера в data-address-query', function() {
                this.addressFact.geocoder_request = 'fake-geocoder-query';

                this.body.innerHTML = this.bodyString;
                this.addressExtractor(this.body, this.params);
                this.$address = $(this.body).find(this.selector);

                expect(this.$address.attr('data-address-query')).to.be.equal(this.addressFact.geocoder_request);
            });

            it('должен положить mid письма в data-ids', function() {
                expect(this.$address.attr('data-ids')).to.be.equal(this.ids);
            });

            it('должен обрабатывать неразрывные пробелы внутри адреса в теле письма', function() {
                this.body.innerHTML = '<div>Город Пемза, улица Великого Ктулху, владение 15.</div>'; //Все пробелы здесь неразрывные
                this.addressExtractor(this.body, this.params);
                expect(this.body.innerHTML).to.contain(this.markupClass);
            });

            it('должен размечать повторения одного и того же адреса', function() {
                this.body.innerHTML = '<div>Город Пемза, улица Великого Ктулху, владение 15. Повторяю: Город Пемза, улица Великого Ктулху, владение 15.</div>';
                ns.Model.get('message-body', this.params).getLocations.returns([this.addressFact, this.addressFact]);

                this.addressExtractor(this.body, this.params);

                expect($(this.body).find(this.selector).size()).to.be.equal(2);
            });

            it('должен эскейпить регекспы, чтобы искать точные совпадения', function() {
                this.addressFact.geo_addr = 'add.ess';
                this.body.innerHTML = '<div>address</div><div>add.ess</div>';

                this.sinon.stub(this.addressExtractor.Nodes, 'getSmallestContainingNodes').returns([]);
                this.addressExtractor(this.body, this.params);

                expect(this.addressExtractor.Nodes.getSmallestContainingNodes.calledOnce).to.be.equal(true);
                expect(this.addressExtractor.Nodes.getSmallestContainingNodes.firstCall.args[1].toString()).to.be.equal("/add\\.ess/g");
            });

            it('не должен размечать один адрес внутри другого', function() {
                this.body.innerHTML = '<div>Город Пемза, улица Великого Ктулху, владение 15. Повторяю: Великого Ктулху, владение 15.</div>';

                var clonedFact = _.cloneDeep(this.addressFact);
                clonedFact.geo_addr = 'Великого Ктулху, владение 15'; //Подстрока большого адреса

                ns.Model.get('message-body', this.params).getLocations.returns([clonedFact, this.addressFact]);

                this.addressExtractor(this.body, this.params);

                var $wrapped = $(this.body).find(this.selector);
                expect($wrapped.size()).to.be.equal(2); //Нашли оба адреса

                var selector = this.selector;
                $wrapped.each(function() {
                    //Ни один найденный адрес не находится внутри другого
                    expect($(this).parents(selector).size()).to.be.equal(0);
                });
            });

            it('должен размечать адресадресадрес адрес', function() {
                this.body.innerHTML = '<div><p>Тростенецкая улица, 20Тростенецкая улица, 20Тростенецкая улица, 20</p><blockquote><div>Тростенецкая улица, 20</div></blockquote>';
                this.addressFact.geo_addr = 'Тростенецкая улица, 20';
                this.addressExtractor(this.body, this.params);

                var $wrapped = $(this.body).find(this.selector);
                expect($wrapped.size()).to.be.equal(4); //Нашли все адреса

                $wrapped.each(function() {
                    expect($(this).html()).to.be.equal('Тростенецкая улица, 20'); //И нашли их правильно
                });
            });

            it('должен размечать адреса с наложением (DARIA-36846)', function() {
                this.body.innerHTML = '<div><p>Тростенецкая улица, 20Тростенецкая улица, 20Тростенецкая улица, 20</p><blockquote><div>Тростенецкая улица, 20</div></blockquote>';

                var clonedFact = _.cloneDeep(this.addressFact);
                clonedFact.geo_addr = 'Тростенецкая улица, 20Тростенецкая';
                this.addressFact.geo_addr = 'Тростенецкая улица, 20';

                ns.Model.get('message-body', this.params).getLocations.returns([clonedFact, this.addressFact]);
                this.addressExtractor(this.body, this.params);

                var $wrapped = $(this.body).find(this.selector);
                expect($wrapped.size()).to.be.equal(3); //Нашли все адреса

                var wrapped = [];
                $wrapped.each(function() {
                    wrapped.push($(this).html());
                });

                var numFound = _.countBy(wrapped, function(element) {
                    return element;
                });

                expect(numFound['Тростенецкая улица, 20Тростенецкая']).to.be.equal(1);
                expect(numFound['Тростенецкая улица, 20']).to.be.equal(2);
                expect(numFound[' улица, 20']).to.be.equal(undefined); //Этот кусочек адреса, но большая его часть лежит внутри другого адреса и поэтому не должна быть размечена
            });

            describe('Адрес внутри ссылки', function() {
                _.each({
                    'просто в ссылке': '<a href="google.com">Город Пемза, улица Великого Ктулху, владение 15.</a>',
                    'в ссылке вместе с другим текстом': '<a href="bing.com">Я ссылка. Тут написано: Город Пемза, улица Великого Ктулху, владение 15.</a>',
                    'в теге внутри ссылки': '<a href="yahoo.com">Lorem ipsum <span>Город Пемза, улица Великого Ктулху, владение 15.</span> dolor sit amet</a>'
                }, function(bodyString, description) {
                    it('не должен быть размечен, если он ' + description, function() {
                        this.body.innerHTML = bodyString;
                        this.addressExtractor(this.body, this.params);

                        expect($(this.body).find(this.selector).size()).to.be.equal(0);
                    });
                });

                it('должен разметить адрес, который находится между двумя ссылками', function() {
                    this.body.innerHTML = '<a href="baidu.cn">not relevant</a> и текст в начале, потом адрес: Город Пемза, улица Великого Ктулху, владение 15. И текст в конце, и ещё ссылка: <a href="duckduckgo.com">irrelevant</a>';
                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).size()).to.be.equal(1);
                });
            });

            describe('Тэги в адресе', function() {
                beforeEach(function() {
                    this.body = document.createElement('div');
                    this.body.innerHTML = '<div><em>район Хамовники, Метро </em>юго-западная, <strong>улица Миклухо-Маклая,</strong> д. 123, <blink>15й километр</blink></div>';
                    this.addressFact.geo_addr = "район Хамовники, Метро юго-западная, улица Миклухо-Маклая, д. 123, 15й километр";
                    this.addressFact.addr = {
                        street: 'Миклухо-Маклая',
                        descr: 'улица',
                        metro: 'юго-западная',
                        m_descr: 'Метро',
                        quarter: 'хамовники',
                        q_descr: 'район',
                        km: '15й',
                        numbers: 'д. 123'
                    };
                });

                it('должен обернуть только часть с улицей, если он есть, а также затримить пробелы', function() {
                    this.addressExtractor(this.body, this.params);
                    expect($(this.body).find(this.selector).html()).to.be.equal('улица Миклухо-Маклая,');
                });

                it('должен обернуть только ту часть с улицей, которая находится ближе всего к адресу', function() {
                    this.body.innerHTML = '<div>ever <em>whatso </em>ever <strong>ever </strong></div>';
                    this.addressFact.geo_addr = 'whatso ever';
                    this.addressFact.addr.street = 'ever';

                    this.addressExtractor(this.body, this.params);

                    var wrapped = $(this.body).find(this.selector).get(0).outerHTML;
                    var split = $(this.body).html().split(wrapped);

                    expect(split.length).to.be.equal(2);
                    expect(split[0]).to.be.equal('<div>ever <em>whatso </em>');
                    expect(split[1]).to.be.equal(' <strong>ever </strong></div>');
                });

                it('должен оба адреса при повторении с тегом внутри', function() {
                    this.body.innerHTML = '<em>whatso </em>ever <strong>ever </strong><em>whatso </em>ever <strong>ever </strong>';
                    this.addressFact.geo_addr = 'whatso ever';
                    this.addressFact.addr.street = 'ever';

                    this.addressExtractor(this.body, this.params);

                    var wrapped = $(this.body).find(this.selector).get(0).outerHTML;
                    var split = $(this.body).html().split(wrapped);

                    expect(split.length).to.be.equal(3);
                    expect(split[0]).to.be.equal('<em>whatso </em>');
                    expect(split[1]).to.be.equal(' <strong>ever </strong><em>whatso </em>');
                    expect(split[2]).to.be.equal(' <strong>ever </strong>');
                });

                it('должен обернуть только часть с районом, если нет улици', function() {
                    delete this.addressFact.addr.street;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('район Хамовники, Метро');
                });

                it('должен обернуть только часть с километром, если нет улици и района', function() {
                    delete this.addressFact.addr.street;
                    delete this.addressFact.addr.quarter;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('15й километр');
                });

                it('должен обернуть только часть с метро, если нет района, километров и улицы', function() {
                    delete this.addressFact.addr.numbers;
                    delete this.addressFact.addr.km;
                    delete this.addressFact.addr.street;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('район Хамовники, Метро');
                });

                it('должен обернуть только часть с районом, если нет номера дома, километров, улицы и метро', function() {
                    delete this.addressFact.addr.numbers;
                    delete this.addressFact.addr.km;
                    delete this.addressFact.addr.street;
                    delete this.addressFact.addr.metro;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('район Хамовники, Метро');
                });

                it('должен разметить самую большую найденную часть адреса, если нет ни дома, ни километров, ни улицы, ни метро, ни района', function() {
                    delete this.addressFact.addr.numbers;
                    delete this.addressFact.addr.km;
                    delete this.addressFact.addr.street;
                    delete this.addressFact.addr.metro;
                    delete this.addressFact.addr.quarter;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('район Хамовники, Метро');
                });

                it('должен размечать только часть адреса в случае, когда кроме дома в текстноде есть посторонний текст', function() {
                    this.body.innerHTML = 'г.Новосибирск,<div>ул.Коминтерна д.65В теперь предвещало нехороший день</div>';
                    this.addressFact.geo = {"city": "новосибирск", "g_descr": "город"};
                    this.addressFact.geo_addr = "г.Новосибирск,ул.Коминтерна д.65В";
                    this.addressFact.addr = {"numbers": "д.65В", "descr": "улица", "street": "коминтерна"};

                    this.addressExtractor(this.body, this.params);
                    expect($(this.body).find(this.selector).html()).to.be.equal('ул.Коминтерна д.65В');
                });

                it('должен размечать текст с wbr внутри', function() {
                    this.body.innerHTML = 'г.Новосибирск,<wbr />ул.Коминтерна д.65В теперь предвещало нехороший день';
                    this.addressFact.geo = {"city": "новосибирск", "g_descr": "город"};
                    this.addressFact.geo_addr = "г.Новосибирск,ул.Коминтерна д.65В";
                    this.addressFact.addr = {"numbers": "д.65В", "descr": "улица", "street": "коминтерна"};

                    this.addressExtractor(this.body, this.params);
                    expect($(this.body).find(this.selector).html()).to.be.equal('г.Новосибирск,<wbr>ул.Коминтерна д.65В');
                });

                it('должен размечать только адрес с wbr внутри и дополнительным текстом в ноде с якорем', function() {
                    //DARIA-36630 - Разнесло текст ссылкой на карту
                    this.body.innerHTML = 'г.Новосибирск,ул.Коминтер<wbr></wbr>на д.65В теперь предвещало нехороший день, так как запах этот начал преследовать прокуратора с рассвета.';
                    this.addressFact.geo = {"city": "новосибирск", "g_descr": "город"};
                    this.addressFact.geo_addr = "г.Новосибирск,ул.Коминтерна д.65В";
                    this.addressFact.addr = {"numbers": "д.65В", "descr": "улица", "street": "коминтерна"};

                    this.addressExtractor(this.body, this.params);
                    expect($(this.body).find(this.selector).html()).to.be.equal('г.Новосибирск,ул.Коминтер<wbr>на д.65В');
                });

                it('должен размечать адрес с нодой внутри ноды', function() {
                    this.body.innerHTML = '<div><p><span>район Хамовники, Метро юго-западная, улица Миклухо-Маклая,</span></p><p><span> д. 123, 15й километр</span></p></div>';

                    this.addressExtractor(this.body, this.params);
                    expect($(this.body).find(this.selector).html()).to.be.equal('район Хамовники, Метро юго-западная, улица Миклухо-Маклая,');
                });
            });

            describe('Обработка пробелов', function() {
                it('должен оборачивать адрес, перед которым стоит тот же символ, с которого адрес начинается', function() {
                    this.body.innerHTML = 'aab';
                    this.addressFact.geo_addr = 'ab';

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('ab');
                });

                it('должен оборачивать адрес, перед которым стоит пробел и такой же символ, с которого адрес начинается', function() {
                    this.body.innerHTML = 'пройти мост по адресу ул. Суздальская, д. 22';
                    this.addressFact.geo_addr = 'ул. Суздальская, д. 22';

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('ул. Суздальская, д. 22');
                });

                it('должен оборачивать адрес с повтором символов вокруг пробела внутри адреса', function() {
                    var address = 'abcd dcba';
                    this.body.innerHTML = address;
                    this.addressFact.geo_addr = address;

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal(address);
                });

                it('должен оборачивать адрес с повторами пробелов', function() {
                    var address = 'ab     cd     dc     ba';
                    this.body.innerHTML = address;
                    this.addressFact.geo_addr = address.replace(/\s+/g, ' ');

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal(address);
                });

                it('должен оборачивать адрес, разбитый тегом на месте пробела', function() {
                    this.body.innerHTML = '<div>длинный</div><div>адрес</div>'; //Без пробела между словами
                    this.addressFact.geo_addr = 'длинный адрес';
                    this.addressFact.addr.street = 'адрес'; //Чтобы искать именно этот кусочек

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('адрес');
                });

                it('должен оборачивать адрес, разбитый между двумя пробелами', function() {
                    this.body.innerHTML = '<div>длинный </div><div> адрес</div>';
                    this.addressFact.geo_addr = 'длинный адрес';
                    this.addressFact.addr.street = 'адрес'; //Чтобы искать именно этот кусочек

                    this.addressExtractor(this.body, this.params);

                    expect($(this.body).find(this.selector).html()).to.be.equal('адрес');
                });
            });
        });
    });

});

