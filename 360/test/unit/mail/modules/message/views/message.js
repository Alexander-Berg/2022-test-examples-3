describe('vMessage', function() {
    beforeEach(function() {
        this.vMessage = ns.View.create('message', { ids: '5' });
        this.vMessage._fixedToolbar = function() {};
        this.vMessage._fixedToolbar.cancel = function() {};

        this.scenarioManager = this.sinon.stubScenarioManager(this.vMessage);

        return this.vMessage.update();
    });

    afterEach(function() {
        this.sinon.restore();
        this.vMessage.destroy();
    });

    describe('Операции над письмом', function() {
        /**
         * Имитация клика по кнопке
         * Контракт общения vMessage с кнопкой таков, что кнопка при клике триггерит
         * событие вида daria:vToolbarButton:operation, а vMessage его слушает и запускает Daria.MOPS.
         * @param {String} operation
         */
        function mopsCalledOnClickTest(operation) {
            var promise = new Vow.Promise();
            var mopsStub = this.sinon.stub(Daria.MOPS, operation).returns(promise);

            ns.events.trigger('daria:vToolbarButton:' + operation);
            expect(mopsStub.calledOnce).to.be.ok;
        }
        /**
         * Имитация дропа темы письма по кнопке
         * Контракт общения vMessage с драгндропом, что драгндроп при дропе темы письма триггерит
         * событие на DOM ноде темы. Событие всплывает и ловится видом vMessage.
         * @param {String} operation
         */
        function mopsCalledOnDropTest(operation) {
            var promise = new Vow.Promise();
            var mopsStub = this.sinon.stub(Daria.MOPS, operation).returns(promise);

            this.vMessage.$node.trigger('dragndrop.drop', { operation: operation });
            expect(mopsStub.calledOnce).to.be.ok;
        }

        describe('#_moveHelper', function() {
            describe('Клик на пользовательскую кнопку запускает _moveHelper', function() {
                beforeEach(function() {
                    this.moveHelperStub = this.sinon.stub(this.vMessage, '_moveHelper');
                });

                var testObj = {
                    archive: 'Архивировать',
                    infolder: 'Переложить в папку',
                    remove: 'Удалить',
                    tospam: 'Спам',
                    notspam: 'Не спам'
                };
                _.forEach(testObj, function(value, key) {
                    it(value, function() {
                        ns.events.trigger('daria:vToolbarButton:' + key);

                        expect(this.moveHelperStub.calledOnce).to.be.ok;
                        expect(this.moveHelperStub.calledWith(key)).to.be.ok;
                    });
                });
            });

            it('Запускает MOPS', function() {
                this.sinon.stub(Daria.messages, 'whereToGoAfterMove').returns({ where: 'test url' });
                var mopsStub = this.sinon.stub(Daria.MOPS, 'archive').callsFake(() => vow.reject());

                this.vMessage._moveHelper('archive', { 1: 'aaa' });

                expect(mopsStub.calledOnce).to.be.ok;
                expect(mopsStub.calledWithExactly(this.vMessage.mMessagesChecked, { 1: 'aaa', where: 'test url' })).to.be.ok;
            });

            it('Определяет url для перехода после перемещения письма', function() {
                var obj = { 1: '2' };
                var returnObj = { where: 'test url' };
                var whereToGoParams = _.extend({}, {
                    ids: this.vMessage.mMessagesChecked.getIds()
                }, obj);
                var whereToGoStub = this.sinon.stub(Daria.messages, 'whereToGoAfterMove').returns(returnObj);
                this.sinon.stub(Daria.MOPS, 'archive').callsFake(() => vow.reject());

                this.vMessage._moveHelper('archive', obj);

                expect(whereToGoStub.calledOnce).to.be.ok;
                expect(whereToGoStub.calledWithExactly('archive', this.vMessage.mMessagesChecked, whereToGoParams)).to.be.ok;
            });

            it('Вызывает #_onMoveMopsFulfilled, когда MOPS promise зарезолвится', function() {
                var obj = { 1: '2' };
                var returnObj = { where: 'test url' };
                this.sinon.stub(Daria.messages, 'whereToGoAfterMove').returns(returnObj);
                this.sinon.stub(Daria.MOPS, 'archive').returns(Vow.fulfill());
                var onMoveStub = this.sinon.stub(this.vMessage, '_onMoveMopsFulfilled');

                return this.vMessage._moveHelper('archive', obj).then(function() {
                    expect(onMoveStub.calledOnce).to.be.ok;
                });
            });
        });

        describe('`Не прочитано`', function() {
            it('Клик по кнопке в тулбаре запускает mops', function() {
                return mopsCalledOnClickTest.call(this, 'unmark');
            });

            it('Дроп темы письма на кнопку запускает mops', function() {
                return mopsCalledOnDropTest.call(this, 'unmark');
            });

            it('Любое действие с кнопкой переносит в папку с письмом', function() {
                this.sinon.stub(Daria.MOPS, 'unmark').returns(Vow.fulfill());
                var fid = this.vMessage.getModel('message').getFolderId();
                var url = '#folder/' + fid;
                this.sinon.stub(ns.router, 'generateUrl').withArgs('messages', { current_folder: fid }).returns(url);

                var pageGoStub = this.sinon.stub(ns.page, 'go').returns(Vow.fulfill());

                return this.vMessage._onUnmarkToolbarButton().then(function() {
                    expect(pageGoStub.calledOnce).to.be.ok;
                    expect(pageGoStub.calledWith(url)).to.be.ok;
                });
            });
        });
    });

    describe('#onshow', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMessage, 'writeToUserJournal');
        });

        it('при открытии письма должен убираться фокус из поиска', function() {
            this.sinon.stub(ns.events, 'trigger');
            this.vMessage.onshow();

            expect(ns.events.trigger).to.be.calledWithExactly('search.blur-input');
        });

        it('при открытии письма должен логировать событие startReading', function() {
            this.vMessage.onshow();

            expect(this.vMessage.writeToUserJournal).to.be.calledWithExactly('startReading');
        });
    });

    describe('#onhide', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMessage, 'resetFixedToolbar');
        });

        it('при закрытии письма должен чистить модель mScrollerMessage и сбросить фиксированность тулбара', function() {
            var initScrollerData = { 'scroll-top': 100, 'scroll-height': 100 };

            this.vMessage.onshow();
            this.vMessage.getModel('scroller-message').setData(initScrollerData);

            expect(this.vMessage.getModel('scroller-message').getData()).to.be.eql(initScrollerData);

            this.vMessage.onhide();
            this.vMessage.onshow();

            expect(this.vMessage.getModel('scroller-message').getData()).to.be.eql({});
            expect(this.vMessage.resetFixedToolbar).to.have.callCount(1);
        });
    });

    describe('#onCloseMessage', function() {
        it('по закрытию письма по крестику в отдельной вкладке происходит переход во входящие', function() {
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(Daria, 'isMessagePage').returns(true);
            this.vMessage.onCloseMessage();
            expect(ns.page.go).to.have.callCount(1);
        });
    });

    describe('метрика качества "Просмотр письма"', function() {
        it('если нет активного сценария чтения - запускает direct-url', function() {
            expect(this.scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'direct-url');
        });

        it('если есть активный сценарий - не запускает новый', function() {
            this.scenarioManager.hasActiveScenario.returns(true);
            this.scenarioManager.getActiveScenario.returns(this.scenarioManager.stubScenario);
            this.scenarioManager.startScenario.resetHistory();

            this.vMessage.onshow();

            expect(this.scenarioManager.startScenario).to.have.callCount(0);
        });

        it('если сценарий поменялся - то не закрываем', function() {
            this.vMessage._initialScenario = {};
            this.scenarioManager.hasActiveScenario.returns(true);
            this.scenarioManager.getActiveScenario.returns(this.scenarioManager.stubScenario);

            this.vMessage.onhide();

            expect(this.scenarioManager.finishScenarioIfActive).to.have.callCount(0);
        });

        it('если сценарий не сменился - то нужно закрыть с соответствующим типом', function() {
            this.vMessage._initialScenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.returns(true);
            this.scenarioManager.getActiveScenario.returns(this.scenarioManager.stubScenario);

            this.vMessage.onhide();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-go-to-another-page');
        });
    });
});
