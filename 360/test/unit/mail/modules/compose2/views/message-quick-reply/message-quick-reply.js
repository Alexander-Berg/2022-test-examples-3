describe('Daria.vMessageQuickReply', function() {
    beforeEach(function() {
        this.mMessageDraft = ns.Model.get('message-draft').setData({});
        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});

        this.view = ns.View.create('message-quick-reply');
        this.view.$node = $('<div />');

        var stubModels = this.sinon.stub(this.view, 'getModel');
        stubModels.withArgs('message-draft').returns(this.mMessageDraft);
        stubModels.withArgs('quick-reply-state').returns(this.mQuickReplyState);
    });

});

