describe('Daria.vComposeAttachQuickReplyMixin', function() {
    beforeEach(function() {
        this.params = { 'test': '55' };
        this.view = ns.View.create('compose-attach-open-disk-mixin');
        this.sinon.stub(this.view, 'params').value(this.params);
        this.sinon.stub(Daria, 'isReactiveCompose').returns(false);
    });

    describe('#addQuickReplyAttach', function() {
        it('Вставляет объекты с данными о аттачах в compose-state.quickReplyAttaches', function() {
            var mComposeState = ns.Model.get('compose-state', this.params);
            mComposeState.setData({ quickReplyAttaches: [] });

            var attachData = {
                id: 'id',
                resource: 'rsc'
            };
            this.view.addQuickReplyAttach(attachData);

            expect(mComposeState.get('.quickReplyAttaches')).to.be.eql([attachData]);
        });
    });
});
