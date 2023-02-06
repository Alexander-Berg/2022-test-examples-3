describe('Daria.vComposeSendLoader', function() {
    beforeEach(function() {
        this.mComposeFsm = ns.Model.get('compose-fsm').setData({});
        this.mComposeFsm.request();

        this.view = ns.View.create('compose-send-loader');
        this.view.$node = $('<div />');

        this.sinon.stub(this.view, 'getModel').withArgs('compose-fsm').returns(this.mComposeFsm);
    });

    describe('#showLoader', function() {
        it('должен показать ноду вида', function() {
            var spy = this.sinon.spy(this.view.$node, 'removeClass');
            this.view.showLoader();
            expect(spy).to.be.calledWithExactly(this.view.CLASS_HIDDEN);
        });
    });

    describe('#hideLoader', function() {
        it('должен скрыть ноду вида', function() {
            var spy = this.sinon.spy(this.view.$node, 'addClass');
            this.view.hideLoader();
            expect(spy).to.be.calledWithExactly(this.view.CLASS_HIDDEN);
        });
    });
});

