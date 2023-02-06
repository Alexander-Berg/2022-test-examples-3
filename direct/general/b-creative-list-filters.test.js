describe('b-creative-list-filters', function() {
    var filtersDataStub = {
            busines_type: {
                name: 'Тип бизнеса',
                values: [{ name: 'Торговля', value: 'retail' }, { name: 'Автомобили', value: 'auto' }]
            },
            type: {
                name: 'Тематика',
                values: [{ name: '111', value: 'NNN' }]
            },
            layoyut: {
                name: 'Макет',
                values: [{ name: 'bla', value: 'NNN', img_src: '...' }]
            },
            size: {
                name: 'Размер',
                values: [
                { name: '100x100', value: '100x100' },
                { name: '200x100', value: '200x100' },
                { name: '300x100', value: '300x100' },
                { name: '400x100', value: '400x100' },
                { name: '600x100', value: '600x100' },
                { name: '700x100', value: '700x100' },
                { name: '800x100', value: '800x100' },
                { name: '900x100', value: '900x100' }
                ]
            },
            create_time: {
                name: 'Время создания',
                values: [{ value: 'NNN', name: '2016-01-01 00:00:00' }]
            },
            campaigns: {
                name: 'Привязка к кампании',
                values: [{ value: '263', name: '№263 — Холодильники' }]
            }
        },
        block,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });

        // В песочнице нет b-page, падает на BEM.blocks['b-page'].getInstance()
        BEM.blocks['b-page'] = {
            getInstance: function() {
                return {
                    domElem: $('.dev-page')
                }
            }
        };
    });

    afterEach(function() {
        sandbox.restore();
        block && block.destruct()
    });

    describe('Начальные данные', function() {
        beforeEach(function() {
            var filters = [{ filter: 'busines_type', value: 'auto' }];

            block = u.createBlock({
                block: 'b-creative-list-filters',
                filters: filters,
                order: 'asc',
                search: 'bla',
                filtersData: filtersDataStub,
                content: [
                    {
                        elem: 'wrapper',
                        content: [
                            {
                                elem: 'controls-panel',
                                filters: filters,
                                filtersData: filtersDataStub
                            }
                        ]
                    },
                    {
                        elem: 'filters-view',
                        filters: filters,
                        filtersData: filtersDataStub
                    }
                ]
            }, { inject: true });
        });

        it('Если блоку передано order: "asc", то getValue должно возвращать order=asc', function() {
            expect(block.getValue().order).to.be.equal('asc');
        });

        it('Если блоку передано search: "bla", то getValue должно возвращать search=bla', function() {
            expect(block.getValue().search).to.be.equal('bla');
        });

        it('Если блоку передано filters: [{ filter: \'busines_type\', value: \'auto\' }], то getValue должно возвращать filters=[{ filter: \'busines_type\', value: \'auto\' }]', function() {
            expect(block.getValue().filters).to.eql([{ filter: 'busines_type', value: 'auto' }]);
        });
    });

    describe('Действия пользователя', function() {
        beforeEach(function() {
            var filters = [{ filter: 'busines_type', value: 'auto' }];

            block = u.createBlock({
                block: 'b-creative-list-filters',
                filters: filters,
                order: 'asc',
                search: 'bla',
                filtersData: filtersDataStub,
                content: [
                    {
                        elem: 'wrapper',
                        content: [
                            {
                                elem: 'controls-panel',
                                filters: filters,
                                filtersData: filtersDataStub
                            }
                        ]
                    },
                    {
                        elem: 'filters-view',
                        filters: filters,
                        filtersData: filtersDataStub
                    }
                ]
            }, { inject: true });
        });

        it('Пользователь ввел blabla в строку поиска. В данных search стало равно blabla', function() {
            block.findBlockOn('search-control', 'input').val('blabla');

            expect(block.getValue().search).to.be.equal('blabla');
        });

        it('Пользователь выбрал порядок desc в контролле для выбора сортировки. В данных order стало равно desc', function() {
            block.findBlockOn('filters-sort-chooser', 'b-chooser').check('desc');

            expect(block.getValue().order).to.be.equal('desc');
        });

        it('Пользователь выбрал filter: campaigns и value 263. Фильтр { filter: campaigns, value: 263 } должен добавиться в список фильтров', function() {
            block.findBlockOn('filters-chooser', 'b-chooser').check('campaigns');
            sandbox.clock.tick(100);
            block.findBlockInside(block._subFilterPopup.domElem, 'b-chooser').check('263');

            expect(block.getValue().filters).to.eql([
                { filter: 'busines_type', value: 'auto' },
                { filter: 'campaigns', value: '263' }
            ]);
        });

        it('Если пользователь снял выделение с filter: busines_type и value auto, то фильтр удалится из значений', function() {
            block.findBlockOn('filters-chooser', 'b-chooser').check('busines-type');
            sandbox.clock.tick(100);
            block.findBlockInside(block._subFilterPopup.domElem, 'b-chooser').uncheck('auto');

            expect(block.getValue().filters.length).to.be.equal(0);
        });
    });
});
