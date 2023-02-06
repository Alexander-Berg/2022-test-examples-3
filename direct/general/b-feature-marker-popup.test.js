describe('b-feature-marker-popup', function() {
    var block;

    function createBlock() {
        return $(BEMHTML.apply({
                block: 'b-feature-marker-popup',
                helpUrl: '#'
            })).bem('b-feature-marker-popup');
    }

    afterEach(function() {
        block && block.destruct();
    });

    it('блок скрывается при клике на кнопку "Понятно"', function() {
        block = createBlock();
        sinon.spy(block, 'trigger');

        block.findBlockOn('close', 'button').trigger('click');

        expect(block.trigger.calledOnce).to.be.ok;
        expect(block.trigger.getCall(0).args[0]).to.equal('hide');
        expect(block.trigger.getCall(0).args[1]).to.equal(false);
    });

    it('блок скрывается при клике на кнопку "Подробнее"', function() {
        block = createBlock();
        sinon.spy(block, 'trigger');

        block.findBlockOn('details', 'button').trigger('click');

        expect(block.trigger.calledOnce).to.be.ok;
        expect(block.trigger.getCall(0).args[0]).to.equal('hide');
        expect(block.trigger.getCall(0).args[1]).to.equal(true);
    });
});
