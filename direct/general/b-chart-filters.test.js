describe('b-chart-filters', function() {
    var sandbox,
        options,
        block;

    function createBlock(options) {
        options = options || {};

        block = u.getInitedBlock({
            block: 'b-chart-filters',
            mods: options.mods || {},
            content: options.content || []
        });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Методы', function() {
        beforeEach(function() {
            options = {
                content: [
                    {
                        block: 'b-chart-filter-control',
                        mods: { type: 'radio' },
                        mix: { block: 'b-chart-filters', elem: 'view' },

                        name: 'view',

                        control: {
                            values: [
                                {
                                    name: 'column',
                                    mods: { checked: 'yes', 'only-icon': 'yes', 'icon-size': '12' },
                                    content: [
                                        {
                                            block: 'radio-button',
                                            elem: 'icon',
                                            content:{
                                                block: 'icon',
                                                mods: { 'size-12': 'chart-columns' }
                                            }
                                        }

                                    ]
                                },
                                {
                                    name: 'line',
                                    mods: { 'only-icon': 'yes', 'icon-size': '12' },
                                    content: [
                                        {
                                            block: 'radio-button',
                                            elem: 'icon',
                                            content:{
                                                block: 'icon',
                                                mods: { 'size-12': 'chart-line' }
                                            }
                                        }

                                    ]
                                }
                            ]
                        }
                    },
                    {
                        block: 'b-chart-filter-control',
                        mods: { type: 'select' },
                        mix: { block: 'b-chart-filters', elem: 'columns' },

                        name: 'columns',

                        control: {
                            text: 'Столбец',
                            values: [
                                { text: 'Показы', value: 'shows', group: 'group1', selected: 'yes' },
                                { text: 'Клики', value: 'clicks', group: 'group1' }
                            ],
                            mods: {
                                multi: 'yes',
                                'group-limit': 2
                            },
                            messages: {
                                'filter-item': {
                                    'group-limit': 'Нельзя одновременно показать данные 3-х типов столбцов. Отключите часть параметров'
                                }
                            }
                        }

                    }
                ]
            };
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        it('getValue, должен получить текущие значения фильтров', function() {
            createBlock(options);

            expect(block.getValue())
                .that.is.an('array')
                .with.deep.property('[0].filter', 'view')
        });

        it('getValue, должен игнорировать выключенные фильтры', function() {
            createBlock(options);
            block.getFilter('columns').disable();

            var selected = block.getValue().map(u._.property('filter'));

            expect(selected).to.not.include('columns');
        });

        it('getFilter, должен вернуть фильтр', function() {
            createBlock(options);
            expect(block.getFilter('view').domElem.hasClass('b-chart-filters__view'))
                .to.be.eq(true)
        });

        it('disableFilter, должен выключить фильтр', function() {
            createBlock(options);
            block.disableFilter('view');
            expect(block.getFilter('view').getFilterBlock()).to.haveMod('disabled', 'yes');
        });

        it('enableFilter, должен включить фильтр', function() {
            createBlock(options);
            block.disableFilter('view');
            block.enableFilter('view');
            expect(block.getFilter('view').getFilterBlock()).to.not.haveMod('disabled', 'yes');
        });
    });

});

