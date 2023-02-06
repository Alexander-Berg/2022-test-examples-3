describe('Daria.mGetUserActivity', function() {

    describe('Изменения данных модели ->', function() {

        it('должен вызвать обработчик изменния lcn', function() {
            this.sinon.stub(Daria.Xiva.handlers, 'processNewLcn');

            ns.Model.get('get_user_activity').setData({
                lcn: 42
            });

            expect(Daria.Xiva.handlers.processNewLcn)
                .to.have.callCount(1)
                .and.to.be.calledWith(42);
        });

    });

    describe('#syncLcn', function() {

        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());
        });

        it('должен вызвать force-обновления себя', function() {
            var mGetUserActivity = ns.Model.get('get_user_activity');

            return mGetUserActivity.syncLcn().then(function() {
                expect(ns.forcedRequest)
                    .to.have.callCount(1)
                    .and.to.be.calledWith([mGetUserActivity]);
            });
        });

    });

});
