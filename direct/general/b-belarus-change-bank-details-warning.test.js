describe('b-belarus-change-bank-details-warning', function() {
    var block,
        sandbox,
        createBlock = function() {
            return u.createBlock({ block: 'b-belarus-change-bank-details-warning' }, { inject: true, hidden: false });
        };

    beforeEach(function() {
        block = createBlock();

        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });

        sandbox.stub(u, 'consts').withArgs('user_options').returns('{}');
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('При клике на кнопку "Не показывать больше" предупреждение скрывается', function() {
        block.findBlockOn('close-button', 'button').trigger('click');
        sandbox.clock.tick(200);

        expect(block.domElem[0].clientWidth).to.be.equal(0);
    });
});
