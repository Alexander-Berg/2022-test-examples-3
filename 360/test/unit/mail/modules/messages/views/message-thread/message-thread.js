describe('Daria.vMessageThread', function() {
    beforeEach(function() {
        this.params = {
            'thread_id': '111',
            'ids': '1'
        };

        this.mMessage = ns.Model.get('message', { 'ids': '1' }).setData({});
        this.mMessages = ns.Model.get('messages', this.params);
        this.mMessages.clear();
        this.mMessages.insert(this.mMessage);
        this.mScrollerMessage = ns.Model.get('scroller-message');
        this.mStateMessageThreadList = ns.Model.get('state-message-thread-list', this.params);

        this.view = ns.View.create('message-thread', this.params);
        this.sinon.stub(this.view, 'getModel');
        this.view.getModel.withArgs('messages').returns(this.mMessages);
        this.view.getModel.withArgs('scroller-message').returns(this.mScrollerMessage);
        this.view.getModel.withArgs('state-message-thread-list').returns(this.mStateMessageThreadList);
    });

    describe('._shouldPreserveScrollPosition - сохранение скролла при перерисовках списка писем', function() {
        it('в 2pane скролл должен сохраняться', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            var view = ns.View.create('message-thread', { thread_id: '111', ids: '1' });

            expect(view._shouldPreserveScrollPosition).to.be.equal(true);
        });

        it('не в 2pane скролл не должен сохраняться', function() {
            this.sinon.stub(Daria, 'is2pane').returns(false);
            var view = ns.View.create('message-thread', { thread_id: '111', ids: '1' });

            expect(view._shouldPreserveScrollPosition).to.be.equal(false);
        });
    });

    describe('#scrollAfterExpand', function() {
        beforeEach(function() {
            this.sinon.stub(this.mScrollerMessage, 'setScrollTop');
            this.sinon.stub(this.mScrollerMessage, 'getScrollTop');
            this.sinon.stub(this.mScrollerMessage, 'getNodeTop');
        });

        it('Должен подскроллить на messageIndex * HEIGHT_COMPACT_MESSAGE', function() {
            this.mScrollerMessage.getNodeTop.returns(0);
            this.mScrollerMessage.getScrollTop.returns(0);
            var newScrollTop = 5 * this.mStateMessageThreadList.get('.HEIGHT_COMPACT_MESSAGE');

            this.view.scrollAfterExpand(5);

            expect(this.mScrollerMessage.setScrollTop).to.be.calledWith(newScrollTop);
        });

        it('Должен подскроллить на messageIndex * HEIGHT_COMPACT_MESSAGE - offset', function() {
            this.mScrollerMessage.getNodeTop.returns(0);
            this.mScrollerMessage.getScrollTop.returns(0);
            var offset = 30;
            var newScrollTop = 5 * this.mStateMessageThreadList.get('.HEIGHT_COMPACT_MESSAGE') - offset;
            this.view.scrollAfterExpand(5, offset);

            expect(this.mScrollerMessage.setScrollTop).to.be.calledWith(newScrollTop);
        });

        it('Должен добавлять scrollTop к высоте скролла', function() {
            this.mScrollerMessage.getNodeTop.returns(0);
            this.mScrollerMessage.getScrollTop.returns(20);
            var offset = 20 + 5 * this.mStateMessageThreadList.get('.HEIGHT_COMPACT_MESSAGE');

            this.view.scrollAfterExpand(5);

            expect(this.mScrollerMessage.setScrollTop).to.be.calledWith(offset);
        });

        it('Должен подскроллить тред к верху страницы, если после разворачивания позиция начала треда будет меньше высоты списка (до искомого письма)', function() {
            this.mScrollerMessage.getScrollTop.returns(20);
            var offset = 20 + 5 * this.mStateMessageThreadList.get('.HEIGHT_COMPACT_MESSAGE');
            // Начало треда на 20 пикселей ниже высоты списка
            this.mScrollerMessage.getNodeTop.returns(offset + 20);

            this.view.scrollAfterExpand(5);

            expect(this.mScrollerMessage.setScrollTop).to.be.calledWith(offset + 20);
        });
    });
});
