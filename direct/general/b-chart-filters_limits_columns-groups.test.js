describe('b-chart-filters', function() {
    var options,
        block;

    function createBlock(options) {
        options = options || {};

        block = u.getInitedBlock({
            block: 'b-chart-filters',
            mods: options.mods || {},
            content: options.content || []
        });
    }

    describe('Модификаторы', function() {

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('limits', function() {
            beforeEach(function() {
                options = {
                    mods: { limits: 'columns-groups' },

                    content: [
                        {
                            block: 'b-chart-filter-control',
                            mods: { type: 'select' },
                            mix: { block: 'b-chart-filters', elem: 'columns' },
                            name: 'columns',

                            control: {
                                mods: {
                                    multi: 'yes',
                                    'group-limit': 2
                                },
                                values: [
                                    { text: 'Показы', value: 'shows', group: 'group1' },
                                    { text: 'Клики', value: 'clicks', group: 'group1' },
                                    { text: 'CTR, %', value: 'ctr', group: 'group2' },
                                    { text: 'Цена, руб', value: 'price', group: 'group3' }
                                ]
                            }
                        },
                        {
                            block: 'b-chart-filter-control',
                            mods: { type: 'select' },
                            mix: { block: 'b-chart-filters', elem: 'groupBy' },
                            name: 'groupBy',

                            control: {
                                text: 'Срез',
                                values: [
                                    { text: 'Пол', value: 'gender' },
                                    { text: 'Возраст', value: 'age' }
                                ],
                                messages: {
                                    switcher: {
                                        disabled: 'Для отображения среза оставьте 1 столбец'
                                    }
                                }
                            }

                        }
                    ]
                };
                createBlock(options);
            });

            it('Если фильтре columns нет выбранных элементов, то фильтр groups выключен', function() {

                expect(block.getFilter('groupBy').getFilterBlock()).to.haveMod('disabled', 'yes');
            });

            it('Если фильтре columns выбран один вариант, то фильтр groups доступен', function() {
                block.setValue('columns', ['shows']);

                expect(block.getFilter('groupBy').getFilterBlock()).to.not.haveMod('disabled', 'yes');
            });

            it('Если фильтре columns выбраны два варианта, то фильтр groups не доступен', function() {
                block.setValue('columns', ['shows', 'clicks']);

                expect(block.getFilter('groupBy').getFilterBlock()).to.haveMod('disabled', 'yes');
            });

            it('Если фильтре groups выбран один вариант, то фильтр columns переходит в режим одиночного выбора', function() {
                block.setValue('columns', ['shows']);
                block.setValue('groupBy', ['age']);

                expect(block.getFilter('columns').getFilterBlock()).to.not.haveMod('multi', 'yes');
            });

        })
    });
});

