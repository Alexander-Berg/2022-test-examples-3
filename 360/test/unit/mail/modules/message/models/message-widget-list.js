describe('Daria.mMessageWidgetList', function() {
    beforeEach(function() {
        this.model = ns.Model.get('message-widget-list', { 'ids': 'test' });
        this.model.clear();
    });

    describe('#insertWidget', function() {
        it('Должен добавить в коллекцию модель message-widget с указанным типом', function() {
            this.model.insertWidget('message-test-widget');
            expect(this.model.models[0].id).to.be.equal('message-widget');
            expect(this.model.models[0].params.type).to.be.equal('message-test-widget');
        });
    });
});

