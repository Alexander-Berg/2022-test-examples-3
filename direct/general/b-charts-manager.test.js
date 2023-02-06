describe('b-charts-manager', function() {
    var sandbox,
        block,
        def,
        constStub,
        response = [{ type: 'column', series: [{ data: ['1'] }] }],
        responseEmpty = [{ type: 'column', series: [{ data: [ ] }] }],
        responseLarge = [{ type: 'column', series: [{ data: u._.range(100) }] }];

    function tick() {
        sandbox.clock.tick(1000);
    }

    function createBlock(options) {
        block = u.createBlock({
            block: 'b-charts-manager',
            js: options.js || {},
            content: u._.compact([
                options.switcher && { elem: 'switcher' },
                options.filters && {
                    elem: 'filters',
                    columns: options.filters.columns || [],
                    groups: options.filters.groups || []
                },
                options.export && {
                    elem: 'export',
                    types: options.export.types || [],
                    print: options.export.print
                },
                { elem: 'chart' }
            ])
        }, { inject: true });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        sandbox.stub(BEM.blocks['i-chart-stat-data'], 'get').callsFake(function() {
            def = $.Deferred();

            return def;
        });
        constStub = u.stubCurrencies2(sandbox);
        constStub.withArgs('rights').returns({ enableStatsMultiClientsMode: false });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Поведение блока', function(){

        var options = {};

        beforeEach(function() {
            options = {
                js: {
                    clientCurrency: 'RUB',
                    reportData: {
                        filters: {},
                        groupByDate: 'day',
                        withNds: 1,
                        date: { from: '2017-02-09', to: '2017-02-16' }
                    }
                },
                switcher: true,
                filters: {
                    columns: [
                        { text: 'Показы', value: 'shows', selected: 'yes' },
                        { text: 'Клики', value: 'clicks' }
                    ],
                    groups: [
                        { text: 'Пол', value: 'gender', group: 'gender', selected: 'yes' }
                    ]
                },
                export: {
                    types: [],
                    print: true
                }
            };
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Переключатель', function() {

            beforeEach(function() {
                createBlock(options);
            });

            it('Содержит переключатель c текстом "Показать график"', function() {
                expect(block.elem('show-switcher-text').is(':visible')).to.be.true;
            });

            describe('Клик по переключателю', function() {

                beforeEach(function() {
                    block.elem('switcher').click();
                    tick();
                });

                it('Прячется "Показать график"', function() {
                    expect(block.elem('show-switcher-text').is(':visible')).to.be.false;
                });

                it('Появляется "Скрыть график"', function() {
                    expect(block.elem('hide-switcher-text').is(':visible')).to.be.true;
                });

                it('Создается график (блок)', function() {
                    expect(block).to.haveBlock('b-chart');
                });

            });

        });

        describe('Кнопка export', function() {

            var exportBlock,
                chartBlock;

            beforeEach(function() {
                createBlock(options);
                block.setMod('show', 'yes');

                def.resolveWith(block, response);
                exportBlock = block.findBlockInside('export', 'b-dropdown-trigger');
                chartBlock = block.findBlockInside('b-chart');

                sandbox.stub(chartBlock, 'exportChart');
            });

            it('Выбор пункта print, вызывает exportChart у блока b-chart с аргументом print', function() {
                exportBlock.trigger('export', { name: 'print' });

                expect(chartBlock.exportChart.args[0][0].print).to.be.true;
            });

            it('Выбор остальных пунктов вызывает метод exportChart и передает type c MIME', function() {
                exportBlock.trigger('export', { MIME: 'TEST/MIME' });

                expect(chartBlock.exportChart.args[0][0].type).to.be.eq('TEST/MIME');
            });

        });

        describe('Изменения фильтров', function() {

            var exportBlock,
                filterBlock,
                chartBlock;

            beforeEach(function() {
                createBlock(options);
                block.setMod('show', 'yes');
                def.resolveWith(block, response);

                sandbox.clock.tick(500);

                chartBlock = block.findBlockInside('b-chart');
                exportBlock = block.findBlockInside('export', 'b-dropdown-trigger');
                filterBlock = block.findBlockInside('filters', 'b-chart-filters');
            });

            describe('Переключение вида (view)', function() {

                describe('Обычное', function() {

                    beforeEach(function() {
                        sandbox.stub(chartBlock, 'setMod');
                    });

                    it('Переключение вида (view) на line', function() {
                        filterBlock.trigger('change', { filter: 'view', data: { currentVal: 'line' } });

                        expect(chartBlock.setMod.args[0][1]).to.be.eq('line');
                    });

                    it('Переключение вида (view) на column', function() {
                        filterBlock.trigger('change', { filter: 'view', data: { currentVal: 'column' } });

                        expect(chartBlock.setMod.args[0][1]).to.be.eq('column');
                    });

                });



                describe('Со срезами', function() {

                    beforeEach(function() {
                        chartBlock.setMod('stacking', 'yes');
                        sandbox.stub(chartBlock, 'setMod');
                    });

                    it('Переключение вида (view) на line-area ', function() { // если включены срезы выбрается area
                        filterBlock.trigger('change', { filter: 'view', data: { currentVal: 'line' } });

                        expect(chartBlock.setMod.args[0][1]).to.be.eq('area');
                    });

                    it('Переключение вида (view) на column', function() {
                        filterBlock.trigger('change', { filter: 'view', data: { currentVal: 'column' } });

                        expect(chartBlock.setMod.args[0][1]).to.be.eq('column');
                    });

                });

            });

            describe('Выбор столбцов/срезов', function() {

                describe('Общее', function() {

                    describe('Если не выбраны columns', function() {

                        beforeEach(function() {
                            sandbox.stub(block, 'buildChart');
                            sandbox.stub(chartBlock, 'showMessage');
                            filterBlock.setValue('columns', []);
                            tick();
                        });

                        it('Показывается сообщение', function() {
                            expect(chartBlock.showMessage.called).to.be.true;
                        });

                        it('Вызывается buildChart', function() {
                            expect(block.buildChart.called).to.be.true;
                        });

                    });

                    describe('Во время вызова', function() {

                        beforeEach(function() {
                            filterBlock.setValue('columns', ['clicks']);
                            tick();
                        });

                        it('Фильтр view выключен', function() {
                            expect(filterBlock.getFilter('view').getFilterBlock()).to.haveMod('disabled');
                        });

                        it('Показывается спиннер у b-chart', function() {
                            expect(block.findBlockInside('b-chart').findElem('spin').is(':visible')).to.be.true;
                        });

                        it('Кнопка export выключена', function() {
                            expect(exportBlock.findBlockInside('dropdown2')).to.haveMod('disabled');
                        });

                    });

                    describe('После вызова', function() {

                        it('Фильтр view включен', function() {
                            filterBlock.setValue('columns', ['clicks']);
                            tick();
                            def.resolveWith(block, response);

                            expect(filterBlock.getFilter('view').getFilterBlock()).to.not.haveMod('disabled');
                        });

                        it('Кнопка export включена', function() {
                            filterBlock.setValue('columns', ['clicks']);
                            tick();
                            def.resolveWith(block, response);

                            expect(exportBlock.findBlockInside('dropdown2')).to.not.haveMod('disabled');
                        });

                    });

                });

                describe('После успешного получения ответа', function() {

                    beforeEach(function() {
                        sandbox.stub(block, 'buildChart');
                        block.findBlockInside('filters', 'b-chart-filters').setValue('columns', ['clicks']);
                        tick();
                        sandbox.stub(chartBlock, 'showErrorMessage');
                    });

                    it('Перестраивается график', function() {
                        def.resolveWith(block, response);

                        expect(block.buildChart.called).to.be.true;
                    });

                    it('Фильтр view переключается в line, если много данных', function() {
                        def.resolveWith(block, responseLarge);

                        expect(filterBlock.getFilter('view').getValue()).to.have.property('data', 'line');
                    });

                    it('Показывается сообщение, если данных нет', function() {
                        def.resolveWith(block, responseEmpty);

                        expect(chartBlock.showErrorMessage.called).to.be.true;
                    });

                });

                describe('После неудачного запроса', function() {

                    beforeEach(function() {
                        block.findBlockInside('filters', 'b-chart-filters').setValue('columns', ['clicks']);
                        tick();
                        chartBlock = block.findBlockInside('b-chart');
                        sandbox.stub(chartBlock, 'showErrorMessage');
                        sandbox.stub(BEM.blocks['b-confirm'], 'open');
                    });

                    describe('Если timeout', function() {

                        beforeEach(function() { def.rejectWith(block, [{}, 'timeout']); });

                        it('Показываем alert', function() {
                            expect(BEM.blocks['b-confirm'].open.called).to.be.true;
                        });

                        it('Показываем ошибку в самом графике', function() {
                            expect(chartBlock.showErrorMessage.called).to.be.true;
                        });
                    });

                    describe('Если слишком много данных', function() {

                        beforeEach(function() { def.rejectWith(block, [{}, 'too_much']); });

                        it('Показываем alert', function() {
                            expect(BEM.blocks['b-confirm'].open.called).to.be.true;
                        });

                        it('Показываем ошибку в самом графике', function() {
                            expect(chartBlock.showErrorMessage.called).to.be.true;
                        });

                    });

                    describe('Если что-то другое', function() {

                        beforeEach(function() { def.rejectWith(block, [{}, 'some_else']); });

                        it('Показываем alert', function() {
                            expect(BEM.blocks['b-confirm'].open.called).to.be.true;
                        });

                        it('Показываем ошибку в самом графике', function() {
                            expect(chartBlock.showErrorMessage.called).to.be.true;
                        });

                    });

                });

            });

        });

    });

    describe('utils', function() {

        describe('applyGroup', function() {

            function testGroup(groupName, listNames) {
                describe('У следующих измерений группа = ' + groupName, function() {
                    var list = listNames.map(function(name) {
                            return { value: name }
                        }),
                        result;

                    before(function() {
                        result = u['b-charts-manager'].applyGroup(list);
                    });

                    listNames.forEach(function(name) {
                        it(name, function() {
                            var appliedName = u._.find(result, function(item) {
                                return item.value === name;
                            })
                            expect(appliedName).to.have.property('group', groupName);
                        })
                    });
                });
            }

            testGroup('percent', [
                'ctr',
                'ectr',
                'bounce_ratio',
                'aconv',
                'winrate',
                'agoalcrr'
            ]);

            testGroup('quantitative', [
                'shows',
                'clicks',
                'fp_clicks_avg_pos',
                'fp_shows_avg_pos',
                'eshows',
                'avg_x',
                'adepth',
                'agoalnum',
                'agoalroi'
            ]);

            testGroup('currency', [
                'sum',
                'av_sum',
                'agoalcost',
                'agoalincome'
            ]);

        });

    });

});

