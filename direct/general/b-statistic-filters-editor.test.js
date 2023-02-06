describe('b-statistic-filters-editor', function() {
    var block,
        clock,
        filters = {},
        extraParams = {
            tags: {
                path: ['Метки'],
                backendName: 'tags',
                tags: [{ title: 'избранное', id: 'tag-1' }]
            },
            agoalincome: {
                path: ['Доход (руб.)'],
                backendName: 'agoalincome'
            },
            adepth: {
                path: ['Глубина (%)'],
                backendName: 'adepth'
            },
            campaign: {
                campaigns: [
                    { title: 'Кампания 1', id: 'campaign-1', favorite: 1 },
                    { title: 'Кампания 2', id: 'campaign-2', favorite: 0 }
                ],
                path: ['Кампании', '№ / название'],
                backendName: 'campaign'
            },
            'click-place': {
                path: ['Место клика'],
                backendName: 'click_place'
            },
            contexttype: {
                path: ['Условия показа'],
                backendName: 'contexttype',
                campTypes: ['dynamic', 'performance']
            },
            ctr: {
                path: ['CTR'],
                backendName: 'ctr'
            },
            region: {
                path: ['Регион'],
                backendName: 'region'
            },
            phrase: {
                path: ['Текст фразы'],
                backendName: 'phrase'
            },
            adgroup: {
                path: ['Группы', '№ / название'],
                backendName: 'adgroup'
            },
            'banner-text': {
                path: ['Группы', '№ / название'],
                backendName: 'banner-text'
            },
            'search-query': {
                path: ['Площадки', 'Название'],
                backendName: 'search-query'
            }
        },
        filtersByGroups = [
            { text: '№ / название', name: 'campaign' },
            { text: 'Метки', name: 'tags' },
            { text: 'Доход (руб.)', name: 'agoalincome' },
            { text: iget2('b-statistic-filters-editor', 'mesto-klika', 'Место клика'), name: 'click_place' }
        ],
        createBlock = function(external, appendTo, options) {
            var blockTree,
                params = $.extend({
                    mods: {},
                    filters: filters,
                    extraParams: extraParams,
                    filtersByGroups: filtersByGroups,
                    calculatedExclusion: { hidden: {} }
                }, external),
                templates = (options || {}).templates || [];

            blockTree = u.getDOMTree({
                block: 'b-statistic-filters-editor',
                js: { extraParams: params.extraParams, exclusionsRules: {}, calculatedExclusion: { hidden: {} } },
                mods: params.mods,
                content: [
                    {
                        elem: 'header',
                        templates: templates
                    },
                    {
                        elem: 'table',
                        content: Object.keys(params.filters).map(function(name, index) {
                            var replacedName = u.beminize(name);

                            return params.extraParams[replacedName] && {
                                elem: 'row',
                                elemMods: { filter: replacedName },
                                filter: {
                                    name: replacedName,
                                    operations: params.filters[name] || {},
                                    extraParams: params.extraParams[replacedName]
                                },
                                index: index + 1
                            }
                        }, this)
                    },
                    {
                        elem: 'dropdown',
                        elemMods: { type: 'filters-chooser' },
                        filtersByGroups: params.filtersByGroups,
                        calculatedExclusion: { hidden: {} }
                    },
                    {
                        elem: 'dropdown',
                        elemMods: { type: 'save-template' },
                        filters: params.filters
                    }
                ]
            });

            BEM.DOM.append($(appendTo || '</div>'), blockTree);

            return $(blockTree).bem('b-statistic-filters-editor');
        };

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
    });

    describe('Методы', function() {
        beforeEach(function() {
            block = createBlock();
        });

        describe('addFilter()', function() {
            it('Должен построить новый фильтр', function() {
                block.addFilter({ name: 'tags' });

                expect(block).to.haveElem('filter', 'type', 'tags');
            });
            it('Должен триггерить событие add', function() {
                sinon.spy(block, 'trigger');
                block.addFilter({ name: 'tags' });

                expect(block).to.triggerEvent('add', { name: 'tags' });
            });
        });

        describe('getFiltersData()', function() {
            var sandbox;
            beforeEach(function() {
                sandbox = sinon.sandbox.create();
            });

            afterEach(function() {
                sandbox.restore();
            })

            it('Должен возвращать данные по давленым фильтрам', function() {
                block
                    .addFilter({
                        name: 'tags',
                        operations: { eq: ['избранное'] }
                    })
                    .addFilter({
                        name: 'agoalincome',
                        operations: { lt: 1, gt: 10 }
                    });

                expect(block.getFiltersData())
                    .to.deep.equal({
                        tags: { eq: ['избранное'] },
                        agoalincome: { lt: 1, gt: 10 }
                    });
            });

            it('Не должен включать фильтры с предупреждением', function() {
                var ctr;

                block
                    .addFilter({
                        name: 'ctr'
                    })
                    .addFilter({
                        name: 'agoalincome',
                        operations: { lt: 1, gt: 10 }
                    });
                ctr = block.elemInstance('filter', 'type', 'ctr');
                sandbox.stub(ctr, 'hasExclusionWarnings').callsFake(function() {
                    return true;
                });

                expect(block.getFiltersData()).to.deep.equal({
                    agoalincome: { lt: 1, gt: 10 }
                })
            });

            it('Должен включать фильтры с предупреждением, если передан параметр', function() {
                var ctr;

                block
                    .addFilter({
                        name: 'ctr'
                    })
                    .addFilter({
                        name: 'agoalincome',
                        operations: { lt: 1, gt: 10 }
                    });
                ctr = block.elemInstance('filter', 'type', 'ctr');
                sandbox.stub(ctr, 'hasExclusionWarnings').callsFake(function() {
                    return true;
                });

                expect(block.getFiltersData({ ctr: true })).to.deep.equal({
                    ctr: {lt: ''},
                    agoalincome: { lt: 1, gt: 10 }
                })
            })
        });
        describe('fill()', function() {
            var sandbox;

            beforeEach(function() {
                sandbox = sinon.sandbox.create();
                sandbox.stub(u, 'consts').withArgs('rights').returns({});
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('Должен удалить все фильтры и построит новые', function() {
                block
                    .addFilter({
                        name: 'tags',
                        operations: { eq: ['избранное'] }
                    })
                    .addFilter({
                        name: 'agoalincome',
                        operations: { lt: 1, gt: 10 }
                    });

                block.fill({
                    agoalincome: { lt: 10, gt: 20 },
                    campaign: { eq: ['campaign-1'] }
                });

                expect(block.getFiltersData())
                    .to.deep.equal({
                        campaign: { eq: ['campaign-1'] },
                        agoalincome: { lt: 10, gt: 20 }
                    });
            });
        });
        describe('isTemplateChanged()', function() {
            var sandbox;

            beforeEach(function() {
                sandbox = sinon.sandbox.create();
                sandbox.stub(u, 'consts').withArgs('rights').returns({});
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('Должен возвращать true, если шаблон изменения', function() {
                block.fill({
                    agoalincome: { lt: 10, gt: 20 },
                    campaign: { eq: ['campaign-1'] }
                });

                block.addFilter({
                    name: 'tags',
                    operations: { eq: ['избранное'] }
                });

                expect(block.isTemplateChanged()).to.equal(true);
            });
            it('Должен возвращать false, если шаблон не измененился', function() {
                block.fill({
                    agoalincome: { lt: 10, gt: 20 },
                    campaign: { eq: ['campaign-1'] }
                });

                block.addFilter({
                    name: 'tags',
                    operations: { eq: ['избранное'] }
                });

                block.elemInstance('row', 'filter', 'tags').remove();

                expect(block.isTemplateChanged()).to.equal(false);
            });
        });
        describe('isValid()', function() {
            it('Должен возвращать false, если есть фильтры с ошибками', function() {
                block.addFilter({
                    name: 'agoalincome',
                    operations: { eq: 'Text' }
                });

                expect(block.isValid()).to.equal(false);
            });
            it('Должен возвращать true, если в фильтрах отсутствуют ошибки', function() {
                block.addFilter({
                    name: 'agoalincome',
                    operations: { eq: 10 }
                });

                expect(block.isValid()).to.equal(true);
            });
        });
        describe('needCompareWarning()', function() {
            it('Должен возвращать true, если заданы фильтры по периодам(В, разница (%), разница)', function() {
                block.addFilter({
                    name: 'agoalincome',
                    operations: {
                        a: { eq: 1 },
                        b: { eq: 2 }
                    }
                });

                expect(block.needCompareWarning()).to.equal(true);
            });
            it('Должен возвращать false, если не заданы фильтры по периодам(В, разница (%), разница)', function() {
                block.addFilter({
                    name: 'agoalincome',
                    operations: {
                        a: { eq: 1 }
                    }
                });

                expect(block.needCompareWarning()).to.equal(false);
            });
        });
    });

    describe('В интерфейсе', function() {

        describe('', function() {
            beforeEach(function() {
                clock = sinon.useFakeTimers();
                block = createBlock(
                    {
                        filters: {
                            agoalincome: { eq: '' }
                        }
                    },
                    'body',

                    // Предварительно сохраненные шаблоны
                    {
                        templates: [
                            // Шаблон сравнения
                            {
                                data: {
                                    ctr: {
                                        a: { lt: '' }
                                    }
                                },
                                name: 'first_template',
                                cid: 17442632,
                                hasCompare: true
                            },

                            // Шаблон без сравнения
                            {
                                data: {
                                    ctr: {
                                        a: { lt: '' }
                                    }
                                },
                                name: 'second_template',
                                cid: 17442132,
                                hasCompare: false
                            }
                        ]
                    }
                );

                block.on('fill', function(e, data) {
                    block.setMod('compare', data.needCompare ? 'yes' : '');
                });
            });

            afterEach(function() {
                clock.restore();
            });

            it('Должна быть не активна кнопка «Сохранить фильтр», если есть фильтры с ошибками', function() {
                var templateSaving = block.getDropdown('save-template').getTemplateSaving(),
                    input = block.findBlockInside(block.findElem('control', 'type', 'input'), 'input');

                input.val('Text');

                expect(templateSaving.findBlockInside('switcher', 'button'))
                    .to.haveMod('disabled', 'yes');
            });
            it('Должна быть активна кнопка «Сохранить фильтр», если исправили ошибку в фильтре', function() {
                var templateSaving = block.getDropdown('save-template').getTemplateSaving(),
                    input = block.findBlockInside(block.findElem('control', 'type', 'input'), 'input');

                input.val('Text');
                input.val(1);

                expect(templateSaving.findBlockInside('switcher', 'button'))
                    .to.not.haveMod('disabled', 'yes');
            });
            it('Должена удалиться строка с фильтром по клику на кнопку button_action_remove', function() {
                block.findElem('button', 'action', 'remove').click();

                clock.tick(0);

                expect(block).to.not.haveElem('row');
            });
            it([
                'Должна быть иконка сравнения «A/B» возле названия сохраненного шаблона фильтров' +
                ' если включено сравнение и есть фильтры относящиеся сравнению'
            ].join(' '), function() {
                block.elemInstance('dropdown')
                    .addTemplate('third_template', { ctr: { a: { lt: '' } } });

                expect(block).to.haveElems('compare-periods-icon', 2);
            });
            it([
                'Должна быть иконка сравнения «A/B» возле названия пересохраненного шаблона фильтров' +
                ' если до этого шаблон был без сравнениия'
            ].join(' '), function() {
                block.elemInstance('dropdown')
                    .addTemplate('second_template', { ctr: {a: {lt: ''} } });

                expect(block).to.haveElems('compare-periods-icon', 2);
            });
            it([
                'Не должна быть иконка сравнения «A/B» возле названия пересохраненного шаблона фильтров' +
                ' если до этого шаблон был для сравнениия'
            ].join(' '), function() {
                block.elemInstance('dropdown')
                    .addTemplate('first_template', { ctr: {} });

                expect(block).to.not.haveElems('compare-periods-icon');
            });
        });

        describe('Dropdown выбора фильтров', function() {
            var sandbox;

            beforeEach(function() {
                sandbox = sinon.sandbox.create();
                sandbox.stub(u, 'consts').withArgs('rights').returns({});

                block = createBlock();
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('Должен добавить новую строку с фильтром, после выбора фильтра', function() {
                block
                    .getDropdown('filters-chooser')
                    .getChooser()
                    .check('tags');

                expect(block).to.haveElem('filter', 'type', 'tags');
            });
            it('Должен закрываться попап после выбора фильтра', function() {
                var filtersChooser = block.getDropdown('filters-chooser');

                filtersChooser
                    .getChooser()
                    .check('tags');

                expect(filtersChooser.getPopup().isShown())
                    .to.equal(false);
            });
            it('Выбранный фильтр должен быть задизейблен', function() {
                var filtersChooser = block.getDropdown('filters-chooser');

                filtersChooser
                    .getChooser()
                    .check('tags');

                expect(filtersChooser.getChooser().getAll('disabled')[0])
                    .have.property('disabled', true);
            });
            it('Должна дизейблиться кнопка «+ Условие фильтрации», если выбраны все фильтры', function() {
                var chooser = block
                    .getDropdown('filters-chooser')
                    .getChooser();

                chooser
                    .getAll()
                    .forEach(function(filter) { chooser.check(filter.name) });

                expect(block.findBlockInside('filters-chooser-switcher', 'button'))
                    .to.haveMod('disabled', 'yes');
            });
            it('Должна быть активна кнопка «+ Условие фильтрации», если выбраны не все фильтры', function() {
                var chooser = block
                    .getDropdown('filters-chooser')
                    .getChooser();

                chooser
                    .getAll()
                    .forEach(function(filter) { chooser.check(filter.name) });

                block.elemInstance('row').remove();

                expect(block.findBlockInside('filters-chooser-switcher', 'button'))
                    .to.not.haveMod('disabled', 'yes');
            });
        });

    });

    describe('Фильтры', function() {

        describe('С пустым текстовым полем', function() {

            ['phrase', 'banner-text', 'adgroup', 'search-query'].map(function(filterName) {
                it(filterName + ' НЕ попадает в итоговый набор', function() {
                    block = createBlock();
                    block.addFilter({
                        name: filterName,
                        operations: { eq: '' }
                    });

                    expect(block.getFiltersData()).to.deep.equal({});
                });
                return filterName;
            });
        });

        describe('С заполненным текстовым полем', function() {

            ['phrase', 'banner-text', 'adgroup', 'search-query'].map(function(filterName) {
                it(filterName + ' попадает в итоговый набор', function() {
                    block = createBlock();
                    block.addFilter({
                        name: filterName,
                        operations: { eq: 'текст' }
                    });

                    var result = {};
                    result[filterName] = { eq: 'текст' };

                    expect(block.getFiltersData()).to.deep.equal(result);
                });
            });
        });

        beforeEach(function() {
            clock = sinon.useFakeTimers();
        });

        afterEach(function() {
            clock.restore();
        });

        describe('Числовые', function() {
            var getButton = function(instance, type) {
                    return instance
                        .findBlockInside(instance.findElem('button', 'action', type + '-operation'), 'button');
                },
                getRatio = function(instance) {
                    return instance.findBlockInside(instance.findElem('select', 'type', 'gt-eq-lt'), 'select');
                },
                getPeriod = function(instance) {
                    return instance.findBlockInside(instance.findElem('select', 'type', 'a-b-delta'), 'select');
                };

            describe('По умолчанию', function() {
                beforeEach(function() {
                    block = createBlock({
                        filters: {
                            agoalincome: {}
                        }
                    }, 'body');
                });

                it('Должна быть быть строка с одним элементом operation', function() {
                    expect(block.elemInstance('filter'))
                        .to.haveElems('operation', 1);
                });
                it('Инпут должен быть пустым', function() {
                    expect(block.elemInstance('filter').findBlockInside('input').val())
                        .to.equal('');
                });
                it('Значение селекта должно быть «меньше»', function() {
                    expect(getRatio(block.elemInstance('filter')).val())
                        .to.equal('lt');
                });
                it('Если фильтр целочисленный, должна быть кнопка равно', function() {
                    var options = getRatio(block.elemInstance('filter', 'type', 'agoalincome')).elem('option'),
                        hasEq = u._.some(options, function(option) {
                            return $(option).val() === 'eq';
                        });

                    expect(hasEq).to.be.true;
                });

                it('Если фильтр дробный, не должно быть кнопки равно', function() {
                    var options,
                        hasEq;

                    block.addFilter({ name: 'adepth' });

                    options = getRatio(block.elemInstance('filter', 'type', 'adepth')).elem('option');
                    hasEq = u._.some(options, function(option) {
                        return $(option).val() === 'eq';
                    }, false);

                    expect(hasEq).to.be.false;
                });
                describe('Кнопка «+»', function() {
                    it('Должна добавить элемент operation, по клику', function() {
                        var filter = block.elemInstance('filter');

                        getButton(filter, 'add').domElem.click();
                        clock.tick(0);

                        expect(block.elemInstance('filter')).to.haveElems('operation', 2);
                    });
                    it('Не должно быть одинаковых значений в селектах, после добавления', function() {
                        var filter = block.elemInstance('filter'),
                            selects = [];

                        getButton(filter, 'add').domElem.click();
                        clock.tick(0);

                        filter._forEachOperation(function(instance, periodSelect, ratioSelect) {
                            selects.push(ratioSelect.val());
                        });

                        expect(selects).to.deep.equal(['lt', 'gt']);
                    });
                    it('Должна быть активна, если один элемент operation и значение селекта «меньше»', function() {
                        expect(getButton(block.elemInstance('filter'), 'add'))
                            .to.not.haveMod('disabled', 'yes');
                    });
                    it('Должна быть активна, если один элемент operation и значение селекта «больше»', function() {
                        var filter = block.elemInstance('filter');

                        getRatio(filter).val('gt');
                        expect(getButton(filter, 'add')).to.not.haveMod('disabled', 'yes');
                    });
                    it('Должна быть не активна, если один элемент operation и значение селекта «равно»', function() {
                        var filter = block.elemInstance('filter');

                        getRatio(block.elemInstance('filter')).val('eq');
                        expect(getButton(block.elemInstance('filter'), 'add')).to.haveMod('disabled', 'yes');
                    });
                    it('Должна быть не активна, если в фильтре два элемента operation', function() {
                        var filter = block.elemInstance('filter');

                        getButton(filter, 'add').domElem.click();
                        clock.tick(0);

                        filter.findBlocksInside(filter.findElem('button', 'action', 'add-operation'), 'button')
                            .forEach(function(button) {
                                expect(button).to.haveMod('disabled', 'yes')
                            });
                    });
                });

                describe('Метод checkExclusion()', function() {
                    // в помощь DIRECT-54939
                    var block2;

                    after(function() {
                        block2.destruct();
                    });

                    it('Должно показывается предупреждение под самой последней строкой', function() {
                        block2 = createBlock({ filters: { ctr: {} } }, 'body');

                        var filter = block2.elemInstance('filter');

                        block2.checkExclusion([],['click_place','contextcond_orig']);

                        getButton(filter, 'add').domElem.click();

                        clock.tick(250);

                        expect(block2.elem('filter-warnings').next()).to.have.lengthOf(0);
                    });
                    it('Должен показывать одно предупреждение', function() {
                        var block = createBlock();
                        block.params.extraParams.ctr.campType = 'text';
                        block.addFilter({ name: 'ctr' });
                        block.checkExclusion([],['click_place','contextcond_orig']);
                        expect(block).to.haveElems('filter-warnings', 1);
                    });
                    it('Должен показывать два предупреждения', function() {
                        var block = createBlock();
                        block.params.extraParams.ctr.campType = 'performance';
                        block.addFilter({ name: 'ctr' });
                        block.checkExclusion([],['click_place','contextcond_orig']);
                        expect(block).to.haveElems('filter-warnings', 2);
                    });
                });

                describe('Метод validate()', function() {
                    it ('Должен быть элемент input-error', function() {
                        var elemFilter = block.elemInstance('filter', 'type', 'agoalincome');

                        expect(elemFilter).to.haveElem('input-error');
                    });
                    it('Должна показывается ошибка под невалидным значением в инпуте', function() {
                        var elemFilter = block.elemInstance('filter', 'type', 'agoalincome');
                        elemFilter.findBlockInside('input').val('one');

                        expect(elemFilter.elemInstance('operation').hasMod('error','yes')).to.be.true;
                    });
                    it('Валидное значение в инпуте. Ошибок быть не должно', function() {
                        var elemFilter = block.elemInstance('filter', 'type', 'agoalincome');
                        elemFilter.findBlockInside('input').val('1');

                        expect(elemFilter.elemInstance('operation').hasMod('error','yes')).to.be.false;
                    });
                });

                describe('Кнопка «-»', function() {
                    it('Должна удалять элемент operation, по клику', function() {
                        var filter = block.elemInstance('filter');

                        getButton(filter, 'add').domElem.click();
                        clock.tick(50);
                        getButton(filter, 'remove').domElem.click();
                        clock.tick(50);

                        expect(block.elemInstance('filter')).to.haveElems('operation', 1);
                    });
                    it('Должна быть активна, если фильтр содержит два элемента operation', function() {
                        var filter = block.elemInstance('filter');

                        getButton(filter, 'add').domElem.click();
                        clock.tick(0);

                        expect(getButton(filter, 'remove')).to.not.haveMod('disabled', 'yes');
                    });
                    it('Должна быть не активна, если фильтр содержит один элемент operation', function() {
                        var filter = block.elemInstance('filter');

                        expect(getButton(filter, 'remove')).to.haveMod('disabled', 'yes');
                    });
                });
                describe('getData()', function() {
                    it([
                        'Должен возвращать данные из селекта отношений и инпута',
                        '(одноуровневый хэш без периодов)'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: { lt: 10, gt: 20 }
                        });

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({ agoalincome: { lt: 10, gt: 20 } });
                    });
                });
                describe('needCompareWarning()', function() {
                    it('Должен возвращать false, если сравнение периодов отключено', function() {
                        block.fill({
                            agoalincome: { lt: 10, gt: 20 }
                        });

                        expect(block.elemInstance('filter').needCompareWarning())
                            .to.equal(false);
                    });
                });
            });

            describe('Режим сравнения периодов', function() {
                beforeEach(function() {
                    block = createBlock({
                        mods: { compare: 'yes' },
                        filters: {
                            agoalincome: {}
                        }
                    }, 'body');
                });

                describe('getData()', function() {
                    it('Должен возвращать данные из селекта периода, селекта отношений и инпута', function() {
                        block.fill({
                            agoalincome: {
                                a: { lt: 10, gt: 20 },
                                b: { eq: 20 }
                            }
                        });

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({
                                agoalincome: {
                                    a: { lt: 10, gt: 20 },
                                    b: { eq: 20 }
                                }
                            });
                    });
                });

                describe('needCompareWarning()', function() {
                    it('Должен возвращать true, если есть селекты с периодами В, разница (%), разница', function() {
                        block.fill({
                            agoalincome: {
                                a: { lt: 10 },
                                b: { lt: 10 }
                            }
                        });

                        expect(block.elemInstance('filter').needCompareWarning())
                            .to.equal(true);
                    });
                    it('Должен возвращать false, если нет селектов с периодами В, разница (%), разница', function() {
                        block.fill({
                            agoalincome: {
                                a: { lt: 10, gt: 20 }
                            }
                        });

                        expect(block.elemInstance('filter').needCompareWarning())
                            .to.equal(false);
                    });
                });

                it('Значение селекта периода по умолчанию должно быть «А»', function() {
                    expect(getPeriod(block.elemInstance('filter')).val())
                        .to.equal('a');
                });
                it([
                    'Должны пропасть все элементы operation со значениями периодов В, разница (%), разница',
                    ' если отключить сравнение'
                ].join(' '), function() {
                    block.fill({
                        agoalincome: {
                            a: { eq: '1' },
                            b: { eq: '1' },
                            delta: { eq: '1' },
                            absdelta: { eq: '1' }
                        }
                    });

                    block.delMod('compare');

                    block
                        .elemInstance('filter')
                        ._forEachOperation(function(instance, periodSelect) {
                            expect(periodSelect.val()).to.equal('a');
                        });
                });
                it([
                    'Должен добавиться элемент operation с периодом «А», если до отключения сравнения',
                    ' элементы operation содержали значения периодов В, разница (%), разница'
                ].join(' '), function() {
                    block.fill({
                        agoalincome: {
                            b: { eq: '1' },
                            delta: { eq: '1' },
                            absdelta: { eq: '1' }
                        }
                    });

                    block.delMod('compare');

                    block
                        .elemInstance('filter')
                        ._forEachOperation(function(instance, periodSelect) {
                            expect(periodSelect.val()).to.equal('a');
                        });
                });
                it([
                    'Значение селекта отношения должно измениться на доступное',
                    ' если поменять селект периода'
                ].join(' '), function() {
                    block.fill({
                        agoalincome: {
                            a: { lt: 10 },
                            b: { lt: 20 }
                        }
                    });

                    getPeriod(block.elemInstance('filter')).val('b');

                    expect(block.elemInstance('filter').getData())
                        .to.deep.equal({
                            agoalincome: {
                                b: { lt: 20, gt: 10 }
                            }
                        });
                });

                describe('Кнопка «+»', function() {
                    it('Должна добавиться новая строка после той где нажали на «+»', function() {
                        var filter = block.elemInstance('filter'),
                            _1stAddButton = getButton(filter, 'add'),
                            _1stOperDomNode = _1stAddButton.domElem.parent();

                        _1stAddButton.domElem.click();
                        clock.tick(0);

                        expect(filter).to.haveElems('operation', 2);
                        expect($(filter.elem('operation')[1]).prev().get(0))
                            .to.be.equal(_1stOperDomNode.get(0));
                    });
                    it([
                        'Должна быть не активна',
                        ' если невозможно составить новую комбинацию из периодов и отношений'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: {
                                a: { eq: '1' },
                                b: { eq: '1' },
                                delta: { eq: '1' },
                                absdelta: { eq: '1' }
                            }
                        });

                        expect(getButton(block.elemInstance('filter'), 'add')).to.haveMod('disabled', 'yes');
                    });
                    it([
                        'Должна добавить новую строку: период «A», отношение «меньше»',
                        ' если есть строка: период «A», отношение «больше»'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: { gt: '10' }
                        });

                        getButton(block.elemInstance('filter'), 'add').domElem.click();
                        clock.tick(0);

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({
                                agoalincome: { a: { gt: 10, lt: '' } }
                            });
                    });
                    it([
                        'Должна добавить новую строку: период «A», отношение «больше»',
                        ' если есть строка: период «A», отношение «меньше»'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: { lt: '10' }
                        });

                        getButton(block.elemInstance('filter'), 'add').domElem.click();
                        clock.tick(0);

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({
                                agoalincome: { a: { gt: '', lt: 10 } }
                            });
                    });
                    it([
                        'Должна добавить новую строку: период «B», отношение «меньше»',
                        ' если для периода «A» нет доступных отношений'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: { eq: '10' }
                        });

                        getButton(block.elemInstance('filter'), 'add').domElem.click();
                        clock.tick(0);

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({
                                agoalincome: { a: { eq: 10 }, b: { lt: '' } }
                            });
                    });
                    it([
                        'Должна добавить новую строку: период «A», отношение «меньше»',
                        ' если есть доступные отношения для периода «A»'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: { b: { eq: '10' } }
                        });

                        getButton(block.elemInstance('filter'), 'add').domElem.click();
                        clock.tick(0);

                        expect(block.elemInstance('filter').getData())
                            .to.deep.equal({ agoalincome: { a: { lt: '' }, b: { eq: 10 } } });
                    });
                });

                describe('Кнопка «-»', function() {
                    it('Должна убрать строку где была нажата кнопка "-"', function() {
                        var filter = block.elemInstance('filter'),
                            firstOper = filter.elem('operation').first();

                        // Добавить "__operation", затем один удалить и проверить как изменился его родитель.
                        // getButton возвращает первый упомянутый DOM-элемент,
                        // соответственно удалена будет первая строка

                        getButton(filter, 'add').domElem.click();
                        clock.tick(0);
                        expect(block.elemInstance('filter')).to.haveElems('operation', 2);

                        getButton(filter, 'remove').domElem.click();
                        clock.tick(0);
                        expect(firstOper.parent()).to.have.lengthOf(0); // потерял родителя - удален
                        expect(block.elemInstance('filter')).to.haveElems('operation', 1);
                    });
                    it([
                        'Должна быть активна',
                        ' если невозможно составить новую комбинацию из периодов и отношений'
                    ].join(' '), function() {
                        block.fill({
                            agoalincome: {
                                a: { eq: '1' },
                                b: { eq: '1' },
                                delta: { eq: '1' },
                                absdelta: { eq: '1' }
                            }
                        });

                        expect(getButton(block.elemInstance('filter'), 'remove')).to.not.haveMod('disabled', 'yes');
                    });
                });

            });

        });

        describe('Место клика(click-place)', function() {
            beforeEach(function() {
                block = createBlock({
                    filters: {
                        click_place: {}
                    }
                });
            });

            it([
                'Должны быть активны чекбатоны с номерами быстых ссылок',
                ' если чекбокс «быстрые ссылки» чекнут'
            ].join(' '), function() {
                var filter = block.elemInstance('filter');

                filter._getCheckbox('sitelinks').setMod('checked', 'yes');
                clock.tick(0);

                filter
                    .findBlocksInside('check-button')
                    .forEach(function(checkButton) {
                        expect((checkButton)).to.not.haveMod('disabled', 'yes')
                    });
            });
            it([
                'Должны быть не активны чекбатоны с номерами быстых ссылок',
                ' если чекбокс «быстрые ссылки» не чекнут'
            ].join(' '), function() {
                var filter = block.elemInstance('filter');

                filter._getCheckbox('sitelinks')
                    .setMod('checked', 'yes')
                    .delMod('checked');

                clock.tick(0);

                filter
                    .findBlocksInside('check-button')
                    .forEach(function(checkButton) {
                        expect((checkButton)).to.haveMod('disabled', 'yes')
                    });
            });
        });

        describe('В зависимости от типа кампании. Мастер отчетов на кампанию', function() {

            describe('Реклама мобильных приложений', function() {

                describe('Условия показа.', function() {
                    it('Должен иметь только условия: фразы, подбор аудитории, ' +
                        'автоматически добавленные фразы, синонимы, автотаргетинг', function() {
                        block = createBlock();
                        block.params.extraParams.contexttype.campType = 'mobile_content';
                        block.addFilter({ name: 'contexttype' });
                        expect(block.elem('control')).to.have.lengthOf(5);
                        ['phrases', 'retargeting', 'auto-added-phrases', 'synonym', 'relevance-match']
                            .forEach(function(name) {
                                expect(block).to.haveElem('control', 'name', name);
                            })
                    });
                });
            });

            describe('Динамические объявления', function() {

                describe('Условия показа.', function() {
                    it('Должен иметь только условия: условия нацеливания и фильтры', function() {
                        block = createBlock();
                        block.params.extraParams.contexttype.campType = 'dynamic';
                        block.addFilter({ name: 'contexttype' });
                        expect(block.elem('control')).to.have.lengthOf(2);
                        ['performance', 'dynamic'].forEach(function(name) {
                            expect(block).to.haveElem('control', 'name', name);
                        })
                    });
                });

            });

            describe('Текстово-графические объявления', function() {

                describe('Условия показа', function() {
                    it('Должен иметь только условия: фразы, подбор аудитории, автоматически добавленные фразы, ' +
                        'синонимы, автотаргетинг', function() {
                        block = createBlock();
                        block.params.extraParams.contexttype.campType = 'text';
                        block.addFilter({ name: 'contexttype' });
                        expect(block.elem('control')).to.have.lengthOf(5);
                        ['phrases', 'retargeting', 'auto-added-phrases', 'synonym', 'relevance-match']
                            .forEach(function(name) {
                                expect(block).to.haveElem('control', 'name', name);
                            });
                    });
                });

            });

        });
    });

});
