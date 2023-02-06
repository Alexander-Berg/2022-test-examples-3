describe('b-date-range-picker_type_super', function() {

    describe('Отображение и методы', function() {
        var block,
            createBlock = function(params) {
                var blockTree = u.getDOMTree($.extend({
                    block: 'b-date-range-picker',
                    mods: { type: 'super' },
                    id: 'first-calendar',
                    start: '2015-10-05',
                    finish: '2015-10-20',
                    maxDate: u.moment().add(1, 'd').startOf('d'),
                    ranges: [
                        {
                            title: 'вчера',
                            name: 'yesterday',
                            start: -1,
                            finish: -1
                        },
                        // Период, где даты указаны точно (не относительно)
                        {
                            title: '3 дня назад',
                            name: '3-days-ago',
                            start: u.moment(new Date()).subtract(3, 'days').format('YYYY-MM-DD'),
                            finish: u.moment(new Date()).subtract(3, 'days').format('YYYY-MM-DD')
                        }
                    ],
                    history: {
                        title: 'История',
                        periods: [
                            {
                                name: 'september',
                                start: '2015-09-05',
                                finish: '2015-09-20'
                            },
                            (function() {
                                var range = {
                                    start: u.moment(new Date()).subtract(3, 'days').format('YYYY-MM-DD'),
                                    name: 'history-3-days-ago'
                                };
                                range.finish = range.start;
                                return range;
                            })()
                        ]
                    }
                }, params || {}));

                $('<div/>').append(blockTree);

                return blockTree.bem('b-date-range-picker');
            };

        beforeEach(function() {
            block = createBlock();
        });

        afterEach(function() {
            block.destruct();
        });

        it('Должна быть кнопка c текстом периода', function() {
            expect(block.elem('super-button-text').text()).to.equal('5 – 20 октября 2015');
        });

        describe('getRange', function() {
            it('Должен возвращать выбранный период', function() {
                expect(block.getRange()).to.deep.equal({ start: '2015-10-05', finish: '2015-10-20' });
            });
        });

        describe('setRange', function() {
            it('Должен устанавливать новый период', function() {
                block.setRange({ start: '2015-10-02', finish: '2015-10-10' });

                expect(block.getRange()).to.deep.equal({ start: '2015-10-02', finish: '2015-10-10' });
            });
            it('Должен измениться текст на кнопке', function() {
                block.setRange({ start: '2015-10-02', finish: '2015-10-10' });

                expect(block.elem('super-button-text').text()).to.equal('2 – 10 октября 2015');
            });
            it(['Выход за допустимые пределы снизу и сверху. Должен для наименьшей даты установить допустимый ',
                'нижний предел, для наибольшей даты установить допустимый верхний предел ',
                '(js-параметр maxDate, свойство экземпляра MIN_DATE или js-параметр minDate)'
            ].join(''), function() {
                var origLimits = block._getLimits(),
                    testLimits = {
                        minDate: origLimits.minDate.clone(),
                        maxDate: origLimits.maxDate.clone()
                    };

                block.setRange({
                    start: testLimits.minDate.subtract(20, 'days').format('YYYY-MM-DD'),
                    finish: testLimits.maxDate.add(20, 'days').format('YYYY-MM-DD')
                });

                expect(block.getRange()).to.deep.equal({
                    start: origLimits.minDate.format('YYYY-MM-DD'),
                    finish: origLimits.maxDate.format('YYYY-MM-DD')
                });
            });
            it(['Выход за допустимый верхний предел. Наибольшая дата должна принять ',
                'значение допустимого верхнего предела, наименьшая остаться неизменной ',
                '(js-параметр maxDate)'
            ].join(''), function() {
                var origLimits = block._getLimits(),
                    testLimits = {
                        minDate: origLimits.minDate.clone(),
                        maxDate: origLimits.maxDate.clone()
                    };

                block.setRange({
                    start: testLimits.minDate.add(20, 'days').format('YYYY-MM-DD'),
                    finish: testLimits.maxDate.add(20, 'days').format('YYYY-MM-DD')
                });

                expect(block.getRange()).to.deep.equal({
                    start: testLimits.minDate.format('YYYY-MM-DD'),
                    finish: origLimits.maxDate.format('YYYY-MM-DD')
                });
            });
            it([
                'Выход за допустимый предел снизу. Наименьшая дата должна принять значение ',
                'допустимого нижнего предела, наибольшая остаться неизменной ',
                '(Свойство экземпляра MIN_DATE, либо js-параметр minDate)'
            ].join(''), function() {
                var origLimits = block._getLimits(),
                    testLimits = {
                        minDate: origLimits.minDate.clone(),
                        maxDate: origLimits.maxDate.clone()
                    };

                block.setRange({
                    start: testLimits.minDate.subtract(20, 'days').format('YYYY-MM-DD'),
                    finish: testLimits.maxDate.subtract(20, 'days').format('YYYY-MM-DD')
                });

                expect(block.getRange()).to.deep.equal({
                    start: origLimits.minDate.format('YYYY-MM-DD'),
                    finish: testLimits.maxDate.format('YYYY-MM-DD')
                });
            });
        });

        describe('getDiff', function() {
            it('Должен возвращать разницу между датами начала и конца периода', function() {
                expect(block.getDiff('days')).to.equal(15);
            });
        });

        describe('isRelativeRange', function() {
            it('Должен вернуть false, если период выбран без использования шаблона', function() {
                block.setRange({ start: '2015-10-02', finish: '2015-10-10' });

                expect(block.isRelativeRange()).to.equal(false);
            });
            it('Должен вернуть true, если период выбран из шаблонов(история исключение)', function() {
                block.getRangesChooser().check('yesterday');

                expect(block.isRelativeRange()).to.equal(true);
            });
            it('Должен вернуть false, если период выбран из истории', function() {
                block.getRangesChooser().check('yesterday');
                block.getRangesChooser().check('september');

                expect(block.isRelativeRange()).to.equal(false);
            });
        });

        describe('Шаблонны периодов', function() {
            it('Должна быть кнопка с текстом «выбрать»', function() {
                expect(block.findBlockInside('dropdown-switcher', 'button').elem('text').text())
                    .to.equal('выбрать');
            });
            it('Должен установить новый период после выбора шаблона', function() {
                var yesterday = u.moment()
                        .add(-1, 'day')
                        .format('YYYY-MM-DD');

                block.getRangesChooser().check('yesterday');

                expect(block.getRange()).to.deep.equal({ start: yesterday, finish: yesterday });
            });
            it('Текст кнопки «выбрать» должен измениться на текст шаблона, после его выбора', function() {
                block.getRangesChooser().check('yesterday');

                expect(block.findBlockInside('dropdown-switcher', 'button').elem('text').text())
                    .to.equal('вчера');
            });
            it([
                'Текст кнопки «выбрать» должен измениться на текст шаблона',
                ' если период выбран с помощью календаря и выбранный период совпадает с одним из шаблонов'
            ].join(''), function() {
                var yesterday = u.moment()
                        .add(-1, 'day')
                        .format('YYYY-MM-DD');

                block.setRange({ start: yesterday, finish: yesterday });

                expect(block.findBlockInside('dropdown-switcher', 'button').elem('text').text())
                    .to.equal('вчера');
            });
            it('Должен быть выбран шаблон, если выбранный период совпадает с периодом из шаблона', function() {
                // в помощь DIRECT-55138
                var chooser = block.getRangesChooser(),
                    range = { finish: u.moment(new Date()).subtract(1, 'days').format('YYYY-MM-DD') };

                range.start = range.finish;

                block.setRange(range);

                expect(chooser.getSelected().name).to.equal('yesterday');
            });
            it('Должен быть выбран шаблон, если выбранный период совпадает и с шаблоном и с историей', function() {
                // шаблоны приоритетнее истории
                // в помощь DIRECT-55138
                var range = {
                    start: u.moment(new Date()).subtract(3, 'days').format('YYYY-MM-DD'),
                    name: 'last'
                },
                chooser = block.getRangesChooser();
                range.finish = range.start;

                block.setRange(range);

                expect(chooser.getSelected().name).to.equal('3-days-ago');
            });

            describe('Взаимодействие двух календарей', function() {
                var block2;

                beforeEach(function() {
                    block2 = createBlock({
                        id: 'second-calendar',
                        ranges: [{
                            title: 'предыдущий период',
                            name: 'previous-period',
                            start: { period: -1 },
                            finish: { period: -1 },
                            dependId: 'first-calendar'
                        }]
                    });
                });

                after(function() {
                    block2.destruct();
                });

                it('Должен выбрать шаблон относительно даты другого календаря', function() {
                    block2.getRangesChooser().check('previous-period');

                    expect(block2.getRange()).to.deep.equal({ start: '2015-09-19', finish: '2015-10-04' });
                });

                it('Календарь в котором выбрам относительный шаблон должен менять период при изменении ' +
                    'периода календаря от которого зависит', function() {
                    block2.getRangesChooser().check('previous-period');
                    block.setRange({ start: '2016-01-10', finish: '2016-01-10' });

                    expect(block2.getRange()).to.deep.equal({ start: '2016-01-09', finish: '2016-01-09' });
                });
            });
        });
    });

    describe('utils', function() {
        describe('isFullMonth', function() {
            it('Должен вернуть true, если период составляет ровно 1 месяц', function() {
                expect(u['b-date-range-picker'].isFullMonth('2015-10-01', '2015-10-31')).to.equal(true);
            });
            it('Должен вернуть false, если период не составляет 1 месяц', function() {
                expect(u['b-date-range-picker'].isFullMonth('2015-10-02', '2015-10-31')).to.equal(false);
            });
        });
        describe('prettifyPeriod', function() {
            it('Дата должна быть в формате D MMMM YYYY, если период составляет 1 день', function() {
                expect(u['b-date-range-picker'].prettifyPeriod('2015-10-01', '2015-10-01')).to.equal('1 октября 2015');
            });
            it('Дата должна быть в формате MMMM YYYY, если период составляет 1 месяц', function() {
                expect(u['b-date-range-picker'].prettifyPeriod('2015-10-01', '2015-10-31')).to.equal('октябрь&nbsp;2015');
            });
            it('Дата должна быть в формате D - D MMMM YYYY, если дата начала и конца находятся в одном месяце', function() {
                expect(u['b-date-range-picker'].prettifyPeriod('2015-10-02', '2015-10-03'))
                    .to.equal('2&nbsp;&ndash;&nbsp;3 октября 2015');
            });
            it([
                'Дата должна быть в формате D MMMM – D MMMM YYYY',
                'если дата начала и конца в рамках одного года но в разных месяцах'
            ].join(''), function() {
                expect(u['b-date-range-picker'].prettifyPeriod('2015-10-02', '2015-11-03'))
                    .to.equal('2 октября&nbsp;&ndash;&nbsp;3 ноября 2015');
            });
            it([
                'Дата должна быть в формате D MMMM YYYY – D MMMM YYYY',
                ' если дата начала и конца находятся в разных годах'
            ].join(''), function() {
                expect(u['b-date-range-picker'].prettifyPeriod('2014-10-02', '2015-11-03'))
                    .to.equal('2 октября 2014&nbsp;&ndash;&nbsp;3 ноября 2015');
            });
        });
        describe('getDateFrom', function() {
            it('Должен отнять 10 дней от переданной даты', function() {
                expect(u['b-date-range-picker'].getDateFrom(-10, '2015-10-31')).to.equal('2015-10-21');
            });
            it('Должен добавить 4 года переданной дате', function() {
                expect(u['b-date-range-picker'].getDateFrom({ years: 4 }, '2015-10-31')).to.equal('2019-10-31');
            });
        });
    });

});
