describe('Daria.vMessageThreadPager', function() {
    beforeEach(function() {
        this.view = ns.View.create('message-thread-pager', { ids: '1233456' });
        this.mMessage = ns.Model.get('message', { ids: '123456' });
        this.sinon.stub(this.mMessage, 'getThreadCount');
        var params = {};
        this.sinon.stub(ns.page.current, 'params').value(params);

        this.mMessages = ns.Model.get('messages', params);
        this.sinon.stub(this.mMessages, 'canLoadMore');
        this.sinon.stub(this.mMessages, 'getUnreads');

        this.mStateMessageThreadList = ns.Model.get('state-message-thread-list');
        this.sinon.stub(this.mStateMessageThreadList, 'getCountHiddenMessages');

        this.sinon.stubGetModel(this.view, [ this.mStateMessageThreadList, this.mMessage, this.mMessages ]);

        this.view.$node = $('<div class="node"></div>');
        this.sinon.stub(this.view.$node, 'toggleClass');

        this.sinon.stub(Daria, 'is2pane');
    });

    describe('#updateCounters', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.view, [ 'setTextInCounters', 'toggleVisible' ]);
        });

        it('Должен вызвать проверку видимости', function() {
            this.mMessages.getUnreads.returns([]);
            this.view.updateCounters();

            expect(this.view.toggleVisible).to.have.callCount(1);
        });

        it('Если есть непрочитанные сообщения в 2pane, добавляем класс-модификатор _withUnread на ноду', function() {
            Daria.is2pane.returns(true);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(42);
            this.mMessages.getUnreads.returns([ 1, 2, 3 ]);

            this.view.updateCounters();

            expect(this.view.$node.toggleClass).to.be.calledWith(this.view.NODE_CLASS + '_withUnread', true);
        });

        it('Если есть непрочитанные сообщения в 3pane, ничего не добавляем', function() {
            Daria.is2pane.returns(false);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(42);
            this.mMessages.getUnreads.returns([ 1, 2, 3 ]);

            this.view.updateCounters();

            expect(this.view.$node.toggleClass).to.have.callCount(0);
        });

        it('Должен обновить текст в счётчиках', function() {
            Daria.is2pane.returns(true);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(42);
            this.mMessages.getUnreads.returns([ 1, 2, 3 ]);
            this.mMessage.getThreadCount.returns(10);

            this.view.updateCounters();

            expect(this.view.setTextInCounters).to.be.calledWithExactly(3, 10);
        });

        it('В 3pane не должен обновлять текст в счётчиках', function() {
            Daria.is2pane.returns(false);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(42);
            this.mMessages.getUnreads.returns([ 1, 2, 3 ]);

            this.view.updateCounters();

            expect(this.view.setTextInCounters).to.have.callCount(0);
        });

        describe('Если нет непрочитанных сообщений:', function() {
            beforeEach(function() {
                this.sinon.stub(this.mMessages, 'getCount');
                this.mMessages.getUnreads.returns([]);
            });

            it('Если нет непрочитанных сообщений, убираем класс-модификатор _withUnread с ноды', function() {
                Daria.is2pane.returns(true);
                this.mStateMessageThreadList.getCountHiddenMessages.returns(42);
                this.view.updateCounters();

                expect(this.view.$node.toggleClass).to.be.calledWith(this.view.NODE_CLASS + '_withUnread', false);
            });
        });
    });

    describe('#onLoadMoreClick', function() {
        it('Должен триггерить событие daria:vMessageThreadList:showMore', function() {
            this.sinon.stub(ns.events, 'trigger');

            this.view.onLoadMoreClick();

            expect(ns.events.trigger).to.be.calledWith('daria:vMessageThreadList:showMore');
        });
    });

    describe('#shouldBeVisible', function() {
        it('Если можем загрузить еще и есть скрытые сообщения то показываем плашки Еще письма', function() {
            this.mMessages.canLoadMore.returns(true);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(1);

            expect(this.view.shouldBeVisible()).to.be.equal(true);
        });

        it('Если можем загрузить еще и нет скрытых сообщений, то показываем плашку', function() {
            this.mMessages.canLoadMore.returns(true);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(0);

            expect(this.view.shouldBeVisible()).to.be.equal(true);
        });

        it('Если не можем загрузить еще письма и нет скрытых сообщений, то не показываем плашку', function() {
            this.mMessages.canLoadMore.returns(false);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(0);

            expect(this.view.shouldBeVisible()).to.be.equal(false);
        });

        it('Если не можем загрузить еще письма, но есть скрытые сообщения, то показываем плашку', function() {
            this.mMessages.canLoadMore.returns(false);
            this.mStateMessageThreadList.getCountHiddenMessages.returns(3);

            expect(this.view.shouldBeVisible()).to.be.equal(true);
        });
    });
});
