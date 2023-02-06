describe('vMessagesItemWrap', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-wrap', { ids: '1' });
        this.view.$node = $('<div />');
        this.mMessage = ns.Model.get('message', { ids: '1' });

        this.sinon.stubGetModel(this.view, [ 'messages-item-state', this.mMessage, 'messages-checked' ]);
    });

    describe('#onToggleCheck', function() {
        it('Ставит is-checked, если письмо было чекнуто', function() {
            this.sinon.stub(this.mMessagesChecked, 'isChecked').returns(true);
            this.view.onToggleCheck();
            expect(this.view.$node.hasClass('is-checked')).to.eql(true);
        });

        it('Снимает is-checked, если письмо было расчекнуто', function() {
            this.sinon.stub(this.mMessagesChecked, 'isChecked').returns(false);
            this.view.onToggleCheck();
            expect(this.view.$node.hasClass('is-checked')).to.eql(false);
        });
    });

    describe('#onItemStateChanged', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'lazyUpdate');
            this.sinon.stub(this.view, 'uncheckMessagesInThread');

            this.sinon.stub(this.mMessagesItemState, 'isClosed');
        });

        describe('тред свернут ->', function() {
            beforeEach(function() {
                this.mMessagesItemState.isClosed.returns(true);
            });

            it('должен вызвать перерисовку', function() {
                this.view.onItemStateChanged();

                expect(this.view.lazyUpdate).to.have.callCount(1);
            });

            it('должен сбросить чекнутые письма внутри треда', function() {
                this.view.onItemStateChanged();

                expect(this.view.uncheckMessagesInThread).to.have.callCount(1);
            });
        });

        describe('тред развернут ->', function() {
            beforeEach(function() {
                this.mMessagesItemState.isClosed.returns(false);
            });

            it('должен вызвать перерисовку', function() {
                this.view.onItemStateChanged();

                expect(this.view.lazyUpdate).to.have.callCount(1);
            });

            it('не должен сбросить чекнутые письма внутри треда', function() {
                this.view.onItemStateChanged();

                expect(this.view.uncheckMessagesInThread).to.have.callCount(0);
            });
        });
    });

    describe('#onMessageLidChanged', function() {
        beforeEach(function() {
            this.isPinnedStub = this.sinon.stub(this.mMessage, 'isPinned');
            this.isReplyLaterStub = this.sinon.stub(this.mMessage, 'isReplyLater');
            this.pageParams = _.clone(ns.page.current.params);
        });

        afterEach(function() {
            ns.page.current.params = _.clone(this.pageParams);
            delete this.pageParams;
        });

        it('Должен поставить класс is-pinned, если выполняются условия', function() {
            this.isPinnedStub.returns(true);
            this.isReplyLaterStub.returns(false);
            ns.page.current.params.current_folder = '12345';
            this.view.onMessageLidChanged();

            expect(this.view.$node.hasClass('is-pinned')).to.be.equal(true);
        });

        it('Должен удалить класс is-pinned, если условия не выполняются', function() {
            this.isPinnedStub.returns(false);
            this.isReplyLaterStub.returns(false);
            this.view.onMessageLidChanged();

            expect(this.view.$node.hasClass('is-pinned')).to.be.equal(false);
        });

        it('Должен поставить класс is-reply-later, если выполняются условия', function() {
            this.isReplyLaterStub.returns(true);
            ns.page.current.params.current_folder = '12345';
            this.view.onMessageLidChanged();

            expect(this.view.$node.hasClass('is-reply-later')).to.be.equal(true);
        });

        it('Должен удалить класс is-pinned, если условия не выполняются', function() {
            this.isReplyLaterStub.returns(false);
            this.view.onMessageLidChanged();

            expect(this.view.$node.hasClass('is-reply-later')).to.be.equal(false);
        });
    });

    describe('#patchLayout', function() {
        describe('3pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(false);
                this.sinon.stub(Daria, 'is3pane').returns(true);

                this.mMessagesItemState = this.view.getModel('messages-item-state');
            });

            it('должен вернуть "layout-messages-item", есть письмо свернуто', function() {
                this.mMessagesItemState.setState(this.mMessagesItemState.STATE.CLOSE);

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item');
            });

            it('должен вернуть "layout-messages-item-thread-inner", есть письмо развернуто', function() {
                this.mMessagesItemState.setState(this.mMessagesItemState.STATE.THREAD_SHORT);

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item-thread-inner');
            });
        });

        describe('2pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(true);
                this.sinon.stub(Daria, 'is3pane').returns(false);

                this.mMessagesItemState = this.view.getModel('messages-item-state');

                this.sinon.stub(Daria.Widgets, 'getWidgetName').returns('');
                this.sinon.stub(Daria.Widgets, 'shouldShowAttachments').returns(false);
            });

            it('должен вернуть "layout-messages-item", есть нет открытого письма', function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    threaded: 'yes'
                });

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item');
            });

            it('должен вернуть "layout-message-opened", если открыто письмо', function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    ids: '1',
                    threaded: 'yes'
                });

                expect(this.view.patchLayout()).to.be.equal('layout-message-opened');
            });

            it('должен вернуть "layout-messages-item-thread-full", если открыт тред', function() {
                var threadView = ns.View.create('messages-item-wrap', { ids: 't1' });
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    thread_id: 't1',
                    threaded: 'yes'
                });

                expect(threadView.patchLayout()).to.be.equal('layout-messages-item-thread-full');
            });

            it('должен вернуть "layout-messages-item-thread-inner", если открыт список треда', function() {
                var threadView = ns.View.create('messages-item-wrap', { ids: 't1' });
                var mMessagesItemState = threadView.getModel('messages-item-state');
                mMessagesItemState.setState(mMessagesItemState.STATE.THREAD_SHORT);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    threaded: 'yes'
                });

                expect(threadView.patchLayout()).to.be.equal('layout-messages-item-thread-inner');
            });

            it('должен вернуть "layout-messages-item-thread-full", если открыт и тред и список треда', function() {
                // ну мало ли :)

                var threadView = ns.View.create('messages-item-wrap', { ids: 't1' });
                var mMessagesItemState = threadView.getModel('messages-item-state');
                mMessagesItemState.setState(mMessagesItemState.STATE.THREAD_SHORT);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    thread_id: 't1',
                    threaded: 'yes'
                });

                expect(threadView.patchLayout()).to.be.equal('layout-messages-item-thread-full');
            });

            it('должен вернуть "layout-messages-item-thread-inner-attachments", если открыт список треда для виджета аттачей', function() {
                Daria.Widgets.shouldShowAttachments.returns(true);

                var threadView = ns.View.create('messages-item-wrap', { ids: 't1' });
                var mMessagesItemState = threadView.getModel('messages-item-state');
                mMessagesItemState.setState(mMessagesItemState.STATE.THREAD_SHORT);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    threaded: 'yes'
                });

                expect(threadView.patchLayout()).to.be.equal('layout-messages-item-thread-inner-attachments');
            });

            it('должен вернуть "layout-messages-item-attachments", если это виджет аттачей и нет других виджетов', function() {
                Daria.Widgets.getWidgetName.returns('');
                Daria.Widgets.shouldShowAttachments.returns(true);

                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '1',
                    threaded: 'yes'
                });

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item-attachments');
            });

            it('должен вернуть виджет, если он есть', function() {
                Daria.Widgets.getWidgetName.returns('widget');
                Daria.Widgets.shouldShowAttachments.returns(false);

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item-widget');
            });

            it('должен вернуть виджет, если он есть, даже если там может быть виджет аттачей (проверка приоритета)', function() {
                Daria.Widgets.getWidgetName.returns('widget');
                Daria.Widgets.shouldShowAttachments.returns(true);

                expect(this.view.patchLayout()).to.be.equal('layout-messages-item-widget');
            });
        });
    });

    describe('#onShow', function() {
        describe('2pane ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is2pane').returns(true);
                this.sinon.stub(Daria, 'is3pane').returns(false);

                this.mMessagesItemState = this.view.getModel('messages-item-state');

                this.sinon.stub(Daria.Widgets, 'getWidgetName').returns('');
                this.sinon.stub(Daria.Widgets, 'shouldShowAttachments').returns(false);
            });

            describe('Если должен быть виджет на письме, то логгируем в jsintegration.log', function() {
                beforeEach(function() {
                    Daria.Widgets.shouldShowAttachments.returns(false);
                    this.sinon.stub(this.view, 'writeLogForWidgets');
                    this.mMessage.setData({
                        widgets: [
                            {
                                info: {
                                    type: 'tickets',
                                    subtype: 'avia',
                                    showType: 'list'
                                },
                                controls: {}
                            }
                        ]
                    });
                });
                it('логгируем факт наличия виджета, operation=exists (виджет не покажется в интерфейсе)', function() {
                    Daria.Widgets.getWidgetName.returns('');
                    this.view.onShow();

                    expect(this.view.writeLogForWidgets)
                        .to.have.callCount(1)
                        .and.to.be.calledWith('exists', {
                            mid: '1',
                            widgetType: 'tickets',
                            widgetSubType: 'avia',
                            origin: 'list-widget'
                        });
                });
                it('логгируем факт показа виджета в интерфейсе, operation=exists, operation=view', function() {
                    Daria.Widgets.getWidgetName.returns('widget');

                    this.view.onShow();

                    expect(this.view.writeLogForWidgets).to.have.callCount(2);
                    expect(this.view.writeLogForWidgets.getCall(0).args[0]).to.be.eql('exists');
                    expect(this.view.writeLogForWidgets.getCall(1).args[0]).to.be.eql('view');
                    expect(this.view.writeLogForWidgets.getCall(0).args[1]).to.be.eql({
                        mid: '1',
                        widgetType: 'tickets',
                        widgetSubType: 'avia',
                        origin: 'list-widget'
                    });

                    expect(this.view.writeLogForWidgets.getCall(1).args[1]).to.be.eql({
                        mid: '1',
                        widgetType: 'tickets',
                        widgetSubType: 'avia',
                        origin: 'list-widget'
                    });
                });
            });
        });
    });

    describe('#onClickCloseMessage', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'uncheckMessagesInThread');

            this.sinon.stub(ns.router, 'generateUrl');
            this.sinon.stub(ns.page, 'go');

            this.sinon.stub(Jane, 'c');
        });

        it('При закрытии письма вызывается ns.page.go', function() {
            this.sinon.stub(this.view, 'isSubjectSelected').returns(false);
            var event = {
                target: $('<div class="js-message-close-button"><div class="krest">x</div></div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(ns.page.go).to.have.callCount(1);
        });

        it('При закрытии письма вызывается метод снятия с невидимых писем выделения', function() {
            this.sinon.stub(this.view, 'isSubjectSelected').returns(false);
            var event = {
                target: $('<div class="js-message-close-button"><div class="krest">x</div></div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(this.view.uncheckMessagesInThread).to.have.callCount(1);
        });

        it('Если выделили тему, то письмо не закрывается', function() {
            this.sinon.stub(this.view, 'isSubjectSelected').returns(true);
            var event = {
                target: $('<div class="js-message-close-button"><div class="krest">x</div></div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(ns.page.go).to.have.callCount(0);
        });

        it('Если мы закрываем по крестику, должен записать нужную метрику', function() {
            this.sinon.stub(this.view, 'isSubjectSelected').returns(false);
            var event = {
                target: $('<div class="js-message-close-button"><div class="krest">x</div></div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(Jane.c).to.be.calledWith('Закрытие письма кликом в шапку в 2pane', 'Закрытие по крестику');
        });

        it('Если мы закрываем по шапке, должен записать нужную метрику', function() {
            this.sinon.stub(this.view, 'isSubjectSelected').returns(false);
            var event = {
                target: $('<div class="head">test</div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(Jane.c).to.be.calledWith('Закрытие письма кликом в шапку в 2pane', 'Закрытие по шапке');
        });

        it('При закрытии письма вызывается финиш сценария "Просмотр письма"', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);
            this.sinon.stub(this.view, 'isSubjectSelected').returns(false);
            var event = {
                target: $('<div class="js-message-close-button"><div class="krest">x</div></div>'),
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };

            this.view.onClickCloseMessage(event);

            expect(scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-expanded');
        });
    });

    describe('#onMessageCountChanged', function() {
        it('Должен схлопнуть открытый тред, если тред больше не тред', function() {
            const threadView = ns.View.create('messages-item-wrap', { ids: 't1' });
            const mMessagesItemState = threadView.getModel('messages-item-state');
            mMessagesItemState.setState(mMessagesItemState.STATE.THREAD_SHORT);
            const mMessage = threadView.getModel('message');
            mMessage.setData({ count: 1 });

            threadView.onMessageCountChanged();

            expect(mMessagesItemState.getState()).to.be.equal(mMessagesItemState.STATE.CLOSE);
        });
    });
});
