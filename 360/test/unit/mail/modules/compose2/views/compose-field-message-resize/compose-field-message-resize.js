describe('Daria.vComposeFieldMessageResize', function() {
    beforeEach(function() {
        this.mComposeState = ns.Model.get('compose-state').setData({});
        this.sinon.stub(this.mComposeState, 'on');

        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});

        this.view = ns.View.create('compose-field-message-resize');
        this.sinon.stub(this.view, 'on');
        this.sinon.stub(this.view, '_messageResizeInitShow');
        var stubModel = this.sinon.stub(this.view, 'getModel');

        stubModel.withArgs('compose-state').returns(this.mComposeState);
        stubModel.withArgs('quick-reply-state').returns(this.mQuickReplyState);
    });

    afterEach(function() {
        this.mComposeState.destroy();
    });

    describe('#messageResizeStart', function() {
        it('должен подписаться на событие изменения высоты редактора', function() {
            this.view.messageResizeInit();
            this.view.messageResizeStart();
            expect(this.view.getModel('compose-state').on).to.be.calledWithExactly('ns-model-changed.messageHeight', this.view._onChangeMessageHeight);
        });
    });

    describe('#_onChangeMessageHeight', function() {
        it('должен вызвать метод onChangeHeight', function() {
            this.sinon.stub(this.view, 'onChangeHeight');
            this.view._onChangeMessageHeight();
            expect(this.view.onChangeHeight).to.have.callCount(1);
        });
    });

    describe('#_getHeight', function() {
        it('должен вернуть содержимое ключа messageHeight стейт модели композа', function() {
            this.mComposeState.set('.messageHeight', 200);
            var val = this.view._getHeight();
            expect(val).to.be.equal(200);
        });
    });
});

