describe('b-retargeting-condition-edit-rule', function() {
    var block,
        sandbox,
        rule0 = {
            allowToUse: true,
            day: 90,
            domain: 'test.domain.goal',
            goalId: 100,
            hasGoalList: true,
            name: 'tests goal',
            type: 'METRIKA_GOAL'
        },
        rule1 = {
            _entityId: '2',
            allowToUse: true,
            day: 90,
            domain: 'test.domain.goal',
            goalId: 10000,
            hasGoalList: true,
            name: 'tests segment',
            type: 'METRIKA_SEGMENT'
        };

    /**
     * Создание блока, который тестируется
     * @param {{ _entityId, rule }} [params]
     */
    function createBlock(params) {
        return u.createBlock(u._.assign({
            block: 'b-retargeting-condition-edit-rule'
        }, params));
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        block = createBlock({
            _entityId: '1',
            rule: rule0
        });
    });

    afterEach(function() {
        block && block.destruct();
        sandbox.restore();
    });

    it('при клике на кнопку удаления правила триггерит событие remove-link', function() {
        sandbox.spy(block, 'trigger');
        block.findBlockOn('remove-rule', 'link').trigger('click');
        expect(block).to.triggerEvent('remove-rule', block.params._entityId);
    });

    it('при изменении типа правила триггерит событие change-rule-type', function() {
        expect(block).to.triggerEvent(
            'change-rule-type',
            { entityId: block.params._entityId, type: 'METRIKA_SEGMENT' },
            function() {
                block.findBlockInside('type', 'select').val('METRIKA_SEGMENT');
                sandbox.clock.tick(1);
            }
        );
    });

    it('триггерит событие change-rule и передает объект данных при записи в поле дни валидного значения', function() {
        sandbox.spy(block, 'trigger');
        block.findBlockInside('field-day', 'input').val('5');
        sandbox.clock.tick(1);
        expect(block).to.triggerEvent('change-rule', {
            entityId: block.params._entityId,
            day: 5,
            valid: true
        });
    });

    it('триггерит событие при открытии списка целей/сегментов', function() {
        block = createBlock({
            _entityId: '1',
            rule: rule0
        });
        sandbox.spy(block, 'trigger');
        block.showListPopup();

        sandbox.clock.tick(100);
        expect(block).to.triggerEvent('open-goal-list', {
            entityId: block.params._entityId,
            type: 'METRIKA_GOAL'
        });
    });

    it('триггерит событие change-rule и передает объект данных при записи в поле дни значения', function() {
        sandbox.spy(block, 'trigger');
        block.findBlockInside('field-day', 'input').val(10);
        sandbox.clock.tick(1);
        expect(block).to.triggerEvent('change-rule', {
            entityId: block.params._entityId,
            day: 10,
            valid: true
        });
    });

    it('модификатор status-load_loaded устанавливается', function() {
        block = createBlock({
            _entityId: '1',
            rule: rule0
        });
        block.applyType({
            allowToUse: true,
            hasGoalList: true
        });
        sandbox.clock.tick(1);
        expect(block).to.haveMod('status-load', 'loaded');
    });

    describe('поле "дни" input', function() {

        it('валидно при вводе числа', function() {
            block.findBlockInside('field-day', 'input').val('5');

            expect(block.findBlockInside('field-day', 'input')).to.not.haveMod('warning', 'yes');
        });

        it('НЕ валидно при вводе не числового значения', function() {
            block.findBlockInside('field-day', 'input').val('уу');

            expect(block.findBlockInside('field-day', 'input')).to.haveMod('warning', 'yes');
        });
    });
});
