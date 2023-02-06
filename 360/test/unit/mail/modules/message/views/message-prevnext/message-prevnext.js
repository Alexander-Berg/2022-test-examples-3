describe('message-prevnext', function() {
    beforeEach(function() {
        this.sinon.stub(ns.router, 'generateUrl').callsFake(() => {});
        this.sinon.stub(ns.page, 'go').callsFake(() => {});

        this.paramsHasNextMsg = { ids: '1' };
        this.handler1 = ns.Model.get('message-nearest', this.paramsHasNextMsg);

        var data1 = {
            message: [
                { mid: '2370000000756293195', prev: true },
                { mid: '2370000000756125132', current: true },
                { mid: 'next-mid', next: true }
            ]
        };

        this.handler1.setData(data1);
    });

    describe('метрика качества "Просмотр письма"', function() {
        beforeEach(function() {
            this.view = ns.View.create('message-prevnext-with-shortcuts', this.paramsHasNextMsg);
            this.scenarioManager = this.sinon.stubScenarioManager(this.view);
        });

        it('переход к следующему письму по шорткату запускает сценарий "message-next-hotkey"', function() {
            this.view.onChangeMessage('daria:vMessagePrevNextInMessage:changeMessage', { direction: 'next' });

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');

            expect(this.scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'message-next-hotkey');
        });

        it('переход к предыдущему письму по шорткату запускает сценарий "message-prev-hotkey"', function() {
            this.view.onChangeMessage('daria:vMessagePrevNextInMessage:changeMessage', { direction: 'prev' });

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');

            expect(this.scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'message-prev-hotkey');
        });

        it('переход к следующему письму по клику запускает сценарий "message-next-click"', function() {
            const $button = $('<a data-direction="next"></a>');
            const testEvent = $.Event('click', { currentTarget: $button[0] });

            this.view.onPrevNextClick(testEvent);

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');

            expect(this.scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'message-next-click');
        });
    });
});
