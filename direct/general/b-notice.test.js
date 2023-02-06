describe('b-notice', function() {
    var block,
        sandbox,
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-notice' }, options || {}),
                { inject: true }
            );
        };

    beforeEach(function() {
        block = createBlock();

        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('Кнопка закрытия есть при передаче параметра hasClose', function() {
        block = createBlock({ hasClose: true });

        expect(block.elem('close').length > 0).to.be.true;
    });

    it('Кнопки закрытия нет если hasClose не указан или false', function() {
        block = createBlock();

        expect(block.elem('close').length).to.be.equal(0);
    });

    it('При клике на кнопку закрыть панель скрывается', function() {
        block = createBlock({ hasClose: true });

        block.elem('close').click();

        expect(block).to.haveMod('closed', 'yes');
    });

    it('При клике на кнопку закрыть триггерится событие close', function() {
        block = createBlock({ hasClose: true });
        sandbox.spy(block, 'trigger');

        block.elem('close').click();

        expect(block).to.triggerEvent('close');
    });
});
