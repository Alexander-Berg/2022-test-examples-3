describe('Daria.vMessagesItemThreadExpand', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-thread-expand', {ids: '1111'});
        this.sinon.stubGetModel(this.view, ['messages-item-state']);
        this.mMessagesItemState = this.getModel('messages-item-state');
        this.view.$node = $('<div></div>');

        this.event = {
            'preventDefault': this.sinon.stub(),
            'stopPropagation': this.sinon.stub()
        };
    });

    afterEach(function() {
        delete this.event;
    });

    describe('#onClickThreadToggle', function() {

        it('-> Если мы открыли раскрывашку треда, то состояние треда переключается с close на thread_short', function() {
            this.sinon.stub(this.mMessagesItemState, 'getState').returns(this.mMessagesItemState.STATE.CLOSE);

            this.view.onClickThreadToggle(this.event);
            this.sinon.stub(this.mMessagesItemState, 'setState');

            expect(this.mMessagesItemState.getData()).to.be.eql({state: 'thread_short'});
        });

        it('-> Если мы закрыли раскрывашку треда, то состояние треда переключается с thread_short на close', function() {
            this.sinon.stub(this.mMessagesItemState, 'getState').returns(this.mMessagesItemState.STATE.THREAD_SHORT);

            this.view.onClickThreadToggle(this.event);
            this.sinon.stub(this.mMessagesItemState, 'setState');

            expect(this.mMessagesItemState.getData()).to.be.eql({state: 'close'});
        });
    });
    describe('#onChangeState', function() {
        it('-> поменялось состояние на THREAD_SHORT - стрелка раскрытая без класса is-folded', function() {
            this.sinon.stub(this.mMessagesItemState, 'getState').returns(this.mMessagesItemState.STATE.THREAD_SHORT);

            this.view.onChangeState();

            expect(this.view.$node.hasClass('is-folded')).to.be.equal(false);
        });
        it('-> поменялось состояние на CLOSE - стрелка закрытая с классом is-folded', function() {
            this.sinon.stub(this.mMessagesItemState, 'getState').returns(this.mMessagesItemState.STATE.CLOSE);

            this.view.onChangeState();

            expect(this.view.$node.hasClass('is-folded')).to.be.equal(true);
        });
    });
});
