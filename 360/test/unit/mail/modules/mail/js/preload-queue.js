describe('Daria.preloadQueue', function() {

    beforeEach(function() {
        this.sinon.stub(ns, 'request').returns(new vow.Promise());
    });

    afterEach(function() {
        Daria.preloadQueue.reset();
    });

    describe('.add', function() {

        it('должен запустить .run', function() {
            this.sinon.stub(Daria.preloadQueue, 'run');

            Daria.preloadQueue.add([]);

            expect(Daria.preloadQueue.run).to.have.callCount(1);
        });

    });

    describe('.run', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.preloadQueue, 'CONNECTIONS_MAX').value(2);
            this.sinon.stub(Daria.preloadQueue, 'MODELS_MAX').value(2);

            this.sinon.spy(Daria.preloadQueue, 'run');
        });

        describe('Пул ->', function() {

            beforeEach(function() {
                // мы все застабили и нам не важно, что грузится на самом деле =)
                Daria.preloadQueue.add([1,2,3,4,5]);
            });

            it('должен создать CONNECTIONS_MAX запросов', function() {
                expect(ns.request).to.have.callCount(Daria.preloadQueue.CONNECTIONS_MAX);
            });

            it('не должен создать еще запрос, если вызвать run руками', function() {
                Daria.preloadQueue.run();
                expect(ns.request).to.have.callCount(Daria.preloadQueue.CONNECTIONS_MAX);
            });

        });

        it('не должен перезапускаться, если нет очереди', function() {
            Daria.preloadQueue.run();
            expect(Daria.preloadQueue.run).to.have.callCount(1);
        });

        it('не должен перезапускаться, если нет очереди', function() {
            Daria.preloadQueue.run();
            expect(Daria.preloadQueue.run).to.have.callCount(1);
        });

        it('должен перезапуститься, если есть очередь', function() {
            Daria.preloadQueue.add([1,2]);
            expect(Daria.preloadQueue.run).to.have.callCount(2);
        });

        it('должен перезапускаться пока не закончится очередь', function() {
            //TODO: не смог придумать как сделать этот тест нормальным

            // делаем синхронный промис
            ns.request.returns({
                always: function(cb) {
                    cb();
                }
            });

            Daria.preloadQueue.add([1,2, 3,4, 5]);

            expect(ns.request.getCall(0).args).to.be.eql([[1, 2]]);
            expect(ns.request.getCall(1).args).to.be.eql([[3, 4]]);
            expect(ns.request.getCall(2).args).to.be.eql([[5]]);
        });

    });

});
