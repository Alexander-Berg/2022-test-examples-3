describe('Daria.vComposeQuickReplyDelete', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message').setData({});
        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});

        this.view = ns.View.create('compose-quick-reply-delete');
        this.view.$node = $('<div />');

        var stubModels = this.sinon.stub(this.view, 'getModel');
        stubModels.withArgs('compose-message').returns(this.mComposeMessage);
        stubModels.withArgs('quick-reply-state').returns(this.mQuickReplyState);
    });

    describe('#onClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'getDraftMid');
            this.sinon.stub(this.mQuickReplyState, 'trigger');
        });

        it('Должен вызвать событие удаления черновика', function() {
            this.mComposeMessage.getDraftMid.returns('123');
            this.view.onClick();
            expect(this.mQuickReplyState.trigger).to.be.calledWithExactly('ns-model:delete-draft', '123');
        });
    });

    describe('#onDraftSaved', function() {
        beforeEach(function() {
            this.sinon.stub(this.view.$node, 'toggleClass');
            this.sinon.stub(this.view, 'toShow');
        });

        it('Должен скрыть вид, если нет черновика', function() {
            this.view.toShow.returns(false);
            this.view.onDraftSaved();
            expect(this.view.$node.toggleClass).to.be.calledWithExactly('is-hidden', true);
        });

        it('Должен показать вид, если есть черновик', function() {
            this.view.toShow.returns(true);
            this.view.onDraftSaved();
            expect(this.view.$node.toggleClass).to.be.calledWithExactly('is-hidden', false);
        });
    });

    describe('#toShow', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'isDraft');
        });

        it('Должен вернуть признак что письмо черновик', function() {
            this.mComposeMessage.isDraft.returns(true);
            expect(this.view.toShow()).to.be.ok;
        });

        it('Должен вернуть признак что письмо не черновик', function() {
            this.mComposeMessage.isDraft.returns(false);
            expect(this.view.toShow()).to.not.be.ok;
        });
    });
});

