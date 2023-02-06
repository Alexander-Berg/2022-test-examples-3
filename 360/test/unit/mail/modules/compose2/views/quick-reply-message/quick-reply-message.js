describe('Daria.vQuickReplyMessage', function() {
    beforeEach(function() {
        this.mMessage = ns.Model.get('message', { 'ids': '123' }).setData({});
        this.mMessageBody = ns.Model.get('message-body', { 'ids': '123' }).setData({
            'info': {
                'message-id': '456'
            }
        });
        this.mStateMessageThreadItem = ns.Model.get('state-message-thread-item', { 'ids': '123' }).setData({});

        this.view = ns.View.create('quick-reply-message', { 'ids': '123' });

        var models = this.sinon.stub(this.view, 'getModel');
        models.withArgs('message-body').returns(this.mMessageBody);

        var modelGet = this.sinon.stub(ns.Model, 'get');
        modelGet.withArgs('state-message-thread-item').returns(this.mStateMessageThreadItem);
        modelGet.withArgs('message').returns(this.mMessage);

        this.insertMessage = {
            'message': {
                'mid': '123',
                'hdr_message_id': '456'
            }
        };
    });

    describe('#onXivaMailInsert', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.utils, 'checkReplaceFakeMessage').returns(true);
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.sinon.stub(this.mMessage, 'isValid').returns(false);
        });

        it('При совпадении message-id должен установить признак открытого письма в треде', function() {
            this.view.onXivaMailInsert('xiva.mail.insert', this.insertMessage);
            expect(this.mStateMessageThreadItem.setOpen).to.be.calledWithExactly(true);
        });
    });
});

