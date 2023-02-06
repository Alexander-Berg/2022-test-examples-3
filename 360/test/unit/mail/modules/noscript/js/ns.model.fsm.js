describe('ns.ModelFsm', function() {
    var states = [ 'start', 'state1', 'state2', 'state3' ];

    before(function() {
        ns.Model.define('fsm-model', {
            methods: {
                start: 'state1',

                transitions: {
                    '*': [ 'state1' ],
                    'start': [ 'state2' ],
                    'state1': [ 'state2', 'state3' ],
                    'state2': [ 'state3' ],
                    'state3': [ '*' ]
                }
            }
        }, ns.ModelFsm);
    });

    beforeEach(function() {
        this.fsmModel = ns.Model.get('fsm-model');
        this.fsmModel.setData({});
        this.fsmModel.log = no.nop;

        /**
         * Описание разрешенных переходов
         */
        this.allowedToTransition = {
            start: [ 'state1', 'state2' ],
            state1: [ 'state2', 'state3' ],
            state2: [ 'state1', 'state3' ],
            state3: [ 'start', 'state1', 'state2' ]
        };
    });

    function shouldResolve(promise, done) {
        promise.then(
            function() {
                done();
            },
            function() {
                done(new Error('Should resolve'));
            }
        );
    }

    function shouldReject(promise, done) {
        promise.then(
            function() {
                done(new Error('Should reject'));
            },
            function() {
                done();
            }
        );
    }

    describe('#request', function() {
        it('должен вызвать ошибку, если не задано начальное состояние модели', function() {
            var that = this;

            expect(function() {
                that.fsmModel.start = undefined;
                that.fsmModel.request();
            }).to.be.throw();
        });

        it('должен установить начальное состояние согласно свойству start', function() {
            this.fsmModel.start = 'state1';
            this.fsmModel.request();

            expect(this.fsmModel.getState()).to.be.equal('state1');
        });

        it('должен вызвать функцию начального состояния, если start определно как функция', function() {
            this.fsmModel.start = function() {
                return 'state2';
            };
            this.fsmModel.request();

            expect(this.fsmModel.getState()).to.be.equal('state2');
        });
    });

    describe('#setState', function() {
        it('должен вернуть promise перехода в новое состояние', function(done) {
            var promise = this.fsmModel.setState('state1');
            expect(promise).to.be.instanceof(vow.Promise);
            shouldResolve(promise, done);
        });

        it('должен запретить переход в новое состояние, если FSM модель отключена', function(done) {
            this.sinon.stub(this.fsmModel, '_isSwitchOn').value(false);
            this.sinon.stub(this.fsmModel, '_inTransition').value(false);
            shouldReject(this.fsmModel.setState('state1'), done);
        });

        states.forEach(function(fromState) {
            states.forEach(function(toState) {
                describe('переход из состояния ' + fromState + ' в состояние ' + toState + '->', function() {
                    beforeEach(function() {
                        this.fsmModel.set('.state', fromState);
                        this.sinon.stub(this.fsmModel, 'trigger');
                    });

                    it('должен вернуть зарезолвленный promise перехода, если переход возможен', function(done) {
                        var promise = this.fsmModel.setState(toState);

                        if (this.allowedToTransition[fromState].indexOf(toState) !== -1) {
                            shouldResolve(promise, done);
                        } else {
                            shouldReject(promise, done);
                        }
                    });

                    it('должен запустить переход, если он разрешен', function(done) {
                        var promise = this.fsmModel.setState(toState);

                        promise.then(function() {
                            expect(this.fsmModel.trigger.firstCall.calledWith(fromState + ' > ' + toState)).to.be.equal(true);
                            done();
                        }.bind(this), function() {
                            expect(this.fsmModel.trigger).to.have.callCount(0);
                            done();
                        }.bind(this));
                    });

                    it('должен передать данные перехода в событие, если он разрешен', function(done) {
                        var stateData = {
                            test: 'test'
                        };
                        var eventData = {
                            from: fromState,
                            to: toState,
                            data: stateData
                        };
                        var promise = this.fsmModel.setState(toState, stateData);

                        promise.then(function() {
                            expect(this.fsmModel.trigger.firstCall.calledWithExactly(fromState + ' > ' + toState, eventData)).to.be.equal(true);
                            done();
                        }.bind(this), function() {
                            expect(this.fsmModel.trigger).to.have.callCount(0);
                            done();
                        }.bind(this));
                    });

                    it('должен запретить переход, если уже начат другой', function(done) {
                        this.fsmModel._inTransition = true;
                        shouldReject(this.fsmModel.setState(toState), done);
                    });
                });
            });
        });
    });

    describe('#getState', function() {
        it('должен вернуть текущее остановленное состояние', function() {
            this.fsmModel.set('.state', 'test');
            expect(this.fsmModel.getState()).to.be.equal('test');
        });
    });

    describe('#isCurrentState', function() {
        it('должен вернуть истину, если передаваемое состояние-строка сейчас текущее', function() {
            this.fsmModel.set('.state', 'state2');

            expect(this.fsmModel.isCurrentState('state2')).to.be.equal(true);
        });

        it('должен вернуть ложь, если передаваемое состояние-строка сейчас не текущее', function() {
            this.fsmModel.set('.state', 'state');

            expect(this.fsmModel.isCurrentState('state2')).to.be.equal(false);
        });

        it('должен вернуть истину, если одно из передаваемым состояний в массиве сейчас текущее', function() {
            this.fsmModel.set('.state', 'state2');

            expect(this.fsmModel.isCurrentState([ 'state2', 'state3' ])).to.be.equal(true);
        });

        it('должен вернуть ложь, если ни одно из передаваемым состояний в массиве сейчас не текущее', function() {
            this.fsmModel.set('.state', 'state1');

            expect(this.fsmModel.isCurrentState([ 'state2', 'state3' ])).to.be.equal(false);
        });
    });

    describe('#transitionEventHandler', function() {
        beforeEach(function() {
            this.eventName = 'start > state1';
            this.eventData = {
                from: 'start',
                to: 'state1',
                data: undefined
            };
            this.promisesArray = [ new vow.Promise(), new vow.Promise(), new vow.Promise() ];
            this.firstCallback = this.sinon.stub().returns(this.promisesArray[0]);
            this.secondCallback = this.sinon.stub().returns(this.promisesArray[1]);
            this.thirdCallback = this.sinon.stub().returns(this.promisesArray[2]);
            this.fsmModel._transitionEvents[this.eventName] = {
                handlers: [
                    this.firstCallback,
                    this.secondCallback
                ],
                from: 'start',
                to: 'state1'
            };
            this.fsmModel._transitionEvents['* > *'] = {
                handlers: [
                    this.thirdCallback
                ],
                from: '*',
                to: '*'
            };

            this.vowDeferred = {
                promise: function() {
                    return this.promiseObj;
                },
                promiseObj: {
                    then: this.sinon.stub()
                },
                resolve: this.sinon.stub(),
                reject: this.sinon.stub()
            };
            this.sinon.stub(vow, 'Deferred').returns(this.vowDeferred);
            this.sinon.stub(vow, 'all').returns({
                then: this.sinon.stub()
            });

            this.fsmModel.set('.state', 'start');
        });

        describe('для события уточненного перехода ->', function() {
            beforeEach(function() {
                this.fsmModel.transitionEventHandler(this.eventName, this.eventData);
            });

            it('должен вызвать все обработчики (в том числе и общий transition), подписанные на передаваемое событие', function() {
                expect(this.firstCallback).to.have.callCount(2);
                expect(this.secondCallback).to.have.callCount(2);
                expect(this.thirdCallback).to.have.callCount(2);
            });

            it('должен передать в обработчики данные о событии', function() {
                expect(this.firstCallback).to.be.calledWithExactly(this.eventName, this.eventData);
                expect(this.secondCallback).to.be.calledWithExactly(this.eventName, this.eventData);
                expect(this.thirdCallback).to.be.calledWithExactly(this.eventName, this.eventData);
            });

            it('должен собрать все promise от обработчиков и дождаться их выполнения', function() {
                expect(vow.all).to.be.calledWithExactly(this.promisesArray);
            });
        });

        describe('для события общего перехода ->', function() {
            beforeEach(function() {
                this.fsmModel.transitionEventHandler('* > *', this.eventData);
            });

            it('должен вызвать только общий обработчик, если был вызван общий transition', function() {
                expect(this.thirdCallback).to.be.calledWithExactly('* > *', this.eventData);
            });

            it('должен собрать promise от общего обработчика события', function() {
                expect(vow.all).to.be.calledWithExactly([ this.promisesArray[2] ]);
            });
        });

        beforeEach(function() {
            this.fsmModel.transitionEventHandler(this.eventName, this.eventData);
        });

        it('должен перевести FSM модель в новое состояние, если все promise успешно завершились (resolve)', function() {
            this.vowDeferred.promise().then.callArg(0);

            expect(this.fsmModel.getState()).to.be.equal('state1');
        });

        it('должен вернуть FSM модель в прежнее состояние, если хоть один из promise неуспешно завершился (reject)', function() {
            this.vowDeferred.promise().then.callArg(1);

            expect(this.fsmModel.getState()).to.be.equal('start');
        });

        it('должен вызвать событие transition failed, если хоть один из promise неуспешно завершился (reject)', function() {
            this.sinon.stub(this.fsmModel, 'trigger');
            this.vowDeferred.promise().then.callArg(1);

            expect(this.fsmModel.trigger.firstCall.calledWith('start !> state1', this.eventData));
            expect(this.fsmModel.trigger.secondCall.calledWith('* !> *', this.eventData));
        });
    });

    describe('#on', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.prototype, 'on');
        });

        describe('подписка на события перехода в новое состояние ->', function() {
            beforeEach(function() {
                this.eventName = 'start > state1';
                this.firstEventHandler = this.sinon.stub();
                this.secondEventHandler = this.sinon.stub();
                this.transitionEventData = {
                    name: this.eventName,
                    handlers: [ this.firstEventHandler ],
                    from: 'start',
                    to: 'state1',
                    callback: this.fsmModel.transitionEventHandler.bind(this)
                };
            });

            it('должен создать объект хранения данных о событии, если он ещё не был создан, и подписаться на событие', function() {
                this.fsmModel._transitionEvents = {};
                this.fsmModel.on(this.eventName, this.firstEventHandler);

                expect(this.fsmModel._transitionEvents[this.eventName]).to.be.an('object');
                expect(ns.Model.prototype.on).to.have.callCount(1);
            });

            it('должен взять существующий объект хранения данных о событии, если он уже создан, и не подписываться повторно на событие', function() {
                this.fsmModel._transitionEvents[this.eventName] = this.transitionEventData;
                this.fsmModel.on(this.eventName, this.secondEventHandler);

                expect(this.fsmModel._transitionEvents[this.eventName]).to.be.equal(this.transitionEventData);
                expect(ns.Model.prototype.on).to.have.callCount(0);
            });

            it('должен при подписке добавлять обработчики в секцию handlers объекта события', function() {
                this.fsmModel.on(this.eventName, this.firstEventHandler);
                this.fsmModel.on(this.eventName, this.secondEventHandler);
                var transitionEventData = this.fsmModel._transitionEvents[this.eventName];

                expect(transitionEventData.handlers).to.be.eql([
                    this.firstEventHandler,
                    this.secondEventHandler
                ]);
            });

            it('должен использовать в качестве обработчика события #transitionEventHandler', function() {
                this.sinon.stub(this.fsmModel, 'transitionEventHandler');
                this.fsmModel.on(this.eventName, this.firstEventHandler);
                ns.Model.prototype.on.callArg(1);

                expect(this.fsmModel.transitionEventHandler).to.have.callCount(1);
            });
        });

        describe('подписка на все события, кроме перехода в новое состояние ->', function() {
            it('должен вызвать подписку на событие у родительского класса', function() {
                this.fsmModel.on('some-event', function() {});

                expect(ns.Model.prototype.on).to.have.callCount(1);
            });

            it('не должен создавать объект хранения данных о событии', function() {
                this.fsmModel.on('some-event', function() {});

                expect(this.fsmModel._transitionEvents['some-event']).to.be.an('undefined');
            });
        });
    });

    describe('#off', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.prototype, 'off');
        });

        describe('отписка от события перехода в новое состояние ->', function() {
            beforeEach(function() {
                this.eventName = 'start > state1';
                this.firstEventHandler = this.sinon.stub();
                this.secondEventHandler = this.sinon.stub();
                this.transitionEventData = {
                    name: this.eventName,
                    handlers: [ this.firstEventHandler, this.secondEventHandler ],
                    from: 'start',
                    to: 'state1',
                    callback: this.fsmModel.transitionEventHandler.bind(this)
                };
                this.fsmModel._transitionEvents[this.eventName] = this.transitionEventData;
            });

            it('не должен отписываться от события, если для данного перехода нет подписок', function() {
                delete this.fsmModel._transitionEvents[this.eventName];
                this.fsmModel.off(this.eventName);

                expect(ns.Model.prototype.off).to.have.callCount(0);
            });

            it('должен удалить обработчик из секции handlers объекта данных о событии', function() {
                this.fsmModel.off(this.eventName, this.firstEventHandler);

                expect(this.fsmModel._transitionEvents[this.eventName].handlers).to.be.eql([
                    this.secondEventHandler
                ]);
                expect(ns.Model.prototype.off).to.have.callCount(0);
            });

            it('должен удалить объект данных о событии и отписаться от события, если все обработчики удалены', function() {
                this.fsmModel.off(this.eventName, this.firstEventHandler);
                this.fsmModel.off(this.eventName, this.secondEventHandler);

                expect(ns.Model.prototype.off).to.have.callCount(1);
                expect(this.fsmModel._transitionEvents[this.eventName]).to.be.an('undefined');
            });
        });

        describe('отписка от всех событий, кроме перехода в новое состояние ->', function() {
            it('должен вызвать отписку от события у родительского класса', function() {
                this.fsmModel.off('some-event', function() {});

                expect(ns.Model.prototype.off).to.have.callCount(1);
            });
        });
    });

    describe('#trigger', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.prototype, 'trigger');
        });

        it('должен запретить вызов события, если FSM модель отключена', function() {
            this.sinon.stub(this.fsmModel, '_isSwitchOn').value(false);
            this.sinon.stub(this.fsmModel, 'getEventData');
            this.fsmModel.trigger('some-event');

            expect(this.fsmModel.getEventData).to.have.callCount(0);
        });

        describe('для события перехода ->', function() {
            beforeEach(function() {
                this.eventName = 'start > state1';
                this.eventData = { from: 'start', to: 'state1' };
                delete this.fsmModel._transitionEvents[this.eventName];
                this.fsmModel.set('.state', 'start');
            });

            it('должен вызвать общий переход, если у уточненного перехода нет обработчиков', function() {
                this.sinon.spy(this.fsmModel, 'trigger');
                this.fsmModel.trigger(this.eventName, this.eventData);

                expect(this.fsmModel.trigger.secondCall.calledWithExactly('* > *', this.eventData)).to.be.equal(true);
            });

            it('должен сразу сменить состояние, если у события общего перехода нет обработчиков', function() {
                this.fsmModel.trigger('* > *', this.eventData);

                expect(this.fsmModel.getState()).to.be.equal('state1');
                expect(ns.Model.prototype.trigger.withArgs(this.eventName, this.eventData)).to.have.callCount(0);
            });

            it('должен вызвать родитеский метод запуска обработчиков события, если у уточненного перехода есть обработчики', function() {
                this.fsmModel.on(this.eventName, function() {});
                this.fsmModel.trigger(this.eventName, this.eventData);

                expect(ns.Model.prototype.trigger.withArgs(this.eventName, this.eventData)).to.have.callCount(1);
            });

            it('должен вызвать родитеский метод запуска обработчиков события, если общего события есть обработчики', function() {
                this.fsmModel.on('* > *', function() {});
                this.fsmModel.trigger('* > *', this.eventData);

                expect(ns.Model.prototype.trigger.withArgs('* > *', this.eventData)).to.have.callCount(1);
            });
        });

        describe('для событий, кроме события перехода ->', function() {
            it('должен вызвать родитеский метод запуска обработчиков события', function() {
                this.fsmModel.trigger('some-event');

                expect(ns.Model.prototype.trigger.withArgs('some-event')).to.have.callCount(1);
            });
        });
    });

    describe('#getEventData', function() {
        var eventsData = {
            '# start': {
                type: 'current',
                from: '',
                to: 'start'
            },
            'start > state1': {
                type: 'transition',
                from: 'start',
                to: 'state1'
            },
            'ns-model-init': {
                type: 'ns-model-init',
                from: '',
                to: ''
            }
        };

        [ '# start', 'start > state1', 'ns-model-init' ].forEach(function(eventName) {
            it('должен сформировать информацию о событии', function() {
                var eventData = this.fsmModel.getEventData(eventName);

                expect(eventData).to.eql(eventsData[eventName]);
            });
        });
    });

    describe('#inTransition', function() {
        it('должен вернуть true, если в данный момент происходит переход в новое состояние', function() {
            this.sinon.stub(this.fsmModel, '_inTransition').value(true);

            expect(this.fsmModel.inTransition()).to.be.equal(true);
        });

        it('должен вернуть false, если в данный момент не происходит переход в новое состояние', function() {
            this.sinon.stub(this.fsmModel, '_inTransition').value(false);

            expect(this.fsmModel.inTransition()).to.be.equal(false);
        });
    });

    describe('#abortTransition', function() {
        it('должен прервать переход в новое состояние, если он был начат', function() {
            var deferred = {
                reject: this.sinon.stub()
            };
            this.fsmModel._transitionDeferred = deferred;
            this.sinon.stub(this.fsmModel, '_inTransition').value(true);
            this.fsmModel.abortTransition();

            expect(deferred.reject).to.have.callCount(1);
        });

        it('должен отработать без ошибки, если переход в новое состояние сейчас не происходит', function() {
            var that = this;
            this.sinon.stub(this.fsmModel, '_inTransition').value(false);

            expect(function() {
                that.fsmModel.abortTransition();
            }).to.not.throw();
        });
    });

    describe('#switchOff', function() {
        it('должен выключить работу FSM модели', function() {
            this.sinon.stub(this.fsmModel, 'abortTransition');
            this.fsmModel.switchOff();

            expect(this.fsmModel.abortTransition).to.have.callCount(1);
            expect(this.fsmModel._isSwitchOn).to.be.equal(false);
        });
    });

    describe('#switchOn', function() {
        it('должен включить работу FSM модели', function() {
            this.fsmModel.switchOn();

            expect(this.fsmModel._isSwitchOn).to.be.equal(true);
        });
    });

    describe('#destroy', function() {
        it('должен включить работу FSM модели', function() {
            this.sinon.stub(this.fsmModel, 'switchOn');
            this.fsmModel.destroy();

            expect(this.fsmModel.switchOn).to.have.callCount(1);
        });

        it('должен прекратить осуществляемый переход', function() {
            this.sinon.stub(this.fsmModel, 'abortTransition');
            this.fsmModel.destroy();

            expect(this.fsmModel.abortTransition).to.have.callCount(1);
        });

        it('должен запустить destroy родитеского класса', function() {
            this.sinon.stub(ns.Model.prototype, 'destroy');
            this.fsmModel.destroy();

            expect(ns.Model.prototype.destroy).to.have.callCount(1);
        });
    });
});
