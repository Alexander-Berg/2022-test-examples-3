describe('Daria.vThreadQuickReplyForm', function() {
    beforeEach(function() {
        this.view = ns.View.create('thread-quick-reply-form', { ids: '1', qrIds: '2' });
        this.viewWithQRForThread = ns.View.create('thread-quick-reply-form', { thread_id: 't1', qrIds: '2' });
        this.mMessages = ns.Model.getValid('messages', { thread_id: 't1' });
        this.mMessage = ns.Model.get('message', { ids: '1' });
    });

    describe('#onHide', function() {
        it('должен переписать this.mMessages, если в нем была модель', function() {
            this.view.vCompose = ns.View.create('quick-reply');
            this.view.vCompose.$node = $('<div />');
            this.view.vCompose.node = this.view.vCompose.$node[0];
            this.view.mMessages = this.mMessages;
            this.view.onHide();


            expect(this.view.mMessages).to.be.eql(null);
        });
    });

    describe('#getComposeParams', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'getReplyMessageMid');
        });

        it('должен получать мид из метода getReplyMessageMid', function() {
            this.view.getComposeParams();

            expect(this.view.getReplyMessageMid).to.have.callCount(1);
        });
    });

    describe('#getReplyMessagesModel', function() {
        it('если в параметрах вьюхи передали thread_id, то должна вернуться модель messages', function() {
            this.stubModels = this.sinon.stub(ns.Model, 'get');
            this.stubModels.withArgs('messages', { thread_id: 't1' }).returns(this.mMessages);

            expect(this.viewWithQRForThread.getReplyMessagesModel()).to.be.eql(this.mMessages);
        });
    });
});
