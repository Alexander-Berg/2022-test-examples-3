describe('Daria.vMessageThreadReplyAll', function() {
    beforeEach(function() {
        this.view = ns.View.create('message-thread-reply-all', {thread_id: 'nonvalidIncomeMessageThread1'});
        this.getModelMessagesMock = this.sinon.stub(this.view, 'getModel').withArgs('messages');

        this.mQuickReplyState = ns.Model.get('quick-reply-state');
        this.isVisibleStub = this.sinon.stub(this.mQuickReplyState, 'isVisible');
        this.sinon.stub(this.view, 'getQuickReplyState').returns(this.mQuickReplyState);
    });

    describe('#onQuickReplyFormVisibilityChanged', function() {
        beforeEach(function() {
            this.view.$node = $('<div>');
        });

        it('Должен спрятать плашку, если QR стал видимым', function() {
            this.view.lastMid = '10';
            this.isVisibleStub.returns(true);
            this.view.onQuickReplyFormVisibilityChanged();
            expect(this.view.$node.hasClass('is-hidden')).to.be.ok;
        });

        it('Должен показать плашку, если QR закрылся', function() {
            this.view.lastMid = '10';
            this.isVisibleStub.returns(false);
            this.view.onQuickReplyFormVisibilityChanged();
            expect(this.view.$node.hasClass('is-hidden')).to.not.be.ok;
        });

        it('Должен спрятать плашку, если пустой mid последнего письма', function() {
            this.view.lastMid = null;
            this.isVisibleStub.returns(true);
            this.view.onQuickReplyFormVisibilityChanged();
            expect(this.view.$node.hasClass('is-hidden')).to.be.ok;
        });
    });

    describe('#reinitLastMessage', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'onQuickReplyFormVisibilityChanged');
            this.sinon.stub(this.mQuickReplyState, 'on').withArgs('ns-model-changed.formIsShown');
            this.sinon.stub(this.mQuickReplyState, 'off').withArgs('ns-model-changed.formIsShown');

            this.lastMessage = ns.Model.get('message', {ids: '5'});
            setModelByMock(this.lastMessage);

            this.mMessages = ns.Model.get('messages');
            this.getModelMessagesMock.returns(this.mMessages);

            this.sinon.stub(this.mMessages, 'getLastReplyMessage');
        });

        it('Если нет и не было входящих, то скрыть блок', function() {
            this.mMessages.getLastReplyMessage.returns(undefined);
            this.sinon.stub(this.view, 'hideView');
            this.view.reinitLastMessage();
            expect(this.view.hideView).to.have.callCount(1);
        });

        it('Если mid последнего сообщения не изменился, то не обновлять подписок и не вызывать onQuickReplyFormVisibilityChanged', function() {
            this.mMessages.getLastReplyMessage.returns(this.lastMessage);
            this.view.lastMid = '5';
            this.view.reinitLastMessage();
            expect(this.mQuickReplyState.off).to.have.callCount(0);
            expect(this.mQuickReplyState.on).to.have.callCount(0);
            expect(this.view.onQuickReplyFormVisibilityChanged).to.have.callCount(0);
        });

        it('Если mid последнего сообщения изменился, вызвать onQuickReplyFormVisibilityChanged', function() {
            this.mMessages.getLastReplyMessage.returns(this.lastMessage);
            this.view.reinitLastMessage();
            expect(this.view.onQuickReplyFormVisibilityChanged).to.have.callCount(1);
        });

        it('Должен отписаться от событий старого quick-reply-state, если было старое сообщение и пришло новое входящее сообщение', function() {
            this.mMessages.getLastReplyMessage.returns(this.lastMessage);
            this.view.lastMid = 'mid09';
            this.view.reinitLastMessage();
            expect(this.mQuickReplyState.off).to.have.callCount(1);
        });

        it('Должен подписаться на события нового quick-reply-state, если есть новое входящее сообщение', function() {
            this.mMessages.getLastReplyMessage.returns(this.lastMessage);
            this.view.reinitLastMessage();
            expect(this.mQuickReplyState.on).to.have.callCount(1);
        });
    });

    describe('#openQuickReply', function() {
        beforeEach(function() {
            this.view.lastMid = 'mid10';
            this.mStateMessageThreadItem = ns.Model.get('state-message-thread-item');
            this.messageIsOpenStub = this.sinon.stub(this.mStateMessageThreadItem, 'isOpen');
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.sinon.stub(this.view, 'getStateMessageThreadItem').returns(this.mStateMessageThreadItem);

            this.sinon.stub(this.mQuickReplyState, 'showForm');
            this.sinon.stub(this.mQuickReplyState, 'set');
        });

        it('Если сообщение открыто, должен показать форму QR', function() {
            this.messageIsOpenStub.returns(true);
            this.view.openQuickReply();
            expect(this.mQuickReplyState.showForm).to.have.callCount(1);
        });

        it('Если сообщение свёрнуто, то должен выставить флаг QR.forceShow=true и раскрыть сообщение', function() {
            this.messageIsOpenStub.returns(false);
            this.view.openQuickReply();
            expect(this.mQuickReplyState.set).to.be.calledWith('.forceShow', true);
            expect(this.mStateMessageThreadItem.setOpen).to.be.calledWith(true);
        });
    });

    describe('#onReplyAllThreadButton', function() {
        it('Метрика клика', function() {
            this.sinon.stub(Jane, 'c');
            this.view.onReplyAllThreadButton();
            expect(Jane.c).to.be.calledWith('Message view', 'Reply all bottom', 'click');
        });
    });

    describe('#showView', function() {
        beforeEach(function() {
            this.view.$node = $('<div>');
        });

        it('Метрика показа', function() {
            this.sinon.stub(Jane, 'c');
            this.view.showView();
            expect(Jane.c).to.be.calledWith('Message view', 'Reply all bottom', 'show');
        });
    });
});
