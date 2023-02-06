describe('Daria.vComposeFieldToExtrasBcc', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-to-extras-bcc');
        this.mComposeState = this.view.getModel('compose-state');
    });

    describe('#onClick', function() {
        it('должен установить фокус на поле bcc', function() {
            var stub = this.sinon.stub(this.mComposeState, 'setFocusField');
            this.view.onClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('bcc');
        });

        it('должен установить признак показа поля', function() {
            var stub = this.sinon.stub(this.mComposeState, 'setIfChanged');
            this.view.onClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isBccVisible', true);
        });
    });
});

