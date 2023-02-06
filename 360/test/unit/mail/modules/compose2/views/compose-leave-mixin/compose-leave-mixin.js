describe('Daria.vComposeLeaveMixin', function() {
    beforeEach(function() {
        this.stubUndefinedMethod = function(obj, methodName) {
            ns.assert(!obj[methodName], 'test Daria.vComposeLeaveMixin', 'method %s should be undefined', methodName);
            obj[methodName] = this.sinon.stub();
            return obj[methodName];
        };

        this.sinon.stub(Function.prototype, 'bind').callsFake(function() {
            return this;
        });

        this.mComposeMessage = ns.Model.get('compose-message');
        this.sinon.stubMethods(this.mComposeMessage, [
            'isDirty',
            'save'
        ]);

        this.mComposeFsm = ns.Model.get('compose-fsm');
        this.sinon.stubMethods(this.mComposeFsm, [
            'setState',
            'inTransition'
        ]);

        this.mComposeState = ns.Model.get('compose-state');

        this.mSettings = ns.Model.get('settings');
        this.mFolders = ns.Model.get('folders');

        this.popupView = ns.View.create('compose-unsaved-popup');
        this.sinon.stub(this.popupView, 'open');

        this.view = ns.View.create('compose-leave-mixin');
        this.sinon.stub(this.view, 'getModel')
            .withArgs('compose-message').returns(this.mComposeMessage)
            .withArgs('settings').returns(this.mSettings)
            .withArgs('compose-fsm').returns(this.mComposeFsm)
            .withArgs('compose-state').returns(this.mComposeState)
            .withArgs('folders').returns(this.mFolders);

        this.fakeFocusModel = {
            setLastActionFocus: this.sinon.spy()
        };

        this.sinon.stub(ns.Model, 'get')
            .withArgs('compose-state', {}).returns(this.mComposeState)
            .withArgs('focus').returns(this.fakeFocusModel)
            .withArgs('folders').returns(this.mFolders)
            .withArgs('settings').returns(this.mSettings);

        this.stubUndefinedMethod(this.view, 'hardResetCompose');

        this.sinon.stub(ns.View, 'create').withArgs('compose-unsaved-popup').returns(this.popupView);

        this.view.leaveInit();

        ns.router.init();
    });

    describe('#leaveInit', function() {
        beforeEach(function() {
            this.sinon.spy(this.view, 'leaveInit');
        });

        describe('Проверки на необходимые предусловия →', function() {
            it('Должен вызвать исключение, если модели mComposeMessage нет в зависимостях', function() {
                this.view.getModel.withArgs('compose-message').returns(undefined);

                try {
                    this.view.leaveInit();
                } catch(e) {

                }

                expect(this.view.leaveInit).to.throw();
            });

            it('Должен вызвать исключение, если модели mSettings нет в зависимостях', function() {
                this.view.getModel.withArgs('settings').returns(undefined);

                try {
                    this.view.leaveInit();
                } catch(e) {

                }

                expect(this.view.leaveInit).to.throw();
            });

            it('Должен вызвать исключение, если модели mComposeFsm нет в зависимостях', function() {
                this.view.getModel.withArgs('compose-fsm').returns(undefined);

                try {
                    this.view.leaveInit();
                } catch(e) {

                }

                expect(this.view.leaveInit).to.throw();
            });

            it('Должен вызвать исключение, если модели mFolders нет в зависимостях', function() {
                this.view.getModel.withArgs('folders').returns(undefined);

                try {
                    this.view.leaveInit();
                } catch(e) {

                }

                expect(this.view.leaveInit).to.throw();
            });

            it('Не должен вызвать исключение, если все условия соблюдены', function() {
                this.view.clean = function() {};

                this.view.leaveInit();

                delete this.view.clean;
            });

            it('Должен создать leaveComposeController', function() {
                this.view.leaveInit();

                expect(this.view.leaveComposeController).to.be.not.equal(undefined);
            });
        });
    });

    describe('#leaveStop', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page.block, 'remove');
        });

        it('Должен вызвать удаление функции проверки перехода', function() {
            this.view.leaveStop();
            expect(ns.page.block.remove).to.be.calledWithExactly(this.view._leaveOnPageGo);
        });
    });

    describe('#leaveStart', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeFsm, 'on');
            this.sinon.stub(ns.page.block, 'add');
        });

        it('Подписывается на событие `# sent`', function() {
            this.view.leaveStart();

            expect(this.mComposeFsm.on).to.be.calledWith('# sent', this.view._leaveOnSent);
        });

        it('Подписывается на событие `# cancel`', function() {
            this.view.leaveStart();

            expect(this.mComposeFsm.on).to.be.calledWith('# cancel', this.view._leaveOnCancel);
        });

        it('Должен добавить метод обработки ухода со страницы в ns.page.block', function() {
            this.view.leaveStart();

            expect(ns.page.block.add).to.be.calledWith(this.view._leaveOnPageGo);
        });
    });

    describe('#_leaveOnPageGo', function() {
        beforeEach(function() {
            this.view.leaveInit();
            this.sinon.stub(this.view, '_showSaveChangesPopup').returns(true);
            this.sinon.stub(this.view.leaveComposeController, 'pageGoUrl').value(null);
            this.sinon.stub(this.view, 'checkSameUrlParams').returns(false);
            this.stubUndefinedMethod(this.view, 'isQuickReply');
        });

        it('Должен сохранить url для перехода', function() {
            this.view._leaveOnPageGo('#inbox');

            expect(this.view.leaveComposeController.pageGoUrl).to.be.equal('#inbox');
        });

        describe('mComposeFsm находится в `sending`, и mComposeMessage грязная ->', function() {
            beforeEach(function() {
                this.mComposeFsm.setInitialState('sending');
                this.mComposeMessage.isDirty.returns(true);
            });

            it('Должен запустить показ попапа о сохранении изменений', function() {
                this.view._leaveOnPageGo();

                expect(this.view._showSaveChangesPopup).to.have.callCount(1);
            });

            it('Должен заблокировать переход', function() {
                expect(this.view._leaveOnPageGo()).to.be.equal(false);
            });
        });

        describe('mComposeFsm находится в `sent`, и mComposeMessage грязная →', function() {
            beforeEach(function() {
                this.mComposeFsm.setInitialState('sent');
                this.mComposeMessage.isDirty.returns(true);
            });

            it('Не должен запустить показ попапа о сохранении изменений', function() {
                this.view._leaveOnPageGo();

                expect(this.view._showSaveChangesPopup).to.have.callCount(0);
            });

            it('Не должен заблокировать переход', function() {
                expect(this.view._leaveOnPageGo()).to.be.equal(true);
            });
        });

        describe('mComposeFsm не находится в `sent`, но mComposeMessage чистая →', function() {
            beforeEach(function() {
                this.mComposeFsm.setInitialState('cancel');
                this.mComposeMessage.isDirty.returns(false);
            });

            it('Не должен запустить показ попапа о сохранении изменений', function() {
                this.view._leaveOnPageGo();

                expect(this.view._showSaveChangesPopup).to.have.callCount(0);
            });

            it('Не должен заблокировать переход', function() {
                expect(this.view._leaveOnPageGo()).to.be.equal(true);
            });
        });

        describe('mComposeFsm не находится в `sent`, и mComposeMessage грязная', function() {
            beforeEach(function() {
                this.mComposeFsm.setInitialState('cancel');
                this.mComposeMessage.isDirty.returns(true);
            });

            it('Должен запустить показ попапа о сохранении изменений', function() {
                this.view._leaveOnPageGo();

                expect(this.view._showSaveChangesPopup).to.have.callCount(1);
            });

            it('Должен заблокировать переход', function() {
                expect(this.view._leaveOnPageGo()).to.be.equal(false);
            });
        });

        describe('Переход возможен и передан forced=true', function() {
            beforeEach(function() {
                this.mComposeMessage.isDirty.returns(false);
            });

            it('-> полная перерисовка композа', function() {
                this.view._leaveOnPageGo('#compose', true);

                expect(this.view.hardResetCompose).to.have.callCount(1);
            });

            it('-> не блокируется переход по другой урл (возвращается true)', function() {
                expect(this.view._leaveOnPageGo('#compose')).to.be.equal(true);
            });
        });

        describe('Переход не возможен', function() {
            beforeEach(function() {
                this.mComposeMessage.isDirty.returns(true);
            });

            it('-> отображается попап', function() {
                this.view._leaveOnPageGo('#compose', true);

                expect(this.view._showSaveChangesPopup).to.have.callCount(1);
            });

            it('-> forced=true прокидывается в метод показа попапа', function() {
                this.view._leaveOnPageGo('#compose', true);

                expect(this.view._showSaveChangesPopup).to.be.calledWith(true);
            });

            it('-> forced=false прокидывается в метод показа попапа', function() {
                this.view._leaveOnPageGo('#compose', false);

                expect(this.view._showSaveChangesPopup).to.be.calledWith(false);
            });

            it('-> блокируется переход по другой урл (возвращается false) если попап попазался', function() {
                this.view._showSaveChangesPopup.returns(true);
                expect(this.view._leaveOnPageGo('#compose')).to.be.equal(false);
            });

            it('-> не блокируется переход по другой урл (возвращается true) если попап не попазывался', function() {
                this.view._showSaveChangesPopup.returns(false);
                expect(this.view._leaveOnPageGo('#compose')).to.be.equal(true);
            });
        });
    });

    describe('#_showSaveChangesPopup', function() {
        beforeEach(function() {
            this.view.leaveInit();
            this.stubUndefinedMethod(this.view, 'autosaveStop');
            this.stubUndefinedMethod(this.view, 'autosaveStart');
            this.stubUndefinedMethod(this.view, 'isQuickReply').returns(false);

            this.sinon.stub(this.view.leaveComposeController, 'leaveCompose');

            this.view.leaveStart();
        });

        it('создаёт попап', function() {
            this.view._showSaveChangesPopup();
            expect(ns.View.create).to.be.calledWith('compose-unsaved-popup');
        });

        it('показывает попап', function() {
            this.view._showSaveChangesPopup();
            expect(this.popupView.open).to.have.callCount(1);
        });

        it('отключает автосохранение', function() {
            this.view._showSaveChangesPopup();
            expect(this.view.autosaveStop).to.have.callCount(1);
        });

        describe('возвращает, показался ли попап', function() {
            it('true в случае показа', function() {
                expect(this.view._showSaveChangesPopup()).to.be.equal(true);
            });

            it('false в случае, если попап был создан ранее и отображается сейчас', function() {
                var fakePopup2 = {};
                this.sinon.stub(this.mComposeState, 'get').withArgs('.nbSaveChangesPopup').returns(fakePopup2);
                expect(this.view._showSaveChangesPopup()).to.be.equal(false);
            });
        });

        describe('по клику на Сохранить (actionName = save)', function() {
            beforeEach(function() {
                this.mComposeMessage.save.returns(vow.reject());
            });

            it('сохраняет черновик', function() {
                this.view._showSaveChangesPopup();
                this.popupView.trigger('resolve', 'save');

                expect(this.mComposeMessage.save).to.have.callCount(1);
            });

            it('проставляет флаг сброса композа после сохранения', function() {
                this.sinon.stub(this.mComposeState, 'set');

                this.view._showSaveChangesPopup(true);
                this.popupView.trigger('resolve', 'save');

                expect(this.mComposeState.set).to.be.calledWith('.saveTemplateAndReset', true);
            });
        });

        describe('по клику на Не сохранять (actionName = cancel)', function() {
            it('не сохраняет черновик', function() {
                this.view._showSaveChangesPopup();
                this.popupView.trigger('resolve', 'cancel');

                expect(this.mComposeMessage.save).to.have.callCount(0);
            });

            it('если нужно сбрасывает композ', function() {
                this.view._showSaveChangesPopup(true);
                this.popupView.trigger('resolve', 'cancel');

                expect(this.view.hardResetCompose).to.have.callCount(1);
            });
        });

        describe('по клику на отмена', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page, 'refresh');
            });

            it('включает автосохранение', function() {
                this.view._showSaveChangesPopup();
                this.popupView.trigger('reject');

                expect(this.view.autosaveStart).to.have.callCount(1);
            });

            it('не сохраняет черновик', function() {
                this.view._showSaveChangesPopup();
                this.popupView.trigger('reject');

                expect(this.mComposeMessage.save).to.have.callCount(0);
            });
        });
    });

    describe('#_leaveOnSent', function() {
        beforeEach(function() {
            this.view.leaveInit();
            this.returnValue = {};
            this.sinon.stub(this.view.leaveComposeController, 'sendMetrika');
            this.sinon.stub(this.view, '_onAfterSent').returns(this.returnValue);
            this.result = this.view._leaveOnSent();
        });

        it('Должен отправить метрику', function() {
            expect(this.view.leaveComposeController.sendMetrika).to.have.callCount(1);
        });

        it('Должен вызвать постобработчик отправки', function() {
            expect(this.view._onAfterSent).to.have.callCount(1);
        });

        it('Должен вернуть результат вызова постобработчик отправки', function() {
            expect(this.result).to.be.equal(this.returnValue);
        });
    });

    describe('#_onAfterSent', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');

            this.sinon.stub(this.view.leaveComposeController, 'leaveCompose').returns(Vow.resolve());
            this.sinon.stub(this.view.leaveComposeController, 'whereToGoAfterSend').returns('#inbox');
            this.sinon.stub(this.view.leaveComposeController, 'showMessageSentNotification');

            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.sentMid').returns('123');
        });

        it('Должен вызвать показ нотифайки об отправке письма', function() {
            return this.view._onAfterSent().then(function() {
                expect(this.view.leaveComposeController.showMessageSentNotification)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly({
                        fromQR: false,
                        withUndo: false,
                        isDelayed: false,
                        sendTime: undefined,
                        sentMid: "123",
                        wasSendCancelled: false
                    });
            }, this);
        });

        it('Заполняет pageGoUrl', function() {
            return this.view._onAfterSent().then(function() {
                expect(this.view.leaveComposeController.pageGoUrl).to.be.equal('#inbox');
            }, this);
        });

        describe('Если переход не удался', function() {
            beforeEach(function() {
                this.view.leaveComposeController.leaveCompose.returns(Vow.reject({ error: 'test' }));
            });

            it('Должен залогировать и показать алерт с ошибкой', function() {
                this.sinon.stub(Daria.Dialog, 'notice');

                return this.view._onAfterSent().then(function() {
                    expect(Jane.ErrorLog.send).to.have.callCount(1);
                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        event: 'after_send_failed',
                        loadingHash: '#inbox',
                        error: 'test'
                    });
                    expect(Daria.Dialog.notice).to.have.callCount(1);
                }, this);
            });
        });

        describe('Переходим на #done', function() {
            beforeEach(function() {
                this.view.leaveComposeController.leaveCompose.returns(Vow.resolve());

                ns.Model.get.withArgs('done').returns({
                    fromComposeMessage: this.doneFromMessage = this.sinon.stub()
                });

                this.view.leaveComposeController.whereToGoAfterSend.returns('#done');
                this.sinon.stub(Jane.Services, 'run').returns(Promise.resolve());
            });

            it('Должен создать модель дана после загрузки модуля', function() {
                return this.view._leaveOnSent().then(function() {
                    expect(this.doneFromMessage).to.be.calledWith(this.mComposeMessage);
                }, this);
            });

            it('Должен запомнить страницу для перехода', function() {
                return this.view._leaveOnSent().then(function() {
                    expect(this.view.leaveComposeController.pageGoUrl).to.be.equal('#done');
                }, this);
            });

            it('Должен запустить уход из композа с очисткой блокирования', function() {
                return this.view._leaveOnSent().then(function() {
                    expect(this.view.leaveComposeController.leaveCompose)
                        .to.be.calledWith({ clearPageBlock: true, neverReturn: true });
                }, this);
            });
        });
    });

    xdescribe('#checkSameUrlParams', function() {
        it('Должен вернуть true, если переход на страницу композа с теми же параметрами, с которыми создан вид',
            function() {
                this.sinon.stub(this.view, 'params').value({ ids: '5', oper: 'reply' });
                var check = this.view.checkSameUrlParams('#compose?ids=5&oper=reply');
                expect(check).to.be.ok;
            }
        );

        it(
            'Должен вернуть true, если у новой страницы все параметры (даже если их меньше) совпадают с параметрами ' +
            'вида',
            function() {
                this.sinon.stub(this.view, 'params').value({ ids: '5', qrIds: '5', oper: 'reply' });
                var check = this.view.checkSameUrlParams('#compose?ids=5&oper=reply');
                expect(check).to.be.ok;
            }
        );
        it('Должен вернуть true, если переход не на страницу композа', function() {
            this.sinon.stub(this.view, 'params').value({ ids: '5', oper: 'reply' });
            var check = this.view.checkSameUrlParams('#message/5');
            expect(check).to.not.be.ok;
        });

        it('Должен вернуть true, если переход на страницу композа с черновиком, который редактировали ранее',
            function() {
                this.sinon.stub(this.mComposeMessage, 'getDraftMid').returns('123');
                this.sinon.stub(this.view, 'params').value({ ids: '5', oper: 'reply' });
                var check = this.view.checkSameUrlParams('#compose/123');
                expect(check).to.be.ok;
            }
        );
    });
});
