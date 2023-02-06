describe('Daria.vComposeSendLink', function() {
    beforeEach(function() {
        this.sinon.stub(ns.Model.get('account-information'), 'getFromEmails').returns(['test']);
        this.sinon.stub(ns.Update.prototype, 'log');
        this.sinon.stub(ns.page.current, 'page').value('compose2');
        this.view = ns.View.create('compose-send-link');
        this.mComposeFsm = ns.Model.get('compose-fsm');
        this.mComposeState = ns.Model.get('compose-state');
        this.mQuickReplyState = ns.Model.get('quick-reply-state');
        var getModelStub = this.sinon.stub(this.view, 'getModel');
        getModelStub.withArgs('compose-fsm').returns(this.mComposeFsm);
        getModelStub.withArgs('compose-state').returns(this.mComposeState);
        getModelStub.withArgs('quick-reply-state').returns(this.mQuickReplyState);
        return this.view.update();
    });

    describe('#onInit', function() {
        it('Запускает инициализацию миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingInit');

            this.view.onInit();

            expect(this.view.disableOnSendingInit).to.have.callCount(1);
        });
    });

    describe('#onShow', function() {
        it('Запускает старт миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingStart');

            this.view.onShow();

            expect(this.view.disableOnSendingStart).to.have.callCount(1);
        });
    });

    describe('#onHide', function() {
        it('Запускает стоп миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingStop');

            this.view.onHide();

            expect(this.view.disableOnSendingStop).to.have.callCount(1);
        });
    });

    describe('#onSendingToSent', function() {

        it('должен перевести compose в состояние отправки письма (sending)', function() {
            this.sinon.stub(this.view, 'setState');
            this.sinon.stub(this.view, 'composeMetrics');
            this.view.onSendingToSent();

            expect(this.view.setState).to.be.calledWithExactly('sending');
        });

    });

    describe('#composeMetrics', function() {
        it('если композ большой, то должен отправлять метрику отправки письма про большой композ', function() {
            this.sinon.stub(Jane, 'c');
            this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(false);

            this.view.composeMetrics();
            expect(Jane.c).to.be.calledWith('Отправка письма', 'compose');
        });

        it('если QR, то должен отправлять метрику отправки письма про QR', function() {
            this.sinon.stub(Jane, 'c');
            this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(true);

            this.view.composeMetrics();
            expect(Jane.c).to.be.calledWith('Отправка письма', 'QR');
        });
    });
});
