describe('b-edit-region-bid', function() {
    var block,
        sandbox,
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-edit-region-bid' }, options || {}),
                { inject: true }
            );
        };

    beforeEach(function() {
        block = createBlock({ value: 70 });

        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('События блока', function() {
        beforeEach(function() {
            sandbox.spy(block, 'trigger');
        });

        it('При клике на кнопку отмены триггерит событие cancel', function() {
            block._cancelBtn.trigger('click');

            expect(block).to.triggerEvent('cancel');
        });

        it('При клике на кнопку сохранения триггерит событие save c корректными данными', function() {
            block._saveBtn.trigger('click');

            expect(block.trigger.calledWith('save', { value: 70, mode: 'up', absValue: 70 }));
        });

        it('Устанавливает фокус на контрол рудактирования ставки при установке модификатора focused', function() {
            block.setMod('focused', 'yes');

            expect(block._editBidControl).to.haveMod('focused', 'yes');
        });
    });
});
