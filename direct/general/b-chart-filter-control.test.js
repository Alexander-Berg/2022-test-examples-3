describe('b-chart-filter-control', function() {
    var sandbox,
        options,
        block;

    function createBlock(options) {
        options = options || {};

        block = u.getInitedBlock({
            block: 'b-chart-filter-control',
            mods: options.mods || {},
            name: options.name || [],
            control: {
                values: options.values || []
            }
        });
    }

    afterEach(function() {
        block.destruct && block.destruct();
    });

    describe('Мод radio', function() {
        beforeEach(function() {
            options = {
                mods:{ type: 'radio' },
                name: 'view',
                values: [
                    { name: 'line', content: 'line', mods: { checked: 'yes'} },
                    { name: 'bar', content: 'bar' }
                ]
            };
        });

        it('Метод getValue возращает значение', function() {
            createBlock(options);

            expect(block.getValue())
                .to.have.all.keys('filter', 'data');
        });

        it('При изменении значения, блок генерирует событие change', function() {
            createBlock(options);

            expect(block).to.triggerEvent('change', function() {
                block.setValue('bar')
            });
        });

        it('Метод disable, должен выключить фильтр', function() {
            createBlock(options);
            block.disable();
            expect(block.getFilterBlock()).to.haveMod('disabled', 'yes');
        });

        it('Метод enable, должен включить фильтр', function() {
            createBlock(options);
            block.disable();
            block.enable();
            expect(block.getFilterBlock()).to.not.haveMod('disabled', 'yes');
        });

    });

    describe('Мод select', function() {
        beforeEach(function() {
            options = {
                block: 'b-chart-filter-control',
                mods: { type: 'select' },

                name: 'columns',
                values: [
                    { text: 'Показы', value: 'shows', group: 'group1', selected: 'yes' },
                    { text: 'Клики', value: 'clicks', group: 'group1' }
                ]
            };
        });

        it('Метод getValue возращает значение', function() {
            createBlock(options);

            expect(block.getValue())
                .to.have.all.keys('filter', 'data');
        });

        it('При изменении значения, блок генерирует событие change', function() {
            createBlock(options);

            expect(block).to.triggerEvent('change', function() {
                block.setValue(['clicks'])
            });
        });

        it('Метод disable, должен выключить фильтр', function() {
            createBlock(options);
            block.disable();
            expect(block.getFilterBlock()).to.haveMod('disabled', 'yes');
        });

        it('Метод enable, должен включить фильтр', function() {
            createBlock(options);
            block.disable();
            block.enable();
            expect(block.getFilterBlock()).to.not.haveMod('disabled', 'yes');
        });

    });

});

