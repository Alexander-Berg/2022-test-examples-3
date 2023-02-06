describe('Daria.vMessageThreadItemWrap', function() {
    beforeEach(function() {
        this.view = ns.View.create('message-thread-item-wrap', { ids: '1234567' });
        this.$node = $('<div class="node">').appendTo('body');
        this.view.$node = this.$node;
        this.mMessage = ns.Model.get('message', { ids: '123456' });
        this.mMessagesChecked = ns.Model.get('messages-checked');
        this.sinon.stubGetModel(this.view, [ 'state-message-thread-item', this.mMessage, this.mMessagesChecked ]);
        this.mStateMessageThreadItem = this.view.getModel('state-message-thread-item');
    });

    describe('#removeStateLoading ->', function() {
        it('Должен удалять класс is-loading', function() {
            this.view.removeStateLoading();

            expect(this.view.$node.attr('class')).to.be.equal('node');
        });
    });

    describe('#closeSilently ->', function() {
        it('Модель должна уничтожаться', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'destroy');
            this.view.closeSilently();

            expect(this.mStateMessageThreadItem.destroy).to.have.callCount(1);
        });
    });

    describe('#isOpen ->', function() {
        it('Должен вызываться isOpen у модели', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'isOpen');
            this.view.isOpen();

            expect(this.mStateMessageThreadItem.isOpen).to.have.callCount(1);
        });
    });

    describe('#open ->', function() {
        it('У модели вызывается метод setOpen', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.view.open();

            expect(this.mStateMessageThreadItem.setOpen).to.have.callCount(1);
        });

        it('Убеждаемся, что в setOpen передается true', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.view.open();

            expect(this.mStateMessageThreadItem.setOpen).to.be.calledWith(true);
        });

        it('закрывает текущий сценарий чтения письма', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);

            this.view.open();

            expect(scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');
        });

        it('запускает новый сценарий чтения письма', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);

            this.view.open();

            expect(scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'thread-message-hotkey');
        });
    });

    describe('#close ->', function() {
        it('У модели вызывается setOpen', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.view.close();

            expect(this.mStateMessageThreadItem.setOpen).to.have.callCount(1);
        });

        it('Убеждаемся, что в setOpen передается false', function() {
            this.sinon.stub(this.mStateMessageThreadItem, 'setOpen');
            this.view.close();

            expect(this.mStateMessageThreadItem.setOpen).to.be.calledWith(false);
        });
    });

    describe('#toggleCheck ->', function() {
        it('Должен вызываться toggleCheck у mMessage', function() {
            this.sinon.stub(this.mMessagesChecked, 'toggleCheck');

            this.view.toggleCheck();
            expect(this.mMessagesChecked.toggleCheck).to.have.callCount(1);
        });
    });
});
