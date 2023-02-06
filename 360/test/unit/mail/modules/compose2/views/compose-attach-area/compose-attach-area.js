describe('Daria.vComposeAttachArea', function() {
    beforeEach(function() {
        this.sinon.stub(ns.page.current, 'page').value('compose2');

        this.view = ns.View.create('compose-attach-area', {
            'ids': '123',
            'oper': 'reply'
        });
        this.$node = $('<div>');
        this.view.node = this.$node[0];
        this.view.$node = this.$node;

        this.mComposeAttachments = ns.Model.get('compose-attachments').setData({});
        this.mComposeMessage = ns.Model.get('compose-message').setData({});
        this.mComposeState = ns.Model.get('compose-state').setData({});

        this.sinon.stub(this.view, 'getModel')
            .withArgs('compose-attachments').returns(this.mComposeAttachments)
            .withArgs('compose-message').returns(this.mComposeMessage)
            .withArgs('compose-state').returns(this.mComposeState);

        this.sinon.stub(this.mComposeMessage, 'markSaved');
    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.mComposeAttachments, [
                'drawAttaches',
                'init'
            ]);
        });

        it('должен проинициализировать модель compose-attachments', function() {
            this.view.onShow();

            expect(this.mComposeAttachments.init).to.be.calledWith({
                'node': this.$node[0],
                'ids': this.view.params.ids,
                'operation': this.view.params.oper,
                'mComposeMessage': this.mComposeMessage,
                'mComposeState': this.mComposeState
            });
        });

        it('должен запустить отрисовку аттачей, если передан ids в параметры вида', function() {
            this.sinon.stub(this.view, 'params').value({ 'ids': '123' });

            this.view.onShow();

            expect(this.mComposeAttachments.drawAttaches).to.have.callCount(1);
        });

        it('не должен запускать отрисовку аттачей, если не передан ids в параметры вида', function() {
            this.sinon.stub(this.view, 'params').value({});

            this.view.onShow();

            expect(this.mComposeAttachments.drawAttaches).to.have.callCount(0);
        });
    });
});

