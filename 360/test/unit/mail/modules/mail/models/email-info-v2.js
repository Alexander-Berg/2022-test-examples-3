describe('Daria.mEmailInfo', function() {
    describe('Создание ->', function() {
        it('должен создать валидную модель, если не передали email', function() {
            const model = ns.Model.get('email-info-v2');
            expect(model.isValid()).to.be.equal(true);
        });

        it('не должен создать валидную модель, если передали email', function() {
            const model = ns.Model.get('email-info-v2', { email: 'doochik@ya.ru' });
            expect(model.isValid()).to.be.equal(false);
        });
    });

    describe('#setError', function() {
        beforeEach(function() {
            this.model = ns.Model.get('email-info-v2');
            this.scenarioManager = this.sinon.stubScenarioManager(this.model);
        });

        it('Должен дописать шаг "view-error-email-info", если есть активный сценарий "Просмотр письма"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('message-view-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('message-view-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'view-error-email-info', severity: 'minor' });
        });
    });
});
