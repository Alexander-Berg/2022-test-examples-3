describe('b-dynamic-condition-edit', function() {
    var sandbox,
        block,
        blockTree,
        campaignDM,
        currency = 'RUB',
        tooLowPrice = 0.29,
        tooHighPrice = 25001,
        validLowPrice = 0.31,
        validHighPrice = 24099,
        autoStrategy = {
            is_search_stop: '',
            search: {
                goal_id: null,
                bid: null,
                name: 'autobudget',
                sum: '400'
            },
            is_net_stop: '',
            name: '',
            is_autobudget: '1',
            net: {
                name: 'default'
            }
        },
        manualStrategy = {
            is_search_stop: '',
            search: {
                name: 'default'
            },
            is_net_stop: '',
            name: '',
            is_autobudget: '',
            net: {
                name: 'default'
            }
        },
        searchOffIndependentStrategy = {
            is_search_stop: '1',
            search: {
                name: 'stop'
            },
            is_net_stop: '',
            name: 'different_places',
            is_autobudget: '',
            net: {
                name: 'maximum_coverage'
            }
        },
        searchOnIndependentStrategy = {
            is_search_stop: '',
            search: {
                place: 'premium',
                name: 'min_price'
            },
            is_net_stop: '',
            name: 'different_places',
            is_autobudget: '',
            net: {
                name: 'maximum_coverage'
            }
        },
        condition = {
            dyn_cond_id: '19792',
            is_suspended: '',
            statusBsSynced: 'No',
            condition_name: '123',
            dyn_id: '19742',
            pid: '818126772',
            price: '12.00',
            price_context: '0.00',
            condition: [
                {
                    kind: 'exact',
                    value: [{ value: 'url'}],
                    type: 'URL'
                },
                {
                    kind: 'not_exact',
                    value: [{ value: 'domain'}],
                    type: 'domain'
                },
                {
                    kind: 'exact',
                    value: [{ value: 'content'}],
                    type: 'content'
                }
            ],
            modelId: '19742'
        },
        isPriceControlExists = function() {
            return !!block.findElem('setting', 'field', 'price').length;
        },
        isPriceContextControlExists = function() {
            return !!block.findElem('setting', 'field', 'price-context').length;
        },

        getGoals = function(num) {
            return [
                { value: [{ isUrl :false, value: '1' }], type: 'content', kind: 'exact' },
                { value: [{ isUrl :false, value: '2' }], type: 'title', kind: 'exact' },
                { value: [{ isUrl :false, value: '4' }], type: 'title', kind: 'not_exact' },
                { value: [{ isUrl :false, value: 'qwe' }], type: 'domain', kind: 'exact' },
                { value: [{ isUrl :false, value: 'etrtrt' }], type: 'domain', kind: 'not_exact' },
                { value: [{ isUrl :true, value: 'http://qwerty.ru' }], type: 'URL_prodlist', kind: 'equals' },
                { value: [{ isUrl :true, value: 'http://msn.ru' }], type: 'URL_prodlist', kind: 'not_equals' },
                { value: [{ isUrl :false, value: '1231232313' }], type: 'content', kind: 'not_exact' },
                { value: [{ isUrl :false, value: '666666' }], type: 'content', kind: 'exact' },
                { value: [{ isUrl :false, value: '777777777' }], type: 'URL', kind: 'exact' }
            ].slice(0, num || 10);
        },

        getAddBtn = function() {
            return block.findBlockOn('goal-control', 'b-control-add-button');
        },

        getFirstDeleteBtn = function() {
            return block.findBlockOn('goal-control', 'b-control-remove-button');
        },

        clickAddBtn = function() {
            getAddBtn().findBlockInside('button').domElem.click();
        },

        createBlock = function(strategy, goals) {
            campaignDM = BEM.MODEL.create({ name: 'm-campaign' }, { currency: currency, strategy: strategy });

            block = u.createBlock({
                block: 'b-dynamic-condition-edit',
                condition: $.extend(
                    condition,
                    { currency: currency, strategy: strategy },
                    goals ? { condition: goals } : {}),
                currency: currency
            },
            { inject: true });
        },
        cleanUp = function() {
            block && block.destruct();
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Набор полей', function() {
        describe('при ручной стратегии, кроме "Независимое размещение":', function() {
            beforeEach(function() {
                createBlock(manualStrategy);
            });

            it('выводится инпут ставки за клик', function() {
                expect(isPriceControlExists()).to.be.equal(true);
            });
            it('инпута ставки на тематич. площадках нет', function() {
                expect(isPriceContextControlExists()).to.be.equal(false);
            });

            afterEach(cleanUp);
        });

        describe('при ручной стратегии "Независимое размещение"', function() {
            describe('и поиск оключен', function() {
                beforeEach(function() {
                    createBlock(searchOffIndependentStrategy);
                });

                it('выводится инпут ставки', function() {
                    expect(isPriceContextControlExists()).to.be.equal(true);
                });
                it('инпута ставки на поиске нет', function() {
                    expect(isPriceControlExists()).to.be.equal(false);
                });

                afterEach(cleanUp);
            });
            describe('и поиск включен', function() {
                beforeEach(function() {
                    createBlock(searchOnIndependentStrategy);
                });

                it('выводится инпут ставки тематич. площадках', function() {
                    expect(isPriceContextControlExists()).to.be.equal(true);
                });
                it('выводится инпут ставки на поиске', function() {
                    expect(isPriceControlExists()).to.be.equal(true);
                });

                afterEach(cleanUp);
            });
        });

        describe('при автоматической стратегии', function() {
            beforeEach(function() {
                createBlock(autoStrategy);
            });

            it('инпута ставки тематич. площадках нет', function() {
                expect(isPriceContextControlExists()).to.be.equal(false);
            });
            it('инпута ставки на поиске нет', function() {
                expect(isPriceControlExists()).to.be.equal(false);
            });

            afterEach(cleanUp);
        });
    });

    describe('Добавление', function() {
        describe('если в модели уже задано 10 правил', function() {
            beforeEach(function() {
                createBlock(autoStrategy, getGoals());

                this.modelsNum = block.model.get('condition').length();
                this.rowsNum = block.findElem('goals-list-item').length;

                clickAddBtn();
            });

            it('флаг модели запрещено добавление', function() {
                expect(block.model.get('canAdd')).to.be.equal(false);
            });

            it('кнопка добавления становится неактивной', function() {
                expect(getAddBtn().getMod('state')).to.be.equal('disabled');
            });

            afterEach(cleanUp);
        });

        describe('если в списке меннее 10 правил', function() {
            beforeEach(function() {
                createBlock(autoStrategy, getGoals(9));

                this.modelsNum = block.model.get('condition').length();
                this.rowsNum = block.findElem('goals-list-item').length;
            });

            it('в модели разрешено добавление', function() {
                expect(block.model.get('canAdd')).to.be.equal(true);
            });

            describe('добавляется', function() {
                it('модель правила', function() {
                    clickAddBtn();

                    sandbox.clock.tick(100);

                    expect((block.model.get('condition').length() - this.modelsNum)).to.be.equal(1);
                });

                it('строчка в списке', function() {
                    clickAddBtn();

                    sandbox.clock.tick(100);

                    expect((block.findElem('goals-list-item').length - this.rowsNum)).to.be.equal(1);
                });
            });

            afterEach(cleanUp);
        });

        describe('если добавляется десятое правило', function() {
            beforeEach(function() {
                createBlock(autoStrategy, getGoals(9));
            });

            it('флаг модели запрещено добавление', function() {
                clickAddBtn();

                sandbox.clock.tick(100);

                expect(block.model.get('canAdd')).to.be.equal(false);
            });

            it('кнопка добавления становится неактивной', function() {
                clickAddBtn();

                sandbox.clock.tick(100);

                expect(getAddBtn().getMod('state')).to.be.equal('disabled');
            });

            afterEach(cleanUp);
        });
    });

    describe('Удаление', function() {
        describe('в списке с одним условием', function() {
            beforeEach(function() {
                createBlock(autoStrategy, [
                    { kind: 'exact', value: [{ value: 'url'}], type: 'URL' }
                ]);
            });

            it('в модели запрещено удаление', function() {
                expect(block.model.get('canDelete')).to.be.equal(false);
            });

            it('кнопка-крестик недоступна', function() {
                expect(getFirstDeleteBtn().getMod('state')).to.be.equal('disabled');
            });

            afterEach(cleanUp);
        });

        describe('с списке с несколькими условиями', function() {
            beforeEach(function() {
                createBlock(autoStrategy, [
                    { kind: 'exact', value: [{ value: 'url'}], type: 'URL' },
                    { kind: 'not_exact', value: [{ value: 'domain'}], type: 'domain' },
                    { kind: 'exact', value: [{ value: 'content'}], type: 'content' }
                ]);
            });

            it('в модели разрешено удаление', function() {
                expect(block.model.get('canDelete')).to.be.equal(true);
            });

            it('кнопки-крестики видимы', function() {
                expect(block.findBlocksOn('delete-goal', 'button').every(function(bl) {
                    return bl.domElem.is(':visible');
                })).to.be.equal(true);
            });

            afterEach(cleanUp);
        });

        describe('по клику на крестик', function() {
            beforeEach(function() {
                createBlock(autoStrategy, [
                    { kind: 'exact', value: [{ value: 'url'}], type: 'URL' },
                    { kind: 'not_exact', value: [{ value: 'domain'}], type: 'domain' },
                    { kind: 'exact', value: [{ value: 'content'}], type: 'content' }
                ]);

                var deleteBtn = getFirstDeleteBtn(),
                    elem = block.closestElem($(deleteBtn.domElem), 'goals-list-item'),
                    listElem = block.elemInstance($(elem));

                this.editGoalBlock = block.findBlockInside(listElem.domElem, 'b-dynamic-goal-edit');
            });

            it('удаляется соответствующая модель', function() {
                var conditions = block.model.get('condition'),
                    initLength = conditions.length(),
                    id = this.editGoalBlock.model.id;

                this.editGoalBlock.destruct();

                expect((!conditions.getById(id) && initLength - conditions.length() == 1)).to.be.equal(true);
            });

            it('удаляется строчка из списка', function() {
                var initLength = block.findBlocksInside('b-dynamic-goal-edit').length;

                this.editGoalBlock.destruct();

                expect((initLength - block.findBlocksInside('b-dynamic-goal-edit').length == 1)).to.be.equal(true);
            });

            afterEach(cleanUp);
        });
    });

    describe('Отмена', function() {
        beforeEach(function() {
            createBlock(autoStrategy, [{ kind: 'exact', value: [{ value: 'content'}], type: 'content' }]);
            this.spy = sinon.spy(block, 'trigger');

            sinon.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(options) {
                options.onYes.apply(block);
            });
        });

        it('по клику на кнопку вызывается событие condition:edit:canceled', function() {
            block.findBlockOn('cancel', 'button').domElem.click();

            sandbox.clock.tick(100);

            expect(this.spy.calledWith('condition:edit:canceled')).to.be.equal(true);
        });

        afterEach(function() {
            cleanUp();
            BEM.blocks['b-confirm'].open.restore();
        });
    });

    describe('Кнопка "Сохранить"', function() {
        beforeEach(function() {
            campaignDM = BEM.MODEL.create({ name: 'm-campaign' }, { currency: currency, strategy: autoStrategy });

            blockTree = u.getDOMTree({
                block: 'b-dynamic-condition-edit',
                condition: {
                    dyn_cond_id: '19792',
                    dyn_id: '19742',
                    price: '',
                    price_context: '',
                    condition: [],
                    currency: currency,
                    strategy: autoStrategy
                }
            });

            block = BEM.DOM.init(blockTree.appendTo('body')).bem('b-dynamic-condition-edit');
        });

        it('кнопка сохранить должна быть задизейблена при создании нового правила', function() {
            expect(block.findBlockOn('save', 'button')).to.haveMod('disabled', 'yes');
        });

        it('кнопка сохранить должна становиться активной, когда модель становится не пустой', function() {
            block.model.set('condition_name', 'updated_name');
            expect(block.findBlockOn('save', 'button')).not.to.haveMod('disabled');
        });

        it('кнопка сохранить должна становиться неактивной, когда модель становится не пустой, а затем снова пустой', function() {
            block.model.set('condition_name', 'updated_name');
            block.model.set('condition_name', '');
            expect(block.findBlockOn('save', 'button')).to.haveMod('disabled', 'yes');
        });

        afterEach(cleanUp);
    });

    describe('Сохранение', function() {
        beforeEach(function() {
            createBlock(autoStrategy);
            this.validateSpy = sinon.spy(block.model, 'validate');
            this.triggerSpy = sinon.spy(block, 'trigger');
        });

        it('по клику на кнопку выполняется валидация модели', function() {
            block.model.set('condition_name', 'updated_name');
            block.findBlockOn('save', 'button').domElem.click();

            sandbox.clock.tick(100);

            expect(this.validateSpy.called).to.be.equal(true);
        });

        describe('если модель валидна', function() {

            it('вызывается событие condition:edit:completed', function() {
                block.model.validate();
                expect(this.triggerSpy.calledWith('condition:edit:completed')).to.be.equal(true);
            });

            it('с вторым аргументом с данными модели', function() {
                block.model.validate();
                expect(Object.keys(this.triggerSpy.args[0][1])).to.be.eql(['id', 'modelData']);
            });
        });

        afterEach(cleanUp);
    });

    describe('События', function() {
        describe('save:error', function() {
            beforeEach(function() {
                createBlock(autoStrategy);
                this.errorText = 'test-error-text';
                block.trigger('save:error', this.errorText);
            });

            it('выводится текст ошибки', function() {
                expect((block.elem('save-status-message').text() == this.errorText)).to.be.equal(true);
            });

            afterEach(cleanUp);
        });
    });

    describe('Валидация', function() {
        describe('при ручной стратегии, кроме "Независимое размещение"', function() {
            beforeEach(function() {
                createBlock(manualStrategy);
            });

            [
                {
                    title: 'минимально допустимые',
                    val: validLowPrice
                },

                {
                    title: 'максимальное допустимые',
                    val: validHighPrice
                }
            ].forEach(function(item) {
                describe('если указано ' + item.title + ' значения ставок', function() {
                    beforeEach(function() {
                        block.model.update({
                            price: item.val,
                            price_context: item.val
                        });

                        this.validResult = block.model.validate();
                    });

                    it('ошибок нет', function() {
                        expect(this.validResult.valid).to.be.equal(true)
                    });
                })
            });

            [
                {
                    title: 'не указаны ставки',
                    val: '',
                    rule: 'lte',
                    field: 'price'
                },
                {
                    title: 'указаны значения меньше допустимых',
                    val: tooLowPrice,
                    rule: 'lte',
                    field: 'price'
                },
                {
                    title: 'указаны значения большие допустимых',
                    val: tooHighPrice,
                    rule: 'gte',
                    field: 'price'
                }
            ].forEach(function(item) {
                describe('если ' + item.title + ', вернется ошибка', function() {
                    beforeEach(function() {
                        block.model.update({
                            price: item.val,
                            price_context: item.val
                        });

                        this.validResult = block.model.validate();
                    });

                    it('по полю ' + item.field, function() {
                        expect(this.validResult.errorFields[0]).to.be.equal(item.field);
                    });

                    it('по правилу ' + item.rule, function() {
                        expect(this.validResult.errors[0].rule).to.be.equal(item.rule);
                    });
                });
            });

            afterEach(cleanUp);
        });

        describe('при ручной стратегии "Независимое размещение"', function() {
            describe('с выключенном поиске', function() {
                beforeEach(function() {
                    createBlock(searchOffIndependentStrategy);
                });

                [
                    {
                        title: 'минимально допустимые',
                        val: validLowPrice
                    },

                    {
                        title: 'максимальное допустимые',
                        val: validHighPrice
                    }
                ].forEach(function(item) {
                    describe('если указано ' + item.title + ' значения ставки', function() {
                        beforeEach(function() {
                            block.model.update({
                                price: item.val,
                                price_context: item.val
                            });

                            this.validResult = block.model.validate();
                        });

                        it('ошибок нет', function() {
                            expect(this.validResult.valid).to.be.equal(true)
                        });
                    })
                });

                [
                    {
                        title: 'не указаны ставки',
                        val: '',
                        rule: 'lte',
                        field: 'price_context'
                    },
                    {
                        title: 'указаны значения меньше допустимых',
                        val: tooLowPrice,
                        rule: 'lte',
                        field: 'price_context'
                    },

                    {
                        title: 'указаны значения большие допустимых',
                        val: tooHighPrice,
                        rule: 'gte',
                        field: 'price_context'
                    }
                ].forEach(function(item) {
                    describe('если ' + item.title + ', вернется ошибка', function() {
                        beforeEach(function() {
                            block.model.update({
                                price: item.val,
                                price_context: item.val
                            });

                            this.validResult = block.model.validate();
                        });

                        it('по полю ' + item.field, function() {
                            expect(this.validResult.errorFields[0]).to.be.equal(item.field);
                        });

                        it('по правилу ' + item.rule, function() {
                            expect(this.validResult.errors[0].rule).to.be.equal(item.rule);
                        });
                    });
                });

                afterEach(cleanUp);
            });

            describe('с включенном поиске', function() {
                beforeEach(function() {
                    createBlock(searchOnIndependentStrategy);
                });

                [
                    {
                        title: 'минимально допустимые',
                        val: validLowPrice
                    },

                    {
                        title: 'максимальное допустимые',
                        val: validHighPrice
                    }
                ].forEach(function(item) {
                    describe('если указано ' + item.title + ' значения ставки', function() {
                        beforeEach(function() {
                            block.model.update({
                                price: item.val,
                                price_context: item.val
                            });

                            this.validResult = block.model.validate();
                        });

                        it('ошибок нет', function() {
                            expect(this.validResult.valid).to.be.equal(true)
                        });
                    })
                });

                [
                    {
                        title: 'не указаны ставки',
                        val: '',
                        rule: 'lte',
                        fields: ['price', 'price_context']
                    },
                    {
                        title: 'указаны значения меньше допустимых',
                        val: tooLowPrice,
                        rule: 'lte',
                        fields: ['price', 'price_context']
                    },
                    {
                        title: 'указаны значения большие допустимых',
                        val: tooHighPrice,
                        rule: 'gte',
                        fields: ['price', 'price_context']
                    }
                ].forEach(function(item) {
                    describe('если ' + item.title + ', вернется ошибка', function() {
                        beforeEach(function() {
                            block.model.update({
                                price: item.val,
                                price_context: item.val
                            });

                            this.validResult = block.model.validate();
                        });

                        item.fields.forEach(function(fieldName, i) {
                            it('по полю ' + fieldName, function() {
                                var index = this.validResult.errorFields.indexOf(fieldName);

                                expect(this.validResult.errorFields[index]).to.be.equal(fieldName);
                            });

                            it('правилу ' + item.rule, function() {
                                expect(this.validResult.errorsData[fieldName][0].rule).to.be.equal(item.rule);
                            });
                        }, this);
                    });
                });

                afterEach(cleanUp);
            });
        });

        describe('при автоматической стратегии', function() {
            beforeEach(function() {
                createBlock(autoStrategy);
            });

            [
                {
                    title: 'не указаны ставки',
                    val: ''
                },
                {
                    title: 'указаны значения меньше допустимых',
                    val: tooLowPrice
                },
                {
                    title: 'указаны значения большие допустимых',
                    val: tooHighPrice
                }
            ].forEach(function(item) {
                describe('если ' + item.title, function() {
                    beforeEach(function() {
                        block.model.update({
                            price: item.val,
                            price_context: item.val
                        });

                        this.validResult = block.model.validate();
                    });

                    it('ошибок нет', function() {
                        expect(this.validResult.valid).to.be.equal(true);
                    });
                });
            });

            afterEach(cleanUp);
        });
    });
});
