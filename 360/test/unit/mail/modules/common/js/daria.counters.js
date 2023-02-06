describe('Jane.Metrika', function() {
    describe('#count', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.Metrika, 'inited').value(true);
            this.sinon.stub(Daria.metrikaGroup, 'addCount');
            Jane.Metrika.counts = [];
        });

        it('Если метрика ещё не инициализирована, то должен добавить параметры в кэш', function() {
            Jane.Metrika.inited = false;
            Jane.Metrika.count('1', '2', '3');

            expect(Jane.Metrika.counts).to.eql([ [ '1', '2', '3' ] ]);
            expect(Daria.metrikaGroup.addCount).have.callCount(0);
        });

        it(
            'Если метрика инициализирована, то должен вызвать Daria.metrikaGroup.addCount с пришедшими параметрами',
            function() {
                Jane.Metrika.count('1', '2', '3');

                expect(Jane.Metrika.counts).to.eql([]);

                expect(Daria.metrikaGroup.addCount).have.been.calledWith([ '1', '2', '3' ]);
                expect(Daria.metrikaGroup.addCount).have.callCount(1);
            }
        );
    });

    describe('#groupedCount', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.Metrika, 'inited').value(true);
            this.sinon.stub(Daria.metrikaGroup, 'addGroupedCount');
            Jane.Metrika.groupedCounts = [];
        });

        it('Если метрика ещё не инициализирована, то должен добавить параметры в кэш', function() {
            Jane.Metrika.inited = false;
            Jane.Metrika.groupedCount('1', '2', '3');

            expect(Jane.Metrika.groupedCounts).to.eql([ [ '1', '2', '3' ] ]);
            expect(Daria.metrikaGroup.addGroupedCount).have.callCount(0);
        });

        it(
            'Если метрика инициализирована, то должен вызвать Daria.metrikaGroup.addCount с пришедшими параметрами',
            function() {
                Jane.Metrika.groupedCount('1', '2', '3');

                expect(Jane.Metrika.groupedCounts).to.eql([]);

                expect(Daria.metrikaGroup.addGroupedCount).have.been.calledWith([ '1', '2', '3' ]);
                expect(Daria.metrikaGroup.addGroupedCount).have.callCount(1);
            }
        );
    });

});
