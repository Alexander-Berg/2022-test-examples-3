describe('b-retargeting-condition-edit-popup', function() {
    var block,
        sandbox,
        rule0 = {
            type: 'METRIKA_GOAL',
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
                type: 'METRIKA_GOAL',
                id: 1234567890,
                day: 90,
                domain: 'test.domain',
                name: 'test.goal',
                allowToUse: true
            },
            {
                type: 'METRIKA_SEGMENT',
                id: 7777,
                day: 44,
                domain: 'test.domain1',
                name: 'test.goal1',
                allowToUse: true
            }
        ];

    /**
     * Создание блока, который тестируется
     * @param params
     */
    function createBlock(params) {
        return u.createBlock(u._.assign(
            {
                block: 'b-retargeting-condition-edit-popup',
                conditionId: +params.conditionId || 0,
                showBtnSaveAsNew: !!params.conditionId
            }, params));
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        sandbox.stub(u['retargeting-dataprovider'], 'getCondition').callsFake(function() {
            return Promise.resolve(condition0);
        });
        sandbox.stub(u['retargeting-dataprovider'], 'forecastVisitor').callsFake(function() {
            return Promise.resolve({
                count: 123456789
            });
        });
        sandbox.stub(u['retargeting-dataprovider'], 'getGoalList').callsFake(function() {
            return new Promise(function() {});
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('проверяет у новой записи', function() {
        var PARAMS_NEW = {
            conditionId: '0'
        };

        beforeEach(function() {
            block = createBlock(PARAMS_NEW);
        });

        it('createInstance создает блок b-retargeting-condition-edit-popup', function() {
            var _block = BEM.blocks['b-retargeting-condition-edit-popup'].createInstance({
                block: 'b-retargeting-condition-edit-popup',
                conditionId: 0,
                showBtnSaveAsNew: false
            });

            expect(_block instanceof BEM.blocks['b-retargeting-condition-edit-popup']).to.be.true;
            _block._onCancel();
        });

        it('кнопка "Сохранить" НЕ "активна", если изменений данных НЕ было' , function() {
            block._onChange({
                isChanged: false,
                isValid: true,
                saveAsNew: false,
                mode: 'new'
            });
            expect(block._saveButton).to.haveMod('disabled', 'yes');
        });

        it('кнопка "Сохранить" НЕ "активна", если изменение данных НЕ валидно' , function() {
            block._onChange({
                isChanged: false,
                isValid: false,
                saveAsNew: false,
                mode: 'new'
            });
            expect(block._saveButton).to.haveMod('disabled', 'yes');
        });

        it('кнопка "Сохранить" "активна", если есть валидные изменения данных' , function() {
            block._onChange({
                isChanged: true,
                isValid: true,
                saveAsNew: false,
                mode: 'new'
            });
            expect(block._saveButton).to.not.haveMod('disabled', 'yes');
        });
    });

    describe('проверяет у существующей записи', function() {
        var PARAMS_EDIT = {
            conditionId: '1'
        };

        beforeEach(function() {
            block = createBlock(PARAMS_EDIT);
        });

        it('кнопка "Сохранить как новое" "активна", если передан флаг, что запись можно сохранить, как новую' , function() {
            block._onChange({
                isChanged: true,
                isValid: true,
                saveAsNew: true,
                mode: 'edit'
            });
            sandbox.clock.tick(1);

            expect(block._saveAsNewButton).to.not.haveMod('disabled', 'yes');
        });

        it('кнопка "Сохранить как новое" НЕ "активна" при создании блока' , function() {
            sandbox.clock.tick(1);

            expect(block._saveAsNewButton).to.haveMod('disabled', 'yes');
        });
    });
});
