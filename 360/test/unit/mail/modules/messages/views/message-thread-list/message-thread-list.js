describe('Daria.vMessageThreadList', function() {
    beforeEach(function() {
        this.mDimensions = ns.Model.get('dimensions');
        this.mMessage = ns.Model.get('message', { ids: '1' }).setData({});
        this.mMessages = ns.Model.get('messages', { thread_id: '111' });
        this.mMessages.clear();
        this.mMessages.insert(this.mMessage);

        this.view = ns.ViewCollection.create('message-thread-list', { thread_id: '111' });
        this.view.mStateMessageThreadList = ns.Model.get('state-message-thread-list', { thread_id: '111' });
        this.view.mScrollerMessage = ns.Model.get('scroller-message');
    });

    afterEach(function() {
        // Очень долго ловил странные плавающие баги после вызова _show() в тестах для _onMessagesNsModelInsert.
        // Вроде бы после такой "чистки" их не стало.
        this.view.destroy();
    });

    describe('#_onNsViewShow', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane');
            this.sinon.stub(this.view, '_hideFirstPartMessages');
            this.sinon.stub(this.view, '_checkVisibleMessages');
            this.triggerBuildTree = this.sinon.stub(ns.events, 'trigger').withArgs('daria:mFocus:buildTree');
        });

        it('должен скрывать часть сообщений в 3pane', function() {
            Daria.is3pane.returns(true);
            this.view._onNsViewShow();
            expect(this.view._hideFirstPartMessages).to.have.callCount(1);
        });

        it('должен скрыть первую часть сообщений, если не 3pane', function() {
            Daria.is3pane.returns(false);
            this.view._onNsViewShow();
            expect(this.view._hideFirstPartMessages).to.have.callCount(1);
        });

        it('должен вызвать событие daria:mFocus:buildTree', function() {
            this.view._onNsViewShow();

            expect(this.triggerBuildTree).to.have.callCount(1);
        });
    });

    describe('#_openMessages', function() {
        beforeEach(function() {
            this.view._blockMessageAutoOpen = false;
            this.sinon.stub(this.view, '_getOpenedMessages').returns([]);
            this.sinon.stub(this.view, '_setMessageOpenState');
        });

        it('Должен получить список сообщений, которые необходимо раскрыть', function() {
            this.view._openMessages();
            expect(this.view._getOpenedMessages).to.have.callCount(1);
        });

        it('Должен сохранить признак открытого письма в стейте писем, которые нужно открыть', function() {
            this.view._getOpenedMessages.returns(this.mMessages.models);
            this.view._openMessages();
            expect(this.view._setMessageOpenState).to.be.calledWithExactly(this.mMessages.models[0], true);
        });
    });

    describe('#_getOpenedMessages', function() {
        function msg(ids, isNew, data) {
            data = data || {};
            data.new = !!isNew;
            var message = ns.Model.get('message', { ids: ids });
            message.setData(data);
            return message;
        }

        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane');
        });

        it('В 3 pane должен вернуть список непрочитанных писем', function() {
            Daria.is3pane.returns(true);
            var message1 = msg('1', true);
            var message2 = msg('2', false);
            var message3 = msg('3', true);

            this.mMessages.insert([ message1, message2, message3 ]);
            var messages = this.view._getOpenedMessages();

            expect(messages).to.be.eql([ message1, message3 ]);
        });

        it('В 3 pane должен вернуть первое письмо, если нет непрочитанных', function() {
            Daria.is3pane.returns(true);
            var message1 = msg('1', false);
            var message2 = msg('2', false);
            var message3 = msg('3', false);

            this.mMessages.insert([ message1, message2, message3 ]);
            var messages = this.view._getOpenedMessages();

            expect(messages).to.be.eql([ message1 ]);
        });

        it('В 2 pane должно вернуться первое письмо', function() {
            Daria.is3pane.returns(false);
            var message1 = msg('1', false);
            var message2 = msg('2', true);
            var message3 = msg('3', true);

            this.mMessages.insert([ message1, message2, message3 ]);
            var messages = this.view._getOpenedMessages();

            expect(messages).to.be.eql([ message1 ]);
        });

        it('В 2 pane должно вернуться первое письмо не черновик', function() {
            Daria.is3pane.returns(false);
            var message1 = msg('1', false);
            var message2 = msg('2', false);
            var message3 = msg('3', false);
            this.sinon.stub(message1, 'isDraftLike').returns(true);

            this.mMessages.insert([ message1, message2, message3 ]);
            var messages = this.view._getOpenedMessages();

            expect(messages).to.be.eql([ message2 ]);
        });

        it('Должен всегда возвращать новый массив', function() {
            Daria.is3pane.returns(true);
            var message1 = msg('1', true);
            var message2 = msg('2', false);
            var message3 = msg('3', true);

            this.mMessages.insert([ message1, message2, message3 ]);
            var res1 = this.view._getOpenedMessages();
            var res2 = this.view._getOpenedMessages();

            expect(res1).not.to.be.equal(res2);
            expect(res1).to.be.eql(res2);
        });

    });

    describe('#_hideFirstPartMessages', function() {
        beforeEach(function() {
            this.$node = $('<div />');

            this.addClassIsHiddenStub = this.sinon.stub(this.$node, 'addClass').withArgs('is-hidden');
            this.sinon.stub(this.view, 'getItemByModel').returns({
                $node: this.$node
            });
            this.sinon.stub(this.view.mStateMessageThreadList, 'setHiddenMessages');
        });

        it('Должен оставить одно письмо до первого открытого', function() {
            var message1 = ns.Model.get('message', { ids: '2' }).setData({ fid: '123' });
            var message2 = ns.Model.get('message', { ids: '3' }).setData({ fid: '123' });
            var message3 = ns.Model.get('message', { ids: '4' }).setData({ fid: '123' });
            this.mMessages.insert(message1);
            this.mMessages.insert(message2);
            this.mMessages.insert(message3);

            this.view._hideFirstPartMessages();

            expect(this.addClassIsHiddenStub).to.have.callCount(2);
            expect(this.view.getItemByModel.getCall(0).args[0]).to.be.equal(message2);
            expect(this.view.getItemByModel.getCall(1).args[0]).to.be.equal(message3);
        });

        it('Не сворачивать письма треда, если над открытым их не больше 2', function() {
            var message1 = ns.Model.get('message', { ids: '2' }).setData({ fid: '123' });
            var message2 = ns.Model.get('message', { ids: '3' }).setData({ fid: '123' });
            this.mMessages.insert(message1);
            this.mMessages.insert(message2);

            this.view._hideFirstPartMessages();

            expect(this.$node.addClass).to.have.callCount(0);
        });

        it('Должен записывать массив скрытых писем в модель состояния', function() {
            var message1 = ns.Model.get('message', { ids: '2' }).setData({ fid: '123' });
            this.mMessages.insert(message1);

            this.view._hideFirstPartMessages();

            expect(this.view.mStateMessageThreadList.setHiddenMessages).to.be.calledWithExactly([]);
        });
    });

    describe('#_onMessagesNsModelInsert', function() {
        describe('пришло новое непрочитанное письмо', function() {
            describe('3pane', function() {
                beforeEach(function() {
                    this.mQuickReplyState = ns.Model.get('quick-reply-state', { qrIds: this.view.params.ids });

                    this.message1 = ns.Model.get('message', { ids: '2' }).setData({ fid: '123' });
                    this.message2 = ns.Model.get('message', { ids: '3' }).setData({ fid: '123', new: true });
                    this.message3 = ns.Model.get('message', { ids: '4' }).setData({ fid: '123', new: true });

                    this.sinon.stub(Daria, 'is3pane').returns(true);

                    this.sinon.stub(this.view, 'trigger');
                    this.sinon.stub(this.view, '_setMessageOpenState');
                    this.sinon.stub(this.view, 'forceUpdate');
                    this.sinon.stub(this.view, '_subscribeFirstUserScroll');
                    this.sinon.stub(this.view, '_unsubscribeFirstUserScroll');

                    _.defer.restore();
                    this.sinon.stub(_, 'defer');

                    var $node = $('<div/>');
                    this.view.node = $node[0];
                    this.view.$node = $node;

                    this.view._show();
                });

                it('ничего не делаем, если открыт QR', function() {
                    this.sinon.stub(this.mQuickReplyState, 'isVisible').returns(true);
                    this.mMessages.insert(this.message2);
                    expect(this.view._setMessageOpenState).to.have.callCount(0);
                });

                describe('QR закрыт ->', function() {
                    beforeEach(function() {
                        this.sinon.stub(this.mQuickReplyState, 'isVisible').returns(false);
                    });

                    it('ничего не делаем с прочитанным письмом', function() {
                        this.mMessages.insert(this.message1);
                        expect(this.view._setMessageOpenState).to.have.callCount(0);
                    });

                    it('разворачиваем пришедшее непрочитанное письмо', function() {
                        this.mMessages.insert(this.message2);
                        expect(this.view._setMessageOpenState).to.have.callCount(1);
                    });

                    it('разворачиваем все пришедшие непрочитанные письма', function() {
                        this.mMessages.insert([ this.message1, this.message2, this.message3 ]);
                        expect(this.view._setMessageOpenState).to.have.callCount(2);
                    });

                    it('остальные непрочитанные в треде не разворачиваются', function() {
                        this.mMessages.insert(this.message2);
                        this.view._setMessageOpenState(this.message2, false, false);

                        expect(this.message2.isNew()).to.be.equal(true);

                        var prevCallCount = this.view._setMessageOpenState.callCount;
                        this.mMessages.insert(this.message3);
                        expect(this.view._setMessageOpenState).to.have.callCount(prevCallCount + 1);
                    });
                });
            });
        });
    });
});
