describe('Daria.vComposeCancelButton', function() {
    beforeEach(function() {
        this.sinon.stub(ns.Model.get('account-information'), 'getFromEmails').returns(['test']);
        this.sinon.stub(ns.Update.prototype, 'log');

        this.view = ns.View.create('compose-cancel-button');
    });

    describe('#onInit', function() {
        it('Запускает инициализацию миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingInit');

            this.view.onInit();

            expect(this.view.disableOnSendingInit).to.have.callCount(1);
        });
    });

    describe('#onShow', function() {
        it('Запускает подключение миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingStart');

            this.view.onShow();

            expect(this.view.disableOnSendingStart).to.have.callCount(1);
        });
    });

    describe('#onHide', function() {
        it('Запускает отключение миксина vComposeDisableOnSendingMixin', function() {
            this.sinon.stub(this.view, 'disableOnSendingStop');

            this.view.onHide();

            expect(this.view.disableOnSendingStop).to.have.callCount(1);
        });
    });

    describe('#onClick', function() {
        it('Должен сменить статус compose на cancel', function() {
            var mComposeFsmSetState = this.sinon.stub(this.view.getModel('compose-fsm'), 'setState');

            this.view.onClick();

            expect(mComposeFsmSetState).to.be.calledWithExactly('cancel');
        });
    });
});
