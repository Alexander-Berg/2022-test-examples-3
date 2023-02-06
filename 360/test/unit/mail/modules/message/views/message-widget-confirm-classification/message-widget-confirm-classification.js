describe('Daria.vMessageWidgetConfirmClassification', function() {
    beforeEach(function() {
        this.view = ns.View.create('message-widget-confirm-classification', { ids: '42' });
        this.sinon.stub(this.view, 'getTypes').returns([ 103, 100, 102 ]);
    });

    describe('#changeState', function() {
        it('должен изменить состояние и перерисовать вид', function() {
            this.sinon.stub(this.view, 'forceUpdate');

            this.view.changeState('testState');
            expect(this.view._state).to.equal('testState');
            expect(this.view.forceUpdate).to.have.callCount(1);
        });
    });

    describe('#_sendMetric', function() {
        it('должен отправить правильную метрику', function() {
            this.sinon.stub(Jane, 'c');
            this.view._type = 1;

            this.view._sendMetric('testMetric');

            expect(Jane.c).to.have.been.calledWith(
                'Классификатор типов писем', 'Запрос к пользователю', 1, 'testMetric'
            );
        });
    });

    describe('#_sendLog', function() {
        beforeEach(function() {
            this.sinon.stub(ns.request, 'addRequestParams').returnsArg(0);
            this.sinon.stub(Daria, 'actionLog').value({
                send: this.sinon.stub()
            });

            this.sinon.stub(this.view, 'getModel').withArgs('message').returns({
                get: this.sinon.stub().returns('testMid')
            });

            this.view._type = 1;
            this.view._category = 'testCategory';
            this.view._state = 'testState';
        });

        it('должен правильно записать лог, если нет кастомных данных для логирования', function() {
            const target = 'testTarget';
            const logParams = JSON.stringify({
                action: 'classification',
                mid: 'testMid',
                types: '103,100,102',
                show_type: 1,
                category: 'testCategory',
                state: 'testState',
                target
            });

            this.view._sendLog(target);

            expect(Daria.actionLog.send).to.have.been.calledWith({ data: logParams });
        });

        it('должен правильно записать лог, если есть кастомные данные для логирования', function() {
            const target = 'testTarget2';
            const logParams = JSON.stringify(
                Object.assign(
                    {
                        action: 'classification',
                        mid: 'testMid',
                        types: '103,100,102',
                        show_type: 1,
                        category: 'testCategory',
                        state: 'testState'
                    },
                    { value: '123' },
                    { target }
                )
            );

            this.view._sendLog(target, { value: '123' });

            expect(Daria.actionLog.send).to.have.been.calledWith({ data: logParams });
        });
    });
});
