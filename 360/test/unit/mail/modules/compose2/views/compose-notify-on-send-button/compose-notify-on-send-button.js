describe('Daria.vComposeNotifyOnSend', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-notify-on-send-button');
        this.sinon.stub(this.view, 'composeUpdate');
        return this.view.update();
    });

    describe('#composeUpdate', function() {
        it('Должен запустить обновление композа composeUpdate при изменении translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', true);
            expect(this.view.composeUpdate).to.have.callCount(1);
        });
    });

    describe('#isMinimize', function() {
        it('Метод isMinimize должен вернуть true, если включен translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', true);
            expect(this.view.isMinimize()).to.be.ok;
        });

        it('Метод isMinimize должен вернуть false, если выключен translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', false);
            expect(this.view.isMinimize()).not.to.be.ok;
        });
    });
});

