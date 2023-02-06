describe('Daria.vMessageQuickReply', function() {

    beforeEach(function() {
        this.view = ns.View.create('message-quick-reply', {ids: '123'});
        this.sinon.stub(this.view, 'isVisible').returns(true);
        this.sinon.stub(this.view, 'isLoading').returns(false);
        this.mQuickReplyState = this.view.getModel('quick-reply-state');
    });

    describe('#patchLayout', function() {
        it('должен вернуть layout-quick-reply если нужно показать форму QR', function() {
            this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(true);
            var layout = this.view.patchLayout();
            expect(layout).to.be.equal('layout-quick-reply');
        });

        it('Должен вернуть layout для отображения Done, если его нужно отобразить', function() {
            this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(false);
            this.sinon.stub(this.mQuickReplyState, 'isShowDone').returns(true);

            var layout = this.view.patchLayout();
            expect(layout).to.be.equal('layout-quick-reply-done');
        });

        it('Должен вернуть layout для отображения скрытого QR, если не нужно показать QR и Done', function() {
            this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(false);
            this.sinon.stub(this.mQuickReplyState, 'isShowDone').returns(false);

            var layout = this.view.patchLayout();
            expect(layout).to.be.equal('layout-quick-reply-placeholder');
        });
    });
});

