describe('b-retargeting-condition-edit-group-rule', function() {
    var block,
        sandbox,
        rule0 = {
            _entityId: '1',
            allowToUse: true,
            day: 90,
            domain: '',
            goalId: 0,
            hasGoalList: true,
            name: '',
            type: 'METRIKA_GOAL',
            valid: false
        },
        rule1 = {
            _entityId: '2',
            allowToUse: true,
            day: 90,
            domain: '',
            goalId: 0,
            hasGoalList: true,
            name: '',
            type: 'METRIKA_GOAL',
            valid: false
        },
        params = {
            _entityId: '1',
            type: 'ALL',
            ruleCollection: [
                rule0
            ]
        };

    /**
     * Создание блока, который тестируется
     * @param {{ name, comment }} [params]
     */
    function createBlock(params) {
        return  u.createBlock(u._.assign({
            block: 'b-retargeting-condition-edit-group-rule'
        }, params));
    }

    /**
     * Получает кол-во "правил" внутри "группы правил"
     * @param _block
     * @returns {*}
     */
    function getCountRulesInsideBlock(_block) {
        return _block.findBlocksInside('b-retargeting-condition-edit-rule').length
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        block = createBlock(params);
    });

    afterEach(function() {
        block && block.destruct();
        sandbox.restore();
    });

    it('при клике на кнопку "Добавить правило" триггерит событие create-rule', function() {
        sandbox.spy(block, 'trigger');
        block.findBlockOn('create-rule', 'link').trigger('click');
        expect(block).to.triggerEvent('create-rule', block.params._entityId);
    });

    it('при клике на кнопку "Удалить группу правил" триггерит событие remove-group-rule с данными уникального идентификатора ', function() {
        sandbox.spy(block, 'trigger');
        block.findBlockOn('group-rule-remove', 'link').trigger('click');
        expect(block).to.triggerEvent('remove-group-rule', block.params._entityId);
    });

    it('при изменении типа группы правил триггерит событие change-group-rule', function() {
        sandbox.spy(block, 'trigger');
        block._fieldType.val('OR');
        expect(block).to.triggerEvent('change-group-rule', {
            _entityId: '1',
            type: 'OR'
        });
    });

    it('добавляет правило в список', function() {
        var countRule = getCountRulesInsideBlock(block);
        block.addRuleToCollection(rule1);
        expect(getCountRulesInsideBlock(block)).to.be.equal(countRule + 1);
    });
});
