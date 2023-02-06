describe('Daria.Xiva.handlers.updateThreadCounters', function() {
    beforeEach(function() {
        setModelsByMock('message');
        this.sinon.stub(ns.Model, 'traverse');
        ns.Model.traverse.callsFake((model, callback) => {
            callback(ns.Model.get('message', { ids: 't52' }));
            callback(ns.Model.get('message', { ids: 't54' }));
            callback(ns.Model.get('message', { ids: '55' }));
        });
    });

    function checkCountersNotChanged() {
        expect(ns.Model.get('message', { ids: 't52' }).get('.new')).to.equals(2);
        expect(ns.Model.get('message', { ids: 't53' }).get('.new')).to.equals(3);
        expect(ns.Model.get('message', { ids: 't54' }).get('.new')).to.equals(4);
        expect(ns.Model.get('message', { ids: '55' }).get('.new')).to.equals(0);
    };

    describe('должен обновить каунтер прочитанности', function() {
        it('для всех тредов', function() {
            checkCountersNotChanged();

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [ '52', '53', '54' ] }).then(() => {
                expect(ns.Model.get('message', { ids: 't52' }).get('.new')).to.equals(5);
                expect(ns.Model.get('message', { ids: 't53' }).get('.new')).to.equals(3);
                expect(ns.Model.get('message', { ids: 't54' }).get('.new')).to.equals(1);
                expect(ns.Model.get('message', { ids: '55' }).get('.new')).to.equals(0);
            });
        });

        it('только для тех тредов, данные которых вернулись из api', function() {
            ns.Model.traverse.callsFake((model, callback) => {
                callback(ns.Model.get('message', { ids: 't52' }));
                callback(ns.Model.get('message', { ids: 't53' }));
                callback(ns.Model.get('message', { ids: 't54' }));
                callback(ns.Model.get('message', { ids: '55' }));
            });

            checkCountersNotChanged();

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [ '52', '53', '54' ] }).then(() => {
                expect(ns.Model.get('message', { ids: 't52' }).get('.new')).to.equals(5);
                expect(ns.Model.get('message', { ids: 't53' }).get('.new')).to.equals(3);
                expect(ns.Model.get('message', { ids: 't54' }).get('.new')).to.equals(1);
                expect(ns.Model.get('message', { ids: '55' }).get('.new')).to.equals(0);
            });
        });
    });

    describe('не должен обновить каунтер прочитанности треда', function() {
        it('если произошла ошибка при походе за моделью', function() {
            checkCountersNotChanged();

            this.sinon.stub(ns, 'forcedRequest').callsFake(() => vow.reject());

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [ '53', '54' ] }).then(function () {
                expect(ns.forcedRequest).to.have.been.calledWithExactly('threads-info', { thread_ids: '54' } );
                checkCountersNotChanged();
            });
        });

        it('если в пуше пришел пустой массив tids', function() {
            checkCountersNotChanged();

            this.sinon.spy(ns, 'forcedRequest');

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [] }).then(function() {
                expect(ns.forcedRequest).not.to.have.been.called;
                checkCountersNotChanged();
            });
        });

        it('если у пользователя нет загруженных тредов, которые пришли в пуше', function() {
            checkCountersNotChanged();

            this.sinon.spy(ns, 'forcedRequest');

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [ '99' ] }).then(function() {
                expect(ns.forcedRequest).not.to.have.been.called;
                checkCountersNotChanged();
            });
        });

        it('если пришёл null или "null"', function() {
            checkCountersNotChanged();

            this.sinon.spy(ns, 'forcedRequest');

            return Daria.Xiva.handlers.updateThreadCounters({ tids: [ 'null', null ] }).then(function() {
                expect(ns.forcedRequest).not.to.have.been.called;
                checkCountersNotChanged();
            });
        });
    });
});
