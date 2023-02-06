describe('b-experiment-info', function() {
    var block,
        createBlock = function(hasActions, exp) {
            block = u.createBlock({
                block: 'b-experiment-info',
                exp: exp,
                hasActions: hasActions
            });
        },
        sandbox;

     beforeEach(function() {
         sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });
     });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('Нет кнопки, если hasActions == false', function() {
        createBlock(false);

        expect(block.findBlockOn('button', 'button')).to.be.equal(null);
    });

    it('Есть кнопка, если hasActions == true', function() {
        createBlock(true);

        expect(block).to.haveBlock('button', 'button');
    });

    it('Отображается подтверждение действия', function() {
        var stub = sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function() { });

        createBlock(true);

        block.findBlockOn('button', 'button').trigger('click');

        expect(stub.calledOnce).to.be.equal(true);
    });

    it('На сервер передаются корректные данные', function() {
        sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(arg) {
            arg.onYes();
        });

        createBlock(true, { experimentId: 1377 });

        block.findBlockOn('button', 'button').trigger('click');

        sandbox.clock.tick(1);

        expect(sandbox.server.requests[0].requestBody).to.be.equal('cmd=ajaxStopExperiment&experiment_id=1377');
    });
});
