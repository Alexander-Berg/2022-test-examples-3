describe('Daria.vComposeFooter', function() {

    beforeEach(function() {
        this.sinon.stub(ns.page.current, 'page').value('compose2');

        this.view = ns.View.create('compose-footer', {
            'ids': '123',
            'oper': 'reply'
        });
        this.$node = $('<div>');
        this.view.node = this.$node[0];
        this.view.$node = this.$node;

        this.mComposeAttachments = ns.Model.get('compose-attachments').setData({});
        this.mComposeMessage = ns.Model.get('compose-message').setData({});

        this.sinon.stub(this.view, 'getModel')
            .withArgs('compose-attachments').returns(this.mComposeAttachments)
            .withArgs('compose-message').returns(this.mComposeMessage);

        this.sinon.stub(this.mComposeMessage, 'markSaved');
    });

    describe('#onAttachmentsAppended', function() {
        it('должен выполнить скрол до блока, если блок вне области видимости', function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            var stub = this.sinon.stub(Jane.DOM, 'placeInViewport');

            this.view.onAttachmentsAppended();

            expect(stub).to.be.calledWith(this.$node[0]);
        });
    });
});
