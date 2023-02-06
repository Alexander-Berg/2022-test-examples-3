describe('vMessagesItem', function() {
    beforeEach(function() {
        this.event = { stopPropagation: no.false };
        this.vMessagesItem = ns.View.create('messages-item', { ids: '1' });
        this.vMessagesItem._setNode(document.createElement('div'));
        this.sinon.stub(this.vMessagesItem, 'hasFocusedClass').returns(false);
        this.sinon.stub(this.vMessagesItem, 'logClickMessageFromSearch');

        // FIXME: Написать тесты для кейса с отключеной настройкой
        this.sinon.stub(ns.Model.get('settings'), 'isSet').withArgs('open-message-list').returns(true);
    });

    // eljusto@ FIXME: закомментировал тесты из-за миксинов messages-item
    // @see DARIA-57773
    xdescribe('Подписка на события ->', function() {
        beforeEach(function() {
            this.sinon.stub(ns.router, 'generateUrl').returns('');
            ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });

            this.sinon.stub(ns.View.prototype, 'forceUpdate');

            return this.vMessagesItem.update();
        });

        it('должен вызвать обновление при пометке replied', function() {
            ns.Model.get('message', { ids: '1' }).markAnswered();
            expect(ns.View.prototype.forceUpdate).to.have.callCount(1);
        });

        it('должен вызвать обновление при пометке forwarded', function() {
            ns.Model.get('message', { ids: '1' }).markForwarded();
            expect(ns.View.prototype.forceUpdate).to.have.callCount(1);
        });
    });

    xdescribe('#onToggleChecked', function() {
        beforeEach(function() {
            this.checkFunction = this.sinon.stub();
            this.event.currentTarget = $('<input type="checkbox">');
            this.sinon.stub(this.vMessagesItem, 'getModel').returns({
                check: this.checkFunction
            });
        });

        it('Должен передать в mMessagesChecked false, если чекбокс не был чекнут', function() {
            this.vMessagesItem.onToggleChecked(this.event);

            expect(this.checkFunction).to.be.calledWith(false);
        });

        it('Должен передать в mMessagesChecked true, если чекбокс был чекнут', function() {
            this.event.currentTarget.prop('checked', true);

            this.vMessagesItem.onToggleChecked(this.event);
            expect(this.checkFunction).to.be.calledWith(true);
        });
    });

    xdescribe('#onMessageCheckedToggle', function() {
        beforeEach(function() {
            this.vMessagesItem.getModel = this.sinon.stub();

            this.mMessage = this.vMessagesItem.getModel.withArgs('message');
            this.mMessagesChecked = this.vMessagesItem.getModel.withArgs('messages-checked').returns({
                isChecked: function(val) {
                    return val;
                }
            });

            this.sinon.stub(this.vMessagesItem, '_setChecked');
            this.sinon.stub(this.vMessagesItem, 'sendThreadNotification');
        });

        it('Должен выставить состяние view в checked если модель присутствоует в mMessagesChecked', function() {
            this.mMessage.returns(true);
            this.vMessagesItem.onMessageCheckedToggle();

            expect(this.vMessagesItem._setChecked).calledWith({ checked: true, force: true });
        });

        it('Должен выставить состяние view в unchecked если модель отсутствует в mMessagesChecked', function() {
            this.mMessage.returns(false);
            this.vMessagesItem.onMessageCheckedToggle();

            expect(this.vMessagesItem._setChecked).calledWith({ checked: false, force: true });
        });

        it('Должен отправить сообщение своему треду, если options. не переданы', function() {
            this.mMessage.returns(false);
            this.vMessagesItem.onMessageCheckedToggle();

            expect(this.vMessagesItem.sendThreadNotification).calledWith(false);
        });

        it('Должен отправить сообщение своему треду, если options.sendNotification == true', function() {
            this.mMessage.returns(false);
            this.vMessagesItem.onMessageCheckedToggle('', { sendNotification: true });

            expect(this.vMessagesItem.sendThreadNotification).calledWith(false);
        });

        it('Не должен отправлять сообщение своему треду, если options.sendNotification == false', function() {
            this.mMessage.returns(false);
            this.vMessagesItem.onMessageCheckedToggle('', { sendNotification: false });

            expect(this.vMessagesItem.sendThreadNotification).notCalled();
        });
    });

    describe('#openMessage', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(Daria.Page.generateUrl, 'contentMessage3pane').returns('#url');
        });

        describe('2pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(true);
                this.sinon.stub(Daria, 'is3pane').returns(false);
            });

            it('должен вызвать логгер поисковых кликов', function() {
                this.vMessagesItem.openMessage();

                expect(this.vMessagesItem.logClickMessageFromSearch).to.have.callCount(1);
            });

            it('логгер поисковых кликов должен быть вызван с параметром mail', function() {
                this.vMessagesItem.openMessage();

                expect(this.vMessagesItem.logClickMessageFromSearch).to.be.calledWith('mail');
            });

            it('должен закрыть список писем треда', function() {
                this.sinon.stub(this.vMessagesItem.getModel('messages-item-state'), 'close');
                this.vMessagesItem.openMessage();

                expect(this.vMessagesItem.getModel('messages-item-state').close).to.have.callCount(1);
            });

            it('должен перейти по урлу, который вернул Daria.Page.generateUrl', function() {
                this.vMessagesItem.openMessage();

                expect(ns.page.go)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('#url');
            });
        });

        describe('3pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(false);
                this.sinon.stub(Daria, 'is3pane').returns(true);
            });

            it('должен вызвать логгер поисковых кликов', function() {
                this.vMessagesItem.openMessage();

                expect(this.vMessagesItem.logClickMessageFromSearch).to.have.callCount(1);
            });

            it('логгер поисковых кликов должен быть вызван с параметром mail', function() {
                this.vMessagesItem.openMessage();

                expect(this.vMessagesItem.logClickMessageFromSearch).to.be.calledWith('mail');
            });

            it('не должен вызвать переход', function() {
                this.vMessagesItem.openMessage();

                expect(ns.page.go).to.have.callCount(0);
            });
        });
    });

    describe('#onClick', function() {
        describe('установка фокуса', function() {
            beforeEach(function() {
                this.sinon.stub(Modernizr, 'mac').value(false);
                this.sinon.stub(this.vMessagesItem, 'openMessage');

                this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);
                this.mFocus = ns.Model.get('focus');
                this.sinon.stub(this.mFocus, 'setFocusByMid');

                this.sinon.stub(Daria, 'CBS').callsFake(function() {
                    this.selectWithShift = function() {};
                });
            });

            sit('зажат shift - фокус не устанавливается', { shiftKey: true }, false);

            sit(
                'клик левой кнопкой с зажатым Ctrl/Meta - фокус не устанавливается',
                { which: 1, ctrlKey: true },
                false
            );

            sit('клик средней кнопкой (колёсиком) - фокус не устанавливается', { which: 2 }, false);

            sit(
                'клик по элементу с классом js-skip-click-message-item - фокус не устанавливается',
                { target: $('<div class="js-skip-click-message-item"/>') }
            );

            sit('обычный клик - выполняется установка фокуса по ids с подскролливанием', {}, true);

            it('в 2pane отменяется браузерный переход по ссылке (ссылка будет сформирована вручную)', function() {
                var e = $.Event('click', {});
                this.sinon.stub(e, 'preventDefault');
                this.sinon.stub(Daria, 'is2pane').returns(true);

                this.vMessagesItem.onClick(e);

                expect(e.preventDefault).to.have.callCount(1);
            });

            function sit(testTitle, eventOptions, shouldSetFocus) {
                it(testTitle, function() {
                    var e = $.Event('click', eventOptions);
                    this.vMessagesItem.onClick(e);
                    expect(this.mFocus.setFocusByMid).to.have.callCount(shouldSetFocus ? 1 : 0);
                });
            }
        });

        describe('метрика качества "Чтение письма"', function() {
            beforeEach(function() {
                this.sinon.stub(this.vMessagesItem, 'openMessage');
                this.sinon.stub(this.vMessagesItem, '_shouldOnlyToggleThread').returns(false);

                this.scenarioManager = this.sinon.stubScenarioManager(this.vMessagesItem);

                this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);

                this.mFocus = ns.Model.get('focus');
                this.sinon.stub(this.mFocus, 'setFocusByMid');

                this.event = $.Event('click');
            });

            it('клик по письму запускает сценарий чтения письма по клику', function() {
                this.vMessagesItem.onClick(this.event);

                expect(this.scenarioManager.startScenario)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('message-view-scenario', 'messages-list-message-click');
            });

            it('клик по треду с раскрытием треда без раскрытия письма не запускает сценарий чтения письма', function() {
                this.vMessagesItem._shouldOnlyToggleThread.returns(true);

                this.vMessagesItem.onClick(this.event);

                expect(this.scenarioManager.startScenario).to.have.callCount(0);
            });

            it('клик по треду в 3-пейн запускает сценарий чтения письма'
                + ' независимо от настройки открытия письма в списке писем', function() {
                this.sinon.stub(this.vMessagesItem, 'getModel')
                    .withArgs('message').returns({ isThread: () => true, getThreadCount: () => 2 });
                this.vMessagesItem._shouldOnlyToggleThread.returns(true);
                this.sinon.stub(Daria, 'is3pane').returns(true);

                this.vMessagesItem.onClick(this.event);

                expect(this.scenarioManager.startScenario)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('message-view-scenario', 'messages-list-thread-click');
            });

            it('клик по письму прекращает предыдущий активный сценарий', function() {
                this.vMessagesItem.onClick(this.event);

                expect(this.scenarioManager.finishScenarioIfActive)
                    .to.have.callCount(2)
                    .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');
            });

            it('шорткат по треду запускает сценарий чтения письма по шорткату', function() {
                this.sinon.stub(this.vMessagesItem, 'getModel')
                    .withArgs('message').returns({ isThread: () => true, getThreadCount: () => 2 });

                this.vMessagesItem.switchOpen();

                expect(this.scenarioManager.startScenario)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('message-view-scenario', 'messages-list-thread-hotkey');
            });
        });
    });

    describe('#toggleFakeFocusByParams', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);
            this.mFocus = ns.Model.get('focus');
            this.sinon.stub(this.mFocus, 'saveViewAsLastFocused');
            this.sinon.stub(this.mFocus, 'setFocusByMid');

            this.sinon.stub(Daria.Focus, 'scrollIntoView');

            this.sinon.stub(this.vMessagesItem, 'addFocus');
            this.sinon.stub(this.vMessagesItem, 'removeFocus');
        });

        describe('фокус стоял ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.vMessagesItem.$node, 'hasClass').withArgs('is-focused').returns(true);
                this.sinon.stub(this.mFocus, 'hasFocus').returns(true);
            });

            it('фокус перемещается на другое письмо - снимаем класс', function() {
                this.sinon.stub(this.vMessagesItem, 'isCurrentByParams').returns(false);
                this.vMessagesItem.toggleFakeFocusByParams();
                expect(this.vMessagesItem.addFocus).to.have.callCount(0);
                expect(this.vMessagesItem.removeFocus).to.have.callCount(1);
            });

            it('фокус должен быть на текущем письме в фокусе - ничего не меняется', function() {
                this.sinon.stub(this.vMessagesItem, 'isCurrentByParams').returns(true);
                this.vMessagesItem.toggleFakeFocusByParams();
                expect(this.vMessagesItem.addFocus).to.have.callCount(0);
                expect(this.vMessagesItem.removeFocus).to.have.callCount(0);
            });

            it('передали forceSet === true - устанавливаем фокус', function() {
                this.sinon.stub(this.vMessagesItem, 'isCurrentByParams');
                this.vMessagesItem.toggleFakeFocusByParams({}, true);

                expect(this.vMessagesItem.addFocus).to.have.callCount(1);
                expect(this.vMessagesItem.removeFocus).to.have.callCount(0);
            });
        });

        describe('фокус не стоял ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.vMessagesItem.$node, 'hasClass').withArgs('is-focused').returns(false);
            });

            it('фокус перемещается на другое письмо - ничего не меняется', function() {
                this.sinon.stub(this.mFocus, 'hasFocus').returns(true);
                this.sinon.stub(this.vMessagesItem, 'isCurrentByParams').returns(false);
                this.vMessagesItem.toggleFakeFocusByParams();
                expect(this.vMessagesItem.addFocus).to.have.callCount(0);
                expect(this.vMessagesItem.removeFocus).to.have.callCount(0);
            });

            describe('фокус должен быть на текущем письме ->', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mFocus, 'hasFocus').returns(true);
                    this.sinon.stub(this.vMessagesItem, 'isCurrentByParams').returns(true);
                    this.vMessagesItem.toggleFakeFocusByParams();
                });

                it('навешивается класс фокуса', function() {
                    expect(this.vMessagesItem.addFocus).to.have.callCount(1);
                });

                it('сохраняем данный вид как последний имевший фокус в колонке со списком писем', function() {
                    expect(this.mFocus.saveViewAsLastFocused)
                        .to.be.calledWith(this.vMessagesItem, 1);
                });

                it('подскролливаем к письму', function() {
                    expect(Daria.Focus.scrollIntoView)
                        .to.have.callCount(1)
                        .and.to.be.calledWith(this.vMessagesItem.$node, 1);
                });
            });

            describe('фокус должен быть на текущем письме + нет текущего активного фокуса ->', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mFocus, 'hasFocus').returns(false);
                    this.sinon.stub(this.vMessagesItem, 'isCurrentByParams').returns(true);

                    this.vMessagesItem.toggleFakeFocusByParams();
                });

                it('выставляется полноценный фокус', function() {
                    expect(this.mFocus.setFocusByMid).to.have.callCount(1);
                });
            });
        });
    });
});
