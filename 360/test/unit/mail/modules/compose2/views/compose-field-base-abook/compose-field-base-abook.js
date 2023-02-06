describe('Daria.vComposeFieldBaseAbook', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message');

        this.view = ns.View.create('compose-field-base-abook');
        this.view.FIELD_NAME = 'to';
        this.sinon.stub(this.view, 'getModel').withArgs('compose-message').returns(this.mComposeMessage);

        // Делаем метод Jane.Services.runModules "синхронным" чтобы не пришлось писать асинхронные тесты.
        this.sinon.stub(Jane.Services, 'runModules').callsFake(function(modules, callback) {
            callback();
        });

    });

    it('#isAbookPopupAvailable', function() {
        expect(this.view.isAbookPopupAvailable).to.be.equal(true);
    });

    describe('#_loadModulesOnce', function() {

        it('метод вызывает предзагрузку модулей Abook', function() {
            this.sinon.stub(Jane.Services, 'load');

            this.view._loadModulesOnce();
            expect(Jane.Services.load).to.have.callCount(1);

            // Повторный вызов не приводит к повторной подгрузке модулей.
            this.view._loadModulesOnce();
            expect(Jane.Services.load).to.have.callCount(1);

            // Новый инстанс поля не приводит к ещё одной подгрузке модулей.
            var view2 = ns.View.create('compose-field-base-abook');
            view2._loadModulesOnce();
            expect(Jane.Services.load).to.have.callCount(1);
        });

    });

    describe('#showAbookContactsPopup', function() {

        describe('открывает попап с имеющимся списком контактов', function() {

            beforeEach(function() {
                this.sinon.stub(ns.action, 'run');
            });

            it('загружает и выполняет модули Abook', function() {
                this.view.showAbookContactsPopup();

                expect(Jane.Services.runModules).to.have.callCount(1);
                expect(Jane.Services.runModules.getCall(0).args[0]).to.be.eql([ 'abook.css', 'abook.js', 'abook.yate' ]);
            });

            it('контактов нет - ничего не передаём в попап', function() {
                this.sinon.stub(this.mComposeMessage, 'getContacts').returns([]);
                this.view.showAbookContactsPopup('compose-field-caption');

                expect(ns.action.run).to.have.callCount(1);
                expect(ns.action.run.getCall(0).args[1].popupType).to.be.eql('select');
                expect(ns.action.run.getCall(0).args[1].emails).to.be.eql({ 'to': [] });
            });

            it('контакты есть - передаём их в попап', function() {
                this.sinon.stub(this.mComposeMessage, 'getContacts').returns([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' }
                ]);
                this.view.showAbookContactsPopup('compose-field-caption');

                expect(ns.action.run).to.have.callCount(1);
                expect(ns.action.run.getCall(0).args[1].popupType).to.be.eql('select');
                expect(ns.action.run.getCall(0).args[1].emails).to.be.eql({ 'to': [
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' }
                ] });
            });

        });

    });

    describe('#onAbookContactsSelected', function() {

        beforeEach(function() {
            this.sinon.stub(ns.action, 'run');
            this.sinon.spy(this.view, 'onAbookContactsSelected');
            this.sinon.stub(this.view, 'onAbookPopupClosed');
            this.sinon.stub(this.mComposeMessage, 'setContacts');
        });

        function sit(testTitle, onPopupClose, checker) {
            it(testTitle,  function(done) {
                this.sinon.stub(this.mComposeMessage, 'getContacts').returns([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' }
                ]);

                this.view.showAbookContactsPopup('compose-field-caption');

                var deferred = ns.action.run.getCall(0).args[1].resultDeferred;

                onPopupClose(deferred);

                setTimeout(function() {
                    checker.call(this);
                    done();
                }.bind(this), 0);
            });
        }

        sit('вызывается при закрытии попапа по кнопке Готово',
            function(deferred) {
                deferred.resolve([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' },
                    { email: 'test3@ya.ru', name: 'test 3' }
                ]);
            },
            function() {
                expect(this.view.onAbookContactsSelected).to.have.callCount(1);
            }
        );

        sit('в обработчик передаётся массив выбранных имейлов',
            function(deferred) {
                deferred.resolve([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' },
                    { email: 'test3@ya.ru', name: 'test 3' }
                ]);
            },
            function() {
                expect(this.view.onAbookContactsSelected).calledWith([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' },
                    { email: 'test3@ya.ru', name: 'test 3' }
                ]);
            }
        );

        sit('выбранные в попапе имейлы сохраняются в правильном поле модели compose-message',
            function(deferred) {
                deferred.resolve([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' },
                    { email: 'test3@ya.ru', name: 'test 3' }
                ]);
            },
            function() {
                expect(this.mComposeMessage.setContacts).to.have.callCount(1);
                expect(this.mComposeMessage.setContacts).calledWith(
                    this.view.FIELD_NAME,
                    [
                        { email: 'test1@ya.ru', name: 'test 1' },
                        { email: 'test2@ya.ru', name: 'test 2' },
                        { email: 'test3@ya.ru', name: 'test 3' }
                    ]
                );
            }
        );

        sit('не вызывается при закрытии попапа по кнопке Отмена',
            function(deferred) {
                deferred.reject();
            },
            function() {
                expect(this.mComposeMessage.setContacts).to.have.callCount(0);
            }
        );

    });

    describe('#onAbookPopupClosed', function() {

        beforeEach(function() {
            this.sinon.stub(ns.action, 'run');
            this.sinon.spy(this.view, 'onAbookContactsSelected');
            this.sinon.stub(this.view, 'onAbookPopupClosed');
            this.sinon.stub(this.mComposeMessage, 'setContacts');
        });

        function sit(testTitle, onPopupClose, checker) {
            it(testTitle,  function(done) {
                this.sinon.stub(this.mComposeMessage, 'getContacts').returns([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' }
                ]);

                this.view.showAbookContactsPopup('compose-field-caption');

                var deferred = ns.action.run.getCall(0).args[1].resultDeferred;

                onPopupClose(deferred);

                setTimeout(function() {
                    checker.call(this);
                    done();
                }.bind(this), 0);
            });
        }

        sit('вызывается при закрытии попапа по кнопке Готово',
            function(deferred) {
                deferred.resolve([
                    { email: 'test1@ya.ru', name: 'test 1' },
                    { email: 'test2@ya.ru', name: 'test 2' },
                    { email: 'test3@ya.ru', name: 'test 3' }
                ]);
            },
            function() {
                expect(this.view.onAbookPopupClosed).to.have.callCount(1);
            }
        );

        sit('не вызывается при закрытии попапа по кнопке Отмена',
            function(deferred) {
                deferred.reject();
            },
            function() {
                expect(this.view.onAbookPopupClosed).to.have.callCount(1);
            }
        );

    });

});
