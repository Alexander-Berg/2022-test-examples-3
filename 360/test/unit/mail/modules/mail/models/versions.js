describe('Daria.mVersions', function() {
    beforeEach(function() {
        this.sinon.stub(Daria.Config, 'version').value(1);

        this.sinon.stub(ns.Model.info('versions').ctor.prototype, 'onInit');
        this.model = ns.Model.get('versions');
        this.model.onInit.restore();
    });

    describe('#onInit', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'version').value(1);

            this.sinon.stub(this.model, 'setData');
            this.sinon.stub(this.model, 'scheduleVersionUpdate');

            this.model.onInit();
        });

        it('должен выставить начальные данные модели', function() {
            expect(this.model.setData)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly({
                    u2709: {
                        version: 1
                    }
                });
        });

        it('должен запланировать перезапрос модели', function() {
            expect(this.model.scheduleVersionUpdate).to.have.callCount(1);
        });
    });

    describe('#request', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria.api, 'versions').value('/fake-url');
            this.sinon.stub(ns, 'http')
                .withArgs('/fake-url', { model: 'versions' }, { type: 'GET' });

            this.sinon.stub(this.model, 'setData');
        });

        it('должен запросить модель с помощью GET запроса', function() {
            ns.http.returns(new vow.Promise());

            this.model.request();

            expect(ns.http).to.have.callCount(1);
        });

        describe('Успешный запрос', function() {
            it('должен установить данные модели, если вернулся js объект', function() {
                ns.http.returns(vow.resolve({ data: true }));

                return this.model.request()
                    .then(() => {
                        expect(this.model.setData)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly({ data: true });
                    });
            });

            it('должен установить пустой объект в качестве данных модели, если вернулся не js объект', function() {
                ns.http.returns(vow.resolve('WHAT!'));

                return this.model.request()
                    .then(() => {
                        expect(this.model.setData)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly({});
                    });
            });
        });

        describe('Неуспешный запрос', function() {
            it('не должен устанавливать данные модели', function() {
                ns.http.returns(vow.reject());

                return this.model.request()
                    .then(() => {
                        expect(this.model.setData).to.have.callCount(0);
                    });
            });

            it('не должен логировать ошибку, если это http ошибка', function() {
                const error = { xhr: { status: 500 } };
                ns.http.returns(vow.reject(error));

                return this.model.request()
                    .then(() => {
                        expect(Jane.ErrorLog.send).to.have.callCount(0);
                    });
            });
        });
    });

    describe('#scheduleVersionUpdate', function() {
        beforeEach(function() {
            this.clock = this.sinon.useFakeTimers();
        });

        it('должен очистить выставленный ранее таймер на перезапрос модели', function() {
            const callback = this.sinon.stub();
            this.model._timer = setTimeout(callback, 10);

            this.model.scheduleVersionUpdate();

            this.clock.tick(10);

            expect(callback).to.have.callCount(0);
        });

        it('не должен ничего делать, если уже известно, что страницу нужно перезагружать', function() {
            this.model.scheduleVersionUpdate(true);

            expect(this.model._timer).to.be.equal(null);
        });

        it('должен выставить таймер на перезапрос модели', function() {
            this.sinon.stub(this.model, 'request');

            this.model.scheduleVersionUpdate();

            expect(this.model._timer).to.not.be.equal(null);
        });

        describe('Перезапрос модели', function() {
            // Хитрый способ дождаться выполнения всех обработчиков, навешанных на промис.
            const flushPromises = () => new Promise(resolve => setTimeout(resolve));

            function sit(title, requestSuccess, updateNeeded, checks) {
                it(title, function() {
                    this.sinon.stub(this.model, 'request').returns(requestSuccess ? vow.resolve() : vow.reject());
                    this.sinon.stub(this.model, '_checkNeedPageUpdate').returns(updateNeeded);

                    this.model.scheduleVersionUpdate();
                    this.sinon.stub(this.model, 'scheduleVersionUpdate');

                    this.clock.tick(this.model.TIMEOUT);

                    this.clock.restore();

                    return flushPromises().then(checks.bind(this));
                });
            }

            sit(
                'должен запросить модель',
                true,
                undefined,
                function() {
                    expect(this.model.request).to.have.callCount(1);
                }
            );

            sit(
                'должен запланировать перезапрос модели и указать, что нужно перезагружать страницу',
                true,
                true,
                function() {
                    expect(this.model.scheduleVersionUpdate)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly(true);
                }
            );

            sit(
                'должен запланировать перезапрос модели и указать, что страницу перезагружать не нужно ' +
                '(версия актуальна)',
                true,
                false,
                function() {
                    expect(this.model.scheduleVersionUpdate)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly(false);
                }
            );

            sit('должен запланировать перезапрос модели и указать, что страницу перезагружать не нужно ' +
                '(запрос зафейлился)',
                false,
                false,
                function() {
                    expect(this.model.scheduleVersionUpdate)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly(false);
                }
            );
        });
    });

    describe('#_checkNeedPageUpdate', function() {
        const currentVersion = 1;
        const nextVersion = 2;

        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'version').value(currentVersion);
            this.sinon.stub(Daria.Config, 'dev').value(false);

            this.sinon.stub(Jane.Services, 'setUpdateNeeded');
            this.sinon.stub(Jane.ErrorLog, 'send');

            this.sinon.stub(this.model, 'scheduleVersionUpdate');
            this.model.onInit();
        });

        function sit(title, options, extraChecks) {
            it(title, function() {
                this.sinon.stub(Daria.Config, 'dev').value(Boolean(options.isDev));
                this.sinon.stub(this.model, 'get')
                    .withArgs('.u2709.version').returns(options.version)
                    .withArgs('.u2709.rt').returns(options.rt);

                const result = this.model._checkNeedPageUpdate();

                expect(result).to.be.equal(options.result);
                expect(Jane.Services.setUpdateNeeded).to.have.callCount(options.result ? 1 : 0);

                if (typeof extraChecks === 'function') {
                    extraChecks.call(this);
                }
            });
        }

        sit('должен вернуть false', {
            isDev: true,
            result: false
        });

        sit('должен вернуть false и залогировать ошибку (в данных нет версии Лизы)', {
            version: undefined,
            result: false
        });

        sit('должен вернуть true и выставить признак "нужно обновить почту" (версия Лизы отличается)', {
            version: nextVersion,
            result: true
        });

        sit('должен вернуть false (время рестарта не вернулось)', {
            version: currentVersion,
            result: false
        });
    });
});
