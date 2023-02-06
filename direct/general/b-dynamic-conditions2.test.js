describe('b-dynamic-conditions2', function() {
    beforeEach(function() {
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
    });

    var block,
        blockTree,
        DM,
        campaignDM,
        strategy = {
            is_search_stop:'',
            search: {
                'name':'default'
            },
            is_net_stop: '',
            name: '',
            is_autobudget: '',
            net: {
                'name':'default'
            }
        },
        conditions = [
            {
                dyn_cond_id:  '19792',
                condition_name:  '123',
                dyn_id:  '19742',
                pid:  '818126772',
                price:  '12.00',
                price_context:  '0.00',
                condition: [
                    {
                        kind:  'exact',
                        value: [{ value: 'url' }],
                        type:  'URL'
                    },
                    {
                        kind:  'not_exact',
                        value:  [{ value: 'domain' }],
                        type:  'domain'
                    },
                    {
                        kind:  'exact',
                        value:  [{ value: 'content' }],
                        type:  'content'
                    }
                ],
                modelId:  '19742'
            },
            {
                dyn_cond_id:  '19797',
                condition_name:  '222',
                dyn_id:  '19747',
                pid:  '818126772',
                price:  '22.00',
                price_context:  '0.00',
                condition: [
                    {
                        kind:  'exact',
                        value:  [{ value: 'url' }],
                        type:  'URL'
                    },
                    {
                        kind:  'not_exact',
                        value: [{ value: 'url_not' }],
                        type:  'URL'
                    }
                ],
                modelId:  '19747'
            },
            {
                dyn_cond_id:  '19802',
                condition_name:  '333',
                dyn_id:  '19752',
                pid:  '818126772',
                price:  '11.00',
                price_context:  '0.00',
                condition: [
                    {
                        kind:  'not_exact',
                        value: [{ value: 'qwerty' }],
                        type:  'URL'
                    }
                ],
                modelId:  '19752'
            },
            {
                dyn_cond_id:  '19807',
                condition_name:  'some',
                dyn_id:  '19757',
                pid:  '818126772',
                price:  '15.00',
                price_context:  '0.00',
                condition: [
                    {
                        kind:  'exact',
                        value:  [{ value: 'title' }],
                        type:  'title'
                    },
                    {
                        kind:  'match',
                        value: [{ value: 'some text' }],
                        type:  'content'
                    }
                ],
                modelId:  '19757'
            },
            {
                dyn_cond_id:  '19812',
                condition_name:  'last one',
                dyn_id:  '19762',
                pid:  '818126772',
                price:  '55.00',
                price_context:  '0.00',
                condition: [
                    {
                        kind:  'exact',
                        value: [{ value: 'url' }],
                        type:  'URL'
                    },
                    {
                        kind:  'exact',
                        value: [{ value: 'title' }],
                        type:  'title'
                    }
                ],
                modelId:  '19762'
            }
        ],
        modelParams = {
            name: 'b-dynamic-conditions2',
            id: 818126772
        },
        createModels = function() {
            DM = BEM.MODEL.create({ name: 'dm-dynamic-group', id: 818126772 }, { dynamic_conditions: conditions });
            campaignDM = BEM.MODEL.create({ name: 'm-campaign' }, { currency: 'RUB', strategy: strategy });
        },
        createBlock = function() {
            blockTree = u.getDOMTree($.extend({
                block: 'b-dynamic-conditions2',
                mods: { type: 'detailed' }
            }, {
                type: 'detailed',
                groupId: 818126772,
                conditions: conditions,
                currency: 'RUB',
                strategy: strategy
            }));

            $('body').append(blockTree);

            block = $(blockTree).bem('b-dynamic-conditions2', {
                modelParams: modelParams,
                strategy: strategy,
                currency: 'RUB'
            });
        },
        cleanUp = function() {
            if (block) {
                block.model.destruct();
                block._popup.hide();
                block.destruct();
            }
        },
        emulateConditionSave = function(block, id, goals) {
            block.trigger('condition:edit:completed', {
                id: id,
                modelData: {
                    dyn_cond_id:  id,
                    condition_name:  'Updated title',
                    dyn_id:  id,
                    price:  66,
                    price_context:  0,
                    goalsState: goals.map(function(goal) {
                        return goal.type + goal.kind + goal.value.map(function(obj) {
                            return obj.value;
                        }).sort();
                    }),
                    condition: goals,
                    modelId:  id
                }
            });
        };

    describe('ПО событию', function() {
        beforeEach(function() {
            createModels();
            createBlock();
            this.timer = sinon.useFakeTimers();

            block.findBlockInside('b-dynamic-condition2').findBlockOn('edit-condition', 'button').trigger('click');
            this.timer.tick(500);
            this.popup = block._popup;
            this.editBlock = this.popup.findBlockInside('b-dynamic-condition-edit');

        });

        describe('condition:edit:canceled', function() {
            beforeEach(function() {
                this.editBlock.trigger('condition:edit:canceled');
            });

            it('поп-ап редактирования должен закрываться', function() {
                expect(this.popup.isShown()).to.be.equal(false);
            });
        });

        describe('condition:edit:completed', function() {
            beforeEach(function() {
                this.spy = sinon.spy(block.model, 'validate');

                emulateConditionSave(this.editBlock, 19762, [
                    {
                        kind:  'exact',
                        value: [{ value: 'updated_URL' }],
                        type:  'URL'
                    },
                    {
                        kind:  'exact',
                        value: [{ value: 'title' }],
                        type:  'title'
                    }
                ]);
            });

            it('происходит валидация модели', function() {
                expect(this.spy.called).to.be.equal(true);
            });
        });

        afterEach(function() {
            this.timer.restore();
            cleanUp();
        });
    });


    describe('Валидация', function() {
        beforeEach(function() {
            createModels();
            createBlock();

            this.timer = sinon.useFakeTimers();

            block.findBlockInside('b-dynamic-condition2').findBlockOn('edit-condition', 'button').trigger('click');
            this.timer.tick(500);
            this.popup = block._popup;
            this.editBlock = this.popup.findBlockInside('b-dynamic-condition-edit');
            this.errorSpy = sinon.spy(this.editBlock ,'trigger');

        });

        describe('модели с корректными данными', function() {
            beforeEach(function() {
                this.syncToSpy = sinon.spy(block.model ,'syncToDM');
                this.syncFromSpy = sinon.spy(block.model ,'syncFromDM');

                emulateConditionSave(this.editBlock, 19762, [
                    {
                        kind:  'exact',
                        value: [{ value: 'url-new' }],
                        type:  'URL'
                    },
                    {
                        kind:  'exact',
                        value: [{ value: 'title' }],
                        type:  'title'
                    }
                ]);

                this.timer.tick(100);
                this.result = block.model.validate();

            });

            it('вызовтся toDM', function() {
                expect(this.syncToSpy.called).to.be.equal(true);
            });

            it('вызовтся fromDM', function() {
                expect(this.syncFromSpy.called).to.be.equal(true);
            });

            it('попап закроется', function() {
                expect(this.popup.isShown()).to.be.equal(false);
            });
        });

        afterEach(function() {
            this.timer.restore();
            cleanUp();
        });
    });

    describe('при переключении типа', function() {
        beforeEach(function() {
            createModels();
            createBlock();
        });

        describe('на "Все страницы сайта"', function() {
            beforeEach(function() {
                block.findBlockInside('radio-button').val('any');
            });

            it('устанавивается единое условие типа any', function() {
                var VMconditions = block.model.get('dynamic_conditions'),
                    DMconditions = DM.get('dynamic_conditions'),
                    isOnlyCondition = DMconditions.length() == VMconditions.length() == 1,
                    isAnyTyped = DMconditions.every(function(cond) {
                        var compare = function(model) {
                            return model.get('condition').every(function(goal) {
                                return goal.get('type') == 'any';
                            })
                        };

                        return compare(cond) && compare(VMconditions.getById(cond.id))
                    });

                expect((isOnlyCondition && isAnyTyped)).to.be.equal(true);
            });

            it('ссылка добавления условия скрывается', function() {
                expect(block.findElem('add').is(':visible')).to.be.equal(false);
            });
        });

        describe('на "Группа страниц"', function() {
            beforeEach(function() {
                block.findBlockInside('radio-button').val('detailed');
            });

            it('восстанавливаются условия типа detailed', function() {
                var VMconditions = block.model.get('dynamic_conditions'),
                    DMconditions = DM.get('dynamic_conditions'),
                    isSameLength = VMconditions.length() == DMconditions.length(),
                    isDetailedTyped = DMconditions.every(function(cond) {
                        var compare = function(model) {
                            return model.get('condition').every(function(goal) {
                                return goal.get('type') !== 'any';
                            })
                        };

                        return compare(cond) && compare(VMconditions.getById(cond.id))
                    });

                expect((isSameLength && isDetailedTyped)).to.be.equal(true);
            });

            it('ссылка добавления условия отображается', function() {
                expect(block.findElem('add').is(':visible')).to.be.equal(true);
            });
        });

        afterEach(cleanUp);
    });

    describe('удаление', function() {
        beforeEach(function() {
            createModels();
            createBlock();
        });

        it('по клику на крестик, условие удаляется как из DM так и из VM', function() {
            var button = block.findBlockOn('delete-condition', 'button'),
                initialLengthVM = block.model.get('dynamic_conditions').length(),
                initialLengthDM = DM.get('dynamic_conditions').length();

            button.domElem.click();

            var conditionsLengthDM = block.model.get('dynamic_conditions').length(),
                conditionsLengthVM =DM.get('dynamic_conditions').length(),
                assert = (initialLengthVM - conditionsLengthVM == 1) && (initialLengthDM - conditionsLengthDM == 1);

            expect((assert)).to.be.equal(true);
        });

        afterEach(cleanUp);
    });

    describe('добавление', function() {
        beforeEach(function() {
            createModels();
            createBlock();
        });

        it('по клику по ссылке "Новое условие" открывается поп-ап', function() {
            block.findBlockOn('add-link', 'link').domElem.click();
            expect(block._popup.isShown()).to.be.equal(true);
        });

        afterEach(cleanUp);

    });
});
