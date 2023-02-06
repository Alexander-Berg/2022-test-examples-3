describe('Daria.vComposeSaveLink', function() {
    beforeEach(function() {
        this.sinon.stub(ns.Model.get('account-information'), 'getFromEmails').returns(['test']);
        this.sinon.stub(ns.Update.prototype, 'log');
        this.sinon.stub(ns.page.current, 'page').value('compose2');
        this.view = ns.View.create('compose-save-link');
        this.mComposeFsm = ns.Model.get('compose-fsm');
        this.mComposeState = ns.Model.get('compose-state');
        this.mComposeMessage = ns.Model.get('compose-message');
        this.sinon.stub(this.mComposeMessage, 'once').withArgs('ns-model:draft-saved');

        var getModelStub = this.sinon.stub(this.view, 'getModel');
        getModelStub.withArgs('compose-fsm').returns(this.mComposeFsm);
        getModelStub.withArgs('compose-state').returns(this.mComposeState);
        getModelStub.withArgs('compose-message').returns(this.mComposeMessage);
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

    describe('#onSave', function() {
        it('должен перевести compose в состояние сохранения письма', function() {
            this.sinon.stub(this.view, 'setState');
            this.view.onSave();

            expect(this.view.setState).to.be.calledWithExactly('save');
        });
    });
});
