describe('Daria.vComposeSendButtonComplex', function() {

    beforeEach(function() {
        //this.mComposeMessage = ns.Model.get('compose-message').setData({});
        this.view = ns.View.create('compose-send-button-complex');
        this.sinon.spy(this.view, 'onChangedSendTime');
        this.sinon.spy(this.view, 'onChangeSubmitButtonText');
        return this.view.update();
    });

    describe('#onChangedSendTime', function() {
        it('Должен вызвать метод изменения описания кнопки Отправить при изменении времени отправки', function() {
            this.view.getModel('compose-message').set('.send_time', null);
            expect(this.view.onChangedSendTime).to.have.callCount(1);
        });
    });

    describe('#onChangeSubmitButtonText', function() {
        it('Должен вызвать метод изменения описания кнопки Отправить при изменении режима переводчика', function() {
            this.view.getModel('compose-state').set('.submitButtonText', 'test');
            expect(this.view.onChangeSubmitButtonText).to.have.callCount(1);
        });
    });

});
