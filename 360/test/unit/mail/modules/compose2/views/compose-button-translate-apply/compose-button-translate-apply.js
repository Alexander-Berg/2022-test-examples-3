describe('Daria.vComposeButtonTranslateApply', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-button-translate-apply');
        this.sinon.stub(this.view, 'composeUpdate');
        return this.view.update();
    });

    describe('#composeUpdate', function() {
        it('Должен запустить обновление композа composeUpdate при изменении translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', true);
            expect(this.view.composeUpdate).to.have.callCount(1);
        });
    });
});

