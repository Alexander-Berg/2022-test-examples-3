describe('b-retargeting-condition-edit', function() {
    var block,
        model,
        sandbox,
        rule0 = {
            type:  'METRIKA_GOAL',
            goalId: 1234567890,
            day: 90,
            domain: 'test.domain',
            name: 'test.rule',
            allowToUse: true
        },
        groupRule0 = {
            type: 'OR',
            ruleCollection: [rule0]
        },
        condition0 = {
            name: 'Название',
            comment: 'Комментарий',
            id: '555',
            isAccessible: true,
            isUsed: true,
            isNegative: false,
            groupRuleCollection: [groupRule0]
        },
        goalList = [
            {
                type:  'METRIKA_GOAL',
                id: 1234567890,
                day: 90,
                domain: 'test.domain',
                name: 'test.goal',
                allowToUse: true
            },
            {
                type:  'METRIKA_SEGMENT',
                id: 7777,
                day: 44,
                domain: 'test.domain1',
                name: 'test.goal1',
                allowToUse: true
            }
        ];

    function getCondition(groupRuleArr) {
        return u._.assign({}, condition0, { groupRuleCollection: groupRuleArr || [getGroupRule()] });
    }

    function getGroupRule(ruleArr) {
        return u._.assign({}, groupRule0, { ruleCollection: ruleArr || [getRule()] });
    }

    function getRule(rule) {
        return u._.assign({}, rule || rule0);
    }

    /**
     * Создание блока, который тестируется
     * @param {Number} conditionId
     */
    function createBlock(conditionId) {
        return u.createBlock({
            block: 'b-retargeting-condition-edit',
            conditionId: conditionId
        });
    }

    /**
     * Создание модели
     * @param {} params
     * @returns {b-retargeting-condition-edit}
     */
    function createModel(params) {
        return BEM.MODEL.create('b-retargeting-condition-edit', u._.assign(getCondition(), params));
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block && block.destruct();
        model && model.destruct();
        sandbox.restore();
    });

    it('метод changeGroupRule корректно изменяет тип группы правил', function() {
        model = createModel(u._.assign({ mode: 'new' }, getCondition()));

        model.changeGroupRule({
            _entityId: model.toJSON().groupRuleCollection[0]._entityId,
            type: 'ALL'
        });

        expect(model.toJSON().groupRuleCollection[0].type).to.be.equal('ALL');
    });

    it('метод changeGroupRule триггерит событие change', function() {
        model = createModel(u._.assign({ mode: 'new' }, getCondition()));
        sandbox.spy(model, 'trigger');

        model.toJSON().groupRuleCollection[0].type;
        model.changeGroupRule({
            _entityId: model.toJSON().groupRuleCollection[0]._entityId,
            type: 'ALL'
        });

        expect(model).to.triggerEvent('change');
    });

    it('метод getRuleGoalId возвращает "цель", установленную у "группы правил"', function() {
        var rule;

        model = createModel(u._.assign({ mode: 'new' }, getCondition()));
        rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

        expect(model.getRuleGoalId(rule._entityId)).to.be.equal(1234567890);
    });

    it('метод getGoalSelectedInsideGroupRule получает ID всех "целей" которые есть у "группы правил"', function() {
        var result;

        model = createModel(
            u._.assign({ mode: 'new' },
                getCondition([
                        getGroupRule([
                            getRule(),
                            getRule()
                        ])
                    ])
            )
        );
        result = model.getGoalSelectedInsideGroupRule(model.toJSON().groupRuleCollection[0].ruleCollection[0]._entityId);

        expect(result).to.deep.equal([1234567890, 1234567890]);
    });

    it('метод isConditionNegative определяет, является ли "условие" "негативным"', function() {
        model = createModel(u._.assign({ mode: 'new' }, getCondition()));

        expect(model.isConditionNegative()).to.be.false;
    });

    it('метод changeData триггерит событие change-data', function() {
        model = createModel(u._.assign({ mode: 'new' }, getCondition()));
        sandbox.spy(model, 'trigger');

        model.changeData();

        expect(model).to.triggerEvent('change-data');
    });

    it('метод changeData триггерит соббытие change-message', function() {
        model = createModel(u._.assign({ mode: 'new' }, getCondition()));
        sandbox.spy(model, 'trigger');

        model.changeData();

        expect(model).to.triggerEvent('change-message');
    });

    describe('метод shouldBeSavedAsNew', function() {

        beforeEach(function() {
            model = createModel(u._.assign({ mode: 'edit' }, getCondition()));
        });

        it('возвращает false при отсутствии изменений в данных', function() {
            expect(model.shouldBeSavedAsNew()).to.be.false;
        });

        it('возвращает true при наличии изменений в данных', function() {
            model.update({
                name: 'sfsdf',
                groupRuleCollectionIsChanged: true
            });
            expect(model.shouldBeSavedAsNew()).to.be.true;
        });
    });

    describe('метод createInstanceGroupRule', function() {

        beforeEach(
            function() {
                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
            });

        it('создает новую группу правил', function() {
            return model.createInstanceGroupRule()
                .then(function(data) {
                    expect(data.ruleCollection.length).to.be.equal(1);
                });
        });

        it('триггерит событие change', function() {
            sandbox.spy(model, 'trigger');

            return model.createInstanceGroupRule()
                .then(function() {
                    expect(model).to.triggerEvent('change');
                });
        });
    });

    describe('метод createInstanceRule', function() {

        beforeEach(
            function() {
                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
            });

        it('создает новое правило', function() {
            return model.createInstanceRule(model.toJSON().groupRuleCollection[0]._entityId)
                .then(function(data) {
                    expect(!!data).to.be.equal(true);
                });
        });

        it('триггерит событие change', function() {
            sandbox.spy(model, 'trigger');

            return model.createInstanceRule(model.toJSON().groupRuleCollection[0]._entityId)
                .then(function() {
                    expect(model).to.triggerEvent('change');
                });
        });
    });

    describe('метод removeGroupRule', function() {

        it('последнюю группу правил удалить нельзя', function() {
            var result;

            model = createModel(u._.assign({ mode: 'new' }, getCondition()));

            result = model.removeGroupRule();

            expect(result).to.be.true;
        });

        it('удаляет группу правил', function() {
            var result;

            model = createModel(
                u._.assign({ mode: 'new' },
                    getCondition([
                            getGroupRule(),
                            getGroupRule()
                        ])
                )
            );
            result = model.removeGroupRule(model.toJSON().groupRuleCollection[0]);

            expect(result).to.be.true;
        });

        it('тригерит событие change', function() {
            model = createModel(
                u._.assign({ mode: 'new' },
                    getCondition([
                            getGroupRule(),
                            getGroupRule()
                        ])
                )
            );
            sandbox.spy(model, 'trigger');

            model.removeGroupRule(model.toJSON().groupRuleCollection[0]);

            expect(model).to.triggerEvent('change');
        });
    });

    describe('метод removeRule', function() {

        it('последнее правило удалить можно (напрямую в модели)', function() {
            var result;

            model = createModel(u._.assign({ mode: 'new' }, getCondition()));
            result = model.removeRule(model.toJSON().groupRuleCollection[0].ruleCollection[0]._entityId);

            expect(result).to.be.false;
        });

        it('удаляет правило', function() {
            var result;

            model = createModel(
                u._.assign({ mode: 'new' },
                    getCondition([
                            getGroupRule([
                                getRule(),
                                getRule()
                            ])
                        ])
                )
            );
            result = model.removeRule(model.toJSON().groupRuleCollection[0].ruleCollection[0]._entityId);

            expect(result).to.be.true;
        });

        it('тригерит событие change', function() {
            model = createModel(
                u._.assign({ mode: 'new' },
                    getCondition([
                            getGroupRule([
                                getRule(), getRule()
                            ])
                        ])
                )
            );
            sandbox.spy(model, 'trigger');

            model.removeRule(model.toJSON().groupRuleCollection[0].ruleCollection[0]._entityId);

            expect(model).to.triggerEvent('change');
        });
    });

    describe('метод changeRule', function() {

            beforeEach(
                function() {
                    model = createModel(u._.assign({ mode: 'new' }, getCondition()));
                });

            it('изменяет тип правила', function() {
                var rule;

                rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];
                model.changeRule(rule._entityId, { type: 'METRIKA_SEGMENT' });

                expect('METRIKA_SEGMENT').to.be.equal(rule.type);
            });

            it('триггерит событие change при изменении типа правила', function() {
                var rule;

                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
                sandbox.spy(model, 'trigger');
                rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

                model.changeRule(rule._entityId, { type: 'METRIKA_SEGMENT' });

                expect(model).to.triggerEvent('change');
            });

            it('изменяет параметр дни у правила', function() {
                var rule;

                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
                rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

                model.changeRule(rule._entityId, { day: 77 });

                expect(77).to.be.equal(rule.day);
            });

            it('триггерит событие change при изменении параметра дни у правила', function() {
               var rule;

               model = createModel(u._.assign({ mode: 'new' }, getCondition()));
               sandbox.spy(model, 'trigger');
               rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

               model.changeRule(rule._entityId, { day: 88 });

               expect(model).to.triggerEvent('change');
           });

            it('изменяет цель/сегмент у правила', function() {
                var rule;

                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
                rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

                model.changeRule(rule._entityId, { goalId: 987654326 });

                expect(987654326).to.be.equal(rule.goalId);
            });

            it('триггерит событие change при изменении цели/сегмента у правила', function() {
                var rule;

                model = createModel(u._.assign({ mode: 'new' }, getCondition()));
                sandbox.spy(model, 'trigger');
                rule = model.toJSON().groupRuleCollection[0].ruleCollection[0];

                model.changeRule(rule._entityId, { goalId: 987654326 });

                expect(model).to.triggerEvent('change');
            });
        });
});
