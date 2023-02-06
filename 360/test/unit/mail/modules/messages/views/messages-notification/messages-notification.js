describe('vMessagesNotification', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-notification');
    });

    describe('#onCreateTemplateClick', function() {
        beforeEach(function() {
            this.mComposePredefinedData = ns.Model.get('compose-predefined-data');
            this.sinon.stub(this.mComposePredefinedData, 'setData');
            this.sinon.stub(Daria, 'composeGo');
        });

        it('Должен выставить save_symbol=template в compose-predefined-data и перейти на страницу композа', function() {
            this.view.onCreateTemplateClick();
            expect(this.mComposePredefinedData.setData).to.be.calledWith({ save_symbol: 'template' });
            expect(Daria.composeGo).to.be.calledWith();
        });
    });
});
