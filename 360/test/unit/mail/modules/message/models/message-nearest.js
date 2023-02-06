describe('handlers/message/message-nearest', function() {
    describe('#getNextMid', function() {
        beforeEach(function() {
            /** @type Daria.mMessageNearest */
            this.handler = ns.Model.get('message-nearest');
        });

        it('должен вернуть mid следующего письма', function() {
            var data = {
                message: [
                    { mid: '2370000000756293195', prev: true },
                    { mid: '2370000000756125132', current: true },
                    { mid: 'next-mid', next: true }
                ]
            };
            this.handler.setData(data);

            expect(this.handler.getNextMid()).to.be.equal('next-mid');
        });

        it('должен вернуть null, если следующего письма нет', function() {
            var data = {
                message: [
                    { mid: '2370000000756293195', prev: true },
                    { mid: '2370000000756125132', current: true },
                    { next: true }
                ]
            };
            this.handler.setData(data);

            expect(this.handler.getNextMid()).to.be.equal(null);
        });

        it('должен вернуть null, если кеша нет', function() {
            expect(this.handler.getNextMid({ ids: 'no-such-mid' })).to.be.equal(null);
        });
    });

    describe('#setError', function() {
        beforeEach(function() {
            this.model = ns.Model.get('message-nearest');
            this.scenarioManager = this.sinon.stubScenarioManager(this.model);
        });

        it('Должен дописать шаг "opening-error-message-nearest", если есть активный сценарий "Просмотр письма"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('message-view-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('message-view-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'opening-error-message-nearest', severity: 'critical' });
        });
    });
});
