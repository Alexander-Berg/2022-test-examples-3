describe('Daria.mSystemStatus', function() {
    beforeEach(function() {
        this.model = ns.Model.get('system-status');
    });

    describe('#onInit', function() {
        it('Должен создать начальное состояние модели', function() {
            this.sinon.stub(ns.events, 'on');

            this.model.onInit();

            expect(this.model._shouldLoad).to.be.equal(true);
            expect(this.model._timer).to.be.equal(null);
        });
    });

    describe('#request', function() {
        beforeEach(function() {
            this.testData = {
                'db-status': 'test-status',
                'problem': ''
            };

            this.sinon.stub(ns.request, 'addRequestParams');
            this.http = this.sinon.stub(ns, 'http');
            this.sinon.stub(this.model, '_checkNeedPageUpdate');
        });

        it('Если не стоит флага _shouldLoad, то не должен делать запрос на сервер', function() {
            this.sinon.spy(Vow, 'resolve');

            this.model._shouldLoad = false;

            return this.model.request().then(function() {
                expect(Vow.resolve).to.have.callCount(1);
            });
        });

        it('Если this._shouldShow = true, то должен сделать запрос за данными', function() {
            this.sinon.stub(ns.request, 'canProcessResponse').returns(true);
            this.http.returns(Vow.resolve({ models: [ { data: this.testData } ] }));

            return this.model.request().then(function() {
                expect(this.model.getData()).to.be.eql(this.testData);
            }, this);
        });

        it('Должен вызвать проверку "нужно ли обновить вкладку с почтой"', function() {
            this.sinon.stub(ns.request, 'canProcessResponse').returns(true);
            this.http.returns(Vow.resolve({ models: [ { data: this.testData } ] }));

            return this.model.request().then(function() {
                expect(this.model._checkNeedPageUpdate).to.have.callCount(1);
            }, this);
        });
    });

    describe('#updateLoop', function() {
        it('Должен изменить Daria.Config["db-status"] и обновить таймер', function() {
            var dbStatus = 'testStatus';

            this.sinon.stub(this.model, 'getData').returns({ 'db-status': dbStatus });
            this.sinon.stub(this.model, '_restartTimer');

            this.model.updateLoop();

            expect(Daria.Config['db-status']).to.be.equal(dbStatus);
            expect(this.model._restartTimer).to.have.callCount(1);
        });
    });

    describe('#_restartTimer', function() {
        beforeEach(function() {
            this.timerId = '1';

            this.clearTimeout = this.sinon.stub(window, 'clearTimeout').callsFake(function(timer) {
                timer = null;
            });
            this.setTimeout = this.sinon.stub(window, 'setTimeout').returns(this.timerId);
        });

        it('Если до этого не было таймеров, то должен поставить новый таймер', function() {
            this.model._restartTimer();

            expect(this.setTimeout).to.have.callCount(1);
            expect(this.model._timer).to.be.equal(this.timerId);
        });

        it('Если есть таймер, то должен его сбросить и поставить новый таймер', function() {
            this.model._timer = 'test';

            this.model._restartTimer();

            expect(this.clearTimeout).to.have.callCount(1);
            expect(this.setTimeout).to.have.callCount(1);

            expect(this.model._timer).to.be.equal(this.timerId);
        });
    });

    describe('#_setDefaultData', function() {
        it('Должен поставить дефолтные данные в модель', function() {
            this.sinon.stub(this.model, 'updateLoop');

            this.model._setDefaultData();

            expect(this.model.getData()).to.be.eql({
                'db-status': 'rw',
                'problem': ''
            });
        });
    });

    describe('#_checkNeedPageUpdate', function() {
        const appStartTime = 10;
        const restratTimeBefore = 5;
        const restratTimeAfter = 15;

        beforeEach(function() {
            this.sinon.stub(Daria, 'timestamp').value(appStartTime);
            this.sinon.stub(Jane.Services, 'setUpdateNeeded');
        });

        function sit(title, options) {
            it(title, function() {
                this.sinon.stub(this.model, 'get')
                    .withArgs('.reload_timestamp').returns(options.reload_timestamp);

                this.model._checkNeedPageUpdate();

                expect(Jane.Services.setUpdateNeeded).to.have.callCount(options.updateNeeded ? 1 : 0);
            });
        }

        sit('должен вернуть true и выставить признак "нужно обновить почту" ' +
            '(вкладка почты была открыта до времени рестарта)', {
            reload_timestamp: restratTimeAfter,
            updateNeeded: true
        });

        sit('должен вернуть false (вкладка почты была открыта после времени рестарта)', {
            reload_timestamp: restratTimeBefore,
            updateNeeded: false
        });

        sit('должен вернуть false (время рестарта не вернулось)', {
            updateNeeded: false
        });
    });
});
