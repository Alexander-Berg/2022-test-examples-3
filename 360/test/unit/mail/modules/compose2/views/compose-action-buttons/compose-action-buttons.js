describe('Daria.vComposeActionButtons', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message');
        this.mComposeMessage.composeParamsService = {
            isTemplate: this.sinon.stub()
        };
        this.mComposeState = ns.Model.get('compose-state');
        this.view = ns.View.create('compose-action-buttons');
        var getModelStub = this.sinon.stub(this.view, 'getModel');
        getModelStub.withArgs('compose-message').returns(this.mComposeMessage);
        getModelStub.withArgs('compose-state').returns(this.mComposeState);
        return this.view.update();
    });

    describe('#patchLayout', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'isTemplate');
            this.sinon.stub(this.mComposeMessage, 'isNewTemplate');
            this.sinon.stub(this.mComposeState, 'set').withArgs('.activeActionButtonView');
        });

        it('Раскладка для композа, когда сообщение не является шаблоном', function() {
            this.mComposeMessage.isTemplate.returns(false);
            this.mComposeMessage.isNewTemplate.returns(false);
            var layout = this.view.patchLayout();
            expect(this.mComposeState.set).to.be.calledWithExactly('.activeActionButtonView', 'compose-send-link');
            expect(layout).to.be.equal('layout-compose-action-buttons');
        });

        it('Раскладка для композа, когда сообщение является сохранённым шаблоном', function() {
            this.mComposeMessage.isTemplate.returns(true);
            this.mComposeMessage.isNewTemplate.returns(false);
            var layout = this.view.patchLayout();
            expect(this.mComposeState.set).to.be.calledWithExactly('.activeActionButtonView', 'compose-send-link');
            expect(layout).to.be.equal('layout-compose-action-buttons-template');
        });

        it('Раскладка для композа, когда сообщение является новым шаблоном', function() {
            this.mComposeMessage.isTemplate.returns(true);
            this.mComposeMessage.isNewTemplate.returns(true);
            var layout = this.view.patchLayout();
            expect(this.mComposeState.set).to.be.calledWithExactly('.activeActionButtonView', 'compose-save-link');
            expect(layout).to.be.equal('layout-compose-action-buttons-new-template');
        });
    });
});
