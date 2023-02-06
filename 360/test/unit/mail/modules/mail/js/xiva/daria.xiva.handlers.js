describe('Daria.Xiva.handlers', function() {
    // TODO вынести тесты про processNewLcn - в .processNewLcn
    describe('.commonChecker', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria, 'invalidateMailHandlers');
        });

        it('должен вернуть false, если lcn нормальный и session_key совпадает', function() {
            this.sinon.stub(Daria, 'lcn').value(9300);
            this.sinon.stub(Daria.Config, 'connection_id').value('feced109151c54b7ca4eac99bed7dff2');

            var msg = {
                bright: true,
                lcn: '9301',
                data: {
                    lcn: '9301',
                    method_id: '',
                    operation: 'reset fresh',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uname: '634276775'
                },
                operation: 'unsupported',
                server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                service: 'mail',
                connection_id: '1',
                session_key: 'feced109151c54b7ca4eac99bed7dff2',
                uid: '204458703',
                version: '1'
            };

            var res = Daria.Xiva.handlers.commonChecker(msg);

            expect(res).to.be.equal(false);
        });

        it('должен вернуть true, если lcn нормальный и session_key совпадает, но operation==insert', function() {
            this.sinon.stub(Daria, 'lcn').value(9300);
            this.sinon.stub(Daria.Config, 'connection_id').value('feced109151c54b7ca4eac99bed7dff2');

            var msg = {
                bright: true,
                lcn: '9301',
                data: {
                    lcn: '9301',
                    method_id: '',
                    operation: 'insert',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uname: '634276775'
                },
                operation: 'insert',
                server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                service: 'mail',
                connection_id: '1',
                session_key: 'feced109151c54b7ca4eac99bed7dff2',
                uid: '204458703',
                version: '1'
            };

            var res = Daria.Xiva.handlers.commonChecker(msg);

            expect(res).to.be.equal(true);
        });

        it('должен вернуть true, если lcn нормальный и session_key не совпадает', function() {
            this.sinon.stub(Daria, 'lcn').value(9300);
            this.sinon.stub(Daria.Config, 'connection_id').value('q43wtesdfa');

            var msg = {
                bright: true,
                lcn: '9301',
                data: {
                    lcn: '9301',
                    method_id: '',
                    operation: 'reset fresh',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uname: '634276775'
                },
                operation: 'unsupported',
                server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                service: 'mail',
                connection_id: 'feced109151c54b7ca4eac99bed7dff2',
                session_key: 'feced109151c54b7ca4eac99bed7dff2',
                uid: '204458703',
                version: '1'
            };

            var res = Daria.Xiva.handlers.commonChecker(msg);

            expect(res).to.be.equal(true);
        });

        it('должен вернуть true, если lcn отличается на 10 и session_key не совпадает', function() {
            this.sinon.stub(Daria, 'lcn').value(9300);
            this.sinon.stub(Daria.Config, 'connection_id').value('q43wtesdfa');

            var msg = {
                bright: true,
                lcn: '9310',
                data: {
                    lcn: '9310',
                    method_id: '',
                    operation: 'reset fresh',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uname: '634276775'
                },
                operation: 'unsupported',
                server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                service: 'mail',
                connection_id: 'feced109151c54b7ca4eac99bed7dff2',
                session_key: 'feced109151c54b7ca4eac99bed7dff2',
                uid: '204458703',
                version: '1'
            };

            var res = Daria.Xiva.handlers.commonChecker(msg);

            expect(res).to.be.equal(true);
        });

        describe('lcn разъехался больше чем на 10 ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'lcn').value(9300);
                this.sinon.stub(Daria.Config, 'connection_id').value('q43wtesdfa');

                var msg = {
                    bright: true,
                    lcn: '9311',
                    data: {
                        lcn: '9311',
                        method_id: '',
                        operation: 'reset fresh',
                        session_key: 'feced109151c54b7ca4eac99bed7dff2',
                        uname: '634276775'
                    },
                    operation: 'unsupported',
                    server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                    service: 'mail',
                    connection_id: 'feced109151c54b7ca4eac99bed7dff2',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uid: '204458703',
                    version: '1'
                };

                this.res = Daria.Xiva.handlers.commonChecker(msg);
            });

            it('должен вернуть true', function() {
                expect(this.res).to.be.equal(false);
            });

            it('должен отправить лог', function() {
                expect(Jane.ErrorLog.send).to.be.calledWith({
                    event: 'xiva.invalid_lcn',
                    diff: 11,
                    newLcn: 9311,
                    oldLcn: 9300
                });
            });

            it('должен сохранить новый lcn', function() {
                expect(Daria.lcn).to.be.equal(9311);
            });

            it('должен инвалидировать кеши', function() {
                expect(Daria.invalidateMailHandlers).to.be.calledWith(false, 'xiva.invalid_lcn');
            });
        });
    });

    describe('.processNewLcn', function() {
        // TODO часть тесткейсов покрыта в тестах '.commonChecker'
        // Надо бы их перелопатить, часть унести сюда.

        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria, 'invalidateMailHandlers');
        });

        it('Не вызываем инвалидацию хендлеров, если расхождение счетчиков меньше 10', function() {
            var testDetails = { oldLcn: 9999, newLcn: 10000 };
            Daria.Xiva.wentOfflineTime = null;

            Daria.Xiva.handlers.processNewLcn(testDetails.newLcn);
            expect(Daria.invalidateMailHandlers).to.have.callCount(0);

            delete Daria.Xiva.wentOfflineTime;
        });

        it('Вызываем инвалидацию хендлеров, если расхождение счетчиков больше 10', function() {
            var testDetails = { oldLcn: 9989, newLcn: 10000 };
            Daria.Xiva.wentOfflineTime = null;

            Daria.Xiva.handlers.processNewLcn(testDetails.newLcn);
            expect(Daria.invalidateMailHandlers).to.have.callCount(0);

            delete Daria.Xiva.wentOfflineTime;
        });

        it('Отправляем в лог событие если не оффлайн и diff lcn больше 10', function() {
            var testDetails = { oldLcn: 9989, newLcn: 10000, offlineTime: null };
            this.sinon.stub(Daria, 'lcn').value(testDetails.oldLcn);

            Daria.Xiva.wentOfflineTime = testDetails.offlineTime;

            Daria.Xiva.handlers.processNewLcn(testDetails.newLcn);
            expect(Daria.invalidateMailHandlers).to.have.callCount(1);
            expect(Jane.ErrorLog.send)
                .to.have.callCount(1)
                .and.calledWith({
                    diff: 11,
                    event: "xiva.invalid_lcn",
                    newLcn: 10000,
                    oldLcn: 9989
            });

            delete Daria.Xiva.wentOfflineTime;
        });

        it('Отправляем в лог событие если не оффлайн и diff lcn больше 10', function() {
            var testDetails = { oldLcn: 9989, newLcn: 10000, offlineTime: 1324324314 };
            this.sinon.stub(Daria, 'lcn').value(testDetails.oldLcn);

            Daria.Xiva.wentOfflineTime = testDetails.offlineTime;

            Daria.Xiva.handlers.processNewLcn(testDetails.newLcn);
            expect(Daria.invalidateMailHandlers).to.have.callCount(1);
            expect(Jane.ErrorLog.send)
                .to.have.callCount(1)
                .and.calledWith({
                    diff: 11,
                    event: "xiva.lcn_changed_after_offline",
                    newLcn: 10000,
                    oldLcn: 9989
            });

            delete Daria.Xiva.wentOfflineTime;
        });

        describe('обработка ухода пользователя в offline', function() {
            afterEach(function() {
                Daria.Xiva.wentOfflineTime = null;
            });

            var cases = {
                'lcn тот же самый, в offline не уходили => true': {
                    offlineTime: null, oldLcn: 100, newLcn: 100,
                    result: { returns: true }
                },
                'lcn отличается слабо, в offline не уходили => true': {
                    offlineTime: null, oldLcn: 100, newLcn: 101,
                    result: { returns: true }
                },
                'lcn отличается сильно, в offline не уходили => false + invalidateMailHandlers': {
                    offlineTime: null, oldLcn: 100, newLcn: 111,
                    result: { returns: false, invalidateModels: 'xiva.invalid_lcn' }
                },

                'lcn тот же самый, в offline уходили => true': {
                    offlineTime: 1497605505888, oldLcn: 100, newLcn: 100,
                    result: { returns: true }
                },
                'lcn отличается слабо, в offline уходили => false + invalidateMailHandlers': {
                    offlineTime: 1497605505888, oldLcn: 100, newLcn: 101,
                    result: { returns: false, invalidateModels: 'xiva.lcn_changed_after_offline' }
                },
                'lcn отличается сильно, в offline уходили => false + invalidateMailHandlers': {
                    offlineTime: 1497605505888, oldLcn: 100, newLcn: 111,
                    result: { returns: false, invalidateModels: 'xiva.lcn_changed_after_offline' }
                }
            };

            for (var testTitle in cases) {
                sit(testTitle, cases[testTitle]);
            }

            function sit(testTitle, testDetails) {
                it(testTitle, function() {
                    this.sinon.stub(Daria, 'lcn').value(testDetails.oldLcn);
                    this.sinon.stub(Daria, 'ContactBubble');

                    Daria.Xiva.wentOfflineTime = testDetails.offlineTime;

                    var result = Daria.Xiva.handlers.processNewLcn(testDetails.newLcn);

                    expect(result).to.be.equal(testDetails.result.returns);

                    if (testDetails.result.invalidateModels) {
                        expect(Daria.invalidateMailHandlers)
                            .to.be.calledWith(false, testDetails.result.invalidateModels);
                    }

                    expect(Daria.Xiva.wentOfflineTime).to.be.equal(null);
                });
            }
        });
    });

    describe('.unsupported', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria.Xiva.handlers, 'commonChecker');
            this.sinon.stub(Daria.Xiva, 'processUnsupported').returns(new vow.Promise());
            this.sinon.stub(Daria, 'updateTitle');
        });

        describe('commonChecker вернул true ->', function() {
            beforeEach(function() {
                Daria.Xiva.handlers.commonChecker.returns(true);
                Daria.gotNewMail = 1;

                this.message = {
                    a: 1
                };

                Daria.Xiva.handlers.unsupported({
                    message: this.message
                }).catch(function() {});
            });

            it('должен вызвать обработку', function() {
                expect(Daria.Xiva.processUnsupported).to.be.calledWith(this.message);
            });

            it('должен вызвать обновление заголовка', function() {
                expect(Daria.updateTitle).to.have.callCount(1);
            });

            it('должен сбросить счетчик', function() {
                expect(Daria.gotNewMail).to.be.equal(0);
            });
        });

        describe('commonChecker вернул false ->', function() {
            beforeEach(function() {
                Daria.Xiva.handlers.commonChecker.returns(false);
                Daria.gotNewMail = 1;

                Daria.Xiva.handlers.unsupported({
                    message: {}
                }).catch(function() {});;
            });

            it('не должен вызвать обработку', function() {
                expect(Daria.Xiva.processUnsupported).to.have.callCount(0);
            });

            it('не должен вызвать обновление заголовка', function() {
                expect(Daria.updateTitle).to.have.callCount(0);
            });

            it('не должен сбросить счетчик', function() {
                expect(Daria.gotNewMail).to.be.equal(1);
            });
        });

        describe('логирование необработанных сообщений ->', function() {
            beforeEach(function() {
                Daria.Xiva.handlers.commonChecker.returns(true);
            });

            it('должен отправить лог, если собщение не обработалось', function() {
                var msg = {
                    bright: true,
                    lcn: '9301',
                    data: {
                        operation: 'reset fresh'
                    },
                    operation: 'unsupported',
                    server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                    service: 'mail',
                    connection_id: 'feced109151c54b7ca4eac99bed7dff2',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uid: '204458703',
                    version: '1'
                };

                Daria.Xiva.processUnsupported.callsFake(() => vow.reject());

                return Daria.Xiva.handlers.unsupported(msg)
                    .then(null, vow.resolve)
                    .then(function() {
                        expect(Jane.ErrorLog.send).to.be.calledWith({
                            event: 'xiva.mail.unsupported.unhandled',
                            data: JSON.stringify(msg)
                        });
                    });
            });

            it('не должен отправить лог, если собщение обработалось', function() {
                var msg = {
                    bright: true,
                    lcn: '9301',
                    data: {
                        operation: 'reset fresh'
                    },
                    operation: 'unsupported',
                    server_notify_id: 'hlcfqtOlXW21:hlcnu9hqwa61:daria:mail',
                    service: 'mail',
                    connection_id: 'feced109151c54b7ca4eac99bed7dff2',
                    session_key: 'feced109151c54b7ca4eac99bed7dff2',
                    uid: '204458703',
                    version: '1'
                };

                Daria.Xiva.processUnsupported.returns(vow.resolve());

                return Daria.Xiva.handlers.unsupported(msg).then(function() {
                    expect(Jane.ErrorLog.send).to.have.callCount(0);
                });
            });
        });

        describe('"raw_data" -> ', function() {
            beforeEach(function() {
                Daria.Xiva.handlers.commonChecker.returns(true);

                this.message = {
                    bright: true,
                    lcn: '1',
                    message: { lcn: '1', session_key: '' },
                    operation: 'unsupported',
                    raw_data: {
                        lcn: '1',
                        operation: 'transfer',
                        sessionKey: '',
                        uid: '1120000000000371',
                        uname: '1120000000000371'
                    },
                    server_notify_id: 'qjJIIM0JdKo1:qjJWPkBh18c1:u2709:mail',
                    service: 'mail',
                    session_key: '',
                    tags: [],
                    uid: '1120000000000371',
                    version: '1'
                };
            });

            it('должен отправить raw_data в обработку', function() {
                Daria.Xiva.handlers.unsupported(this.message).catch(function() {});

                expect(Daria.Xiva.processUnsupported)
                    .to.have.callCount(1)
                    .and.to.be.calledWith(this.message.raw_data);
            });

            describe('Выбор правильных данных из ксивы для обработки результата для табных пушей', function() {
                beforeEach(function() {
                    this.messagePush = Object.assign({}, this.message);
                    this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
                    this.mFolders = ns.Model.get('folders');
                    this.sinon.stub(ns.Model, 'get').returns(this.mFolders);
                    this.sinon.stub(this.mFolders, 'getFidBySymbol').returns('1');
                    this.messagePush.message.fid = '1';
                    Object.assign(this.messagePush, {
                        raw_data: {
                            envelopes: [{ fid: '1', tab: 'news', operation: 'move mails' }],
                            operation: 'move mails'
                        }
                    });
                });

                it('если у нас операция move mails и признаки наличия табов в перенесенных письмах, добавляем таб',
                    function() {
                    Daria.Xiva.handlers.unsupported(this.messagePush);
                    expect(this.messagePush.message.tabId).to.be.eql('news');
                });

                it('Если по какой-то причине нет инфы о табах в пуше, кидаем ошибку', function() {
                    Object.assign(this.messagePush, {
                        raw_data: {
                            envelopes: [{ fid: '1', operation: 'move mails' }],
                            operation: 'move mails'
                        }
                    });

                    Daria.Xiva.handlers.unsupported(this.messagePush);
                    expect(Jane.ErrorLog.send).to.have.callCount(1)
                        .and.calledWith({ errorType: 'no_tab_in_xiva_push' });
                    expect(this.messagePush.message.tabId).to.be.eql(undefined);
                });
            });
        });
    });

    describe('.transfer', function() {
        it('должен перекинуть на домашнюю страницу', function() {
            this.sinon.stub(Daria, 'goHomePage');
            Daria.Xiva.handlers.transfer();

            expect(Daria.goHomePage).to.have.callCount(1);
        });
    });
});
