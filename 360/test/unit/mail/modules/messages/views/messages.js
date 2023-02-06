describe('Daria.vMessages', function() {
    beforeEach(function() {
        this.vMessages = ns.View.create('messages', {
            current_folder: '255000007002385558600'
        });
        this.sinon.stub(Daria, 'React').value({
            saveShowSearchResultsTimestamp: this.sinon.stub()
        });
    });

    describe('#getOpenMessageState', function() {
        beforeEach(function() {
            /** @type Daria.vMessages */
            this.vMessages = ns.View.create('messages', {
                current_folder: '2550000070023855586'
            });
        });

        it('должен вернуть ids, если открыто письмо в списке писем', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '2550000070023855586',
                ids: '12'
            });

            expect(this.vMessages.getOpenMessageState()).to.be.eql('12');
        });

        it('должен вернуть thread_id, если открыт тред в списке писем', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '2550000070023855586',
                thread_id: 't12'
            });

            expect(this.vMessages.getOpenMessageState()).to.be.eql('t12');
        });

        it('должен вернуть пустую строку, если ничего не открыто', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '2550000070023855586'
            });

            expect(this.vMessages.getOpenMessageState()).to.be.eql('');
        });

        it('должен вернуть ids, если открыто письмо на странице треда', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: 't1',
                ids: '12'
            });

            expect(this.vMessages.getOpenMessageState()).to.be.eql('12');
        });

        it('должен вернуть пустую строку, если ничего не открыто на странице треда', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: 't1'
            });

            expect(this.vMessages.getOpenMessageState()).to.be.eql('');
        });
    });

    describe('#isUnreadList', function() {
        it('должен вернуть true, для списка непрочитаных в папке', function() {
            var vMessages = ns.View.create('messages', {
                current_folder: '2550000070023855586',
                extra_cond: 'only_new'
            });

            expect(vMessages.isUnreadList()).to.be.equal(true);
        });

        it('должен вернуть true, для списка всех непрочитаных', function() {
            var vMessages = ns.View.create('messages', {
                extra_cond: 'only_new',
                goto: 'all'
            });

            expect(vMessages.isUnreadList()).to.be.equal(true);
        });

        it('должен вернуть false, для списка писем в папке', function() {
            var vMessages = ns.View.create('messages', {
                current_folder: '2550000070023855586',
                threaded: 'yes'
            });

            expect(vMessages.isUnreadList()).to.be.equal(false);
        });
    });

    describe('#invalidateUnreadMessages', function() {
        beforeEach(function() {
            this.vMessages = ns.View.create('messages');

            this.sinon.stub(this.vMessages, 'isUnreadList');
            this.sinon.stub(this.vMessages, 'isVisible');

            this.sinon.stub(this.vMessages.getModel('messages'), 'invalidate');
        });

        it('должен инвалидировать, если это список непрочитанных и он не виден', function() {
            this.vMessages.isUnreadList.returns(true);
            this.vMessages.isVisible.returns(false);

            this.vMessages.invalidateUnreadMessages();
            expect(this.vMessages.getModel('messages').invalidate).to.have.callCount(1);
        });

        it('не должен инвалидировать, если это список непрочитанных и он виден', function() {
            this.vMessages.isUnreadList.returns(true);
            this.vMessages.isVisible.returns(true);

            this.vMessages.invalidateUnreadMessages();
            expect(this.vMessages.getModel('messages').invalidate).to.have.callCount(0);
        });

        it('не должен инвалидировать, если это не список непрочитанных', function() {
            this.vMessages.isUnreadList.returns(false);
            this.vMessages.isVisible.returns(false);

            this.vMessages.invalidateUnreadMessages();
            expect(this.vMessages.getModel('messages').invalidate).to.have.callCount(0);
        });
    });

    describe('#oninit', function() {
        beforeEach(function() {
            this.scenarioManager = this.sinon.stubScenarioManager(this.vMessages);
        });

        it('должен запустить сценарий "Поиск писем", если на странице поиска и нет активного сценария', function() {
            this.sinon.stub(this.vMessages, 'isSearchResults').returns(true);
            this.scenarioManager.hasActiveScenario.returns(false);

            this.vMessages.oninit();

            expect(this.scenarioManager.startScenario)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('search-scenario', 'direct-url');
        });

        it('не должен вызывать scenarioManager, если не на странице поиска', function() {
            this.sinon.stub(this.vMessages, 'isSearchResults').returns(false);

            this.vMessages.oninit();

            expect(this.scenarioManager.hasActiveScenario).to.be.not.calledWith('search-scenario');
        });
    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMessages, 'logTimings');
            this.vMessages.$node = $('<div>');

            this.is3ph = this.sinon.stub(Daria, 'is3ph');
            this.is3ph.returns(true);
        });

        describe('сценарий "Поиск писем"', function() {
            beforeEach(function() {
                this.scenarioManager = this.sinon.stubScenarioManager(this.vMessages);
                const scenario = this.scenarioManager.stubScenario;
                this.scenarioManager.hasActiveScenario.returns(true);
                this.scenarioManager.getActiveScenario.returns(scenario);
                scenario.getTimeFromStart.returns(24);
                this.sinon.stub(this.vMessages, 'isSearchResults').returns(true);
            });

            it('должен залогировать "successful-render" шаг сценария "Поиск"', function() {
                this.vMessages._onShow();

                expect(this.scenarioManager.stubScenario.logStep)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('successful-render', { duration: 24 });
            });

            it('должен запустить сценарий с триггером "direct-url", если нет сценария', function() {
                this.scenarioManager.hasActiveScenario.returns(false);

                this.vMessages._onShow();

                expect(this.scenarioManager.startScenario)
                    .to.have.callCount(1)
                    .and.to.be.calledWith('search-scenario', 'direct-url');
            });
        });
    });

    describe('#finishSearchScenarioOnLeave', function() {
        beforeEach(function() {
            this.scenarioManager = this.sinon.stubScenarioManager(this.vMessages);
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.returns(true);
            this.scenarioManager.getActiveScenario.returns(scenario);
            this.vMessages._initialScenario = this.scenarioManager.stubScenario;
        });

        it('не завершаем сценарий, если его нет', function() {
            this.scenarioManager.hasActiveScenario.returns(false);

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive).to.be.not.called;
        });

        it('не завершаем сценарий, если он не совпадает с изначальным', function() {
            this.scenarioManager.hasActiveScenario.returns(true);
            this.vMessages._initialScenario = {};

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive).to.be.not.called;
        });

        it('завершаем с финализатором "additional-search", если ушли в композ', function() {
            this.sinon.stub(Daria, 'isSearchPage').returns(true);

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('search-scenario', 'additional-search');
        });


        it('завершаем с финализатором "go-to-compose", если ушли в композ', function() {
            this.sinon.stub(Daria, 'isComposePage').returns(true);

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('search-scenario', 'go-to-compose');
        });

        it('завершаем с финализатором "open-message", если открыли письмо', function() {
            this.sinon.stub(Daria, 'isMessagePage').returns(true);

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('search-scenario', 'open-message');
        });

        it('завершаем с финализатором "go-to-another-messages-list", если открыли папку', function() {
            this.sinon.stub(Daria, 'isMessagesListPage').returns(true);

            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('search-scenario', 'go-to-another-messages-list');
        });

        it('завершаем с финализатором "go-to-another-page" в других случаях', function() {
            this.vMessages.finishSearchScenarioOnLeave();

            expect(this.scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('search-scenario', 'go-to-another-page');
        });
    });

    describe('onInsertMessages', function() {
        beforeEach(function() {
            var viewParams = { current_folder: '1' };

            this.mMessages = ns.Model.get('messages', viewParams).setData({ message: [ { mid: 1 }, { mid: 2 } ] });
            this.mSettings = ns.Model.get('settings').setData({});

            this.view = ns.View.create('messages', viewParams);
            this.view.$node = $('<div/>');
            this.sinon.stub(this.view, 'forceUpdate');
            this.sinon.stub(this.view, '_itemsListWillUpdate');

            this.view._show();
        });

        it('при вставке - должен сообщать во внешний мир о том, что список писем будет перерисован', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '1'
            });
            this.mMessages.insert(ns.Model.get('message', { ids: 123 }));

            expect(this.view._itemsListWillUpdate).to.have.callCount(1);
        });

        it('при вставке сообщения делаем forceUpdate', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '1'
            });
            this.view.onInsertMessages();
            expect(this.view.forceUpdate).to.have.callCount(1);
        });
    });

    describe('#onRemoveMessages', function() {
        beforeEach(function() {
            var viewParams = { current_folder: '1' };

            this.mMessages = ns.Model.get('messages', viewParams).setData({ message: [ { mid: 1 }, { mid: 2 } ] });

            this.loadMore = this.sinon.stub(this.mMessages, 'loadMore');
            this.canLoadMore = this.sinon.stub(this.mMessages, 'canLoadMore');
            this.canLoadMore.returns(true);

            this.view = ns.View.create('messages', viewParams);
            this.view.$node = $('<div/>');
            this.sinon.stub(this.view, 'forceUpdate');
            this.sinon.stub(this.view, '_itemsListWillUpdate');

            this.mSettings = ns.Model.get('settings').setData({});
            this.messagesPerPage = this.sinon.stub(this.mSettings, 'getSetting').withArgs('messages_per_page');
            this.messagesPerPage.returns('6');

            var stubModel = this.sinon.stub(this.view, 'getModel');
            stubModel.withArgs('messages').returns(this.mMessages);
            stubModel.withArgs('settings').returns(this.mSettings);

            this.view._show();
        });

        it('при удалении больше половины от messages_per_page, загружаем еще писем', function() {
            this.view.onRemoveMessages();
            this.view._onActionComplete('daria:MOPS:action-complete', {});

            expect(this.loadMore.called).to.be.equal(true);
        });

        it('при удалении меньше, чем половины от messages_per_page, не загружаем еще писем', function() {
            this.mMessages.models = [
                {}, {}, {}, {}
            ];
            this.view.onRemoveMessages();
            this.view._onActionComplete('daria:MOPS:action-complete', {});

            expect(this.loadMore.called).to.be.equal(false);
        });

        it('если невозможно загрузить больше писем, то loadMore не должен вызываться', function() {
            this.canLoadMore.returns(false);
            this.view.onRemoveMessages();
            this.view._onActionComplete('daria:MOPS:action-complete', {});

            expect(this.loadMore.called).to.be.equal(false);
        });

        it('при удалении - должен сообщать во внешний мир о том, что список писем будет перерисован', function() {
            this.mMessages.remove(this.mMessages.models[0]);

            expect(this.view._itemsListWillUpdate).to.have.callCount(1);
        });

        it('при удалении сообщения делаем forceUpdate', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '1'
            });
            this.view.onRemoveMessages();
            expect(this.view.forceUpdate).to.have.callCount(1);
        });
    });

    describe('#_moveDynamicNodes', function() {
        beforeEach(function() {
            var viewParams = { current_folder: '1' };

            this.mMessages = ns.Model.get('messages', viewParams).setData({ message: [ { mid: 1 }, { mid: 2 } ] });
            this.mSettings = ns.Model.get('settings').setData({});

            this.view = ns.View.create('messages', viewParams);
            this.view.$node = $('<div/>');
            this.sinon.stub(this.view, 'forceUpdate');
            this.sinon.stub(this.view, '_updateHeaders');
            this.sinon.stub(this.view, '_triggerItemsListUpdated');

            this.view._show();
        });

        it('сообщаем во внешний мир о том, что список был перерисован', function() {
            this.view._moveDynamicNodes();

            expect(this.view._triggerItemsListUpdated).to.have.callCount(1);
        });
    });

    describe('#_itemsListWillUpdate', function() {
        beforeEach(function() {
            var viewParams = { current_folder: '1' };

            this.mMessages = ns.Model.get('messages', viewParams).setData({ message: [ { mid: 1 }, { mid: 2 } ] });
            this.mSettings = ns.Model.get('settings').setData({});

            this.view = ns.View.create('messages', viewParams);
            this.view.$node = $('<div/>');
            this.view._show();
        });

        it('взводит флаг о том, что список писем был изменён', function() {
            this.view._itemsListChanged = false;
            this.view._itemsListWillUpdate();

            expect(this.view._itemsListChanged).to.be.equal(true);
        });

        it('триггерит глобальное событие о том, что список будет перерисован', function() {
            var spy = this.sinon.spy(ns.events, 'trigger');
            this.view._itemsListWillUpdate();

            expect(spy).to.have.callCount(1);
            expect(spy).to.be.calledWith('vMessages:items-list-will-update');
        });
    });

    describe('#_triggerItemsListUpdated', function() {
        beforeEach(function() {
            var viewParams = { current_folder: '1' };

            this.mMessages = ns.Model.get('messages', viewParams).setData({ message: [ { mid: 1 }, { mid: 2 } ] });
            this.mSettings = ns.Model.get('settings').setData({});

            this.view = ns.View.create('messages', viewParams);
            this.view.$node = $('<div/>');
            this.view._show();

            this.sinon.stub(Daria.MessagesLogger, 'log');
        });

        it('если флаг _itemsListChanged не взведён - ничего не происходит', function() {
            var spy = this.sinon.spy(ns.events, 'trigger');
            this.view._itemsListChanged = false;
            this.view._triggerItemsListUpdated();

            expect(spy).to.have.callCount(0);
            expect(Daria.MessagesLogger.log).to.have.callCount(0);
        });

        describe('если флаг _itemsListChanged взведён', function() {
            it('триггерит глобальное событие о том, что список был перерисован', function() {
                var spy = this.sinon.spy(ns.events, 'trigger');
                this.view._itemsListChanged = true;
                this.view._triggerItemsListUpdated();

                expect(spy).to.have.callCount(1);
                expect(spy).to.be.calledWith('vMessages:items-list-updated');
                expect(Daria.MessagesLogger.log).to.have.callCount(1);
                expect(Daria.MessagesLogger.log).to.be.calledWithExactly(this.mMessages.models);
            });

            it('сбрасывает флаг _itemsListChanged', function() {
                this.view._itemsListChanged = true;
                this.view._triggerItemsListUpdated();

                expect(this.view._itemsListChanged).to.be.equal(false);
            });
        });
    });

    describe('#scrollToMessage', function() {
        beforeEach(function() {
            this.vMessages = ns.View.create('messages', {
                current_folder: '2550000070023855586',
                threaded: 'yes'
            });

            this.sinon.stub(this.vMessages, 'scrollMessageIntoViewport');
            this.sinon.stub(this.vMessages, 'highlightClosedMessage');
        });

        describe('2pane ->', function() {
            beforeEach(function() {
                this.vMessages.$node = $('<div/>');

                this.sinon.stub(Daria, 'is2pane').returns(true);
                this.sinon.stub(Daria, 'is3pane').returns(false);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '2550000070023855586',
                    threaded: 'yes',
                    thread_id: 't1'
                });
            });

            it('должен подскроллить к открытому письму', function() {
                this.vMessages.scrollToMessage();

                expect(this.vMessages.scrollMessageIntoViewport).to.have.callCount(1);
            });

            it('не должен подскролить, если состояние не поменялось', function() {
                this.vMessages.scrollToMessage();
                this.vMessages.scrollMessageIntoViewport.reset();
                this.vMessages.scrollToMessage();

                expect(this.vMessages.scrollMessageIntoViewport).to.have.callCount(0);
            });

            describe('Закрытие письма ->', function() {
                beforeEach(function() {
                    this.vMessages.scrollToMessage();
                    this.vMessages.scrollMessageIntoViewport.reset();

                    this.sinon.stub(ns.page.current, 'params').value({
                        current_folder: '2550000070023855586',
                        threaded: 'yes'
                    });
                });

                it('должен подскролить к закрытому письму', function() {
                    this.vMessages.scrollToMessage();

                    expect(this.vMessages.scrollMessageIntoViewport).to.have.callCount(1);
                });

                it('должен подсветить его', function() {
                    this.vMessages.scrollToMessage();

                    expect(this.vMessages.highlightClosedMessage).to.have.callCount(1);
                });
            });
        });

        describe('3pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(false);
                this.sinon.stub(Daria, 'is3pane').returns(true);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '2550000070023855586',
                    threaded: 'yes',
                    thread_id: 't1'
                });
            });

            it('не должен никуда скроллить', function() {
                this.vMessages.scrollToMessage();

                expect(this.vMessages.scrollMessageIntoViewport).to.have.callCount(0);
            });

            it('не должен запоминать состояние', function() {
                this.vMessages.scrollToMessage();

                expect(this.vMessages._openMessageState).to.be.equal(null);
            });
        });
    });

    xdescribe('Операции над письмами', function() {
        beforeEach(function() {
            var MessagesData = mock.messages;
            var params = { current_folder: '1' };
            ns.Model.get('messages', params).setData(MessagesData[0].data);
            ns.Model.get('settings').setData({});
            setModelByMock(ns.Model.get('labels'));
            ns.Model.get('filters').setData({});
            ns.Model.get('messages-checked', params).setData({});

            this.vMessages = ns.View.create('messages', params);
            this.sinon.stub(this.vMessages, 'hasVerticalScroll');

            // блокируем ns.page.go
            this.sinon.stub(ns.page, 'go');

            return this.vMessages.update();
        });

        afterEach(function() {
            this.vMessages.destroy();
        });

        function deselectCalledTest(operation) {
            var deselectStub = this.sinon.stub(this.vMessages, 'deselect');

            ns.events.trigger('daria:vToolbarButton:' + operation);

            expect(deselectStub.calledOnce).to.be.ok;
        }

        function mopsCalledTest(operation) {
            this.sinon.stub(this.vMessages, 'deselect');
            var mopsStub = this.sinon.stub(Daria.MOPS, operation).returns(new Vow.Promise());

            ns.events.trigger('daria:vToolbarButton:' + operation);
            expect(mopsStub.calledOnce).to.be.ok;
        }

        it('Операция `Прочитано` снимает выделение с писем', function() {
            return deselectCalledTest.call(this, 'mark');
        });
        it('Операция `Непрочитано` снимает выделение с писем', function() {
            return deselectCalledTest.call(this, 'unmark');
        });

        it('Операция `Архивировать` запускает MOPS', function() {
            return mopsCalledTest.call(this, 'archive');
        });

        it('Операция `Переложить в папке` запускает MOPS', function() {
            return mopsCalledTest.call(this, 'infolder');
        });

        it('При проставление метки в списке писем в 3pane не запускается ns.page.no', function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(this.vMessages, '_onToolbarButton').returns();
            ns.events.trigger('daria:vToolbarButton:label', {});

            // все обновление работает через подписки на модели, поэтому page.go не нужен
            expect(ns.page.go).to.have.callCount(0);
        });
    });

    describe('#fillMessagesList', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMessages, '_canFillMessagesList').returns(true);
            this.sinon.stub(this.vMessages, 'isVisible').returns(true);
            this.sinon.stub(this.vMessages, '_onMessagesLoad');
        });

        it('Должен вызвать #_onMessagesLoad, если прошли все проверки', function() {
            this.vMessages.fillMessagesList();

            expect(this.vMessages._onMessagesLoad).to.have.callCount(1);
        });

        it('Не должен вызывать #_onMessagesLoad, если вьюха скрыта', function() {
            this.vMessages.isVisible.returns(false);

            this.vMessages.fillMessagesList();

            expect(this.vMessages._onMessagesLoad).to.have.callCount(0);
        });

        it('Не должен вызывать #_onMessagesLoad, если #_canFillMessagesList выдал false', function() {
            this.vMessages._canFillMessagesList.returns(false);

            this.vMessages.fillMessagesList();

            expect(this.vMessages._onMessagesLoad).to.have.callCount(0);
        });
    });

    describe('#_canFillMessagesList', function() {
        beforeEach(function() {
            this.mScrollerMessages = {
                getHeight: this.sinon.stub(),
                getOffsetHeight: this.sinon.stub(),
                getScrollHeight: this.sinon.stub()
            };

            this.$node = {
                rect: this.sinon.stub().returns({ bottom: 123 })
            };

            this.sinon.stub(this.vMessages, 'getModel').withArgs('scroller-messages').returns(this.mScrollerMessages);
            this.sinon.stub(this.vMessages, '$node').value(this.$node);
        });

        describe('2pane', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(true);
            });

            it('Если нижняя граница ноды вьюхи меньше высоты window, то должен выдать true', function() {
                this.mScrollerMessages.getHeight.returns(124);
                expect(this.vMessages._canFillMessagesList()).to.equal(true);
            });

            it('Если нижняя граница ноды вьюхи равна высоте window, то должен выдать true', function() {
                this.mScrollerMessages.getHeight.returns(123);
                expect(this.vMessages._canFillMessagesList()).to.equal(true);
            });

            it('Если нижняя граница ноды вьюхи больше высоты window, то должен выдать false', function() {
                this.mScrollerMessages.getHeight.returns(122);
                expect(this.vMessages._canFillMessagesList()).to.equal(false);
            });
        });

        describe('3pane', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(false);
            });

            it('Если высота контейнера, меньше высоты его контента, то должен выдать false', function() {
                this.mScrollerMessages.getScrollHeight.returns(2);
                this.mScrollerMessages.getOffsetHeight.returns(1);
                expect(this.vMessages._canFillMessagesList()).to.equal(false);
            });

            it('Если высота контейнера, равна высоте его контента, то должен выдать true', function() {
                this.mScrollerMessages.getScrollHeight.returns(2);
                this.mScrollerMessages.getOffsetHeight.returns(2);
                expect(this.vMessages._canFillMessagesList()).to.equal(true);
            });

            it('Если высота контейнера, больше высоты его контента, то должен выдать true', function() {
                this.mScrollerMessages.getScrollHeight.returns(2);
                this.mScrollerMessages.getOffsetHeight.returns(3);
                expect(this.vMessages._canFillMessagesList()).to.equal(true);
            });
        });
    });

    describe('#_onActionComplete', function() {
        beforeEach(function() {
            this.loadMoreMessagesIfNeed = this.sinon.stub(this.vMessages, 'loadMoreMessagesIfNeed');
            this._processRedirect = this.sinon.stub(this.vMessages, '_processRedirect');
        });

        it('должен вызвать loadMoreMessagesIfNeed и _processRedirect', function() {
            const data = { action: 'delete', ids: { ids: [ '1' ] } };
            this.vMessages._onActionComplete('daria:MOPS:action-complete', data);
            expect(this.loadMoreMessagesIfNeed).to.have.callCount(1);
            expect(this._processRedirect).to.have.callCount(1);
            expect(this._processRedirect).to.have.calledWithExactly(data);
        });
    });

    describe('#_processRedirect', function() {
        beforeEach(function() {
            ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                mid: '1',
                tid: 't1'
            });
            ns.Model.get('message', { ids: '2' }).setData({
                count: 1,
                mid: '2',
                tid: 't2'
            });
            this.sinon.stub(ns.page, 'go').resolves();
            this.sinon.stub(_, 'delay').callsFake((fn, delay, param) => fn(param));
        });

        describe('обычный режим (без группировки по тредам)', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    ids: '1'
                });
            });

            it('в обычном режиме должен осуществлять редирект, если действие касается текущего сообщения', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [ '1' ], tids: [] },
                    whereToGo: { where: '#1/messages/2' }
                });
                expect(ns.page.go).to.have.callCount(1);
            });

            it('в обычном режиме не должен осуществлять редирект, если действие не касается текущего сообщения', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [ '2' ], tids: [] },
                    whereToGo: { where: '#1/messages/3' }
                });
                expect(ns.page.go).to.have.callCount(0);
            });
        });

        describe('режим показа по тредам', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    thread_id: 't1'
                });
            });

            it('в тредном режиме должен осуществлять редирект, если действие касается текущего сообщения (передан mid)', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [ '1' ], tids: [] },
                    whereToGo: { where: '#1/thread/t2' }
                });
                expect(ns.page.go).to.have.callCount(1);
            });

            it('в тредном режиме не должен осуществлять редирект, если действие не касается текущего сообщения (передан mid)', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [ '2' ], tids: [] },
                    whereToGo: { where: '#1/thread/t3' }
                });
                expect(ns.page.go).to.have.callCount(0);
            });

            it('в тредном режиме должен осуществлять редирект, если действие касается текущего треда (передан tid)', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [], tids: [ 't1' ] },
                    whereToGo: { where: '#1/thread/t2' }
                });
                expect(ns.page.go).to.have.callCount(1);
            });

            it('в тредном режиме не должен осуществлять редирект, если действие не касается текущего треда (передан tid)', function() {
                this.vMessages._processRedirect({
                    action: 'delete',
                    ids: { ids: [], tids: [ 't2' ] },
                    whereToGo: { where: '#1/thread/t3' }
                });
                expect(ns.page.go).to.have.callCount(0);
            });
        });
    });

    describe('#_isActionEligibleForRedirect', function() {

        it('должен возвращать true для archive', function() {
            expect(this.vMessages._isActionEligibleForRedirect('archive')).to.be.equal(true);
        });

        it('должен возвращать true для move', function() {
            expect(this.vMessages._isActionEligibleForRedirect('move')).to.be.equal(true);
        });

        it('должен возвращать true для notspam', function() {
            expect(this.vMessages._isActionEligibleForRedirect('notspam')).to.be.equal(true);
        });

        it('должен возвращать true для remove', function() {
            expect(this.vMessages._isActionEligibleForRedirect('remove')).to.be.equal(true);
        });

        it('должен возвращать true для tospam', function() {
            expect(this.vMessages._isActionEligibleForRedirect('tospam')).to.be.equal(true);
        });

        it('должен возвращать true для infolder', function() {
            expect(this.vMessages._isActionEligibleForRedirect('infolder')).to.be.equal(true);
        });

        it('должен возвращать true для delete', function() {
            expect(this.vMessages._isActionEligibleForRedirect('delete')).to.be.equal(true);
        });

        it('должен возвращать false для mark', function() {
            expect(this.vMessages._isActionEligibleForRedirect('mark')).to.be.equal(false);
        });

        it('должен возвращать false для unmark', function() {
            expect(this.vMessages._isActionEligibleForRedirect('unmark')).to.be.equal(false);
        });
    });

    describe('#_isMessagesListAffectedByAction', function() {
        beforeEach(function() {
            ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                mid: '1',
                tid: 't1'
            });
            ns.Model.get('message', { ids: '2' }).setData({
                count: 1,
                mid: '2',
                tid: 't2'
            });
        });

        describe('обычный режим (без группировки по тредам)', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    ids: '1'
                });
            });

            it('в обычном режиме для корректного mid должен возращать true', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [ '1' ], tids: [] }
                })).to.be.equal(true);
            });

            it('в обычном режиме для некорректного mid должен возращать false', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [ '2' ], tids: [] }
                })).to.be.equal(false);
            });
        });

        describe('режим показа по тредам', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    thread_id: 't1'
                });
            });

            it('в тредном режиме для корректного mid должен возращать true', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [ '1' ], tids: [] }
                })).to.be.equal(true);
            });

            it('в тредном режиме для некорректного mid должен возращать false', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [ '2' ], tids: [] }
                })).to.be.equal(false);
            });

            it('в тредном режиме для корректного tid должен возращать true', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [], tids: [ 't1' ] }
                })).to.be.equal(true);
            });

            it('в тредном режиме для некорректного tid должен возращать false', function() {
                expect(this.vMessages._isMessagesListAffectedByAction({
                    action: 'delete',
                    ids: { ids: [], tids: [ 't2' ] }
                })).to.be.equal(false);
            });
        });
    });

    describe('#_findAffectedIds', function() {
        beforeEach(function() {
            ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                mid: '1',
                tid: 't1'
            });
            ns.Model.get('message', { ids: '2' }).setData({
                count: 1,
                mid: '2',
                tid: 't2'
            });
        });

        it('должен возвращать пустой массив, если ids не передан', function() {
            expect(this.vMessages._findAffectedIds()).to.be.eql([]);
        });

        it('должен возвращать пустой массив, если ids пустой объект', function() {
            expect(this.vMessages._findAffectedIds({})).to.be.eql([]);
        });

        it('должен возвращать пустой массив, если ids пустой массив', function() {
            expect(this.vMessages._findAffectedIds({})).to.be.eql([]);
        });

        it('должен находить tids, если переданы только mids { ids: [\'1\'], tids: [] } корректно', function() {
            const ids = { ids: [ '1' ], tids: [] };
            expect(this.vMessages._findAffectedIds(ids)).to.be.eql([ '1', 't1' ]);
        });

        it('должен возвращать только tids из объекта { ids: [], tids: [ \'t1\' ] }', function() {
            const ids = { ids: [], tids: [ 't1' ] };
            expect(this.vMessages._findAffectedIds(ids)).to.be.eql([ 't1' ]);
        });

        it('должен находить tid для mid из объекта { ids: [ \'1\' ], tids: [ \'t2\' ] }, а второй tid возвращать как есть', function() {
            const ids = { ids: [ '1' ], tids: [ 't2' ] };
            expect(this.vMessages._findAffectedIds(ids)).to.be.eql([ '1', 't1', 't2' ]);
        });
    });

    describe('#_expandMessage', function() {
        beforeEach(function() {
            ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                mid: '1',
                tid: 't1'
            });
            ns.Model.get('message', { ids: '2' }).setData({
                count: 1,
                mid: '2',
                tid: 't2'
            });
            this.messageState = ns.Model.get('state-message-thread-item', { ids: '1' }).setData({
                mid: '1',
                tid: 't1'
            });
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '1',
                thread_id: 't1'
            });
            const messageWrap = ns.View.create('messages-item-wrap', { ids: '1' });
            ns.Model.get('state-message-thread-list', { thread_id: 't1' }).setData({
                hiddenMessages: [ messageWrap ]
            });
            this.sinon.stubGetModel(messageWrap, [ 'state-message-thread-item', this.messageState ]);

            this.sinon.stub(this.messageState, 'setOpen');
            this.sinon.stub(ns.events, 'trigger');
        });
        it('должен раскрывать сообщение, если оно скрыто', function() {
            this.vMessages._expandMessage('1');
            expect(this.messageState.setOpen).to.have.callCount(1);
            expect(ns.events.trigger).to.have.callCount(1).and.calledWith('daria:vMessageThreadList:showMore');
        });

        it('не должен раскрывать сообщение, если оно не скрыто', function() {
            this.vMessages._expandMessage('2');
            expect(this.messageState.setOpen).to.have.callCount(0);
            expect(ns.events.trigger).to.have.callCount(0);
        });
    });
});
