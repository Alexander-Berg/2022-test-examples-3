describe('Daria.metrikaGroup', function() {

    describe('#addCount', function() {

        function sit(caseNum, inputValue, expectedOutputValue) {
            it('Должен правильно обработать и добавить параметры в очередь case #'+ caseNum, function() {
                Daria.metrikaGroup.addCount(inputValue);

                expect(Daria.metrikaGroup._paramsQueue).to.be.eql([ expectedOutputValue ]);
            });
        }

        beforeEach(function() {
            Daria.metrikaGroup._sendParamsOnIdle.cancel();
            Daria.metrikaGroup._paramsQueue = [];

            this.sinon.stub(Daria.metrikaGroup, '_sendParamsOnIdle');
        });

        it('изначально в очереди ничего нет', function() {
            expect(Daria.metrikaGroup._paramsQueue).to.be.eql([]);
        });

        it('параметры добавляются в очередь', function() {
            var params = { 'count': 'this' };
            Daria.metrikaGroup.addCount(params);

            expect(Daria.metrikaGroup._paramsQueue).to.be.eql([ params ]);
        });

        sit('1', { 'count': 'this' }, { 'count': 'this' });
        sit('2', [ 'A', 'B', 'C' ], { 'A': { 'B': 'C' } });
        sit('3', [ 'A', [ 'B' ], 'C' ], { 'A': { 'B': 'C' } });
        sit('4', [ [ 'A', 'B' ], 'C' ], { 'A': { 'B': 'C' } });
        sit('5', [ [ 'A', 'B', 'C' ] ],   { 'A': { 'B': 'C' } });
        sit('6', [ 'A', [ 'B', 'C' ] ],   { 'A': { 'B': 'C' } });
        sit('7', [ {'A': { 'B': 'C' } }], { 'A': { 'B': 'C' } });


        it('инициируется отложенная отправка метрики', function() {
            var params = { 'count': 'this' };
            Daria.metrikaGroup.addCount(params);

            expect(Daria.metrikaGroup._sendParamsOnIdle).to.have.callCount(1);
        });

    });

    describe('#addGroupedCount', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.metrikaGroup._paramsGrouper, 'push');
            this.sinon.stub(Daria.metrikaGroup, '_sendParamsOnIdle');
        });

        function sit(caseNum, inputValue, expectedOutputValue) {
            it('Должен правильно обработать и добавить параметр в _paramsGrouper case #' + caseNum, function() {
                Daria.metrikaGroup.addGroupedCount(inputValue);

                expect(Daria.metrikaGroup._paramsGrouper.push).have.been.calledWith(expectedOutputValue);
                expect(Daria.metrikaGroup._paramsGrouper.push).have.callCount(1);
            });
        }

        sit('1', [ '1', '2', '3' ], [ '1', '2', '3' ]);
        sit('2', [ 'A', [ 'B' ], 'C' ], [ 'A',  'B' , 'C' ]);
        sit('3', [ [ 'A', 'B' ], 'C' ], [ 'A',  'B' , 'C' ]);
        sit('4', [ [ 'A', 'B', 'C' ] ], [ 'A',  'B' , 'C' ]);
        sit('5', [ 'A', [ 'B', 'C' ] ], [ 'A',  'B' , 'C' ]);

        it('Должен вызвать #_sendParamsOnIdle', function() {
            Daria.metrikaGroup.addGroupedCount([ '1', '2', '3' ]);
            expect(Daria.metrikaGroup._sendParamsOnIdle).have.callCount(1);
        });
    });

    describe('#sendAllImmediate', function() {

        beforeEach(function() {
            Daria.metrikaGroup._sendParamsOnIdle.cancel();
            Daria.metrikaGroup._paramsQueue = [];

            this.sinon.stub(Daria.metrikaGroup._sendParamsOnIdle, 'forceExecute');
        });

        it('выполняет принудительную отправку данных в метрику', function() {
            Daria.metrikaGroup.sendAllImmediate();

            expect(Daria.metrikaGroup._sendParamsOnIdle.forceExecute).to.have.callCount(1);
        });

    });

    describe('#_sendParamsPortion', function() {

        beforeEach(function() {
            Daria.metrikaGroup._sendParamsOnIdle.cancel();
            Daria.metrikaGroup._paramsQueue = [];

            this.sinon.stub(Daria.metrikaGroup._paramsGrouper, 'isEmpty').returns(true);
            this.sinon.stub(Daria.metrikaGroup._paramsGrouper, 'release').returns([]);

            this.sinon.stub(Daria.metrikaGroup, '_sendParamsOnIdle');
            this.sinon.stub(Jane.Metrika, 'counter').value({ params: this.sinon.spy() });
        });

        it('ничего не делаем, если очередь с параметрами пуста', function() {
            Daria.metrikaGroup._sendParamsPortion();
            expect(Jane.Metrika.counter.params).to.have.callCount(0);
        });

        it('если очередь параметров непуста - выполняет отправку порции в метрику', function() {
            Daria.metrikaGroup.addCount({ one: 1 });
            Daria.metrikaGroup.addCount({ one: 1 });
            Daria.metrikaGroup._sendParamsPortion();

            expect(Jane.Metrika.counter.params).to.have.callCount(1);
        });

        it('в каждую порцию прокидываем версию пакета', function() {
            Daria.metrikaGroup.addCount({ one: 1 });
            Daria.metrikaGroup._sendParamsPortion();

            expect(Jane.Metrika.counter.params).to.have.been.calledWithExactly([
                { one: 1 },
                { version: 'VER' }
            ]);
        });

        it('размер порции - не больше 128', function() {
            for (var i = 0; i < 300; i++) {
                Daria.metrikaGroup.addCount({ one: 1 });
            }

            // Первая порция - 128
            Daria.metrikaGroup._sendParamsPortion();
            expect(Jane.Metrika.counter.params).to.have.callCount(1);
            expect(Jane.Metrika.counter.params.getCall(0).args[0].length).to.be.equal(128);

            // Вторая порция - 300 - 128 = 172
            Daria.metrikaGroup._sendParamsPortion();
            expect(Jane.Metrika.counter.params).to.have.callCount(2);
            expect(Jane.Metrika.counter.params.getCall(1).args[0].length).to.be.equal(128);

            // Третья порция - 172 - 128 + 3(версия клиента) = 47
            Daria.metrikaGroup._sendParamsPortion();
            expect(Jane.Metrika.counter.params).to.have.callCount(3);
            expect(Jane.Metrika.counter.params.getCall(2).args[0].length).to.be.equal(47);

            // Больше отправлять нечего.
            Daria.metrikaGroup._sendParamsPortion();
            expect(Jane.Metrika.counter.params).to.have.callCount(3);
        });

        it('если остались неотправленные данные - возвращает true', function() {
            for (var i = 0; i < 200; i++) {
                Daria.metrikaGroup.addCount({ one: 1 });
            }

            expect(Daria.metrikaGroup._sendParamsPortion()).to.be.equal(true);
        });

        it('если были отправлены все данные - возвращает false', function() {
            for (var i = 0; i < 200; i++) {
                Daria.metrikaGroup.addCount({ one: 1 });
            }

            Daria.metrikaGroup._sendParamsPortion();

            expect(Daria.metrikaGroup._sendParamsPortion()).to.be.equal(false);
        });

    });

    describe('Daria.metrikaGroup._paramsGrouper', function() {
        beforeEach(function() {
            this.grouper = Daria.metrikaGroup._paramsGrouper;
        });

        describe('#push', function() {
            beforeEach(function() {
                this.sinon.stub(this.grouper, '_metrikaIds').value({});
            });

            it('Если ещё нет добавляемых параметров, то должен добавить параметры и увеличить счётчик', function() {
                this.grouper.push([ '1', '2', '3' ]);

                expect(this.grouper._metrikaIds).to.eql({
                    '1__2__3': [ 1 ]
                });

                expect(this.grouper._uniqueParamsCount).to.equal(1);
            });

            it('Если добавляемыe параметры уже есть, то должен только увеличеть счётчик группы параметра', function() {
                this.grouper._metrikaIds = { '1__2__3': [ 1 ]};
                this.grouper._uniqueParamsCount = 1;

                this.grouper.push([ '1', '2', '3' ]);

                expect(this.grouper._metrikaIds).to.eql({
                    '1__2__3': [ 2 ]
                });

                expect(this.grouper._uniqueParamsCount).to.equal(1);
            });

            it(
                'Если у добавляемых параметров уже есть переполненная группа, ' +
                'то должен сделать счётчик новой группы параметров',

                function() {
                    this.grouper._metrikaIds = { '1__2__3': [ 128 ]};
                    this.grouper._uniqueParamsCount = 1;

                    this.grouper.push([ '1', '2', '3' ]);

                    expect(this.grouper._metrikaIds).to.eql({
                        '1__2__3': [ 128, 1 ]
                    });

                    expect(this.grouper._uniqueParamsCount).to.equal(1);
                }
            );
        });

        describe('#release', function() {
            beforeEach(function() {
                this.sinon.stub(this.grouper, '_metrikaIds').value({
                    '1__2__3': [ 1 ],
                    '4__5__6': [ 5 ],
                    '7__8__9': [ 7 ],
                    '10__11__12': [ 10 ],
                    '101__102__103': [ 3 ]
                });

                this.sinon.stub(this.grouper, '_uniqueParamsCount').value(5);
                this.sinon.stub(this.grouper, '_pop').returns({ test: 'test' });
            });

            it('Если структура данных пустая, то должен выдать пустой массив', function() {
                this.grouper._uniqueParamsCount = 0;
                expect(this.grouper.release(1)).to.eql([]);
            });

            it('Должен вызвать #_pop указанное кол-во раз и вернуть правильный результат', function() {
                expect(this.grouper.release(3)).to.eql(_.fill(Array(3), { test: 'test' }));
                expect(this.grouper._pop).have.callCount(3);
            });

            it('Если передан кол-во больше, чем есть, то должен выдать все хранящиеся объекты', function() {
                expect(this.grouper.release(6)).to.eql(_.fill(Array(5), { test: 'test' }));
                expect(this.grouper._pop).have.callCount(5);
            });

            it('Если ничего не передано, то должен выдать все хранящиеся объекты', function() {
                expect(this.grouper.release()).to.eql(_.fill(Array(5), { test: 'test' }));
                expect(this.grouper._pop).have.callCount(5);
            });

            it('Если передана не корректное кол-во, то должен выдать все хранящиеся объекты', function() {
                expect(this.grouper.release(-1)).to.eql(_.fill(Array(5), { test: 'test' }));
                expect(this.grouper._pop).have.callCount(5);
            });

            it('Если передан 0, то должен выдать все хранящиеся объекты', function() {
                expect(this.grouper.release(0)).to.eql(_.fill(Array(5), { test: 'test' }));
                expect(this.grouper._pop).have.callCount(5);
            });

            it('Если #_pop возвращает null, то должен продолжать перебор paramsId', function() {
                this.grouper._pop.returns(null);
                expect(this.grouper.release(5)).to.eql([]);
                expect(this.grouper._pop).have.callCount(5);
            });
        });

        describe('#_pop', function() {
            beforeEach(function() {
                this.sinon.stub(this.grouper, '_metrikaIds').value({
                    '10__11__12': [ 10 ],
                    '101__102__103': [ 128, 3 ],
                    'test__test': []
                });

                this.sinon.stub(this.grouper, '_uniqueParamsCount').value(2);
            });

            it('Если у переданного paramsId нет в группировщике, то должен вернуть null', function() {
                expect(this.grouper._pop('123__456__789')).to.eql(null);
            });

            it('Если у переданного paramsId нет групп, то должен вернуть null и стереть paramsId', function() {
                expect(this.grouper._pop('test__test')).to.eql(null);
                expect('test__test' in this.grouper._metrikaIds).to.equal(false);
            });

            it(
                'Если у paramsId есть одна группа, то должен выдать данные, уменьшить счётчик и стереть paramsId',
                function() {
                    expect(this.grouper._pop('10__11__12')).to.eql({ '10': { '11': _.fill(Array(10), '12') } });
                    expect('10__11__12' in this.grouper._metrikaIds).to.equal(false);
                    expect(this.grouper._uniqueParamsCount).to.equal(1);
                }
            );

            it(
                'Если у paramsId есть несколько групп, то должен выдать данные заполненной группы',
                function() {
                    expect(this.grouper._pop('101__102__103')).to.eql({ '101': { '102': _.fill(Array(128), '103') } });
                    expect(this.grouper._metrikaIds['101__102__103']).to.eql([ 3 ]);
                    expect(this.grouper._uniqueParamsCount).to.equal(2);
                }
            );
        });
    });

});
