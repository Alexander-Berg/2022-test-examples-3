describe('Daria.mResetUnvisited', function() {
    describe('#resetUnvisited', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest').returns(
                Vow.resolve([{ get: this.sinon.stub() }])
            );
        });

        it('должен вызвать forcedRequest себя если есть свежие письма', function() {
            var mResetUnvisited = ns.Model.get('reset-unvisited', { fid: '123' });
            this.sinon.stub(ns.Model.get('folder', { fid: '123' }), 'get').withArgs('.recent').returns(true);

            return mResetUnvisited.resetUnvisited().then(function() {
                expect(ns.forcedRequest).to.have.callCount(1);
                expect(ns.forcedRequest).to.be.calledWith([mResetUnvisited]);
            });
        });

        it('не должен вызвать forcedRequest если свежих писем нет', function() {
            var mResetUnvisited = ns.Model.get('reset-unvisited', { fid: '123' });
            this.sinon.stub(ns.Model.get('folder', { fid: '123' }), 'get').withArgs('.recent').returns(false);

            return mResetUnvisited.resetUnvisited().then(function() {
                expect(ns.forcedRequest).to.have.callCount(0);
            });
        });

        it('должен вызвать forcedRequest себя если есть свежие письма в табе', function() {
            var mResetTabUnvisited = ns.Model.get('reset-unvisited', { tabId: 'social' });
            this.sinon.stub(ns.Model.get('tab', { id: 'social' }), 'get').withArgs('.counters.recent').returns(true);

            return mResetTabUnvisited.resetUnvisited().then(function() {
                expect(ns.forcedRequest).to.have.callCount(1);
                expect(ns.forcedRequest).to.be.calledWith([mResetTabUnvisited]);
            });
        });

        it('не должен вызвать forcedRequest если свежих писем в табе нет', function() {
            var mResetUnvisited = ns.Model.get('reset-unvisited', { tabId: 'social' });
            this.sinon.stub(ns.Model.get('tab', { id: 'social' }), 'get').withArgs('.recent').returns(false);

            return mResetUnvisited.resetUnvisited().then(function() {
                expect(ns.forcedRequest).to.have.callCount(0);
            });
        });
    });
});
